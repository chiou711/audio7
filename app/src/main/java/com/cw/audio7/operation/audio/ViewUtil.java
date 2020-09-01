package com.cw.audio7.operation.audio;

import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;

public class ViewUtil {

	public static void measure(@NonNull final View view) {
		final ViewGroup.LayoutParams layoutParams = view.getLayoutParams();

		final int horizontalMode;
		final int horizontalSize;

		switch (layoutParams.width) {
			case ViewGroup.LayoutParams.MATCH_PARENT:
				horizontalMode = View.MeasureSpec.EXACTLY;
				if (view.getParent() instanceof LinearLayout
						&& ((LinearLayout) view.getParent()).getOrientation() == LinearLayout.VERTICAL) {
					ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
					horizontalSize = ((View) view.getParent()).getMeasuredWidth() - lp.leftMargin - lp.rightMargin;
				} else {
					horizontalSize = ((View) view.getParent()).getMeasuredWidth();
				}
				break;
			case ViewGroup.LayoutParams.WRAP_CONTENT:
				horizontalMode = View.MeasureSpec.UNSPECIFIED;
				horizontalSize = 0;
				break;
			default:
				horizontalMode = View.MeasureSpec.EXACTLY;
				horizontalSize = layoutParams.width;
				break;
		}

		final int horizontalMeasureSpec = View.MeasureSpec
				.makeMeasureSpec(horizontalSize, horizontalMode);

		final int verticalMode;
		final int verticalSize;

		switch (layoutParams.height) {
			case ViewGroup.LayoutParams.MATCH_PARENT:
				verticalMode = View.MeasureSpec.EXACTLY;
				if (view.getParent() instanceof LinearLayout
						&& ((LinearLayout) view.getParent()).getOrientation() == LinearLayout.HORIZONTAL) {
					ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
					verticalSize = ((View) view.getParent()).getMeasuredHeight() - lp.topMargin - lp.bottomMargin;
				} else {
					verticalSize = ((View) view.getParent()).getMeasuredHeight();
				}
				break;
			case ViewGroup.LayoutParams.WRAP_CONTENT:
				verticalMode = View.MeasureSpec.UNSPECIFIED;
				verticalSize = 0;
				break;
			default:
				verticalMode = View.MeasureSpec.EXACTLY;
				verticalSize = layoutParams.height;
				break;
		}

		final int verticalMeasureSpec = View.MeasureSpec
				.makeMeasureSpec(verticalSize, verticalMode);

		view.measure(horizontalMeasureSpec, verticalMeasureSpec);
	}

}