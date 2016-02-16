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
import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.DimenRes;
import android.support.v4.app.Fragment;
import android.util.Pair;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.GridLayout;
import android.widget.TextView;

import java.util.Calendar;

import io.codetail.animation.SupportAnimator;
import io.codetail.animation.ViewAnimationUtils;

import static com.github.mostroverkhov.materialtimepicker.MaterialTimePicker.ARGS_KEY_REQUEST_CODE;
import static com.github.mostroverkhov.materialtimepicker.MaterialTimePicker.ARGS_KEY_TARGET;
import static com.github.mostroverkhov.materialtimepicker.MaterialTimePicker.ActivityCallbacks;
import static com.github.mostroverkhov.materialtimepicker.MaterialTimePicker.Callbacks;
import static com.github.mostroverkhov.materialtimepicker.MaterialTimePicker.FragmentCallbacks;
import static com.github.mostroverkhov.materialtimepicker.MaterialTimePicker.PrivateCallbacks;
import static com.github.mostroverkhov.materialtimepicker.MaterialTimePicker.Time;

/**
 * Created by maksym ostroverkhov on 13.02.2016.
 */
public class Util {

    static void circularReveal(final View view, Point absStartPoint) {
        circularReveal(view, absStartPoint, false, null);
    }

    static void reverseCircularReveal(final View view,
                                      Point absStartPoint,
                                      final SupportAnimator.AnimatorListener listener) {

        circularReveal(view, absStartPoint, true, listener);
    }

    static void circularReveal(final View view,
                               final Point absStartPoint,
                               final boolean reverse,
                               final SupportAnimator.AnimatorListener listener) {

        view.post(new Runnable() {
            @Override
            public void run() {

                final Point relStartPoint = isDefault(absStartPoint)
                        ? defaultPointFromView(view)
                        : Util.getNearestDialogCoord(view, absStartPoint);

                final int cx = relStartPoint.x;
                final int cy = relStartPoint.y;

                int dx = Math.max(cx, view.getWidth() - cx);
                int dy = Math.max(cy, view.getHeight() - cy);

                float radius = (float) Math.hypot(dx, dy);
                float fromRadius = reverse ? radius : 0;
                float toRadius = reverse ? 0 : radius;

                final SupportAnimator animator = ViewAnimationUtils.createCircularReveal(view,
                        cx,
                        cy,
                        fromRadius,
                        toRadius);

                animator.setInterpolator(new AccelerateDecelerateInterpolator());
                animator.setDuration(300);
                if (listener != null) {
                    animator.addListener(listener);
                }
                animator.start();
            }
        });
    }

    static Point defaultRevealStartPoint() {
        return new Point(-1, -1);
    }

    static boolean isDefault(Point point) {
        return point.x == -1 && point.y == -1;
    }

    private static Point defaultPointFromView(View view) {

        int cx = view.getLeft() + view.getWidth() / 2;
        int cy = view.getTop() + view.getHeight() / 2;

        return new Point(cx, cy);
    }

    static View newGridItem(LayoutInflater inflater, ViewGroup parent, int val, int row, int col) {

        final View view = inflater.inflate(R.layout.time_keyboard_item, parent, false);
        final TextView textView = (TextView) view.findViewById(R.id.time_key_board_view);
        textView.setText(String.valueOf(val));

        final GridLayout.LayoutParams params = new GridLayout.LayoutParams(view.getLayoutParams());
        params.columnSpec = GridLayout.spec(col);
        params.rowSpec = GridLayout.spec(row);

        view.setLayoutParams(params);
        view.setTag(R.id.time_kb_val, val);

        return view;
    }

