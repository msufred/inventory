package org.gemseeker.app.views.frameworks;

import javafx.scene.Node;
import javafx.scene.control.SplitPane;

import java.util.ArrayList;
import java.util.List;

public class SplitController {

    public enum Target { FIRST, LAST }

    private final SplitPane splitPane;
    private final Target target;
    private final Node targetNode; // Node to show/hide
    private double dividerPosition; // divider position backup

    public SplitController(SplitPane splitPane, Target target) {
        assert splitPane != null;
        assert splitPane.getItems().size() >= 1;

        this.splitPane = splitPane;
        this.target = target;

        final List<Node> children = splitPane.getItems();
        final int targetIndex = target == Target.FIRST ? 0 : children.size() - 1;
        this.targetNode = children.get(targetIndex);
    }

    public void showTarget() {
        if (!isTargetVisible()) {
            // Put back the target node back in the split pane items
            if (target == Target.FIRST) {
                splitPane.getItems().add(0, targetNode);
            } else {
                splitPane.getItems().add(targetNode);
            }

            // Restore the target divider position if any
            final List<SplitPane.Divider> dividers = splitPane.getDividers();
            if (!dividers.isEmpty() && dividerPosition != -1.0) {
                final SplitPane.Divider divider = getTargetDivider();
                assert divider != null;
                divider.setPosition(dividerPosition);
            }
        }
    }

    public void hideTarget() {
        if (isTargetVisible()) {
            final List<SplitPane.Divider> dividers = splitPane.getDividers();
            final List<Double> positionsList = toList(splitPane.getDividerPositions());

            // Backup target divider positions (if any) so we can restore
            // on showing
            final SplitPane.Divider targetDivider = getTargetDivider();
            if (targetDivider != null) {
                dividerPosition = targetDivider.getPosition();
                int targetDividerIndex = target == Target.FIRST ? 0 : dividers.size() - 1;
                positionsList.remove(targetDividerIndex);
            }

            // Removes the target node from the split pane items
            splitPane.getItems().remove(targetNode);

            // Set back remaining divider positions if any
            if (!positionsList.isEmpty()) {
                double[] positionsArray = toArray(positionsList);
                splitPane.setDividerPositions(positionsArray);
            }
        }
    }

    /**
     * Toggle show or hide target
     */
    public void toggleTarget() {
        if (isTargetVisible()) {
            hideTarget();
        } else {
            showTarget();
        }
    }

    public void setTargetVisible(boolean visible) {
        if (visible) {
            showTarget();
        } else {
            hideTarget();
        }
    }

    public boolean isTargetVisible() {
        return splitPane.getItems().contains(targetNode);
    }

    private SplitPane.Divider getTargetDivider() {
        final SplitPane.Divider divider;
        final List<SplitPane.Divider> dividers = splitPane.getDividers();
        if (!dividers.isEmpty()) {
            if (target == Target.FIRST) {
                divider = dividers.get(0);
            } else {
                divider = dividers.get(dividers.size() - 1);
            }
        } else {
            divider = null;
        }
        return divider;
    }

    private List<Double> toList(double[] array) {
        final List<Double> list = new ArrayList<>();
        for (double d : array) {
            list.add(d);
        }
        return list;
    }

    private double[] toArray(List<Double> list) {
        final double[] array = new double[list.size()];
        for (int i=0; i<list.size(); i++) {
            array[i] = list.get(i);
        }
        return array;
    }
}
