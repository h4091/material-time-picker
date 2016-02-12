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

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewPropertyAnimator;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by maksym ostroverkhov on 12.02.2016.
 */
public class DigitAnimator {

    private TextView curView;
    private View animatedView;
    private final TextView digitAnimationUtil;
    private final TextView digitAnimationUtilSpecialCase;
    private final int duration;
    private final Callback callback;
    private boolean isAnimating;

    public DigitAnimator(TextView curView,
                         View animatedView,
                         TextView digitAnimationUtil,
                         TextView digitAnimationUtilSpecialCase,
                         Callback callback) {

        this.curView = curView;
        this.animatedView = animatedView;
        this.digitAnimationUtil = digitAnimationUtil;
        this.digitAnimationUtilSpecialCase = digitAnimationUtilSpecialCase;
        this.duration = 200;
        this.callback = callback;

        init();
    }

    public TextView getCurrentView() {
        return curView;
    }

    public boolean isAnimating() {
        return isAnimating;
    }

    /*selection animation from current to target view, w/o animate changing value of current view*/
    public void animateTo(final TextView target) {
        animateTo(target, null);
    }

    /*selection animation from current to target view, animate changing value of current view to newValue*/
    public void animateTo(final TextView target, String newValue) {

        if (!isAnimating) {

            onAnimationStarted();

            if (target == curView) {
                onAnimationFinished();
            } else {

                if (newValue != null) {
                    startSelectionAndValueSwitchAnimators(target, newValue);
                } else {
                    startSelectionAnimator(target);
                }
            }
        }
    }

    public void animateValue(final TextView view, final String newValue) {
        animateValue(view, newValue, 0);
    }

    /*animate value of view to newValue*/
    public void animateValue(final TextView view, final String newValue, int delay) {

        new AnimatorSet(delay).add(new AnimatorCreator() {
            @Override
            public ViewPropertyAnimator createAnimator() {
                return newDigitAnimator(view);
            }

            @Override
            public Animator.AnimatorListener createListener() {
                return newDigitAnimatorListener(view);
            }
        }).add(new AnimatorCreator() {
            @Override
            public ViewPropertyAnimator createAnimator() {
                return newUtilAnimator(view, digitAnimationUtilSpecialCase);
            }

            @Override
            public Animator.AnimatorListener createListener() {
                return newUtilAnimatorListener(view, digitAnimationUtilSpecialCase, newValue);
            }
        }).start();

    }

    private void startSelectionAnimator(final TextView target) {

        new AnimatorSet().add(new AnimatorCreator() {
            @Override
            public ViewPropertyAnimator createAnimator() {
                return newSelectionAnimator(target);
            }

            @Override
            public Animator.AnimatorListener createListener() {
                return newSelectionAnimatorListener(target);
            }
        }).start();
    }


    private void startSelectionAndValueSwitchAnimators(final TextView target,
                                                       final String newValue) {

        new AnimatorSet().add(new AnimatorCreator() {
            @Override
            public ViewPropertyAnimator createAnimator() {
                return newDigitAnimator(curView);
            }

            @Override
            public Animator.AnimatorListener createListener() {
                return newDigitAnimatorListener(curView);
            }
        }).add(new AnimatorCreator() {
            @Override
            public ViewPropertyAnimator createAnimator() {
                return newUtilAnimator(curView, digitAnimationUtil);
            }

            @Override
            public Animator.AnimatorListener createListener() {
                return newUtilAnimatorListener(curView, digitAnimationUtil, newValue);
            }
        }).add(new AnimatorCreator() {
            @Override
            public ViewPropertyAnimator createAnimator() {
                return newSelectionAnimator(target);
            }

            @Override
            public Animator.AnimatorListener createListener() {
                return newSelectionAnimatorListener(target);
            }
        }).start();
    }

    @NonNull
    private Animator.AnimatorListener newSelectionAnimatorListener(TextView target) {
        final SelectionListener selListener = new SelectionListener(
                DigitAnimator.this,
                target);

        return selListener;
    }

    private ViewPropertyAnimator newSelectionAnimator(TextView target) {
        final ViewPropertyAnimator selAnimator = animatedView.animate()
                .translationXBy(target.getX() - curView.getX())
                .setDuration(duration)
                .setInterpolator(new AccelerateDecelerateInterpolator());

        return selAnimator;
    }

    @NonNull
    private Animator.AnimatorListener newUtilAnimatorListener(TextView targetView,
                                                              TextView utilView,
                                                              String newValue) {
        final UtilViewListener utilListener = new UtilViewListener(
                utilView,
                targetView,
                newValue);

        return utilListener;
    }

    private ViewPropertyAnimator newUtilAnimator(TextView targetView, TextView utilView) {
        final ViewPropertyAnimator utilAnimation = utilView
                .animate()
                .yBy(-targetView.getHeight())
                .setDuration(duration);

        return utilAnimation;
    }

    @NonNull
    private Animator.AnimatorListener newDigitAnimatorListener(TextView textView) {
        Animator.AnimatorListener digitListener = new DigitListener(textView);
        return digitListener;
    }

