package com.darkforge317.goaltracker.ui.inputs;

import com.darkforge317.goaltracker.GoalTrackerPlugin;
import com.darkforge317.goaltracker.models.Goal;
import com.darkforge317.goaltracker.models.task.CombatLevelTask;

import javax.swing.JSpinner;
import javax.swing.SwingConstants;
import javax.swing.SpinnerNumberModel;
import java.awt.BorderLayout;

/** Input for creating a combat-level goal. */
public final class CombatLevelTaskInput extends TaskInput
{
    static final int MIN_COMBAT_LEVEL = 3;
    static final int MAX_COMBAT_LEVEL = 126;
    static final double COMBAT_LEVEL_STEP = 0.25;

    private final JSpinner target = new JSpinner(new SpinnerNumberModel(
        (double) MAX_COMBAT_LEVEL, (double) MIN_COMBAT_LEVEL, (double) MAX_COMBAT_LEVEL, COMBAT_LEVEL_STEP)
    {
        @Override public void setValue(Object value)
        {
            if (!(value instanceof Number))
            {
                throw new IllegalArgumentException("Combat level must be between 3 and 126.");
            }
            Number number = (Number) value;
            double target = number.doubleValue();
            if (!Double.isFinite(target) || target < MIN_COMBAT_LEVEL || target > MAX_COMBAT_LEVEL
                || Math.abs(target * 100 - Math.rint(target * 100)) > 0.000001)
            {
                throw new IllegalArgumentException("Combat level must be between 3 and 126, with at most two decimal places.");
            }
            super.setValue(target);
        }
    });

    public CombatLevelTaskInput(GoalTrackerPlugin plugin, Goal goal)
    {
        super(plugin, goal, "Combat level");
        JSpinner.NumberEditor editor = new JSpinner.NumberEditor(target, "0.00");
        editor.getTextField().setColumns(5);
        editor.getTextField().setHorizontalAlignment(SwingConstants.LEFT);
        target.setEditor(editor);
        target.setToolTipText("Combat level 3–126; up to two decimal places");
        getInputRow().add(target, BorderLayout.CENTER);
    }

    @Override
    protected void submit()
    {
        addTask(CombatLevelTask.builder().targetCombatLevel(((Number) target.getValue()).doubleValue()).build());
    }

    @Override
    protected void reset()
    {
        target.setValue(MAX_COMBAT_LEVEL);
    }

    JSpinner getTargetSpinner()
    {
        return target;
    }
}
