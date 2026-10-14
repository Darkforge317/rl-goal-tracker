package com.darkforge317.goaltracker.ui.inputs;

import com.darkforge317.goaltracker.GoalTrackerPlugin;
import com.darkforge317.goaltracker.models.Goal;
import com.darkforge317.goaltracker.models.task.QuestPointsTask;

import javax.swing.JFormattedTextField;
import javax.swing.JOptionPane;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.text.DefaultFormatter;
import javax.swing.text.DefaultFormatterFactory;
import java.awt.BorderLayout;
import java.text.ParseException;

/** Input for creating a Quest point goal. */
public final class QuestPointsTaskInput extends TaskInput
{
    private final JSpinner target = createTargetSpinner(QuestPointsTask.MAX_QUEST_POINTS);

    public QuestPointsTaskInput(GoalTrackerPlugin plugin, Goal goal)
    {
        super(plugin, goal, "Quest points");
        configureEditor(target);
        target.setToolTipText("Whole number of Quest points (" + QuestPointsTask.MIN_QUEST_POINTS
            + "–" + QuestPointsTask.MAX_QUEST_POINTS + ")");
        getInputRow().add(target, BorderLayout.CENTER);
    }

    @Override
    protected void submit()
    {
        try
        {
            target.commitEdit();
        }
        catch (java.text.ParseException | IllegalArgumentException ex)
        {
            JOptionPane.showMessageDialog(this,
                "Enter a whole number from " + QuestPointsTask.MIN_QUEST_POINTS + " to " + QuestPointsTask.MAX_QUEST_POINTS + ".",
                "Invalid Quest point target", JOptionPane.ERROR_MESSAGE);
            return;
        }
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

    public static void configureEditor(JSpinner spinner)
    {
        JSpinner.NumberEditor editor = new JSpinner.NumberEditor(spinner, "0");
        JFormattedTextField field = editor.getTextField();
        field.setColumns(5);
        field.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);

        DefaultFormatter formatter = new DefaultFormatter()
        {
            @Override
            public Object stringToValue(String text) throws ParseException
            {
                if (text == null || text.isEmpty()) return null;
                if (!text.matches("[0-9]+"))
                {
                    throw new ParseException("Quest point targets must be whole numbers.", 0);
                }
                try
                {
                    int points = Integer.parseInt(text);
                    if (points < QuestPointsTask.MIN_QUEST_POINTS || points > QuestPointsTask.MAX_QUEST_POINTS)
                    {
                        throw new ParseException("Quest point target is outside the supported range.", 0);
                    }
                    return points;
                }
                catch (NumberFormatException ex)
                {
                    throw new ParseException("Quest point target is outside the supported range.", 0);
                }
            }

            @Override
            public String valueToString(Object value)
            {
                return value == null ? "" : value.toString();
            }
        };
        formatter.setAllowsInvalid(false);
        formatter.setCommitsOnValidEdit(false);
        field.setFormatterFactory(new DefaultFormatterFactory(formatter));
        spinner.setEditor(editor);
    }
}
