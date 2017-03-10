package com.tricktekno.webview;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class ScaleImageViewActivity extends Activity implements OnClickListener {

    Button btndraw, btnzoom, btnsave;

    public static ScaleImageView imageview;
    public static boolean flag = true;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initwidget();
    }

    private void initwidget() {

        imageview = (ScaleImageView) findViewById(R.id.image);

        btnsave = (Button) findViewById(R.id.activity_main_save);
        btndraw = (Button) findViewById(R.id.activity_main_zoom_draw);
        btnzoom = (Button) findViewById(R.id.activity_main_zoom_zoom);

        btndraw.setOnClickListener(this);
        btnzoom.setOnClickListener(this);
        btnsave.setOnClickListener(this);
    }

    @Override
    public void onClick(View arg0) {
        // TODO Auto-generated method stub
        if (btndraw.equals(arg0)) {
            flag = true;
        } else if (btnzoom.equals(arg0)) {
            flag = false;
        } else if (btnsave.equals(arg0)) {
            ScaleImageView.save();
        }
    }
}