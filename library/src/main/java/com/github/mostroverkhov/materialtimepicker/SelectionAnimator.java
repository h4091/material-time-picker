package com.github.mostroverkhov.materialtimepicker;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

/**
 * Created by maksym ostroverkhov on 12.02.2016.
 */
public class SelectionAnimator {

    private View curView;
    private View animatedView;
    private final int duration;
    private final Callback callback;

    public SelectionAnimator(View curView,
                             View animatedView,
                             int duration,
                             Callback callback) {

        this.curView = curView;
        this.animatedView = animatedView;
        this.duration = duration;
        this.callback = callback;

        init();
    }

    public void setCurrent(View curView, View animatedView) {
        this.curView = curView;
        this.animatedView = animatedView;
        init();
    }

    public View getCurrentView() {
        return curView;
    }

    public void animateTo(final View target) {

        callback.onAnimationStarted();

        animatedView.animate()
                .translationYBy(target.getY() - curView.getY())
                .translationXBy(target.getX() - curView.getX())
                .setDuration(duration)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        curView.setSelected(false);
                        target.setSelected(true);

                        curView = target;
                        callback.onAnimationFinished();
                    }
                })
                .start();
    }

    public interface Callback {

        void onAnimationStarted();

        void onAnimationFinished();
    }

    private void init() {
        animatedView.setX(curView.getX());
        curView.setSelected(true);
    }
}
