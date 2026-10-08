package com.darkforge317.goaltracker.ui.components;

import com.darkforge317.goaltracker.ui.SimpleDocumentListener;
import net.runelite.api.Experience;

import javax.swing.*;
import java.awt.*;
import java.util.Locale;

/** Two views of one target. The last edited field determines how the task is saved. */
public final class SkillTargetFields extends JPanel
{
    private final JTextField level = new JTextField(6);
    private final JTextField xp = new JTextField(10);
    private boolean syncing;
    private boolean levelTarget = true;

    public SkillTargetFields()
    {
        super(new GridBagLayout());
        setOpaque(false);
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(2, 0, 2, 8);
        constraints.anchor = GridBagConstraints.WEST;
        add(new JLabel("Level"), constraints);
        constraints.gridx = 1;
        constraints.weightx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(2, 0, 2, 0);
        level.setPreferredSize(new Dimension(120, 30));
        add(level, constraints);
        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.weightx = 0;
        constraints.insets = new Insets(2, 0, 2, 8);
        add(new JLabel("XP"), constraints);
        constraints.gridx = 1;
        constraints.weightx = 1;
        constraints.insets = new Insets(2, 0, 2, 0);
        xp.setPreferredSize(new Dimension(120, 30));
        add(xp, constraints);
        level.getAccessibleContext().setAccessibleName("Target level");
        xp.getAccessibleContext().setAccessibleName("Target XP");
        level.setToolTipText("Level 1–99; updates XP to the level's minimum");
        xp.setToolTipText("XP 0–200M; accepts k/m suffixes; shows the corresponding level");
        level.getDocument().addDocumentListener((SimpleDocumentListener) e -> sync(true));
        xp.getDocument().addDocumentListener((SimpleDocumentListener) e -> sync(false));
        setLevelTarget(99);
    }

    private void sync(boolean fromLevel)
    {
        if (syncing) return;
        levelTarget = fromLevel;
        try
        {
            int value = fromLevel ? parseLevel(level.getText()) : parseXp(xp.getText());
            syncing = true;
            if (fromLevel) xp.setText(Integer.toString(Experience.getXpForLevel(value)));
            else level.setText(Integer.toString(Math.min(99, Experience.getLevelForXp(value))));
        }
        catch (IllegalArgumentException ex) { /* Allow empty and incomplete input while typing. */ }
        finally { syncing = false; }
    }

    public static int parseLevel(String text)
    {
        if (!text.trim().matches("[0-9]+")) throw new IllegalArgumentException("Enter a level from 1 to 99.");
        int value = Integer.parseInt(text.trim());
        if (value < 1 || value > 99) throw new IllegalArgumentException("Enter a level from 1 to 99.");
        return value;
    }

    public static int parseXp(String text)
    {
        String value = text.trim().toLowerCase(Locale.ROOT);
        if (!value.matches("[0-9]+[km]?")) throw new IllegalArgumentException("Enter XP from 0 to 200M (k/m allowed).");
        int multiplier = value.endsWith("k") ? 1000 : value.endsWith("m") ? 1000000 : 1;
        if (multiplier != 1) value = value.substring(0, value.length() - 1);
        long number = Long.parseLong(value);
        if (number > 200000000L / multiplier) throw new IllegalArgumentException("Enter XP from 0 to 200M.");
        return (int) (number * multiplier);
    }

    public boolean isLevelTarget() { return levelTarget; }
    public int getTargetLevel() { return parseLevel(level.getText()); }
    public int getTargetXp()
    {
        return levelTarget ? Experience.getXpForLevel(getTargetLevel()) : parseXp(xp.getText());
    }
    public void setLevelTarget(int value) { level.setText(Integer.toString(value)); }
    public void setXpTarget(int value) { xp.setText(Integer.toString(value)); }
}
