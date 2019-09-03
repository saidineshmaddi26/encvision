package com.example.encvision;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.SurfaceTexture;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.VideoView;


import com.longdo.mjpegviewer.MjpegView;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;

public class enc_client extends Activity {

    MjpegView viewer;
    String url="http://10.10.20.48:8080";
    Button start,stop;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.enc_client);
        start=(Button)findViewById(R.id.start);
        stop=(Button)findViewById(R.id.stop);
        start.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                thread.start();
            }
        });


    }



    Thread thread=new Thread(new Runnable() {
        @Override
        public void run() {
            viewer = (MjpegView) findViewById(R.id.mjpegview);

            viewer.setMode(MjpegView.MODE_FIT_WIDTH);
            viewer.setAdjustHeight(true);
            viewer.setUrl(url);
            viewer.startStream();

        }
    });



}
