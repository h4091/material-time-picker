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
import android.graphics.Point;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.StyleRes;
import android.support.v4.app.Fragment;
import android.view.View;

/**
 * Created by maksym ostroverkhov on 13.02.2016.
 */
public class MaterialTimePickerBuilder {

    static final String TARGET_ACTIVITY = "args:target_activity";
    static final String TARGET_FRAGMENT = "args:target_fragment";

    private long millis;
    private String target;
    private Object targetInstance;
    private int requestCode;
    private View clickedOnView;
    private int themeResId;

    public MaterialTimePickerBuilder withActivity(@NonNull Activity activity) {
        target = TARGET_ACTIVITY;
        targetInstance = activity;
        return this;
    }

    public MaterialTimePickerBuilder withFragment(@NonNull Fragment fragment, int requestCode) {
        target = TARGET_FRAGMENT;
        targetInstance = fragment;
        this.requestCode = requestCode;
        return this;
    }

    public MaterialTimePickerBuilder withTime(long millis) {
        this.millis = millis;
        return this;
    }

    public MaterialTimePickerBuilder withTheme(@StyleRes int themeResId) {
        this.themeResId = themeResId;
        return this;
    }

    public MaterialTimePickerBuilder revealFromView(@NonNull View view) {
        clickedOnView = view;
        return this;
    }

    public MaterialTimePicker build() {

        assertArgs();

        final MaterialTimePicker dialogFragment = new MaterialTimePicker();

        final Bundle args = new Bundle();
        args.putLong(MaterialTimePicker.ARGS_KEY_TIME, millis);
        args.putString(MaterialTimePicker.ARGS_KEY_TARGET, target);

        if (clickedOnView != null) {
            final Point touchPoint = Util.getAbsCoordsFor(clickedOnView);
            args.putParcelable(MaterialTimePicker.ARGS_KEY_TOUCH_POINT, touchPoint);
        }

        if (themeResId > 0) {
            args.putInt(MaterialTimePicker.ARGS_KEY_THEME_RES_ID, themeResId);
        }

        if (target.equals(TARGET_FRAGMENT)) {
            args.putInt(MaterialTimePicker.ARGS_KEY_REQUEST_CODE, requestCode);
            dialogFragment.setTargetFragment((Fragment) targetInstance, requestCode);
        }

        dialogFragment.setArguments(args);

        return dialogFragment;
    }

    private void assertArgs() {
        if (target == null) {
            throw new IllegalArgumentException("target is required: fragment or activity");
        }
        if (targetInstance == null) {
            throw new IllegalArgumentException("provided target should not be null");
        }

        if (target.equals(TARGET_ACTIVITY) && !(targetInstance instanceof MaterialTimePicker.Callbacks)) {
            throw new IllegalArgumentException("provided activity should implement MaterialPickerDialog.Callbacks");
        }
    }
}
