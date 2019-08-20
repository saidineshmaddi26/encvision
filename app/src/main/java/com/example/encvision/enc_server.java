package com.example.encvision;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.icu.text.SimpleDateFormat;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

import net.majorkernelpanic.streaming.SessionBuilder;
import net.majorkernelpanic.streaming.gl.SurfaceView;
import net.majorkernelpanic.streaming.rtp.H264Packetizer;
import net.majorkernelpanic.streaming.rtp.MediaCodecInputStream;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Date;

public class enc_server extends Activity {

    private static final String TAG = "ScreenCaptureFragment";

    private static final int REQUEST_MEDIA_PROJECTION = 1;

    private int mResultCode;
    private Intent mResultData;
    private final int PORT_OUT = 9999;
    private Surface mSurface;
    private Surface previewSurface;
    private MediaProjection mMediaProjection;
    private VirtualDisplay mVirtualDisplay;
    private MediaProjectionManager mMediaProjectionManager;

    Button start, stop;


    private int streamWidth = 360;
    private int streamHeight = 640;
    private int mScreenDensity;


    private DatagramSocket sock;
    private InetAddress group;
    private DatagramPacket currentPacket;
    private boolean configSent = false;

    private MediaCodec encoder;

    SurfaceView mSurfaceview;

    H264Packetizer packetizer;
    MediaCodecInputStream mediaCodecInputStream;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.enc_server);
        mSurfaceview=(SurfaceView)findViewById(R.id.surfaceview);
        mMediaProjectionManager = (MediaProjectionManager)
                getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        start = (Button) findViewById(R.id.start);
        stop = (Button) findViewById(R.id.stop);

        packetizer=new H264Packetizer();






        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (mVirtualDisplay == null) {

                    startScreenCapture();
                    Log.e("this","startscreencapture");

                }
            }
        });
        stop.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                if (mVirtualDisplay != null)
                    stopScreenCapture();

            }
        });
        startBroadcast();



    }
    private void startBroadcast(){

        try {
            encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
            MediaFormat format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC,
                    streamWidth, streamHeight);
            format.setInteger(MediaFormat.KEY_BIT_RATE, 220000);
            format.setInteger(MediaFormat.KEY_FRAME_RATE, 20);
            format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                    MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
            format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 0);
            format.setInteger(MediaFormat.KEY_REPEAT_PREVIOUS_FRAME_AFTER, 1000000);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                format.setInteger(MediaFormat.KEY_LATENCY, 0);
            }
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 5);
            format.setInteger(MediaFormat.KEY_PRIORITY, 0x00);
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            mSurface = MediaCodec.createPersistentInputSurface();
            encoder.setInputSurface(mSurface);

        //    mediaCodecInputStream=new MediaCodecInputStream(encoder);


            encoder.setCallback(new MediaCodec.Callback() {
                @Override
                public void onInputBufferAvailable(@NonNull MediaCodec codec, int index) {

                }

                @Override
                public void onOutputBufferAvailable(@NonNull MediaCodec codec, int index, @NonNull MediaCodec.BufferInfo info) {
                    if(!configSent){
                        codec.releaseOutputBuffer(index, false);
                        return;
                    }

                    ByteBuffer outputBuffer = codec.getOutputBuffer(index);
                    ByteBuffer buf;

                    if (outputBuffer != null) {


                        buf = ByteBuffer.allocate(outputBuffer.limit());
                        buf.put(outputBuffer);
                        buf.flip();
                        Log.e(TAG, "Wrote " + outputBuffer.limit() + " bytes.");
                        
                    }
                    else{
                        return;
                    }

                    codec.releaseOutputBuffer(index, false);
                }

                @Override
                public void onError(@NonNull MediaCodec codec, @NonNull MediaCodec.CodecException e) {

                }

                @Override
                public void onOutputFormatChanged(@NonNull MediaCodec codec, @NonNull MediaFormat format) {
                    Log.d(TAG, "Updated output format! New height:"
                            + format.getInteger(MediaFormat.KEY_HEIGHT) + " new width: " +
                            format.getInteger(MediaFormat.KEY_WIDTH));

                    ByteBuffer sps = format.getByteBuffer("csd-0");
                    ByteBuffer pps = format.getByteBuffer("csd-1");


                    packetizer.setStreamParameters(pps.array(),sps.array());

//                    BroadcastTask spsTask = new BroadcastTask(new DatagramPacket(sps.array(), sps.limit(), group, PORT_OUT));
//                    BroadcastTask ppsTask = new BroadcastTask(new DatagramPacket(pps.array(), pps.limit(), group, PORT_OUT));
//                    spsTask.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
//                    ppsTask.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
                    configSent = true;
                }
            });
            encoder.start();}
        catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_MEDIA_PROJECTION) {
            if (resultCode != Activity.RESULT_OK) {
                Log.i(TAG, "User cancelled");
                return;
            }
            Log.i(TAG, "Starting screen capture");
            mResultCode = resultCode;
            Log.e(TAG,"Result Code"+resultCode);
            mResultData = data;
            setUpMediaProjection();
            setUpVirtualDisplay();
        }
    }

    private void startScreenCapture() {

        DisplayMetrics displayMetrics = new DisplayMetrics();
        WindowManager wm = (WindowManager) this.getSystemService(WINDOW_SERVICE);
        wm.getDefaultDisplay().getRealMetrics(displayMetrics);
        mScreenDensity = displayMetrics.densityDpi;

        Log.i(TAG, "Requesting confirmation");
        startActivityForResult(
                mMediaProjectionManager.createScreenCaptureIntent(),
                REQUEST_MEDIA_PROJECTION);
    }

    private void setUpMediaProjection() {
        mMediaProjection = mMediaProjectionManager.getMediaProjection(mResultCode, mResultData);
    }

    private void setUpVirtualDisplay() {

        mVirtualDisplay = mMediaProjection.createVirtualDisplay("ScreenCapture",
                streamWidth, streamHeight, mScreenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mSurface, null, null);

    }

    private void stopScreenCapture() {
        if (mVirtualDisplay == null) {
            return;
        }
        mVirtualDisplay.release();
        mVirtualDisplay = null;
    }

}
