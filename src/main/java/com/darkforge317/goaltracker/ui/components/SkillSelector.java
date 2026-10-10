package com.darkforge317.goaltracker.ui.components;

import net.runelite.api.Skill;
import net.runelite.client.game.SkillIconManager;
import net.runelite.client.ui.ColorScheme;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/** Skill dropdown using the same control and arrow icons as quest selection. */
public final class SkillSelector extends JPanel
{
    private final ComboBox<Skill> dropdown;

    public SkillSelector(SkillIconManager icons)
    {
        this(icons, false);
    }

    public SkillSelector(SkillIconManager icons, boolean includeOverall)
    {
        super(new BorderLayout());
        setBackground(ColorScheme.DARKER_GRAY_COLOR);
        List<Skill> skills = Arrays.stream(Skill.values())
            .filter(skill -> includeOverall || skill != Skill.OVERALL)
            .collect(Collectors.toList());
        dropdown = new ComboBox<>(skills);
        dropdown.setFont(new Font("RuneScape UF", Font.PLAIN, 10));
        dropdown.setBorder(BorderFactory.createLineBorder(ColorScheme.DARK_GRAY_COLOR, 1));
        dropdown.getAccessibleContext().setAccessibleName("Choose a skill");
        ListCellRenderer<? super Skill> renderer = dropdown.getRenderer();
        dropdown.setRenderer((list, value, index, selected, focused) -> {
            JPanel row = (JPanel) renderer.getListCellRendererComponent(list, value, index, selected, focused);
            JLabel label = (JLabel) row.getComponent(0);
            label.setText(value == null ? "Choose a skill" : value.getName());
            label.setIcon(value == null ? null : new ImageIcon(icons.getSkillImage(value, true)));
            return row;
        });
        dropdown.setSelectedIndex(-1);
        add(dropdown, BorderLayout.CENTER);
    }

    public Skill getSelectedSkill()
    {
        return (Skill) dropdown.getSelectedItem();
    }

    public void setSelectedSkill(Skill skill)
    {
        if (skill != null)
        {
            boolean available = false;
            for (int i = 0; i < dropdown.getItemCount(); i++)
                if (dropdown.getItemAt(i) == skill) available = true;
            if (!available) throw new IllegalArgumentException("Choose a trainable skill");
        }
        dropdown.setSelectedItem(skill);
    }
}
