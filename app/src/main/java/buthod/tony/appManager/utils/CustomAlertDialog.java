package buthod.tony.appManager.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

/**
 * Custom alert dialog.
 * The main point of this class is to dispatch touch events.
 */
public class CustomAlertDialog extends AlertDialog {

    private static class AlertDialogParams {
        public final Context context;
        public final LayoutInflater inflater;

        public CharSequence title;
        public CharSequence positiveButtonText;
        public DialogInterface.OnClickListener positiveButtonListener;
        public CharSequence negativeButtonText;
        public DialogInterface.OnClickListener negativeButtonListener;
        public CharSequence neutralButtonText;
        public DialogInterface.OnClickListener neutralButtonListener;
        public DialogInterface.OnCancelListener onCancelListener;
        public DialogInterface.OnDismissListener onDismissListener;
        public DialogInterface.OnKeyListener onKeyListener;
        public View view;
        public boolean cancelable;

        public AlertDialogParams(@NonNull Context context) {
            this.context = context;
            this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        public void apply(CustomAlertDialog dialog) {
            if (title != null)
                dialog.setTitle(title);
            if (positiveButtonText != null) {
                dialog.setButton(DialogInterface.BUTTON_POSITIVE, positiveButtonText,
                        positiveButtonListener);
            }
            if (negativeButtonText != null) {
                dialog.setButton(DialogInterface.BUTTON_NEGATIVE, negativeButtonText,
                        negativeButtonListener);
            }
            if (neutralButtonText != null) {
                dialog.setButton(DialogInterface.BUTTON_NEUTRAL, neutralButtonText,
                        neutralButtonListener);
            }
            if (view != null) {
                dialog.setView(view);
            }
        }
    }

    public static class Builder {

        private AlertDialogParams mParams;

        public Builder(@NonNull Context context) {
            mParams = new AlertDialogParams(context);
        }

        public void setTitle(@StringRes int titleId) {
            mParams.title = mParams.context.getText(titleId);
        }

        public void setView(View v) {
            mParams.view = v;
        }

        public void setNegativeButton(CharSequence text, final DialogInterface.OnClickListener listener) {
            mParams.negativeButtonText = text;
            mParams.negativeButtonListener= listener;
        }

        public void setPositiveButton(CharSequence text, final DialogInterface.OnClickListener listener) {
            mParams.positiveButtonText = text;
            mParams.positiveButtonListener= listener;
        }

        public void setNeutralButton(CharSequence text, final DialogInterface.OnClickListener listener) {
            mParams.neutralButtonText = text;
            mParams.neutralButtonListener= listener;
        }

        public void setCancelable(boolean cancelable) {
            mParams.cancelable = cancelable;
        }

        public CustomAlertDialog create() {
            final CustomAlertDialog dialog = new CustomAlertDialog(mParams.context);
            dialog.setCancelable(mParams.cancelable);
            mParams.apply(dialog);
            if (mParams.cancelable) {
                dialog.setCanceledOnTouchOutside(true);
            }
            dialog.setOnCancelListener(mParams.onCancelListener);
            dialog.setOnDismissListener(mParams.onDismissListener);
            if (mParams.onKeyListener != null) {
                dialog.setOnKeyListener(mParams.onKeyListener);
            }
            return dialog;
        }
    }

    protected CustomAlertDialog(@NonNull Context context) {
        super(context);
    }

    @Override
    public boolean dispatchTouchEvent(@NonNull MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
            View v = getCurrentFocus();
            if (v instanceof EditText) {
                int pos[] = new int[2];
                v.getLocationOnScreen(pos);
                float x = event.getRawX(), y = event.getRawY();
                if (x < pos[0] || x > pos[0] + v.getWidth() || y < pos[1] || y > pos[1] + v.getHeight()) {
                    v.clearFocus();
                    InputMethodManager imm =
                            (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null)
                        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                }
            }
        }
        return super.dispatchTouchEvent(event);
    }
}
