package com.hapielabs.scrollingviewpager;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.hardware.SensorManager;
import android.os.Handler;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.LinearInterpolator;

import static java.lang.Math.max;


/**
 * Created by hapie on 15/7/17.
 */

public class NewBehavior extends AppBarLayout.Behavior {

    private View mTargetView;

    private boolean waitForRV;
    private ValueAnimator mHeightAnimator;

    private int prev;

    private static final int MAX_OFFSET_ANIMATION_DURATION = 392; // ms
    private boolean disableFling;

    public NewBehavior(Context context, AttributeSet attrs) {
        super();
        prev = 0;
    }

    /*

    @Override
    public boolean onTouchEvent(CoordinatorLayout parent, AppBarLayout child, MotionEvent ev) {
        return true; //super.onTouchEvent(parent, child, ev);
    }

    */

    @Override
    public void onNestedPreScroll(CoordinatorLayout coordinatorLayout, AppBarLayout child,
                                  View target, int dx, int dy, int[] consumed) {

        mTargetView = child.findViewWithTag("vp");

        if (dy != 0) {
            if (dy >= 0 && target instanceof RecyclerView &&
                    ((LinearLayoutManager) ((RecyclerView) target).getLayoutManager())
                            .findFirstVisibleItemPosition() != 1) { // was 0 earlier
                consumed[1] = 0;
                return;
            } else if (dy < 0 && target instanceof RecyclerView &&
                    ((LinearLayoutManager) ((RecyclerView) target).getLayoutManager())
                            .findFirstCompletelyVisibleItemPosition() != 1) { // was 0 earlier
                consumed[1] = 0;
                return;
            }

            int val = dy;
            if ((dy > 0 && prev < 0) || (dy < 0 && prev > 0)) {
                val = 0;
                disableFling = true;
            } else {
                disableFling = false;
            }

            int maxx = dp2px(mTargetView.getContext(), 296);
            int minn = dp2px(mTargetView.getContext(), 100);
            int height = Math.min(maxx,
                    max(minn, mTargetView.getLayoutParams().height - (int) (2.5*val )));

            ViewPager vp = (ViewPager) mTargetView;

            int currentItem = vp.getCurrentItem();
            float offset = (height)/(float)dp2px(mTargetView.getContext(), 296);
            ((MyCustomPagerAdapter)vp.getAdapter()).setPageWidth(offset);

            int padding = dp2px(mTargetView.getContext(), 32);
            vp.setPadding(padding + (dp2px(mTargetView.getContext(), 296)-height)/2,
                    0,
                    padding - (dp2px(mTargetView.getContext(), 296)-height)/2,
                    0);

            /*vp.setPageMargin(max(-(int)((dp2px(mTargetView.getContext(), 296+64))/2) ,
                    (int)(dp2px(mTargetView.getContext(), 24) - (dp2px(mTargetView.getContext(), 296)-height))));
            */

            prev = dy;
            if (mTargetView.getLayoutParams().height != height) {
                vp.getAdapter().notifyDataSetChanged();
                vp.setCurrentItem(currentItem, true);

                mTargetView.getLayoutParams().height = height;
                mTargetView.requestLayout();
            }

            if (height == minn || height == maxx) {
                consumed[1] = 0;
            } else {
                consumed[1] = val == 0 ? 2*dy : (int)(2.5*dy);
            }
        }
    }

    @Override
    public void onNestedScroll(CoordinatorLayout coordinatorLayout, AppBarLayout child, View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed) {
    }

    private int calculateFlingDistance(int velocity) {

        // Calculation for fling distance in android
        // SO: https://stackoverflow.com/questions/23952293/detect-fling-distance-android
        // src: https://android.googlesource.com/platform/frameworks/base/+/jb-release/core/java/android/widget/OverScroller.java
        final float ppi = mTargetView.getContext().getResources().getDisplayMetrics().density * 160.0f;
        float PHYSICAL_COEF = SensorManager.GRAVITY_EARTH // g (m/s^2)
                * 39.37f // inch/meter
                * ppi
                * 0.84f; // look and feel tuning
        float DECELERATION_RATE = (float) (Math.log(0.78) / Math.log(0.9));
        float INFLEXION = 0.35f; // Tension lines cross at (INFLEXION, 1)
        float mFlingFriction = ViewConfiguration.getScrollFriction();

        final double l = Math.log(INFLEXION * Math.abs(velocity) / (mFlingFriction * PHYSICAL_COEF));
        final double decelMinusOne = DECELERATION_RATE - 1.0;
        double splineDistance = mFlingFriction * PHYSICAL_COEF * Math.exp(DECELERATION_RATE / decelMinusOne * l);
        int mSplineDistance = (int) (splineDistance * Math.signum(velocity));

        return mSplineDistance;
    }