    private ViewPropertyAnimator newDigitAnimator(TextView textView) {
        final ViewPropertyAnimator digitAnimation = textView
                .animate()
                .translationYBy(-curView.getHeight() / 2)
                .alpha(0)
                .setDuration(duration / 2);

        return digitAnimation;
    }

    private void onAnimationStarted() {
        callback.onAnimationStarted();
        curView.setSelected(false);
        isAnimating = true;
    }

    private void onAnimationFinished() {
        callback.onAnimationFinished();
        isAnimating = false;
    }

    /*Start animations simultaneously. Runs listeners onAnimationStart in order, before animations
    are created (that's the reason why AnimatorCreator is needed).
    Runs listeners onAnimationEnd after all animations are finished*/
    private static class AnimatorSet {

        private final int delay;

        private final List<AnimatorCreator> creators = new ArrayList<>();

        public AnimatorSet add(AnimatorCreator creator) {

            creators.add(creator);
            return this;
        }

        public AnimatorSet(int delay) {
            if (delay < 0) {
                throw new IllegalArgumentException("Delay should not be negative");
            }
            this.delay = delay;
        }

        public AnimatorSet() {
            this(0);
        }

        public void start() {

            final ArrayList<Animator.AnimatorListener> listeners = new ArrayList<>(creators.size());

            for (AnimatorCreator creator : creators) {
                listeners.add(creator.createListener());
            }

            /*calls on animationEnd once all animations are finished*/
            final CountingAnimListener countingListener = new CountingAnimListener(
                    creators.size(),
                    listeners);

            for (int i = 0; i < creators.size(); i++) {

                /*listeners are used to configure views taking part in animation,
                * so those have to e configured before animation creation*/
                listeners.get(i).onAnimationStart(null);

                /*start animations*/
                final AnimatorCreator creator = creators.get(i);
                final ViewPropertyAnimator animator = creator.createAnimator();
                animator.setListener(countingListener);
                if (delay > 0) {
                    animator.setStartDelay(delay);
                }
                animator.start();
            }
        }
    }

    private static class CountingAnimListener extends AnimatorListenerAdapter {

        private final List<Animator.AnimatorListener> listeners;
        private final int count;
        private int curCount = 0;

        public CountingAnimListener(int count,
                                    List<Animator.AnimatorListener> listeners) {

            this.listeners = listeners;
            this.count = count;
        }

        @Override
        public void onAnimationEnd(Animator animation) {

            curCount++;
            if (curCount == count) {
                curCount = 0;
                for (Animator.AnimatorListener listener : listeners) {
                    listener.onAnimationEnd(null);
                }
            }
        }
    }


    private interface AnimatorCreator {
        /*create animator*/
        ViewPropertyAnimator createAnimator();

        /*create listener for animator: it's onAnimStart() is used for
        configuration of animated views, will be called before animator
        is created; onAnimEnd() will be called after all animations in AnimatorSet
        are finished; they animator instance provided in callbacks is always null*/

        Animator.AnimatorListener createListener();
    }

    /*Callbacks for DigitAnimator clients: start/end
    * events of animations set*/
    public interface Callback {

        void onAnimationStarted();

        void onAnimationFinished();
    }

    private void init() {

        animatedView.post(new Runnable() {
            @Override
            public void run() {
                animatedView.setX(curView.getX());
            }
        });

        curView.setSelected(true);
    }

    private static class DigitListener extends AnimatorListenerAdapter {

        private final View curView;
        private float prevY;
        private float prevAlpha;

        public DigitListener(View curView) {
            this.curView = curView;

        }

        @Override
        public void onAnimationEnd(Animator nil) {
            curView.setY(prevY);
            curView.setAlpha(prevAlpha);
        }

        @Override
        public void onAnimationStart(Animator nil) {
            prevAlpha = curView.getAlpha();
            prevY = curView.getY();
        }
    }

    private static class UtilViewListener extends AnimatorListenerAdapter {

        private final TextView digitAnimationUtil;
        private final TextView targetView;
        private final String newValue;

        public UtilViewListener(TextView digitAnimationUtil, TextView targetView, String newValue) {
            this.digitAnimationUtil = digitAnimationUtil;
            this.targetView = targetView;
            this.newValue = newValue;
        }

        @Override
        public void onAnimationEnd(Animator nil) {
            digitAnimationUtil.setVisibility(View.INVISIBLE);
        }

        @Override
        public void onAnimationStart(Animator nil) {
            digitAnimationUtil.setVisibility(View.VISIBLE);
            digitAnimationUtil.setText(newValue);
            digitAnimationUtil.setX(targetView.getX());
            digitAnimationUtil.setY(targetView.getY() + targetView.getHeight());
        }
    }

    private static class SelectionListener extends AnimatorListenerAdapter {

        private final DigitAnimator animator;
        private final TextView target;

        public SelectionListener(DigitAnimator animator, TextView target) {
            this.animator = animator;
            this.target = target;
        }

        @Override
        public void onAnimationEnd(Animator nil) {
            target.setSelected(true);
            animator.curView = target;
            animator.onAnimationFinished();
        }
    }
}
