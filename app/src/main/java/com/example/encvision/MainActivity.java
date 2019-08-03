package com.example.encvision;


import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.OrientationEventListener;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;

public class MainActivity extends AppCompatActivity {




    private ServerSocket serverSocket;
    private Socket tempClientSocket;
    Thread serverThread = null;
    public static final int SERVER_PORT = 3003;
    Socket socket;
    Bitmap bMap;
   Bitmap bitmap = null;
   int[] bitmap_flag=new int[2];
   int turn=0;

   boolean stop=false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.qr_code);
        Button startButton = (Button) findViewById(R.id.start);
        startButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                stop=false;
                takescreenshot();
              //  startService(new Intent(MainActivity.this,layer.class));

                serverThread = new Thread(new ServerThread());
                serverThread.start();


            }
        });

        Button stopButton = (Button) findViewById(R.id.stop);
        stopButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                stop=true;
            }
        });


    }

    class ServerThread implements Runnable {

        public void run() {
            try {
                serverSocket = new ServerSocket(SERVER_PORT);
                Log.e("tag","started server");
            } catch (IOException e) {
                e.printStackTrace();
            }
                while (!stop) {
                    try {



                        Log.e("this","waiting for client");
                            socket = serverSocket.accept();
                            if(socket!=null)
                            {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        decreasebrightness();

                                    }
                                });
                                Log.e("this","client connected");
                                OutputStream out = socket.getOutputStream();
                                File root = Environment.getExternalStorageDirectory();

                           int flag=0;
                           if(flag==0 && IMAGES_PRODUCED>5)
                           {
                               bMap = BitmapFactory.decodeFile(root+"/screenshots/screencap"+(IMAGES_PRODUCED-4)+".png");
                               Log.e("sent",""+(IMAGES_PRODUCED-4));
                               flag=1;
                           }

//
//                                bitmap_flag[1]=0;
//                                turn=0;
//                                while(turn==0 && bitmap_flag[0]==0);
//
//                                    bitmap.compress(Bitmap.CompressFormat.JPEG,100,out);
//                                    bitmap_flag[1]=1;
//




                                if(bitmap!=null)
                                {


                                    if(flag==1)
                                    {
                                        bMap.compress(Bitmap.CompressFormat.JPEG,100,out);
                                        Log.e("tag","sent into stream");
                                        flag=0;
                                    }

                                }

//
//

                            }




                  } catch (IOException e) {
                       e.printStackTrace();
                    }
               }
           }
        }

    private void decreasebrightness() {

        Settings.System.putInt(this.getContentResolver(),
                Settings.System.SCREEN_BRIGHTNESS, 0);

//        WindowManager.LayoutParams lp = getWindow().getAttributes();
//        lp.screenBrightness =0.2f;// 100 / 100.0f;
//        getWindow().setAttributes(lp);
    }


    public static final String TAG = MainActivity.class.getName();
    public static final int REQUEST_CODE = 100;
    public static String STORE_DIRECTORY;
    public static int IMAGES_PRODUCED;
    public static final String SCREENCAP_NAME = "screencap";
    public static final int VIRTUAL_DISPLAY_FLAGS = DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY | DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC;
    public static MediaProjection sMediaProjection;

    public MediaProjectionManager mProjectionManager;
    public ImageReader mImageReader;
    public Handler mHandler;
    public Display mDisplay;
    public VirtualDisplay mVirtualDisplay;
    public int mDensity;
    public int mWidth;
    public int mHeight;
    public int mRotation;
    public MainActivity.OrientationChangeCallback mOrientationChangeCallback;



    public class ImageAvailableListener implements ImageReader.OnImageAvailableListener {
        @Override
        public void onImageAvailable(ImageReader reader) {
            Image image = null;
            FileOutputStream fos = null;

            try {
                image = reader.acquireLatestImage();
                if (image != null) {
                    Image.Plane[] planes = image.getPlanes();
                    ByteBuffer buffer = planes[0].getBuffer();
                    int pixelStride = planes[0].getPixelStride();
                    int rowStride = planes[0].getRowStride();
                    int rowPadding = rowStride - pixelStride * mWidth;




                        bitmap_flag[0]=0;
                        turn=1;
                     //   if(!(turn==1 && bitmap_flag[1]==0))
                      //  {
                            if (bitmap != null) {
                                bitmap.recycle();
                            }
                            bitmap = Bitmap.createBitmap(mWidth + rowPadding / pixelStride, mHeight, Bitmap.Config.ARGB_8888);
                            bitmap.copyPixelsFromBuffer(buffer);

                      //  }
                        bitmap_flag[0]=1;



                    // write bitmap to a file


                    fos = new FileOutputStream(STORE_DIRECTORY +"screencap"+IMAGES_PRODUCED +".png");

                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);


                    IMAGES_PRODUCED++;
              //      Log.e(TAG, "captured image: " + IMAGES_PRODUCED);
                }

            } catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG,"unable to write");
            } finally {
                if (fos != null) {
                    try {
                        fos.close();
                    } catch (IOException ioe) {
                        ioe.printStackTrace();
                    }
                }



                if (image != null) {
                    image.close();
                }
            }
        }
    }
    public void takescreenshot() {


        mProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);

        startActivityForResult(mProjectionManager.createScreenCaptureIntent(), REQUEST_CODE);

        new Thread() {
            @Override
            public void run() {
                Looper.prepare();
                mHandler = new Handler();
                Looper.loop();
            }
        }.start();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE) {
            sMediaProjection = mProjectionManager.getMediaProjection(resultCode, data);

            if (sMediaProjection != null) {
                File externalFilesDir = getExternalFilesDir(null);
                if (externalFilesDir != null) {
                    STORE_DIRECTORY = Environment.getExternalStorageDirectory() + "/screenshots/";
                    File storeDirectory = new File(STORE_DIRECTORY);
                    if (!storeDirectory.exists()) {
                        boolean success = storeDirectory.mkdirs();
                        if (!success) {
                            Log.e(TAG, "failed to create file storage directory.");
                            return;
                        }
                    }
                } else {
                    Log.e(TAG, "failed to create file storage directory, getExternalFilesDir is null.");
                    return;
                }

                // display metrics
                DisplayMetrics metrics = getResources().getDisplayMetrics();
                mDensity = metrics.densityDpi;
                mDisplay = getWindowManager().getDefaultDisplay();

                // create virtual display depending on device width / height
                createVirtualDisplay();

                // register orientation change callback
                mOrientationChangeCallback = new OrientationChangeCallback(this);
                if (mOrientationChangeCallback.canDetectOrientation()) {
                    mOrientationChangeCallback.enable();
                }

                // register media projection stop callback
                sMediaProjection.registerCallback(new MediaProjectionStopCallback(), mHandler);
            }
        }
    }
    public void stopProjection() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (sMediaProjection != null) {
                    sMediaProjection.stop();
                }
            }
        });
    }
    public class MediaProjectionStopCallback extends MediaProjection.Callback {
        @Override
        public void onStop() {
            Log.e("ScreenCapture", "stopping projection.");
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (mVirtualDisplay != null) mVirtualDisplay.release();
                    if (mImageReader != null) mImageReader.setOnImageAvailableListener(null, null);
                    if (mOrientationChangeCallback != null) mOrientationChangeCallback.disable();
                    sMediaProjection.unregisterCallback(MediaProjectionStopCallback.this);
                }
            });
        }
    }
    public void createVirtualDisplay() {
        // get width and height
        Point size = new Point();
        mDisplay.getSize(size);
        mWidth = size.x;
        mHeight = size.y;

        // start capture reader
        mImageReader = ImageReader.newInstance(mWidth, mHeight, PixelFormat.RGBA_8888, 2);
        mVirtualDisplay = sMediaProjection.createVirtualDisplay(SCREENCAP_NAME, mWidth, mHeight, mDensity, VIRTUAL_DISPLAY_FLAGS, mImageReader.getSurface(), null, mHandler);
        mImageReader.setOnImageAvailableListener(new ImageAvailableListener(), mHandler);
    }

    public class OrientationChangeCallback extends OrientationEventListener {

        OrientationChangeCallback(Context context) {
            super(context);
        }

        @Override
        public void onOrientationChanged(int orientation) {
            final int rotation = mDisplay.getRotation();
            if (rotation != mRotation) {
                mRotation = rotation;
                try {
                    // clean up
                    if (mVirtualDisplay != null) mVirtualDisplay.release();
                    if (mImageReader != null) mImageReader.setOnImageAvailableListener(null, null);

                    // re-create virtual display depending on device width / height
                    createVirtualDisplay();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }


}
