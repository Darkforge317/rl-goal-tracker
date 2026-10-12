package com.darkforge317.goaltracker.ui;

import com.darkforge317.goaltracker.models.task.ManualTask;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class TaskInteractionStateTest
{
    @Test
    void startsInNormalModeWithoutASelectionSource()
    {
        TaskInteractionState state = new TaskInteractionState();
        assertEquals(TaskInteractionState.Mode.NORMAL, state.getMode());
        assertNull(state.getParentSelectionSource());
    }

    @Test
    void shiftKeyEntersAndLeavesDeleteMode()
    {
        TaskInteractionState state = new TaskInteractionState();
        state.setShiftDown(true);
        assertEquals(TaskInteractionState.Mode.SHIFT_DELETE, state.getMode());
        state.setShiftDown(false);
        assertEquals(TaskInteractionState.Mode.NORMAL, state.getMode());
    }

    @Test
    void parentSelectionTemporarilyTakesPrecedenceOverShiftDelete()
    {
        TaskInteractionState state = new TaskInteractionState();
        ManualTask source = ManualTask.builder().description("source").build();
        state.setShiftDown(true);
        state.beginParentSelection(source);
        state.setShiftDown(false);

        assertEquals(TaskInteractionState.Mode.SET_PARENT, state.getMode());
        assertSame(source, state.getParentSelectionSource());
        state.endParentSelection();
        assertEquals(TaskInteractionState.Mode.NORMAL, state.getMode());
        assertNull(state.getParentSelectionSource());
    }

    @Test
    void endingParentSelectionRestoresHeldShiftDeleteMode()
    {
        TaskInteractionState state = new TaskInteractionState();
        state.beginParentSelection(ManualTask.builder().build());
        state.setShiftDown(true);
        state.endParentSelection();
        assertEquals(TaskInteractionState.Mode.SHIFT_DELETE, state.getMode());
    }

    @Test
    void changingShiftWhileSelectingDoesNotInterruptSelection()
    {
        TaskInteractionState state = new TaskInteractionState();
        ManualTask source = ManualTask.builder().build();
        state.beginParentSelection(source);
        state.setShiftDown(true);
        state.setShiftDown(false);

        assertEquals(TaskInteractionState.Mode.SET_PARENT, state.getMode());
        assertSame(source, state.getParentSelectionSource());
    }

    @Test
    void listenersFireOnlyWhenModeChangesAndCanBeRemoved()
    {
        TaskInteractionState state = new TaskInteractionState();
        AtomicInteger notifications = new AtomicInteger();
        Runnable listener = notifications::incrementAndGet;
        state.addListener(listener);
        state.setShiftDown(false);
        state.setShiftDown(true);
        state.setShiftDown(true);
        state.beginParentSelection(ManualTask.builder().build());
        state.endParentSelection();
        assertEquals(3, notifications.get());

        state.removeListener(listener);
        state.setShiftDown(false);
        assertEquals(3, notifications.get());
    }
}
