package buthod.tony.appManager.utils;

import android.content.ClipData;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.RectF;
import android.util.Log;
import android.view.DragEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import java.util.ArrayList;

import buthod.tony.appManager.R;

public class DragAndDropLinearLayout implements View.OnDragListener {
    /** Fields initialized when the class is allocated **/
    private Context mContext;
    private Paint mPaintFeedback;
    private int mOffsetFeedback, mWidthFeedback;

    /** Fields updated each time a dragging starts **/
    private LinearLayout mLayout = null;
    private int mDragViewIndex = -1;
    private View mDragView = null;
    private OnDragAndDropListener mListener;
    private boolean mDropped = false;
    // The Y position of the center of each element in the linear layout.
    private static ArrayList<Float> mChildrenYPosition;
    // Feedback elements
    private View mFeedbackView;
    private RectF mRectFeedback;
    private int mFeedbackIndex;

    /**
     * Constructor.
     * @param context The activity context used to instantiate feedback view.
     * @param paintFeedback The paint to use for the feedback view.
     * @param offsetFeedback The offset of the feedback view on left and right.
     * @param widthFeedback The width of the feedback view.
     */
    public DragAndDropLinearLayout(Context context, Paint paintFeedback, int offsetFeedback,
                                   int widthFeedback) {
        mContext = context;
        // Create the feedback view and add it to the linear layout
        mPaintFeedback = paintFeedback;
        mOffsetFeedback = offsetFeedback;
        mWidthFeedback = widthFeedback;
    }
    public DragAndDropLinearLayout(Context context) {
        this(context, null, 10, 4);
        mPaintFeedback = new Paint();
        mPaintFeedback.setColor(context.getResources().getColor(R.color.colorAccent));
        mPaintFeedback.setStyle(Paint.Style.FILL);
    }

    /**
     * Start the dragging process of an element inside the linear layout.
     * @param layout The linear layout. Must contain the drag view.
     * @param dragView The view to drag contained in the linear layout.
     * @param listener The listener used when the view is dropped.
     */
    public void StartDragging(LinearLayout layout, View dragView, OnDragAndDropListener listener) {
        mLayout = layout;
        mDragView = dragView;
        mDragViewIndex = layout.indexOfChild(mDragView);
        mListener = listener;
        if (mDragViewIndex == -1)
            return;
        /** Update feedback view with layout width **/
        mRectFeedback = new RectF(mOffsetFeedback, 0,
                mLayout.getWidth() - 2 * mOffsetFeedback, mLayout.getHeight());
        Log.d("Debug", "New feedback view");
        mFeedbackView = new View(mContext) {
            @Override
            public void onDraw(Canvas canvas) {
                canvas.drawRect(mRectFeedback, mPaintFeedback);
            }
        };
        mFeedbackView.setLayoutParams(new RelativeLayout.LayoutParams(
                mLayout.getWidth(), mWidthFeedback
        ));
        /** Initialize the drag and drop **/
        mDropped = false;
        computeChildrenYPosition();
        layout.setOnDragListener(this);
        StepDragShadowBuilder dragShadowBuilder = new StepDragShadowBuilder(mDragView);
        mDragView.startDrag(ClipData.newPlainText("", ""), dragShadowBuilder, mDragView, 0);
        mLayout.removeView(mDragView);
    }

    /**
     * Compute the y position for each child in the layout without taking into account the dragged view.
     */
    private void computeChildrenYPosition() {
        int childCount = mLayout.getChildCount();
        mChildrenYPosition = new ArrayList<>();
        // For each view with index lower than the dragged view, the position is unchanged.
        for (int i = 0; i < mDragViewIndex; ++i) {
            View v = mLayout.getChildAt(i);
            mChildrenYPosition.add(v.getY() + v.getHeight() / 2.0f);
        }
        if (mDragViewIndex + 1 < childCount) {
            // For views with a greater index, need to take off the offset after removing the drag view.
            float offset = mLayout.getChildAt(mDragViewIndex + 1).getY() - mDragView.getY();
            for (int i = mDragViewIndex + 1; i < childCount; ++i) {
                View v = mLayout.getChildAt(i);
                mChildrenYPosition.add(v.getY() + v.getHeight() / 2.0f - offset);
            }
        }
    }

