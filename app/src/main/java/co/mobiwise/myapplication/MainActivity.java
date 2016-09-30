package co.mobiwise.myapplication;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import co.mobiwise.library.radio.RadioListener;


public class MainActivity extends Activity {

    Button radioActivity;
    Button mediaActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeUI();

    }

    public void initializeUI() {
        radioActivity = (Button) findViewById(R.id.radioActivity);
        mediaActivity = (Button) findViewById(R.id.mediaActivity);

        radioActivity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, RadioActivity.class);
                startActivity(intent);
            }
        });

        mediaActivity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, MediaActivity.class);
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

}
