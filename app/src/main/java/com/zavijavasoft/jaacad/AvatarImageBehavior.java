package com.zavijavasoft.jaacad;


import android.content.Context;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.design.widget.CoordinatorLayout;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.view.View;

import de.hdodenhof.circleimageview.CircleImageView;


/**
 * Класс, реализующий поведение (Behavior) иконки-аватара в CoordinatorLayout
 */
public class AvatarImageBehavior extends CoordinatorLayout.Behavior<CircleImageView> {


    private final static String TAG = "AvatarImageBehavior";
    private Context context;
    private float customFinalHeight;

    private int startXPosition;
    private float startToolbarPosition;
    private int startYPosition;
    private int finalYPosition;
    private int startHeight;
    private int finalXPosition;
    private float changeBehaviorPoint;


    public AvatarImageBehavior(Context context, AttributeSet attrs) {
        this.context = context;

        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.AvatarImageBehavior);
            customFinalHeight = a.getDimension(R.styleable.AvatarImageBehavior_finalHeight, 0);
            a.recycle();
        }

        init();
    }

    private void init() {
        bindDimensions();
    }

    private void bindDimensions() {
    }


    @Override
    public boolean layoutDependsOn(CoordinatorLayout parent, CircleImageView child, View dependency) {
        return dependency instanceof Toolbar;
    }


    /**
     * Основной метод обратного вызова, определяющий повдение
     *
     * @param parent     - родительский CoordinatorLayout
     * @param child      - объект иконки-аватара
     * @param dependency - объект, от поведения которого зависит поведение иконки - в нашем случае это Toolbar
     * @return
     */
    @Override
    public boolean onDependentViewChanged(CoordinatorLayout parent, CircleImageView child, View dependency) {

        maybeInitProperties(child, dependency);

        final int maxScrollDistance = (int) (startToolbarPosition);
        float expandedPercentageFactor = dependency.getY() / maxScrollDistance;

        if (expandedPercentageFactor < changeBehaviorPoint) {
            //  Тулбар поднимается выше changeBehaviorPoint - движение по горизнотали и масштабирование
            float heightFactor = (changeBehaviorPoint - expandedPercentageFactor) / changeBehaviorPoint;

            float distanceXToSubtract = ((startXPosition - finalXPosition)
                    * heightFactor) + (child.getHeight() / 2);
            float distanceYToSubtract = ((startYPosition - finalYPosition)
                    * (1f - expandedPercentageFactor)) + (child.getHeight() / 2);

            float newY = startYPosition - distanceYToSubtract;
            if (newY < finalYPosition / 2)
                newY = finalYPosition / 2;

            child.setX(startXPosition - distanceXToSubtract);
            child.setY(newY);

            float heightToSubtract = ((startHeight - customFinalHeight) * heightFactor);

            CoordinatorLayout.LayoutParams lp = (CoordinatorLayout.LayoutParams) child.getLayoutParams();
            lp.width = (int) (startHeight - heightToSubtract);
            lp.height = (int) (startHeight - heightToSubtract);
            child.setLayoutParams(lp);
        } else {
            //  Тулбар опускается ниже changeBehaviorPoint. Тут движение только по вертикали
            float distanceYToSubtract = ((startYPosition - finalYPosition)
                    * (1f - expandedPercentageFactor)) + (startHeight / 2);

            float newX = startXPosition - child.getWidth() / 2;
            child.setX(newX);
            child.setY(startYPosition - distanceYToSubtract);

            CoordinatorLayout.LayoutParams lp = (CoordinatorLayout.LayoutParams) child.getLayoutParams();
            lp.width = startHeight;
            lp.height = startHeight;
            child.setLayoutParams(lp);
        }
        return true;
    }

    /**
     * Сохранение некоторых настроек. В оригинальном примере этого поведения, экран был заморожен в
     * портретном положении, поэтому проблем не возникало. При смене ориентации, если тулбар
     * был в верхнем положении (это определялось положением Collapsing toolbar, которое запоминается
     * системой автоматически), настройки сбивались.
     * @param parent
     * @param child
     * @return
     */
    @Override
    public Parcelable onSaveInstanceState(CoordinatorLayout parent, CircleImageView child) {

        Bundle behaviorState = new Bundle();
        behaviorState.putInt("startYPosition", startYPosition);
        behaviorState.putFloat("startToolbarPosition", startToolbarPosition);
        behaviorState.putParcelable("super", super.onSaveInstanceState(parent, child));

        return behaviorState;
    }

    @Override
    public void onRestoreInstanceState(CoordinatorLayout parent, CircleImageView child, Parcelable state) {
        Bundle behaviorState = (Bundle) state;
        behaviorState.setClassLoader(child.getContext().getClassLoader());

        startYPosition = behaviorState.getInt("startYPosition");
        startToolbarPosition = behaviorState.getFloat("startToolbarPosition");
        Parcelable superState = behaviorState.getParcelable("super");

        super.onRestoreInstanceState(parent, child, superState);

    }

    /**
     * Метод инициализации переменных, если они не инициализированы
     * @param child - иконка-аватар
     * @param dependency - тулбар
     */
    private void maybeInitProperties(CircleImageView child, View dependency) {

        if (startYPosition == 0)
            startYPosition = (int) (dependency.getY() - child.getHeight() / 3);

        if (finalYPosition == 0)
            finalYPosition = (dependency.getHeight() / 2);

        if (startHeight == 0)
            startHeight = child.getHeight();

        if (startXPosition == 0)
            startXPosition = (int) (child.getX() + (child.getWidth() / 2));

        if (finalXPosition == 0)
            finalXPosition = context.getResources().getDimensionPixelOffset(R.dimen.abc_action_bar_content_inset_material) + ((int) customFinalHeight / 2);

        if (startToolbarPosition == 0)
            startToolbarPosition = dependency.getY();

        if (changeBehaviorPoint == 0) {
            changeBehaviorPoint = (child.getHeight() - customFinalHeight) / (2f * (startYPosition - finalYPosition));
        }
    }


}
