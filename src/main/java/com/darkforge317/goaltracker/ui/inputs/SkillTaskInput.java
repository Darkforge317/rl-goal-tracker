package com.darkforge317.goaltracker.ui.inputs;

import com.darkforge317.goaltracker.GoalTrackerPlugin;
import com.darkforge317.goaltracker.models.Goal;
import com.darkforge317.goaltracker.models.task.SkillLevelTask;
import com.darkforge317.goaltracker.models.task.SkillXpTask;
import com.darkforge317.goaltracker.ui.components.SkillSelector;
import com.darkforge317.goaltracker.ui.components.SkillTargetFields;

import javax.swing.*;
import java.awt.*;

/** A single skill goal form accepting either a level or an exact XP target. */
public final class SkillTaskInput extends TaskInput
{
    private final SkillSelector skill;
    private final SkillTargetFields target = new SkillTargetFields();

    public SkillTaskInput(GoalTrackerPlugin plugin, Goal goal)
    {
        super(plugin, goal, "Skill goal");
        skill = new SkillSelector(plugin.getSkillIconManager());
        JPanel body = new JPanel(new BorderLayout(0, 8));
        body.setOpaque(false);
        body.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        body.add(skill, BorderLayout.NORTH);
        body.add(target, BorderLayout.CENTER);
        getInputRow().add(body, BorderLayout.CENTER);
    }

    @Override protected void submit()
    {
        if (skill.getSelectedSkill() == null) return;
        try
        {
            if (target.isLevelTarget())
                addTask(SkillLevelTask.builder().skill(skill.getSelectedSkill())
                    .targetSkillLevel(target.getTargetLevel()).build());
            else
                addTask(SkillXpTask.builder().skill(skill.getSelectedSkill())
                    .targetSkillXp(target.getTargetXp()).build());
        }
        catch (IllegalArgumentException ex)
        {
            JOptionPane.showMessageDialog(this, "Enter a level from 1 to 99 or XP from 0 to 200M (k/m allowed).",
                "Invalid skill target", JOptionPane.ERROR_MESSAGE);
        }
    }

    @Override protected void reset()
    {
        target.setLevelTarget(99);
        skill.setSelectedSkill(null);
    }
}
