package com.softdesign.devintensive.ui.views.behaviors;

import android.content.Context;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.util.AttributeSet;
import android.view.View;

/**
 * @author Sergey Vorobyev
 */

@SuppressWarnings("unused")
public class ProfileInfoNestedScrollViewBehavior extends AppBarLayout.ScrollingViewBehavior {

    public ProfileInfoNestedScrollViewBehavior(Context context, AttributeSet attrs) {
    }

    /**
     * @param child Profile info NestedScrollView
     * @param dependency Scores LinearLayout
     */
    @Override
    public boolean onDependentViewChanged(CoordinatorLayout parent, View child, View dependency) {
        int translation = (int) (dependency.getTranslationY() + dependency.getHeight());
        child.setTranslationY(translation);
        return super.onDependentViewChanged(parent, child, dependency);
    }
}
