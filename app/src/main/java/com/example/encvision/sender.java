package com.example.encvision;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class sender extends Service {



    ServerSocket serverSocket;
    Socket socket;
    static final int SEND_TO_CLIENT = 1;


    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case SEND_TO_CLIENT:
                    byte[] array= (byte[]) msg.obj;
                    broadcast cast=new broadcast(array);
                    cast.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                break;
                default:
                    super.handleMessage(msg);
            }
        }
    }
    final Messenger mMessenger = new Messenger(new IncomingHandler());
    Thread thread=new Thread(new Runnable() {
        @Override
        public void run() {

                try {
                    socket = serverSocket.accept();
                } catch (IOException e) {
                    e.printStackTrace();
                }


        }

    });



    @Override
    public IBinder onBind(Intent intent) {
        try {
            serverSocket = new ServerSocket(8080);
        } catch (IOException e) {
            e.printStackTrace();
        }
        thread.start();
        Toast.makeText(getApplicationContext(), "binding", Toast.LENGTH_SHORT).show();
        return mMessenger.getBinder();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }



    OutputStream output;
    int res=0;
    int id=0;
    class broadcast extends AsyncTask<String,Void,Void>
    {


        byte[] buffer;
        broadcast(byte[] bytes)
        {
            buffer=bytes;
        }
        @Override
        protected Void doInBackground(String... strings) {

            try{
            if(res==0 && socket!=null) {
                res = 1;

                output = socket.getOutputStream();
                // Initial header for M-JPEG.
                String header = "HTTP/1.0 200 OK\r\n" +
                        "Connection: close\r\n" +
                        "Max-Age: 0\r\n" +
                        "Expires: 0\r\n" +
                        "Cache-Control: no-store, no-cache, must-revalidate, pre-check=0, post-check=0, max-age=0\r\n" +
                        "Pragma: no-cache\r\n" +
                        "Content-Type: multipart/x-mixed-replace;boundary=--boundary\r\n\r\n";
                output.write(header.getBytes());
            }


            if (socket!=null && socket.isConnected())
                Log.e("server", "connected socket");


            if (socket!=null &&!socket.isClosed()) {

                    Log.i("Web Server", "Size of image: " + buffer.length + "bytes");

                    output.write(("--boundary\r\n" +
                            "Content-Type: image/jpeg\r\n" +
                            "Content-Length: " + buffer.length + "\r\n\r\n").getBytes());
                    output.write(buffer);
                    Log.e("Web Server", "Current id: " + id);
                    id++;

            }
            }
            catch(IOException e)
            {
                e.printStackTrace();

            }
            return null;
        }
    }

        }





