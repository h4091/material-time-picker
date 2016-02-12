/*
 * Copyright 2016 Maksym Ostroverkhov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.mostroverkhov.materialtimepicker;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.GridLayout;
import android.widget.TextView;

import java.util.HashSet;
import java.util.Set;

import static com.github.mostroverkhov.materialtimepicker.Util.*;
import static com.github.mostroverkhov.materialtimepicker.Util.assertArgs;
import static com.github.mostroverkhov.materialtimepicker.Util.circularReveal;
import static com.github.mostroverkhov.materialtimepicker.Util.fillGridAndSetItemsListener;
import static com.github.mostroverkhov.materialtimepicker.Util.millisToTime;
import static com.github.mostroverkhov.materialtimepicker.Util.newCallbacks;
import static com.github.mostroverkhov.materialtimepicker.Util.reverseCircularReveal;
import static com.github.mostroverkhov.materialtimepicker.Util.timeToMillis;

/**
 * Created by maksym ostroverkhov on 12.02.2016.
 */
public class MaterialTimePicker extends DialogFragment {

    private static final int SPECIAL_CASE_ANIM_DELAY = 70;

    private static final String SAVE_KEY_STATE = "savedState:state";
    private static final String SAVE_KEY_TIME = "savedState:time";

    public static final String ARGS_KEY_TIME = "args:time";
    public static final String ARGS_KEY_TARGET = "args:target";
    public static final String ARGS_KEY_TOUCH_POINT = "args:touchAbsPoint";
    public static final String ARGS_KEY_THEME_RES_ID = "args:themeResId";

    public static final String ARGS_KEY_REQUEST_CODE = "args:requestCode";
    public static final String EXTRA_SELECTED_MILLIS = "result:timeMillis";

    private HashSet<Integer> digitsSet;

    private View root;
    private View contentView;
    private TextView firstHour;
    private TextView secHour;
    private TextView firstMin;
    private TextView secMin;
    private GridLayout keyboardGridView;
    private View animatedView;
    private TextView animationUtilView;
    private TextView animationUtilSpecial;
    private View okButton;

    private DigitAnimator animator;
    private boolean newDialogInstance;

    /*dialog state */
    private State state;
    private Time time;
    private PrivateCallbacks callbacks;
    private Point revealStartPoint;
    private int dialogThemeResId;

    public interface Callbacks {

        void onTimeSelected(long millis);

        void onCancelled();
    }

    @Override
    public void onAttach(Activity activity) {

        super.onAttach(activity);
        final Bundle args = getArguments();
        assertArgs(args);
        callbacks = newCallbacks(activity, this, args);
    }

    @Override
    public void onDetach() {

        callbacks = null;
        super.onDetach();
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        initState(savedInstanceState);

        final int resourceId = getStyleReference(getActivity(), dialogThemeResId);
        final ContextThemeWrapper context = new ContextThemeWrapper(getActivity(), resourceId);

        LayoutInflater inflater = LayoutInflater.from(context);
        root = inflater.inflate(R.layout.material_dialog_root, null);
        contentView = root.findViewById(R.id.material_dialog_root_id);
        keyboardGridView = (GridLayout) root.findViewById(R.id.material_dialog_keyboard);
        animatedView = root.findViewById(R.id.material_dialog_animated_bg);
        animationUtilView = (TextView) root.findViewById(R.id.material_dialog_digit_animation_util);
        animationUtilSpecial = (TextView) root.findViewById(R.id.material_dialog_digit_animation_util_special_case);
        firstHour = (TextView) root.findViewById(R.id.material_dialog_first_hour);
        secHour = (TextView) root.findViewById(R.id.material_dialog_second_hour);
        firstMin = (TextView) root.findViewById(R.id.material_dialog_first_min);
        secMin = (TextView) root.findViewById(R.id.material_dialog_second_min);
        okButton = root.findViewById(R.id.material_dialog_ok_button);

        bindDigitViewsToStates();

        animator = new DigitAnimator(
                state.targetView(this),
                animatedView,
                animationUtilView,
                animationUtilSpecial,
                new SelectionAnimatorListener());

        setUiListeners(inflater);

        state.setUi(this);

        return newDialog();
    }