    @Override
    public boolean onNestedPreFling(CoordinatorLayout coordinatorLayout, AppBarLayout child, View target, float velocityX, float velocityY) {

        Log.e("PRE-FLING", String.valueOf(velocityY) + "");

        if (disableFling) {
            disableFling = false;
            return true;
        }

        if (velocityY >= 0) {
            // We're scrolling up
            if (mTargetView == null) {
                mTargetView = child.findViewWithTag("vp");
            }
            int height = mTargetView.getHeight();

            if (height == dp2px(mTargetView.getContext(), 100)) {
                return false;
            }

            int flingDistance = calculateFlingDistance((int) velocityY);
            final int targetScroll = Math.abs(flingDistance);

            if (targetScroll >= 0) {
                animateOffsetTo(coordinatorLayout, child, target, -targetScroll, velocityY);
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean onNestedFling(final CoordinatorLayout coordinatorLayout,
                                 final AppBarLayout child, View target, float velocityX, float velocityY,
                                 boolean consumed) {

        boolean flung = false;
        mTargetView = child.findViewWithTag("vp");

        if (!consumed) {
            Log.e("DY-SCROLL", "NOT CONSUMED......***");
        } else {
            // If we're scrolling up and the child also consumed the fling. We'll fake scroll
            // up to our 'collapsed' offset
            if (velocityY < 0) {
                // We're scrolling down
                int flingDistance = calculateFlingDistance((int) velocityY);
                int targetScroll = Math.abs(flingDistance);

                waitForRV = true;
                if (targetScroll >= 0) {
                    // If we're currently not expanded more than the target scroll, we'll
                    // animate a fling
                    animateOffsetTo(coordinatorLayout, child, target, targetScroll, velocityY);
                    flung = true;
                }
            }
        }
        return flung;
    }

    private void animateOffsetTo(final CoordinatorLayout coordinatorLayout,
                                 final AppBarLayout child, View target, final int distance, float velocity) {

        final int duration;
        float newVelocity = Math.abs(velocity);
        if (newVelocity > 0) {
            duration = 3 * Math.round(1000 * (Math.abs(distance) / newVelocity));
        } else {
            final float distanceRatio = (float) Math.abs(distance) / child.getHeight();
            duration = (int) ((distanceRatio + 1) * 150);
        }

        animateOffsetWithDuration(coordinatorLayout, child, target, distance, duration, (int) velocity);
    }

    private void animateOffsetWithDuration(final CoordinatorLayout coordinatorLayout,
                                           final AppBarLayout child, final View target,
                                           final int distance, final int duration,
                                           final int velocity) {


        if (mHeightAnimator != null && mHeightAnimator.isRunning()) {
            mHeightAnimator.cancel();
        }
        int height = mTargetView.getLayoutParams().height;

        final int maxx = dp2px(mTargetView.getContext(), 296);
        final int minn = dp2px(mTargetView.getContext(), 100);

        if (mHeightAnimator == null) {

            mHeightAnimator = new ValueAnimator();
            mHeightAnimator.setInterpolator(new LinearInterpolator());
            mHeightAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    int height = mTargetView.getLayoutParams().height;
                    int animatorValue = (int) valueAnimator.getAnimatedValue();
                    int newHeight = Math.min(maxx, max(minn, animatorValue));

                    ViewPager vp = (ViewPager) mTargetView;

                    /*vp.setPageMargin(max(-(int)((dp2px(mTargetView.getContext(), 296+64))/2) ,
                            (int)(dp2px(mTargetView.getContext(), 24) - (dp2px(mTargetView.getContext(), 296)-height))));
                    */

                    int currentItem = vp.getCurrentItem();
                    float offset = (height)/(float)dp2px(mTargetView.getContext(), 296);
                    ((MyCustomPagerAdapter)vp.getAdapter()).setPageWidth(offset);

                    int padding = dp2px(mTargetView.getContext(), 32);
                    vp.setPadding(padding + (dp2px(mTargetView.getContext(), 296)-newHeight)/2,
                            0,
                            padding - (dp2px(mTargetView.getContext(), 296)-newHeight)/2,
                            0);

                    if (mTargetView.getLayoutParams().height != newHeight) {
                        vp.getAdapter().notifyDataSetChanged();
                        vp.setCurrentItem(currentItem, true);

                        mTargetView.getLayoutParams().height = newHeight;
                        mTargetView.requestLayout();
                    }

                }
            });
        } else {
            mHeightAnimator.cancel();
        }

        mHeightAnimator.setDuration(Math.min(duration, MAX_OFFSET_ANIMATION_DURATION));
        int newHeight = Math.min(maxx+50, Math.max(minn-50, height + (int)(5*distance) ));
        mHeightAnimator.setIntValues(height, newHeight);

        mHeightAnimator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {}

            @Override
            public void onAnimationEnd(Animator animator) {
                if (mTargetView.getLayoutParams().height == minn) {
                    ((RecyclerView) target).fling(0, (int) velocity);
                }
            }

            @Override
            public void onAnimationCancel(Animator animator) {}

            @Override
            public void onAnimationRepeat(Animator animator) {}
        });


        if (!waitForRV && height != newHeight) {
            mHeightAnimator.start();
        }
    }


    @Override
    public void onStopNestedScroll(CoordinatorLayout coordinatorLayout, AppBarLayout abl,
                                   View target) {

        final View newTarget = target;

        ((RecyclerView) coordinatorLayout.findViewWithTag("rv")).addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);

                if (newState == RecyclerView.SCROLL_STATE_IDLE && waitForRV) {

                    new Handler().post(new Runnable() {
                        @Override
                        public void run() {
                            if (waitForRV && newTarget instanceof RecyclerView &&
                                    ((LinearLayoutManager) ((RecyclerView) newTarget).getLayoutManager())
                                            .findFirstCompletelyVisibleItemPosition() <= 1) { // was 0 earlier
                                if (mHeightAnimator != null) {
                                    if (mHeightAnimator.isRunning()) {
                                        mHeightAnimator.cancel();
                                    }
                                    mHeightAnimator.start();
                                }
                                waitForRV = false;
                            }
                        }
                    });
                }
            }
        });
    }


    public int dp2px(Context context, int dp ) {
        float m = context.getResources().getDisplayMetrics().density;
        return (int) (dp * m + 0.5f);
    }
}
