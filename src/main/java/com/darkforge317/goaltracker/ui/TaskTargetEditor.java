package com.darkforge317.goaltracker.ui;

import com.darkforge317.goaltracker.GoalTrackerPlugin;
import com.darkforge317.goaltracker.models.Goal;
import com.darkforge317.goaltracker.models.enums.Status;
import com.darkforge317.goaltracker.models.task.*;
import net.runelite.api.Skill;
import com.darkforge317.goaltracker.ui.components.SkillTargetFields;

import javax.swing.*;
import java.awt.*;
import java.util.Locale;

/** Edits targets in place so their position, children and collapsed state survive. */
final class TaskTargetEditor
{
    static boolean supports(Task task)
    {
        return task instanceof SkillLevelTask || task instanceof SkillXpTask || task instanceof ItemTask;
    }

    static int parseTarget(String value, int maximum)
    {
        String text = value.trim().toLowerCase(Locale.ROOT);
        long multiplier = 1;
        if (text.endsWith("k") || text.endsWith("m"))
        {
            multiplier = text.endsWith("k") ? 1000 : 1000000;
            text = text.substring(0, text.length() - 1);
        }
        if (!text.matches("[0-9]+")) throw new IllegalArgumentException();
        long result = Math.multiplyExact(Long.parseLong(text), multiplier);
        if (result < 1 || result > maximum) throw new IllegalArgumentException();
        return (int) result;
    }

    /** Preserve list placement and hierarchy when switching between level and exact XP targets. */
    static Task retargetSkill(Task task, boolean level, int value)
    {
        Skill skill = task instanceof SkillLevelTask ? ((SkillLevelTask) task).getSkill()
            : ((SkillXpTask) task).getSkill();
        Task result;
        if (level)
        {
            SkillLevelTask target = task instanceof SkillLevelTask ? (SkillLevelTask) task
                : SkillLevelTask.builder().build();
            target.setSkill(skill);
            target.setTargetSkillLevel(value);
            target.setStatus(target.hasReachedTargetLevel() ? Status.COMPLETED : Status.NOT_STARTED);
            result = target;
        }
        else
        {
            SkillXpTask target = task instanceof SkillXpTask ? (SkillXpTask) task
                : SkillXpTask.builder().build();
            target.setSkill(skill);
            target.setTargetSkillXp(value);
            target.setStatus(target.hasReachedTargetXp() ? Status.COMPLETED : Status.NOT_STARTED);
            result = target;
        }
        result.setIndentLevel(task.getIndentLevel());
        result.setCollapsed(task.isCollapsed());
        return result;
    }

    static JSpinner createQuantitySpinner(int value)
    {
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(Math.max(1, value), 1, Integer.MAX_VALUE, 1));
        JSpinner.NumberEditor editor = new JSpinner.NumberEditor(spinner, "0");
        JFormattedTextField field = editor.getTextField();
        field.setColumns(12);
        field.setFormatterFactory(new javax.swing.text.DefaultFormatterFactory(new JFormattedTextField.AbstractFormatter() {
            @Override public Object stringToValue(String text) throws java.text.ParseException
            {
                try { return parseTarget(text, Integer.MAX_VALUE); }
                catch (IllegalArgumentException | ArithmeticException ex)
                {
                    throw new java.text.ParseException("Enter a positive whole quantity (k/m allowed).", 0);
                }
            }
            @Override public String valueToString(Object number)
            {
                return number == null ? "" : number.toString();
            }
        }));
        field.getAccessibleContext().setAccessibleName("Target quantity");
        field.setToolTipText("Quantity 1–2147483647; accepts k/m suffixes");
        spinner.setEditor(editor);
        return spinner;
    }

    static void open(Component parent, GoalTrackerPlugin plugin, Goal goal, Task task)
    {
        if (!supports(task)) return;
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(parent), "Edit goal", Dialog.ModalityType.MODELESS);
        JPanel fields = new JPanel(new BorderLayout(0, 12));
        fields.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        boolean level = task instanceof SkillLevelTask;
        ItemTask item = task instanceof ItemTask ? (ItemTask) task : null;
        int target = item != null ? item.getQuantity()
            : level ? ((SkillLevelTask) task).getTargetSkillLevel() : ((SkillXpTask) task).getTargetSkillXp();
        String name = item != null ? item.getItemName()
            : (level ? ((SkillLevelTask) task).getSkill() : ((SkillXpTask) task).getSkill()).getName();
        fields.add(new JLabel(name), BorderLayout.NORTH);
        SkillTargetFields skillTarget = new SkillTargetFields();
        JSpinner quantity = createQuantitySpinner(item == null ? 1 : target);
        if (item != null)
        {
            JPanel quantityRow = new JPanel(new BorderLayout(8, 0));
            quantityRow.add(new JLabel("Quantity"), BorderLayout.WEST);
            quantityRow.add(quantity, BorderLayout.CENTER);
            fields.add(quantityRow, BorderLayout.CENTER);
        }
        else
        {
            if (level) skillTarget.setLevelTarget(target);
            else skillTarget.setXpTarget(target);
            fields.add(skillTarget, BorderLayout.CENTER);
        }
        JButton cancel = new JButton("Cancel");
        JButton save = new JButton("Save");
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.add(cancel);
        buttons.add(save);
        fields.add(buttons, BorderLayout.SOUTH);
        cancel.addActionListener(e -> dialog.dispose());
        save.addActionListener(e -> {
            int maximum = item != null ? Integer.MAX_VALUE : level ? 99 : 200000000;
            final int value;
            try
            {
                if (item != null) quantity.commitEdit();
                value = item != null ? (Integer) quantity.getValue()
                    : skillTarget.isLevelTarget() ? skillTarget.getTargetLevel() : skillTarget.getTargetXp();
            }
            catch (IllegalArgumentException | ArithmeticException | java.text.ParseException ex)
            {
                JOptionPane.showMessageDialog(dialog, item != null
                    ? "Enter a whole number from 1 to " + maximum + ". You can use k or m."
                    : "Enter a level from 1 to 99 or XP from 0 to 200M (k/m allowed).",
                    "Invalid target", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (!goal.getTasks().contains(task) || !plugin.getGoalManager().getGoals().contains(goal))
            {
                dialog.dispose();
                return;
            }
            if (item != null)
            {
                item.setQuantity(value);
                item.recomputeFromCount(item.getAcquired());
            }
            Task savedTask = task;
            if (item == null)
            {
                savedTask = retargetSkill(task, skillTarget.isLevelTarget(), value);
                if (savedTask != task)
                {
                    goal.getTasks().set(goal.getTasks().indexOf(task), savedTask);
                    GoalPanel panel = (GoalPanel) SwingUtilities.getAncestorOfClass(GoalPanel.class, parent);
                    if (panel != null) panel.refreshTaskList();
                }
            }
            savedTask.setNotified(false);
            plugin.getGoalManager().save();
            plugin.getUiStatusManager().refresh(savedTask);
            plugin.getUiStatusManager().refresh(goal);
            plugin.refreshEditedTask(savedTask, goal);
            dialog.dispose();
        });
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.setContentPane(fields);
        dialog.getRootPane().setDefaultButton(save);
        dialog.getRootPane().registerKeyboardAction(e -> dialog.dispose(), KeyStroke.getKeyStroke("ESCAPE"), JComponent.WHEN_IN_FOCUSED_WINDOW);
        dialog.pack();
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
    }
}
