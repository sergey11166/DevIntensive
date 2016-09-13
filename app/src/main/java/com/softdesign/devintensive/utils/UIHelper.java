package com.softdesign.devintensive.utils;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Point;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;

/**
 * @author Sergey Vorobyev
 */

public class UIHelper {

    public static int getMinHeight(View v) {
        int heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        int widthMeasureSpec = View.MeasureSpec
                .makeMeasureSpec(getScreenWidth(v), View.MeasureSpec.AT_MOST);

        v.measure(widthMeasureSpec, heightMeasureSpec);
        return v.getMeasuredHeight();
    }

    private static int getScreenWidth(View view) {
        WindowManager manager = (WindowManager) view.getContext().getSystemService(Context.WINDOW_SERVICE);
        Display display = manager.getDefaultDisplay();
        int deviceWidth;
        Point point = new Point();
        display.getSize(point);
        deviceWidth = point.x;
        return deviceWidth;
    }

    public static float getAppBarSize(Context context) {
        final TypedArray styledAttributes = context.getTheme().obtainStyledAttributes(
                new int[]{android.R.attr.actionBarSize});
        float mActionBarSize = styledAttributes.getDimension(0, 0);
        styledAttributes.recycle();

        return mActionBarSize;
    }

    public static int getStatusBarHeight(Context context) {
        int result = 0;
        int resId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resId > 0) {
            result = context.getResources().getDimensionPixelSize(resId);
        }
        return result;
    }
}