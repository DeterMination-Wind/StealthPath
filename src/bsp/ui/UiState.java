package bsp.ui;

import bsp.core.model.GridPoint;

/**
 * Cross-package UI state: the draggable target point used by the
 * "player -> target point" goal source.
 */
public final class UiState{
    private static GridPoint targetPoint;

    private UiState(){}

    public static GridPoint targetPoint(){
        return targetPoint;
    }

    public static void targetPoint(GridPoint p){
        targetPoint = p;
    }
}
