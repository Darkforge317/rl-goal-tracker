package com.darkforge317.goaltracker.ui.components;

import org.junit.jupiter.api.Test;

import javax.swing.JPanel;
import java.awt.Component;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DragInsertionGeometryTest
{
    @Test
    void measuresActualAdjacentRowGapBeforeTheGroupIsRemovedFromLayout()
    {
        List<Component> rows = Arrays.asList(row(0, 20), row(25, 20), row(50, 20));
        assertEquals(5, DragInsertionGeometry.measureRowGap(rows));
        assertEquals(0, DragInsertionGeometry.measureRowGap(Collections.singletonList(row(0, 20))));
        assertEquals(0, DragInsertionGeometry.measureRowGap(Collections.emptyList()));
    }

    @Test
    void insertionTargetsRemainCorrectWhenRowsAfterPlaceholderAreShifted()
    {
        // A 40px dragged subtree occupies slot 1; later remaining rows are shifted
        // by subtree height plus the actual 2px row gap.
        List<Component> remaining = Arrays.asList(row(0, 20), row(64, 20), row(86, 20));

        assertEquals(22, DragInsertionGeometry.insertionY(1, 1, remaining, 40, 2));
        assertEquals(44, DragInsertionGeometry.insertionY(2, 1, remaining, 40, 2));
        assertEquals(66, DragInsertionGeometry.insertionY(3, 1, remaining, 40, 2));
    }

    @Test
    void insertionTargetsWorkWhenDraggedGroupStartsAtEnd()
    {
        List<Component> remaining = Arrays.asList(row(0, 20), row(22, 20), row(44, 20));
        assertEquals(22, DragInsertionGeometry.insertionY(1, 3, remaining, 40, 2));
        assertEquals(66, DragInsertionGeometry.insertionY(3, 3, remaining, 40, 2));
    }

    @Test
    void nearestSlotChoosesClosestLegalBoundaryAndClampRespectsLimits()
    {
        List<Component> remaining = Arrays.asList(row(0, 20), row(64, 20), row(86, 20));
        assertEquals(1, DragInsertionGeometry.nearestSlotByY(30, Arrays.asList(1, 3), 1, remaining, 40, 2));
        assertEquals(3, DragInsertionGeometry.nearestSlotByY(60, Arrays.asList(1, 3), 1, remaining, 40, 2));
        assertEquals(10, DragInsertionGeometry.clamp(5, 10, 30));
        assertEquals(30, DragInsertionGeometry.clamp(40, 10, 30));
        assertEquals(20, DragInsertionGeometry.clamp(20, 10, 30));
    }

    private static JPanel row(int y, int height)
    {
        JPanel panel = new JPanel();
        panel.setBounds(0, y, 100, height);
        return panel;
    }
}
