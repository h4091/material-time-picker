package com.github.mostroverkhov.materialtimepicker;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.github.mostroverkhov.materialtimepicker.app.R;

import java.util.Date;

public class MainActivity extends AppCompatActivity implements
        MaterialTimePicker.Callbacks {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(com.github.mostroverkhov.materialtimepicker.app.R.layout.activity_main);

        final View btn1 = findViewById(R.id.show_dialog_button1);
        final View btn2 = findViewById(R.id.show_dialog_button2);
        final View btn3 = findViewById(R.id.show_dialog_button3);
        final View btn4 = findViewById(R.id.show_dialog_button4);

        btn3.setTag(R.style.RedDialog);
        btn2.setTag(R.style.GreenDialog);
        btn1.setTag(R.style.BlueDialog);
        btn4.setTag(R.style.OrangeDialog);

        final ClickListener clickListener = new ClickListener(btn1, btn2, btn3, btn4);
    }

    private void showDialog(View clicked, int themeResId) {
        final MaterialTimePickerBuilder builder = new MaterialTimePickerBuilder().withActivity(MainActivity.this)
                .withTime(System.currentTimeMillis())
                .withTheme(themeResId)
                .revealFromView(clicked);

        final MaterialTimePicker materialTimePicker = builder.build();
        materialTimePicker.show(getSupportFragmentManager(), "material picker");
    }

    @Override
    public void onTimeSelected(long millis) {
        Log.d("material-picker-demo", new Date(millis).toString());
    }

    @Override
    public void onCancelled() {
        Log.d("material-picker-demo", "picker cancelled");

    }

    private class ClickListener implements View.OnClickListener {

        public ClickListener(View... views) {
            for (View view : views) {
                view.setOnClickListener(this);

            }
        }

        @Override
        public void onClick(View v) {
            showDialog(v, (Integer) v.getTag());
        }
    }
}
