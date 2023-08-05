package com.example.splashscreen;

import static android.content.ContentValues.TAG;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.TextView;


import java.util.Locale;

public class MainActivity extends AppCompatActivity implements GestureDetector.OnGestureListener {

    private static int MIN_DISTANCE = 100;
    private float x1,x2,y1,y2;
    private GestureDetector gestureDetector;
    TextView OD, TD;
    TextToSpeech tts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        OD = findViewById(R.id.OD);
        TD = findViewById(R.id.TD);

        //create an object textToSpeech and adding features to it
        tts = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int i) {
                if (i != tts.ERROR) {
                    // ch0ose language
                    tts.setLanguage(Locale.ENGLISH);
                    tts.speak(OD.getText().toString(), tts.QUEUE_ADD,null);
                    tts.speak(TD.getText().toString(), tts.QUEUE_ADD,null);
                }
            }
        });

        //initialize gesturedetector
        this.gestureDetector = new GestureDetector(MainActivity.this, this);

    }

    //override on touch event


    @Override
    public boolean onTouchEvent(MotionEvent event) {

        gestureDetector.onTouchEvent(event);

        switch (event.getAction()) {
            //starting to swipe time gesture
            case MotionEvent.ACTION_DOWN:
                x1 = event.getX();
                y1 = event.getY();
                break;

                //ending time swipe gesture
            case MotionEvent.ACTION_UP:
                x2 = event.getX();
                y2 = event.getY();


                // value for vertical swipe
                float valueY = y2 - y1;

                if (Math.abs(valueY) > MIN_DISTANCE) {

                    //detect top to bottom swipe
                    if (y2 >y1)
                    {
                        Log.d(TAG, "Bottom Swipe");
                        openTextDect();
                    }
                    else
                    {
                        Log.d(TAG, "Top Swipe");
                        openObjectDect();
                    }
                }

        }

        return super.onTouchEvent(event);
    }

    @Override
    public boolean onDown(@NonNull MotionEvent motionEvent) {
        return false;
    }

    @Override
    public void onShowPress(@NonNull MotionEvent motionEvent) {

    }

    @Override
    public boolean onSingleTapUp(@NonNull MotionEvent motionEvent) {
        return false;
    }

    @Override
    public boolean onScroll(@NonNull MotionEvent motionEvent, @NonNull MotionEvent motionEvent1, float v, float v1) {
        return false;
    }

    @Override
    public void onLongPress(@NonNull MotionEvent motionEvent) {

    }

    @Override
    public boolean onFling(@NonNull MotionEvent motionEvent, @NonNull MotionEvent motionEvent1, float v, float v1) {
        return false;
    }

    public void openTextDect() {
        Intent intent = new Intent(this, TextDetection.class);
        startActivity(intent);
    }

    public void openObjectDect() {
        Intent intent = new Intent(this, ObjectDetection.class);
        startActivity(intent);
    }
}