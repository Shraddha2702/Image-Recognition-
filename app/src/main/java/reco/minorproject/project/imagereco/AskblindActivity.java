package reco.minorproject.project.imagereco;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.speech.tts.TextToSpeech;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;

import java.util.HashMap;
import java.util.Locale;

public class AskblindActivity extends AppCompatActivity implements GestureDetector.OnGestureListener,
        GestureDetector.OnDoubleTapListener {

    private static final int SWIPE_THRESHOLD = 100;
    private static final int SWIPE_VELOCITY_THRESHOLD = 100;
    private GestureDetectorCompat gestureDetector;
    private TextToSpeech t1;
    Session session;
    int flag = 0;

    //Name of Shared Preference is blindknow
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_askblind);

        flag = 0;

        session = new Session(getApplicationContext());
        //sp = getApplicationContext().getSharedPreferences("blindknow",
                //Context.MODE_PRIVATE);

        t1=new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != TextToSpeech.ERROR) {
                    t1.setLanguage(Locale.UK);
                    String toSpeak = "Are you Blind ? If you are then Swipe Left, else Swipe Right.";
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        String utteranceId=this.hashCode() + "";
                        t1.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null, utteranceId);
                    }else{
                        HashMap<String, String> map = new HashMap<>();
                        map.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "MessageId");
                        t1.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, map);
                    }
                }
            }
        });
        Log.d("ishere","57");
        this.gestureDetector = new GestureDetectorCompat(this, this);
        Log.d("ishere","59");
        gestureDetector.setOnDoubleTapListener(this);
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        boolean result = false;
        try {
            float diffY = e2.getY() - e1.getY();
            float diffX = e2.getX() - e1.getX();
            Log.d("diffY",""+diffY);
            Log.d("diffX",""+diffX);

            if (Math.abs(diffX) > Math.abs(diffY)) {
                if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                    if (diffX > 0) {
                        Log.d("diffX > 0","73");
                        onSwipeRight();
                    } else {
                        Log.d("diffX < 0","76");
                        onSwipeLeft();
                    }
                }
                result = true;
            }

            else if (Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                if (diffY > 0) {
                    Log.d("diffY > 0","85");
                } else {
                    Log.d("diffY < 0","87");
                }
            }

        } catch (Exception exception) {
            exception.printStackTrace();
        }
        return result;
    }


    public void onSwipeLeft() {

        //Is Blind

        //Store value in SP, that it is blind for Ui
        //editor = sp.edit();
        //editor.putString("isblind", "yes");
        //Log.d("sp done","isblindyes");
        //editor.commit();

        session.setBlind(true);
        Log.d("isblind",""+session.isBlind());
        Intent intent;
        intent = new Intent(AskblindActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    public void onSwipeRight() {

        //Is Not Blind
        //Store value in SP, that it's not blind for Ui
        //editor = sp.edit();
        //editor.putString("isblind", "no");
        //Log.d("sp done","isblindno");
        //editor.commit();
       // Log.d("isblind",sp.getString("isblind",""));

        session.setBlind(false);
        Log.d("isblind",""+session.isBlind());
        Intent intent;
        intent = new Intent(AskblindActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();

    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent motionEvent) {

        Log.d("ishere","125");
        /*editor = sp.edit();
        editor.putString("isblind", "no");
        Log.d("sp done","isblindno");
        editor.commit();

        Log.d("isblind",sp.getString("isblind",""));
        Intent intent;
        intent = new Intent(AskblindActivity.this, MainActivity.class);
        startActivity(intent);*/
        return true;
    }

    @Override
    public boolean onDoubleTap(MotionEvent motionEvent) {

        Log.d("ishere","141");
        /*editor = sp.edit();
        editor.putString("isblind", "no");
        Log.d("sp done","isblindno");
        editor.commit();

        Log.d("isblind",sp.getString("isblind",""));
        Intent intent;
        intent = new Intent(AskblindActivity.this, LoginActivity.class);
        startActivity(intent);*/
        return true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        this.gestureDetector.onTouchEvent(event);
        return super.onTouchEvent(event);
    }

    @Override
    public boolean onDoubleTapEvent(MotionEvent motionEvent) {
        return true;
    }

    @Override
    public boolean onDown(MotionEvent motionEvent) {
        return true;
    }

    @Override
    public void onShowPress(MotionEvent motionEvent) {

    }

    @Override
    public boolean onSingleTapUp(MotionEvent motionEvent) {
        return true;
    }

    @Override
    public boolean onScroll(MotionEvent motionEvent, MotionEvent motionEvent1, float v, float v1) {
        return true;
    }

    @Override
    public void onLongPress(MotionEvent motionEvent) {

    }

    public void onResume() {
        super.onResume();  // Always call the superclass method firs
        if(flag==1){
            t1=new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
                @Override
                public void onInit(int status) {
                    if(status != TextToSpeech.ERROR) {
                        t1.setLanguage(Locale.UK);
                        String toSpeak = "Are you Blind ? If you are then Swipe Left, else Swipe Right.";

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            String utteranceId=this.hashCode() + "";
                            t1.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null, utteranceId);
                        }else{
                            HashMap<String, String> map = new HashMap<>();
                            map.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "MessageId");
                            t1.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, map);
                        }
                    }
                }
            });
        }
    }
    public void onPause() {
        super.onPause();  // Always call the superclass method first
    }
    protected void onStop() {
        super.onStop();
        flag=1;
    }
}