    static int getStyleReference(Activity activity, int defStyle) {
        final TypedArray typedArray = activity.getTheme().obtainStyledAttributes(new int[]{R.attr.materialDialogStyle});
        final int resourceId = typedArray.getResourceId(0, defStyle);
        typedArray.recycle();
        return resourceId;
    }

    @NonNull
    private Dialog newDialog() {

        Dialog dialog = new Dialog(getActivity());

        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(root);

        final Window window = dialog.getWindow();

        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        window.setWindowAnimations(R.style.DialogAnimations);

        return dialog;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt(SAVE_KEY_STATE, state.getSaveState());
        outState.putParcelable(SAVE_KEY_TIME, time);
    }

    @Override
    public void onStart() {
        super.onStart();

        setDialogSize(getActivity(),
                getDialog(),
                R.dimen.dialog_max_width,
                R.dimen.dialog_max_height);

        if (newDialogInstance) {
            newDialogInstance = false;
            circularReveal(contentView, revealStartPoint);
        }
    }

    private void setUiListeners(LayoutInflater inflater) {

        setHoursAnimatorListener(animator, firstHour, secHour, firstMin, secMin);

        fillGridAndSetItemsListener(inflater, keyboardGridView, new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (!animator.isAnimating()) {

                    final MaterialTimePicker self = MaterialTimePicker.this;
                    final Integer value = (Integer) v.getTag(R.id.time_kb_val);
                    final boolean changed = state.setTimeValue(self, value);
                    state = state.next(self);

                    if (changed) {
                        animator.animateTo(state.targetView(self), String.valueOf(value));
                    } else {
                        animator.animateTo(state.targetView(self));
                    }

                    addressSpecialCases();
                }
            }
        });
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (callbacks != null) {
                    reverseCircularReveal(contentView, getAbsCoordsFor(okButton),
                            new SupportAnimatorListener() {
                                @Override
                                public void onAnimationEnd() {
                            /*hide root as to avoid dialog exit anim after reverse circular reveal*/
                                    root.setVisibility(View.INVISIBLE);
                                    callbacks.onTimeSelected(timeToMillis(time));
                                    dismiss();
                                }
                            });
                }
            }
        });
    }

    private void addressSpecialCases() {

        /*selected hours -> 15, then change first hour to 2 (valid value) yields 25. Address by
        * changing second hour to 3 with animation up*/
        if (time.getFirstHour() == 2 && time.getSecHour() > 4) {
            time.setSecHour(3);
            animator.animateValue(State.SEC_HOUR.targetView(MaterialTimePicker.this),
                    "3",
                    SPECIAL_CASE_ANIM_DELAY);
        }
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        if (callbacks != null) {
            callbacks.onCancelled();
        }
        super.onCancel(dialog);
    }

    private void bindDigitViewsToStates() {
        firstHour.setTag(R.id.view_state_id, State.FIRST_HOUR);
        secHour.setTag(R.id.view_state_id, State.SEC_HOUR);
        firstMin.setTag(R.id.view_state_id, State.FIRST_MIN);
        secMin.setTag(R.id.view_state_id, State.SEC_MIN);
    }

    private void initState(Bundle savedInstanceState) {

        final Bundle arguments = getArguments();

        if (savedInstanceState == null) {
            initProvidedState(arguments);
        } else {
            initSavedState(savedInstanceState);
        }
        revealStartPoint = arguments.containsKey(ARGS_KEY_TOUCH_POINT)
                ? arguments.<Point>getParcelable(ARGS_KEY_TOUCH_POINT)
                : defaultRevealStartPoint();

        dialogThemeResId = arguments.containsKey(ARGS_KEY_THEME_RES_ID)
                ? arguments.getInt(ARGS_KEY_THEME_RES_ID)
                : R.style.DefMaterialDialogStyle;
    }

    private void initSavedState(Bundle savedInstanceState) {
        time = savedInstanceState.getParcelable(SAVE_KEY_TIME);
        state = State.getFromSaveState(savedInstanceState.getInt(SAVE_KEY_STATE));
    }

    private void initProvidedState(Bundle args) {

        newDialogInstance = true;

        final long timeMillis = args.getLong(ARGS_KEY_TIME);

        time = timeMillis == 0 ? new Time() : millisToTime(timeMillis);
        state = State.FIRST_HOUR;
    }

    private void setHoursAnimatorListener(final DigitAnimator animator, View... views) {

        final View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                state = (State) v.getTag(R.id.view_state_id);
                animator.animateTo(state.targetView(MaterialTimePicker.this));
            }
        };

        for (View view : views) {
            view.setOnClickListener(listener);
        }
    }

    private void setEnabledKeyboardDigits(int... enabledDigits) {

        final Set<Integer> digits = getDigitsSet(enabledDigits.length);
        for (int digit : enabledDigits) {
            digits.add(digit);
        }

        for (int i = 0; i < keyboardGridView.getChildCount(); i++) {
            final View child = keyboardGridView.getChildAt(i);
            child.setEnabled(digits.contains(child.getTag(R.id.time_kb_val)));
        }
    }

    private Set<Integer> getDigitsSet(int size) {

        if (digitsSet == null) {
            digitsSet = new HashSet<>(size);
        } else {
            digitsSet.clear();
        }
        return digitsSet;
    }

    private class SelectionAnimatorListener implements DigitAnimator.Callback {
        @Override
        public void onAnimationStarted() {

        }

        @Override
        public void onAnimationFinished() {
            state.setUi(MaterialTimePicker.this);
        }
    }

    private enum State {

        FIRST_HOUR {
            @Override
            void setUi(MaterialTimePicker dialog) {
                super.setUi(dialog);
                dialog.setEnabledKeyboardDigits(0, 1, 2);
            }

            @Override
            State next(MaterialTimePicker dialog) {
                return SEC_HOUR;
            }

            @Override
            TextView targetView(MaterialTimePicker dialog) {
                return dialog.firstHour;
            }

            @Override
            boolean setTimeValue(MaterialTimePicker dialog, int value) {
                final Time time = dialog.time;
                boolean changed = time.getFirstHour() != value;
                time.setFirstHour(value);

                return changed;
            }
        },

        SEC_HOUR {
            @Override
            void setUi(MaterialTimePicker dialog) {
                super.setUi(dialog);

                if (dialog.time.firstHour == 2) {
                    dialog.setEnabledKeyboardDigits(0, 1, 2, 3);
                } else {
                    dialog.setEnabledKeyboardDigits(0, 1, 2, 3, 4, 5, 6, 7, 8, 9);
                }
            }

            @Override
            State next(MaterialTimePicker dialog) {
                return FIRST_MIN;
            }

            @Override
            TextView targetView(MaterialTimePicker dialog) {
                return dialog.secHour;
            }

            @Override
            boolean setTimeValue(MaterialTimePicker dialog, int value) {
                final Time time = dialog.time;
                boolean changed = time.getSecHour() != value;
                time.setSecHour(value);

                return changed;
            }
        },

        FIRST_MIN {
            @Override
            void setUi(MaterialTimePicker dialog) {
                super.setUi(dialog);
                dialog.setEnabledKeyboardDigits(0, 1, 2, 3, 4, 5);
            }

            @Override
            State next(MaterialTimePicker dialog) {
                return SEC_MIN;
            }

            @Override
            TextView targetView(MaterialTimePicker dialog) {
                return dialog.firstMin;
            }

            @Override
            boolean setTimeValue(MaterialTimePicker dialog, int value) {

                final Time time = dialog.time;
                boolean changed = time.getFirstMin() != value;
                time.setFirstMin(value);

                return changed;
            }
        },

        SEC_MIN {
            @Override
            void setUi(MaterialTimePicker dialog) {
                super.setUi(dialog);
                dialog.setEnabledKeyboardDigits(0, 1, 2, 3, 4, 5, 6, 7, 8, 9);
            }

            @Override
            State next(MaterialTimePicker dialog) {
                return FIRST_HOUR;
            }

            @Override
            TextView targetView(MaterialTimePicker dialog) {
                return dialog.secMin;
            }

            @Override
            boolean setTimeValue(MaterialTimePicker dialog, int value) {

                final Time time = dialog.time;
                boolean changed = time.getSecMin() != value;
                time.setSecMin(value);

                return changed;
            }
        };

        void setUi(MaterialTimePicker dialog) {
            dialog.firstHour.setText(String.valueOf(dialog.time.getFirstHour()));
            dialog.secHour.setText(String.valueOf(dialog.time.getSecHour()));
            dialog.firstMin.setText(String.valueOf(dialog.time.getFirstMin()));
            dialog.secMin.setText(String.valueOf(dialog.time.getSecMin()));

        }

        abstract State next(MaterialTimePicker dialog);

        abstract TextView targetView(MaterialTimePicker dialog);

        abstract boolean setTimeValue(MaterialTimePicker dialog, int value);

        public int getSaveState() {
            return ordinal();
        }

        public static State getFromSaveState(int state) {
            return values()[state];
        }
    }

    static class Time implements Parcelable {

        private int firstHour;
        private int secHour;
        private int firstMin;
        private int secMin;

        public Time() {
        }

        public Time(int firstHour, int secHour, int firstMin, int secMin) {
            this.firstHour = firstHour;
            this.secHour = secHour;
            this.firstMin = firstMin;
            this.secMin = secMin;
        }

        protected Time(Parcel in) {
            firstHour = in.readInt();
            secHour = in.readInt();
            firstMin = in.readInt();
            secMin = in.readInt();
        }

        public static final Creator<Time> CREATOR = new Creator<Time>() {
            @Override
            public Time createFromParcel(Parcel in) {
                return new Time(in);
            }

            @Override
            public Time[] newArray(int size) {
                return new Time[size];
            }
        };

        public int getFirstHour() {
            return firstHour;
        }

        public void setFirstHour(int firstHour) {
            this.firstHour = firstHour;
        }

        public int getSecHour() {
            return secHour;
        }

        public void setSecHour(int secHour) {
            this.secHour = secHour;
        }

        public int getFirstMin() {
            return firstMin;
        }

        public void setFirstMin(int firstMin) {
            this.firstMin = firstMin;
        }

        public int getSecMin() {
            return secMin;
        }

        public void setSecMin(int secMin) {
            this.secMin = secMin;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(firstHour);
            dest.writeInt(secHour);
            dest.writeInt(firstMin);
            dest.writeInt(secMin);
        }

        @Override
        public String toString() {
            return "Time{" +
                    "firstHour=" + firstHour +
                    ", secHour=" + secHour +
                    ", firstMin=" + firstMin +
                    ", secMin=" + secMin +
                    '}';
        }
    }

    interface PrivateCallbacks extends Callbacks {
    }

    static class ActivityCallbacks implements PrivateCallbacks {

        private final Callbacks actCallbacks;

        public ActivityCallbacks(Callbacks actCallbacks) {
            this.actCallbacks = actCallbacks;
        }

        @Override
        public void onTimeSelected(long millis) {
            actCallbacks.onTimeSelected(millis);
        }

        @Override
        public void onCancelled() {
            actCallbacks.onCancelled();
        }
    }

    static class FragmentCallbacks implements PrivateCallbacks {

        private final Fragment fragment;
        private final int requestCode;

        public FragmentCallbacks(Fragment fragment, int requestCode) {
            this.fragment = fragment;
            this.requestCode = requestCode;
        }

        @Override
        public void onTimeSelected(long millis) {
            fragment.onActivityResult(requestCode, Activity.RESULT_OK, newResultTimeIntent(millis));
        }

        @Override
        public void onCancelled() {
            fragment.onActivityResult(requestCode, Activity.RESULT_CANCELED, new Intent());
        }
    }

    @NonNull
    private static Intent newResultTimeIntent(long millis) {
        final Intent intent = new Intent();
        intent.putExtra(EXTRA_SELECTED_MILLIS, millis);
        return intent;
    }
}
