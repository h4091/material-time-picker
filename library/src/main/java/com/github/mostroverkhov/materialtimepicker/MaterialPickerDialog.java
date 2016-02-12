package com.github.mostroverkhov.materialtimepicker;

import android.app.AlertDialog;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.GridLayout;
import android.widget.TextView;

/**
 * Created by maksym ostroverkhov on 12.02.2016.
 */
public class MaterialPickerDialog extends DialogFragment {

    private View firstHour;
    private View secHour;
    private View firstMin;
    private View secMin;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        final LayoutInflater inflater = LayoutInflater.from(getActivity());
        final View root = inflater.inflate(R.layout.material_dialog_root, null);
        final GridLayout keyboardGridView = (GridLayout) root.findViewById(R.id.material_dialog_keyboard);
        final View animatedView = root.findViewById(R.id.material_dialog_animated_bg);
        firstHour = root.findViewById(R.id.material_dialog_first_hour);
        secHour = root.findViewById(R.id.material_dialog_second_hour);
        firstMin = root.findViewById(R.id.material_dialog_first_min);
        secMin = root.findViewById(R.id.material_dialog_second_min);

        fillGrid(inflater, keyboardGridView);

        final SelectionAnimator animator = new SelectionAnimator(firstHour, animatedView, 200, new SelectionAnimator.Callback() {
            @Override
            public void onAnimationStarted() {
                setEnabledTimeView(false);
            }

            @Override
            public void onAnimationFinished() {
                setEnabledTimeView(true);
            }
        });
        setAnimatorListener(animator, firstHour, secHour, firstMin, secMin);

        return new AlertDialog.Builder(getActivity()).setView(root).create();
    }

    private void setAnimatorListener(final SelectionAnimator animator, View... views) {
        for (View view : views) {
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    animator.animateTo(v);
                }
            });
        }
    }

    private void setEnabled(boolean enabled, View... views) {
        for (View view : views) {
            view.setEnabled(enabled);
        }
    }

    private void setEnabledTimeView(boolean enabled) {
        setEnabled(enabled, firstHour, secHour, firstMin, secMin);
    }


    @Override
    public void onStart() {
        super.onStart();

        final Window window = getDialog().getWindow();
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        final WindowManager.LayoutParams params = window.getAttributes();
        params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        window.setAttributes(params);
    }

    private View newGridItem(LayoutInflater inflater, ViewGroup parent, String text, int row, int col) {

        final View view = inflater.inflate(R.layout.time_keyboard_item, parent, false);
        final TextView textView = (TextView) view.findViewById(R.id.time_key_board_view);
        textView.setText(text);

        final GridLayout.LayoutParams params = new GridLayout.LayoutParams(view.getLayoutParams());
        params.columnSpec = GridLayout.spec(col);
        params.rowSpec = GridLayout.spec(row);

        view.setLayoutParams(params);

        return view;
    }

    private void fillGrid(LayoutInflater inflater, GridLayout parent) {
        int pos = 1;
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                parent.addView(newGridItem(inflater, parent, String.valueOf(pos), i, j));
                pos++;
            }
        }
        parent.addView(newGridItem(inflater, parent, "0", 3, 1));
    }
}
