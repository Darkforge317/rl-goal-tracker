package com.darkforge317.goaltracker.ui.components;

import java.awt.Component;
import java.util.List;

/** Pure geometry helpers used by the grouped task drag pane. */
final class DragInsertionGeometry
{
    private DragInsertionGeometry() {}

    static int measureRowGap(List<? extends Component> rows)
    {
        if (rows.size() < 2) return 0;
        return Math.max(0, rows.get(1).getY() - rows.get(0).getY() - rows.get(0).getHeight());
    }

    static int insertionY(int index, int dragIndex, List<? extends Component> remaining,
                          int groupHeight, int rowGap)
    {
        if (index < dragIndex && index < remaining.size()) return remaining.get(index).getY();
        if (index == dragIndex)
        {
            if (index == 0) return 0;
            Component previous = remaining.get(Math.min(index - 1, remaining.size() - 1));
            return previous.getY() + previous.getHeight() + rowGap;
        }
        if (index < remaining.size()) return remaining.get(index).getY() - groupHeight - rowGap;
        if (remaining.isEmpty()) return 0;
        Component last = remaining.get(remaining.size() - 1);
        return last.getY() + last.getHeight() - groupHeight;
    }

    static int nearestSlotByY(int y, List<Integer> allowedSlots, int dragIndex,
                              List<? extends Component> remaining, int groupHeight, int rowGap)
    {
        int best = allowedSlots.get(0);
        int distance = Math.abs(y - insertionY(best, dragIndex, remaining, groupHeight, rowGap));
        for (int slot : allowedSlots)
        {
            int nextDistance = Math.abs(y - insertionY(slot, dragIndex, remaining, groupHeight, rowGap));
            if (nextDistance < distance)
            {
                best = slot;
                distance = nextDistance;
            }
        }
        return best;
    }

    static int clamp(int value, int min, int max)
    {
        return Math.max(min, Math.min(value, max));
    }
}