    static void fillGridAndSetItemsListener(LayoutInflater inflater,
                                            GridLayout parent,
                                            View.OnClickListener itemListener) {
        int pos = 1;
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                final View view = newGridItem(inflater, parent, pos, i, j);
                view.setOnClickListener(itemListener);
                parent.addView(view);
                pos++;
            }
        }
        final View child = newGridItem(inflater, parent, 0, 3, 1);
        child.setOnClickListener(itemListener);
        parent.addView(child);
    }

    static long timeToMillis(Time time) {

        final Calendar calendar = Calendar.getInstance();
        int hours = time.getFirstHour() * 10 + time.getSecHour();
        int mins = time.getFirstMin() * 10 + time.getSecMin();

        calendar.set(Calendar.HOUR_OF_DAY, hours);
        calendar.set(Calendar.MINUTE, mins);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        return calendar.getTimeInMillis();
    }

    static Time millisToTime(long millis) {

        final Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(millis);
        int hours = calendar.get(Calendar.HOUR_OF_DAY);
        int mins = calendar.get(Calendar.MINUTE);

        int firstHour = hours / 10;
        int secHour = hours - firstHour * 10;

        int firstMin = mins / 10;
        int secMin = mins - firstMin * 10;

        return new Time(firstHour, secHour, firstMin, secMin);
    }

    static void assertArgs(Bundle args) {
        if (args == null) {
            throw new IllegalArgumentException("fragment arguments should be set");
        }
        final String target = args.getString(ARGS_KEY_TARGET);
        if (target == null) {
            throw new IllegalArgumentException("fragment argument: target should be present");
        }
        if (!target.equals(MaterialTimePickerBuilder.TARGET_ACTIVITY) && !target.equals(MaterialTimePickerBuilder.TARGET_FRAGMENT)) {
            throw new IllegalArgumentException("Target should be Builder.TARGET_ACTIVITY " +
                    "or Builder.TARGET_FRAGMENT");
        }
    }

    static PrivateCallbacks newCallbacks(Activity parentActivity,
                                         Fragment thisFragment,
                                         Bundle args) {

        final String target = args.getString(ARGS_KEY_TARGET);
        if (MaterialTimePickerBuilder.TARGET_FRAGMENT.equals(target)) {
            return new FragmentCallbacks(
                    thisFragment.getTargetFragment(),
                    args.getInt(ARGS_KEY_REQUEST_CODE));
        } else if (MaterialTimePickerBuilder.TARGET_ACTIVITY.equals(target)) {
            return new ActivityCallbacks((Callbacks) parentActivity);
        } else {
            throw new IllegalStateException("Unknown target: " + String.valueOf(target));
        }
    }

    static void setDialogSize(Activity activity, Dialog dialog,
                              @DimenRes int maxWidthDimenResId,
                              @DimenRes int maxHeightDimenResId) {

        final Pair<Integer, Integer> wh = getWindowSize(activity);

        final Window window = dialog.getWindow();
        final WindowManager.LayoutParams params = window.getAttributes();

        final Context context = dialog.getContext();

        int widthLimit = getDimension(context, maxWidthDimenResId);
        int heightLimit = getDimension(context, maxHeightDimenResId);

        params.width = Math.min(widthLimit, wh.first);
        params.height = Math.min(heightLimit, wh.second);

        window.setAttributes(params);
    }

    private static int getDimension(Context context, int resId) {
        return (int) context.getResources().getDimension(resId);
    }

    static class SupportAnimatorListener implements SupportAnimator.AnimatorListener {

        @Override
        public void onAnimationStart() {

        }

        @Override
        public void onAnimationEnd() {

        }

        @Override
        public void onAnimationCancel() {

        }

        @Override
        public void onAnimationRepeat() {

        }
    }

    static Pair<Integer, Integer> getWindowSize(Activity c) {
        final Display defaultDisplay = ((WindowManager) c
                .getSystemService(Context.WINDOW_SERVICE))
                .getDefaultDisplay();

        final Point wh = new Point();

        defaultDisplay.getSize(wh);

        Rect rect = new Rect();
        Window window = c.getWindow();
        window.getDecorView().getWindowVisibleDisplayFrame(rect);
        int statusBarHeight = rect.top;

        return new Pair<>(wh.x, wh.y - statusBarHeight);

    }

    public static Point getAbsCoordsFor(View view) {
        int[] location = new int[2];
        view.getLocationOnScreen(location);
        return new Point(location[0], location[1]);
    }


    static Point getNearestDialogCoord(View rootView, Point target) {

        final int[] loc = new int[2];

        rootView.getLocationOnScreen(loc);

        final Point left = new Point(loc[0], loc[1]);
        final Point bot = new Point(loc[0] + rootView.getWidth(), loc[1] + rootView.getHeight());

        Point point;

        if (isInsideTri(left, bot, target)) {
            point = new Point(target);
        } else if (isOnXpath(left, bot, target)) {
            point = new Point(getNearestX(left, bot, target), target.y);
        } else if (isOnYpath(left, bot, target)) {
            point = new Point(target.x, getNearestY(left, bot, target));
        } else {
            point = new Point(getNearestX(left, bot, target), getNearestY(left, bot, target));
        }
        point.offset(-left.x, -left.y);
        return point;
    }


    /*can reach point by moving on x coord only*/
    private static boolean isOnXpath(Point left, Point bot, Point target) {
        return target.y >= left.y && target.y <= bot.y;
    }

    /*can reach point by moving on y coord only*/
    private static boolean isOnYpath(Point left, Point bot, Point target) {
        return target.x >= left.x && target.x <= bot.x;
    }


    private static boolean isInsideTri(Point left, Point bot, Point target) {

        return (target.x >= left.x) && (target.y >= left.y) && (target.x <= bot.x) && (target.y <= bot.y);
    }

    private static int getNearestX(Point left, Point bot, Point target) {
        final int toLeft = Math.abs(target.x - left.x);
        final int toBot = Math.abs(target.x - bot.x);
        int lower = Math.min(toLeft, toBot);

        return lower == toLeft ? left.x : bot.x;
    }

    private static int getNearestY(Point left, Point bot, Point target) {
        final int toLeft = Math.abs(target.y - left.y);
        final int toBot = Math.abs(target.y - bot.y);
        int lower = Math.min(toLeft, toBot);

        return lower == toLeft ? left.y : bot.y;
    }
}
