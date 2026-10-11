package com.darkforge317.goaltracker.ui.components;

import com.darkforge317.goaltracker.ui.SimpleDocumentListener;
import net.runelite.api.Experience;

import javax.swing.*;
import java.awt.*;
import java.util.Locale;
import java.math.BigDecimal;
import javax.swing.text.*;

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
        level.setToolTipText("Level 1–127; accepts up to two decimal places");
        xp.setToolTipText("XP 0–200M; accepts k/m suffixes; shows the corresponding level");
        ((AbstractDocument) level.getDocument()).setDocumentFilter(new TargetFilter(true));
        ((AbstractDocument) xp.getDocument()).setDocumentFilter(new TargetFilter(false));
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
            int value = fromLevel ? xpForLevel(parseLevel(level.getText())) : parseXp(xp.getText());
            syncing = true;
            if (fromLevel) xp.setText(Integer.toString(value));
            else level.setText(BigDecimal.valueOf(levelForXp(value)).stripTrailingZeros().toPlainString());
        }
        catch (IllegalArgumentException ex) { /* Allow empty and incomplete input while typing. */ }
        finally { syncing = false; }
    }

    public static double parseLevel(String text)
    {
        if (!text.trim().matches("[0-9]+(\\.[0-9]{0,2})?")) throw new IllegalArgumentException("Enter a level from 1 to 127.");
        double value = Double.parseDouble(text.trim());
        if (value < 1 || value > 127) throw new IllegalArgumentException("Enter a level from 1 to 127.");
        return value;
    }

    // RuneLite's XP table ends at 126; the capped level-127 endpoint is 200M.
    private static int xpAtLevel(int value)
    {
        return value == 127 ? 200000000 : Experience.getXpForLevel(value);
    }

    public static int xpForLevel(double value)
    {
        int whole = (int) value;
        int lower = xpAtLevel(whole);
        if (whole == 127) return lower;
        return (int) Math.round(lower + (value - whole) * (xpAtLevel(whole + 1) - lower));
    }

    public static double levelForXp(int value)
    {
        int whole = 1;
        while (whole < 127 && xpAtLevel(whole + 1) <= value) whole++;
        if (whole == 127) return 127;
        double fraction = (double) (value - xpAtLevel(whole))
            / (xpAtLevel(whole + 1) - xpAtLevel(whole));
        return Math.round((whole + fraction) * 100) / 100.0;
    }

    /** Reject unsupported syntax and clamp bounds before text reaches the document. */
    private final class TargetFilter extends DocumentFilter
    {
        private final boolean forLevel;
        private TargetFilter(boolean forLevel) { this.forLevel = forLevel; }
        @Override public void insertString(FilterBypass fb, int offset, String text, AttributeSet attrs)
            throws BadLocationException { replace(fb, offset, 0, text, attrs); }
        @Override public void remove(FilterBypass fb, int offset, int length)
            throws BadLocationException { replace(fb, offset, length, "", null); }
        @Override public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
            throws BadLocationException
        {
            String old = fb.getDocument().getText(0, fb.getDocument().getLength());
            String value = old.substring(0, offset) + (text == null ? "" : text) + old.substring(offset + length);
            String candidate = value;
            if (!syncing && !value.isEmpty())
            {
                if (!value.matches(forLevel ? "-?[0-9]+(\\.[0-9]{0,2})?" : "-?[0-9]+[kKmM]?")) return;
                String numeric = value.replaceAll("[kKmM]$", "");
                BigDecimal number = new BigDecimal(numeric);
                if (!forLevel && value.toLowerCase(Locale.ROOT).endsWith("k")) number = number.multiply(BigDecimal.valueOf(1000));
                if (!forLevel && value.toLowerCase(Locale.ROOT).endsWith("m")) number = number.multiply(BigDecimal.valueOf(1000000));
                int min = forLevel ? 1 : 0;
                int max = forLevel ? 127 : 200000000;
                if (number.compareTo(BigDecimal.valueOf(min)) < 0) value = Integer.toString(min);
                if (number.compareTo(BigDecimal.valueOf(max)) > 0) value = Integer.toString(max);
            }
            if (value.equals(candidate)) fb.replace(offset, length, text, attrs);
            else fb.replace(0, old.length(), value, attrs);
        }
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

    /** Fractional and virtual targets use XP tasks so progress and completion remain exact. */
    public boolean isLevelTarget()
    {
        if (!levelTarget) return false;
        double value = parseLevel(level.getText());
        return value <= 99 && value == Math.floor(value);
    }
    public Double getEnteredLevel() { return levelTarget ? parseLevel(level.getText()) : null; }
    public int getTargetLevel() { return (int) parseLevel(level.getText()); }
    public int getTargetXp()
    {
        return levelTarget ? xpForLevel(parseLevel(level.getText())) : parseXp(xp.getText());
    }
    public void setLevelTarget(double value) { level.setText(BigDecimal.valueOf(value).stripTrailingZeros().toPlainString()); }
    public void setXpTarget(int value) { xp.setText(Integer.toString(value)); }
}
