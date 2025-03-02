package io.zak.inventory;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

public class SwipeToDeleteCallback extends ItemTouchHelper.SimpleCallback {

    private final Context context;
    private final SwipeToDeleteListener listener;
    private final ColorDrawable background;
    private final Drawable deleteIcon;
    private final int iconMargin;
    private final float swipeThreshold = 0.3f; // Threshold for swipe action
    private final float maxSwipeDistance = 0.3f; // Maximum swipe distance as fraction of view width
    private float lastDX = 0;
    private boolean isElasticActive = false;

    public interface SwipeToDeleteListener {
        void onItemSwiped(int position);
    }

    public SwipeToDeleteCallback(Context context, SwipeToDeleteListener listener) {
        // Only allow left swipe (RIGHT direction)
        super(0, ItemTouchHelper.LEFT);
        this.context = context;
        this.listener = listener;

        // Red background
        this.background = new ColorDrawable(Color.rgb(239, 55, 55)); // Spotify-like red

        // White trash icon
        this.deleteIcon = ContextCompat.getDrawable(context, R.drawable.ic_delete_white);
        this.iconMargin = context.getResources().getDimensionPixelSize(R.dimen.delete_icon_margin);
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
        return false; // We don't support moving items
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
        int position = viewHolder.getAdapterPosition();
        listener.onItemSwiped(position);
    }

    @Override
    public float getSwipeThreshold(@NonNull RecyclerView.ViewHolder viewHolder) {
        return swipeThreshold;
    }

    @Override
    public float getSwipeEscapeVelocity(float defaultValue) {
        return defaultValue * 5f; // Make it harder to trigger swipe
    }

    @Override
    public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder,
                            float dX, float dY, int actionState, boolean isCurrentlyActive) {

        View itemView = viewHolder.itemView;
        int itemHeight = itemView.getHeight();
        int itemWidth = itemView.getWidth();

        // Apply elastic effect when swiping beyond max distance
        float maxDistance = itemWidth * maxSwipeDistance;
        float elasticDX = dX;

        if (dX < -maxDistance) {
            // Apply elastic resistance - logarithmic dampening
            float overshoot = -dX - maxDistance;
            float dampening = (float) (maxDistance * 0.1 * Math.log(1 + overshoot / 100));
            elasticDX = -maxDistance - dampening;
            isElasticActive = true;
        } else {
            isElasticActive = false;
        }

        // Store last dx for animation
        lastDX = elasticDX;

        // Draw the red background
        background.setBounds(
                itemView.getRight() + (int)elasticDX,
                itemView.getTop(),
                itemView.getRight(),
                itemView.getBottom()
        );
        background.draw(c);

        // Calculate position for the delete icon
        int iconTop = itemView.getTop() + (itemHeight - deleteIcon.getIntrinsicHeight()) / 2;
        int iconBottom = iconTop + deleteIcon.getIntrinsicHeight();
        int iconLeft = itemView.getRight() - iconMargin - deleteIcon.getIntrinsicWidth();
        int iconRight = itemView.getRight() - iconMargin;

        // Ensure the icon is visible only when swiping
        if (elasticDX < 0) {
            deleteIcon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
            deleteIcon.draw(c);
        }

        super.onChildDraw(c, recyclerView, viewHolder, elasticDX, dY, actionState, isCurrentlyActive);
    }

    @Override
    public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        // Apply bounce-back animation if needed
        if (isElasticActive) {
            final View itemView = viewHolder.itemView;
            itemView.animate()
                    .translationX(0)
                    .setDuration(300)
                    .setInterpolator(new android.view.animation.OvershootInterpolator(1.2f))
                    .start();
        }
        super.clearView(recyclerView, viewHolder);
    }
}