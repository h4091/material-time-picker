package com.github.mostroverkhov.materialtimepicker;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(com.github.mostroverkhov.materialtimepicker.app.R.layout.activity_main);

        final MaterialPickerDialog materialPickerDialog = new MaterialPickerDialog();
        materialPickerDialog.show(getSupportFragmentManager(),"material picker");
    }
}
