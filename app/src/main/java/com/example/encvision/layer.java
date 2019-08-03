package com.example.encvision;

import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;

public class layer extends Service {

    int state=0;
    // 0 for inactive and 1 for active

    LinearLayout mView;
    ImageView qr;
    WindowManager wm;
    WindowManager.LayoutParams params;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        if(state==1)
        {

            wm.removeView(mView);
            state=0;
        }

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY ,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE  ,
                PixelFormat.TRANSLUCENT);
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);

        LinearLayout.LayoutParams params1=new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        mView = new LinearLayout(this);
        mView.setBackgroundColor(Color.argb(170,0,0,0));
        mView.setLayoutParams(params1);

        if(state==0)
        {

            wm.addView(mView,params);
            state=1;
        }

        return super.onStartCommand(intent, flags, startId);

    }
}
