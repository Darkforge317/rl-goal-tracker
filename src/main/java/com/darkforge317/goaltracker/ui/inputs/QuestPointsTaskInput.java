package com.darkforge317.goaltracker.ui.inputs;

import com.darkforge317.goaltracker.GoalTrackerPlugin;
import com.darkforge317.goaltracker.models.Goal;
import com.darkforge317.goaltracker.models.task.QuestPointsTask;

import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import java.awt.BorderLayout;

/** Input for creating a Quest point goal. */
public final class QuestPointsTaskInput extends TaskInput
{
    private final JSpinner target = createTargetSpinner(QuestPointsTask.MAX_QUEST_POINTS);

    public QuestPointsTaskInput(GoalTrackerPlugin plugin, Goal goal)
    {
        super(plugin, goal, "Quest points");
        JSpinner.NumberEditor editor = new JSpinner.NumberEditor(target, "0");
        editor.getTextField().setColumns(5);
        editor.getTextField().setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        target.setEditor(editor);
        target.setToolTipText("Quest points (" + QuestPointsTask.MIN_QUEST_POINTS + "–" + QuestPointsTask.MAX_QUEST_POINTS + ")");
        getInputRow().add(target, BorderLayout.CENTER);
    }

    @Override
    protected void submit()
    {
        addTask(QuestPointsTask.builder().targetQuestPoints(((Number) target.getValue()).intValue()).build());
    }

    @Override
    protected void reset()
    {
        target.setValue(QuestPointsTask.MAX_QUEST_POINTS);
    }

    JSpinner getTargetSpinner()
    {
        return target;
    }

    public static JSpinner createTargetSpinner(int value)
    {
        int initialValue = Math.max(QuestPointsTask.MIN_QUEST_POINTS, Math.min(QuestPointsTask.MAX_QUEST_POINTS, value));
        SpinnerNumberModel model = new SpinnerNumberModel(
            initialValue, QuestPointsTask.MIN_QUEST_POINTS, QuestPointsTask.MAX_QUEST_POINTS, 1)
        {
            @Override
            public void setValue(Object value)
            {
                if (!(value instanceof Number))
                {
                    throw new IllegalArgumentException("Quest points must be a whole number within the supported range.");
                }
                long points = ((Number) value).longValue();
                if (points < QuestPointsTask.MIN_QUEST_POINTS || points > QuestPointsTask.MAX_QUEST_POINTS
                    || ((Number) value).doubleValue() != points)
                {
                    throw new IllegalArgumentException("Quest points must be a whole number within the supported range.");
                }
                super.setValue((int) points);
            }
        };
        return new JSpinner(model);
    }
}
