package com.darkforge317.goaltracker.models;

import com.darkforge317.goaltracker.models.task.ManualTask;
import com.darkforge317.goaltracker.models.task.Task;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ActionHistoryTest
{
    @Test
    void undoRedoMovesActionsBetweenStacks()
    {
        ActionHistory history = new ActionHistory();
        List<String> state = new ArrayList<>();
        ActionHistory.Action action = action(() -> state.add("changed"), () -> state.clear());

        history.push(action);
        assertTrue(history.hasUndo());
        assertFalse(history.hasRedo());
        history.undo();
        assertTrue(state.isEmpty());
        assertFalse(history.hasUndo());
        assertTrue(history.hasRedo());
        history.redo();
        assertEquals(Arrays.asList("changed"), state);
        assertTrue(history.hasUndo());
        assertFalse(history.hasRedo());
    }

    @Test
    void pushingNewActionClearsRedoHistory()
    {
        ActionHistory history = new ActionHistory();
        history.push(action(() -> {}, () -> {}));
        history.undo();
        assertTrue(history.hasRedo());
        history.push(action(() -> {}, () -> {}));
        assertFalse(history.hasRedo());
        assertTrue(history.hasUndo());
    }

    @Test
    void emptyUndoAndRedoAreNoOps()
    {
        ActionHistory history = new ActionHistory();
        assertDoesNotThrow(history::undo);
        assertDoesNotThrow(history::redo);
        assertFalse(history.hasUndo());
        assertFalse(history.hasRedo());
    }

    @Test
    void clearEmptiesBothStacksAndNotifiesListeners()
    {
        ActionHistory history = new ActionHistory();
        AtomicInteger notifications = new AtomicInteger();
        history.addListener(notifications::incrementAndGet);
        history.push(action(() -> {}, () -> {}));
        history.undo();
        history.clear();
        assertFalse(history.hasUndo());
        assertFalse(history.hasRedo());
        assertEquals(3, notifications.get());
    }

    @Test
    void removeTasksActionRestoresOriginalPositionsAndCanRedo()
    {
        Task a = task("A");
        Task b = task("B");
        Task c = task("C");
        Task d = task("D");
        List<Task> tasks = new ArrayList<>(Arrays.asList(a, b, c, d));
        RemoveTasksAction action = new RemoveTasksAction(tasks, Arrays.asList(b, c));

        action.redo();
        assertEquals(Arrays.asList(a, d), tasks);
        action.undo();
        assertEquals(Arrays.asList(a, b, c, d), tasks);
        action.redo();
        assertEquals(Arrays.asList(a, d), tasks);
    }

    @Test
    void reorderTaskActionRestoresItsOldAndNewIndices()
    {
        Task a = task("A");
        Task b = task("B");
        Task c = task("C");
        List<Task> tasks = new ArrayList<>(Arrays.asList(a, b, c));
        ReorderTaskAction action = new ReorderTaskAction(tasks, a, 0, 2);

        action.redo();
        assertEquals(Arrays.asList(b, c, a), tasks);
        action.undo();
        assertEquals(Arrays.asList(a, b, c), tasks);
    }

    private static ActionHistory.Action action(Runnable redo, Runnable undo)
    {
        return new ActionHistory.Action()
        {
            @Override public void undo() { undo.run(); }
            @Override public void redo() { redo.run(); }
        };
    }

    private static ManualTask task(String name)
    {
        return ManualTask.builder().description(name).build();
    }
}
