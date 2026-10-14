package com.darkforge317.goaltracker.ui;

import com.darkforge317.goaltracker.GoalTrackerPlugin;
import com.darkforge317.goaltracker.models.Goal;
import com.darkforge317.goaltracker.models.enums.Status;
import com.darkforge317.goaltracker.models.task.*;
import net.runelite.api.Skill;
import com.darkforge317.goaltracker.ui.components.SkillTargetFields;
import com.darkforge317.goaltracker.ui.inputs.QuestPointsTaskInput;

import javax.swing.*;
import java.awt.*;
import java.util.Locale;

/** Edits targets in place so their position, children and collapsed state survive. */
final class TaskTargetEditor
{
    static boolean supports(Task task)
    {
        return task instanceof SkillLevelTask || task instanceof SkillXpTask || task instanceof ItemTask
            || task instanceof CombatLevelTask || task instanceof QuestPointsTask;
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
        result.setTaskId(task.getTaskId());
        result.setParentTaskId(task.getParentTaskId());
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

    static JSpinner createCombatLevelSpinner(int value)
    {
        return createCombatLevelSpinner((double) value);
    }

    static JSpinner createCombatLevelSpinner(double value)
    {
        double boundedValue = Math.max(3, Math.min(126, value));
        SpinnerNumberModel model = new SpinnerNumberModel(boundedValue, 3.0, 126.0, 0.25)
        {
            @Override public void setValue(Object value)
            {
                if (!(value instanceof Number))
                {
                    throw new IllegalArgumentException("Combat level must be between 3 and 126.");
                }
                Number number = (Number) value;
                double target = number.doubleValue();
                if (!Double.isFinite(target) || target < 3 || target > 126
                    || Math.abs(target * 100 - Math.rint(target * 100)) > 0.000001)
                {
                    throw new IllegalArgumentException("Combat level must be between 3 and 126, with at most two decimal places.");
                }
                super.setValue(target);
            }
        };
        JSpinner spinner = new JSpinner(model);
        JSpinner.NumberEditor editor = new JSpinner.NumberEditor(spinner, "0.00");
        editor.getTextField().setColumns(5);
        editor.getTextField().setHorizontalAlignment(SwingConstants.LEFT);
        editor.getTextField().getAccessibleContext().setAccessibleName("Target combat level");
        editor.getTextField().setToolTipText("Combat level 3–126; up to two decimal places");
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
        CombatLevelTask combat = task instanceof CombatLevelTask ? (CombatLevelTask) task : null;
        QuestPointsTask questPoints = task instanceof QuestPointsTask ? (QuestPointsTask) task : null;
        int target = item != null ? item.getQuantity()
            : combat != null ? 1
            : questPoints != null ? questPoints.getTargetQuestPoints()
            : level ? ((SkillLevelTask) task).getTargetSkillLevel() : ((SkillXpTask) task).getTargetSkillXp();
        String name = item != null ? item.getItemName()
            : combat != null ? "Combat"
            : questPoints != null ? "Quest points"
            : (level ? ((SkillLevelTask) task).getSkill() : ((SkillXpTask) task).getSkill()).getName();
        fields.add(new JLabel(name), BorderLayout.NORTH);
        SkillTargetFields skillTarget = new SkillTargetFields();
        JSpinner quantity = createQuantitySpinner(item == null ? 1 : target);
        JSpinner combatTarget = combat == null ? null : createCombatLevelSpinner(combat.getTargetCombatLevel());
        JSpinner questPointsTarget = questPoints == null ? null : createQuestPointsSpinner(target);
        if (item != null)
        {
            JPanel quantityRow = new JPanel(new BorderLayout(8, 0));
            quantityRow.add(new JLabel("Quantity"), BorderLayout.WEST);
            quantityRow.add(quantity, BorderLayout.CENTER);
            fields.add(quantityRow, BorderLayout.CENTER);
        }
        else if (combat != null)
        {
            JPanel combatRow = new JPanel(new BorderLayout(8, 0));
            combatRow.add(new JLabel("Combat level (3–126)"), BorderLayout.WEST);
            combatRow.add(combatTarget, BorderLayout.CENTER);
            fields.add(combatRow, BorderLayout.CENTER);
        }
        else if (questPoints != null)
        {
            JPanel questPointsRow = new JPanel(new BorderLayout(8, 0));
            questPointsRow.add(new JLabel("Target quest points"), BorderLayout.WEST);
            questPointsRow.add(questPointsTarget, BorderLayout.CENTER);
            fields.add(questPointsRow, BorderLayout.CENTER);
        }
        else
        {
            if (task instanceof SkillXpTask && ((SkillXpTask) task).getTargetLevel() != null)
                skillTarget.setLevelTarget(((SkillXpTask) task).getTargetLevel());
            else if (level) skillTarget.setLevelTarget(target);
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
            final int value;
            try
            {
                if (item != null) quantity.commitEdit();
                if (combat != null) combatTarget.commitEdit();
                if (questPoints != null) questPointsTarget.commitEdit();
                value = item != null ? (Integer) quantity.getValue()
                    : questPoints != null ? (Integer) questPointsTarget.getValue()
                    : skillTarget.isLevelTarget() ? skillTarget.getTargetLevel() : skillTarget.getTargetXp();
            }
            catch (IllegalArgumentException | ArithmeticException | java.text.ParseException ex)
            {
                String message = item != null
                    ? "Enter a whole number from 1 to " + Integer.MAX_VALUE + ". You can use k or m."
                    : questPoints != null ? "Enter a whole number from " + QuestPointsTask.MIN_QUEST_POINTS + " to " + QuestPointsTask.MAX_QUEST_POINTS + "."
                    : combat != null ? "Enter a combat level from 3 to 126, with up to two decimal places."
                    : "Enter a level from 1 to 127 (up to two decimal places) or XP from 0 to 200M (k/m allowed).";
                JOptionPane.showMessageDialog(dialog, message, "Invalid target", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (!goal.getTasks().contains(task) || !plugin.getGoalManager().getGoals().contains(goal))
            {
                dialog.dispose();
                return;
            }
            boolean wasCompleted = task.getStatus().isCompleted();
            if (item != null)
            {
                item.setQuantity(value);
                item.recomputeFromCount(item.getAcquired());
            }
            else if (questPoints != null)
            {
                questPoints.setTargetQuestPoints(value);
                questPoints.setStatus(questPoints.hasReachedTarget() ? Status.COMPLETED
                    : questPoints.getCurrentQuestPoints() > 0 ? Status.IN_PROGRESS : Status.NOT_STARTED);
            }
            else if (combat != null)
            {
                combat.setTargetCombatLevel(((Number) combatTarget.getValue()).doubleValue());
                combat.setStatus(combat.hasReachedTargetLevel() ? Status.COMPLETED : Status.NOT_STARTED);
            }
            Task savedTask = task;
            if (item == null && combat == null && questPoints == null)
            {
                savedTask = retargetSkill(task, skillTarget.isLevelTarget(), value);
                if (savedTask instanceof SkillXpTask)
                    ((SkillXpTask) savedTask).setTargetLevel(skillTarget.getEnteredLevel());
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
            plugin.refreshEditedTask(savedTask, goal, wasCompleted);
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

    static JSpinner createQuestPointsSpinner(int value)
    {
        JSpinner spinner = QuestPointsTaskInput.createTargetSpinner(value);
        JSpinner.NumberEditor editor = new JSpinner.NumberEditor(spinner, "0");
        editor.getTextField().setColumns(5);
        editor.getTextField().setHorizontalAlignment(SwingConstants.LEFT);
        spinner.setEditor(editor);
        return spinner;
    }
}