    /**
     * Handle events when a drag is detected.
     * @param view The view on which the drag handler has been attached.
     *             In this class, it corresponds to the linear layout.
     * @param event The drag event.
     */
    public boolean onDrag(View view, DragEvent event) {
        int action = event.getAction();
        switch (action) {
            case DragEvent.ACTION_DRAG_STARTED:
                if (mFeedbackView.getParent() != null)
                    ((LinearLayout)mFeedbackView.getParent()).removeView(mFeedbackView);
                mLayout.addView(mFeedbackView, mDragViewIndex);
                mFeedbackIndex = mDragViewIndex;
                break;
            case DragEvent.ACTION_DRAG_ENTERED:
                mFeedbackView.setVisibility(View.VISIBLE);
                break;
            case DragEvent.ACTION_DRAG_LOCATION:
                int index = getDropIndexFromPosition(event.getY());
                if (index != mFeedbackIndex) {
                    mFeedbackIndex = index;
                    mLayout.removeView(mFeedbackView);
                    mLayout.addView(mFeedbackView, mFeedbackIndex);
                }
                break;
            case DragEvent.ACTION_DRAG_EXITED:
                mFeedbackView.setVisibility(View.GONE);
                break;
            case DragEvent.ACTION_DROP:
                // Element dropped in the layout, then change its position.
                mDropped = true;
                int dropIndex = getDropIndexFromPosition(event.getY());
                mLayout.addView(mDragView, dropIndex);
                if (mListener != null)
                    mListener.onDrop(mDragViewIndex, dropIndex);
                break;
            case DragEvent.ACTION_DRAG_ENDED:
                // Remove the feedback view.
                Log.d("Debug", "Action drag ended");
                mLayout.removeView(mFeedbackView);
                mFeedbackView = null;
                if (!mDropped) {
                    // The view was dropped outside the layout, insert the view back at the same position.
                    mLayout.addView(mDragView, mDragViewIndex);
                }
                if (mListener != null)
                    mListener.onEnd();
                break;
        }
        return true;
    }

    /**
     * Get the dropped index position from y positions.
     * TODO : improve performances by started from the feedback index.
     * @param y The y position in the layout.
     */
    private int getDropIndexFromPosition(float y) {
        int i = 0;
        int size = mChildrenYPosition.size();
        while (i < size && y > mChildrenYPosition.get(i))
            i++;
        return i;
    }

    /**
     * Custom DragShadowBuilder for step drag and drop.
     */
    public class StepDragShadowBuilder extends View.DragShadowBuilder {

        private Paint mRectPaint;

        public StepDragShadowBuilder(View view)
        {
            super(view);
            mRectPaint = new Paint();
            mRectPaint.setStyle(Paint.Style.FILL);
            mRectPaint.setStrokeWidth(10.0f);
            mRectPaint.setColor(Color.argb(85, 0, 125, 210));
        }

        @Override
        public void onProvideShadowMetrics(Point shadowSize, Point shadowTouchPoint)
        {
            super.onProvideShadowMetrics(
                    shadowSize,
                    shadowTouchPoint);
        }

        @Override
        public void onDrawShadow(Canvas canvas)
        {
            canvas.drawRect(new RectF(0, 0, canvas.getWidth(), canvas.getHeight()), mRectPaint);
            super.onDrawShadow(canvas);
        }
    }

    public static class OnDragAndDropListener {

        /**
         * Called when the dragged element is dropped in the view with a different position.
         * @param dragIndex The index of the view when the drag began.
         * @param dropIndex The index where the dropped view is inserted.
         */
        public void onDrop(int dragIndex, int dropIndex) {}

        /**
         * Called each time when the drag and drop ends.
         */
        public void onEnd() {}
    }
}
