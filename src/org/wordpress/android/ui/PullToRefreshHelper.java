package org.wordpress.android.ui;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.view.View;

import org.wordpress.android.R;
import org.wordpress.android.ui.PullToRefreshHeaderTransformer.OnTopScrollChangedListener;
import org.wordpress.android.util.DisplayUtils;

import uk.co.senab.actionbarpulltorefresh.extras.actionbarsherlock.PullToRefreshLayout;
import uk.co.senab.actionbarpulltorefresh.library.ActionBarPullToRefresh;
import uk.co.senab.actionbarpulltorefresh.library.ActionBarPullToRefresh.SetupWizard;
import uk.co.senab.actionbarpulltorefresh.library.Options;
import uk.co.senab.actionbarpulltorefresh.library.listeners.OnRefreshListener;
import uk.co.senab.actionbarpulltorefresh.library.viewdelegates.ViewDelegate;

public class PullToRefreshHelper implements OnRefreshListener {
    private static final String NEED_PTR_TIP = "NEED_PTR_TIP";
    private PullToRefreshHeaderTransformer mHeaderTransformer;
    private PullToRefreshLayout mPullToRefreshLayout;
    private RefreshListener mRefreshListener;
    private OnTopMessage mOnTopMessage;
    private boolean mShowTip;
    private boolean mTipShouldBeVisible;
    private Context mContext;

    public PullToRefreshHelper(Activity activity, PullToRefreshLayout pullToRefreshLayout, RefreshListener listener) {
        init(activity, pullToRefreshLayout, listener, null);
    }

    public PullToRefreshHelper(Activity activity, PullToRefreshLayout pullToRefreshLayout, RefreshListener listener,
                               java.lang.Class<?> viewClass) {
        init(activity, pullToRefreshLayout, listener, viewClass);
    }

    public void init(Activity activity, PullToRefreshLayout pullToRefreshLayout, RefreshListener listener,
                     java.lang.Class<?> viewClass) {
        mContext = activity;
        mRefreshListener = listener;
        mPullToRefreshLayout = pullToRefreshLayout;
        mHeaderTransformer = new PullToRefreshHeaderTransformer();
        SetupWizard setupWizard = ActionBarPullToRefresh.from(activity).options(Options.create().headerTransformer(
                mHeaderTransformer).build()).allChildrenArePullable().listener(this);
        if (viewClass != null) {
            setupWizard.useViewDelegate(viewClass, new ViewDelegate() {
                        @Override
                        public boolean isReadyForPull(View view, float v, float v2) {
                            return true;
                        }
                    }
            );
        }
        setupWizard.setup(mPullToRefreshLayout);
        mHeaderTransformer.setOnTopScrollChangedListener(new OnTopScrollChangedListener() {
            @Override
            public void onTopScrollChanged(boolean scrolledOnTop) {
                if (scrolledOnTop) {
                    showTip();
                } else {
                    hideTipTemporarily();
                }
            }
        });
        createTipView(activity);
    }

    public void setRefreshing(boolean refreshing) {
        mHeaderTransformer.setShowProgressBarOnly(refreshing);
        mPullToRefreshLayout.setRefreshing(refreshing);
    }

    @Override
    public void onRefreshStarted(View view) {
        mRefreshListener.onRefreshStarted(view);
        hideTip();
    }

    public interface RefreshListener {
        public void onRefreshStarted(View view);
    }

    public void hideTipTemporarily() {
        if (mShowTip && mOnTopMessage != null && mPullToRefreshLayout.isEnabled()) {
            mOnTopMessage.hideAnimated();
        }
        mTipShouldBeVisible = false;
    }

    public void showTip() {
        if (mShowTip && mOnTopMessage != null && mPullToRefreshLayout.isEnabled()) {
            mOnTopMessage.showAnimated();
        }
        mTipShouldBeVisible = true;
    }

    public void setEnabled(boolean enabled) {
        if (mTipShouldBeVisible && !enabled) {
            hideTipTemporarily();
        }
        mPullToRefreshLayout.setEnabled(enabled);
        if (mTipShouldBeVisible && enabled) {
            showTip();
        }
    }

    private void hideTip() {
        if (mShowTip && mOnTopMessage != null) {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
            Editor editor = preferences.edit();
            editor.putBoolean(NEED_PTR_TIP, false);
            editor.commit();
            mOnTopMessage.hideAnimated();
            mShowTip = false;
            mTipShouldBeVisible = false;
        }
    }

    private void createTipView(Activity activity) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        mShowTip = preferences.getBoolean(NEED_PTR_TIP, true);
        if (mShowTip) {
            mOnTopMessage = new OnTopMessage(activity, mPullToRefreshLayout);
            if (DisplayUtils.hasActionBarOverlay(activity.getWindow())) {
                mOnTopMessage.setTopMargin(DisplayUtils.getActionBarHeight(mContext) +
                                           activity.getResources().getDimensionPixelOffset(R.dimen.ptr_tip_margin_top));
            }
            mOnTopMessage.setMessage(activity.getString(R.string.ptr_tip_message));
            mOnTopMessage.show();
            mTipShouldBeVisible = true;
        }
    }
}
