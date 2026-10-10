package com.darkforge317.goaltracker.ui.components;

import net.runelite.api.Experience;
import org.junit.jupiter.api.Test;
import javax.swing.*;
import static org.junit.jupiter.api.Assertions.*;

class SkillTargetFieldsTest
{
    @Test void roundsToNearestHundredthAndInterpolatesMidpoints()
    {
        assertEquals(6856441, SkillTargetFields.xpForLevel(92.5));
        assertEquals(12420019, SkillTargetFields.xpForLevel(98.5));
        assertEquals(14391160, SkillTargetFields.xpForLevel(100));
        assertEquals(92.5, SkillTargetFields.levelForXp(6856441));
        int low = Experience.getXpForLevel(100);
        int gap = Experience.getXpForLevel(101) - low;
        assertEquals(100.12, SkillTargetFields.levelForXp(low + (int) Math.round(gap * 0.124)));
        assertEquals(100.13, SkillTargetFields.levelForXp(low + (int) Math.round(gap * 0.126)));
    }

    @Test void syncsBothDirectionsAndPreservesExactXp() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            SkillTargetFields fields = new SkillTargetFields();
            JTextField level = (JTextField) fields.getComponent(1);
            JTextField xp = (JTextField) fields.getComponent(3);
            level.setText("70");
            assertTrue(fields.isLevelTarget());
            assertEquals(Experience.getXpForLevel(70), fields.getTargetXp());
            xp.setText("1M");
            assertFalse(fields.isLevelTarget());
            assertEquals(1000000, fields.getTargetXp());
            assertEquals(Double.toString(SkillTargetFields.levelForXp(1000000)), level.getText());
            xp.setText("200m");
            assertEquals(127, fields.getTargetLevel());
            assertEquals(200000000, fields.getTargetXp());
            level.setText("1");
            assertEquals(0, fields.getTargetXp());
        });
    }

    @Test void invalidActiveFieldCannotSubmitStaleTarget() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            SkillTargetFields fields = new SkillTargetFields();
            JTextField level = (JTextField) fields.getComponent(1);
            JTextField xp = (JTextField) fields.getComponent(3);
            level.setText("");
            assertThrows(IllegalArgumentException.class, fields::getTargetXp);
            xp.setText("999999999999999999999");
            assertEquals(200000000, fields.getTargetXp());
            xp.setText("0");
            assertEquals(0, fields.getTargetXp());
            assertEquals(1, fields.getTargetLevel());
            fields.setLevelTarget(99);
            assertTrue(fields.isLevelTarget());
            assertEquals(13034431, fields.getTargetXp());
        });
    }

    @Test void validatesBoundsAndSuffixes()
    {
        assertEquals(25000, SkillTargetFields.parseXp("25K"));
        assertEquals(200000000, SkillTargetFields.parseXp("200M"));
        for (String value : new String[]{"", "-1", "201m", "1.5m", "abc"})
            assertThrows(IllegalArgumentException.class, () -> SkillTargetFields.parseXp(value));
        for (String value : new String[]{"", "0", "128", "1k", "92.555", "127.01", "0.99", "NaN"})
            assertThrows(IllegalArgumentException.class, () -> SkillTargetFields.parseLevel(value));
    }
    @Test void supportsVirtualAndFractionalLevels() throws Exception
    {
        assertEquals(92.5, SkillTargetFields.parseLevel("92.50"));
        assertEquals(127, SkillTargetFields.parseLevel("127"));
        SwingUtilities.invokeAndWait(() -> {
            SkillTargetFields fields = new SkillTargetFields();
            JTextField level = (JTextField) fields.getComponent(1);
            JTextField xp = (JTextField) fields.getComponent(3);
            level.setText("100");
            assertEquals(Experience.getXpForLevel(100), fields.getTargetXp());
            assertFalse(fields.isLevelTarget());
            for (String text : new String[]{"1.01", "92.5", "98.50", "126.99"})
            {
                level.setText(text);
                double value = Double.parseDouble(text);
                int whole = (int) value;
                int expected = (int) Math.round(Experience.getXpForLevel(whole)
                    + (value - whole) * ((whole == 126 ? 200000000 : Experience.getXpForLevel(whole + 1)) - Experience.getXpForLevel(whole)));
                assertEquals(expected, fields.getTargetXp());
                assertFalse(fields.isLevelTarget());
                xp.setText(Integer.toString(expected));
                assertEquals(value, Double.parseDouble(level.getText()), 0.001);
                assertEquals(expected, fields.getTargetXp());
            }
            level.setText("99");
            assertTrue(fields.isLevelTarget());
        });
    }

    @Test void preventsUnsupportedPrecisionAndClampsBounds() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            SkillTargetFields fields = new SkillTargetFields();
            JTextField level = (JTextField) fields.getComponent(1);
            JTextField xp = (JTextField) fields.getComponent(3);
            level.setText("92.55");
            level.setText("92.555");
            assertEquals("92.55", level.getText());
            level.setText("127");
            assertEquals(200000000, fields.getTargetXp());
            level.setText("128");
            assertEquals("127", level.getText());
            level.setText("0.99");
            assertEquals("1", level.getText());
            level.setText("-10");
            assertEquals("1", level.getText());
            xp.setText("201m");
            assertEquals("200000000", xp.getText());
            xp.setText("1.5");
            assertEquals("200000000", xp.getText());
            xp.setText("-1");
            assertEquals("0", xp.getText());
            xp.setText("13034432");
            assertEquals("99", level.getText());
            assertEquals(13034432, fields.getTargetXp());
            level.setText("10");
            try { level.getDocument().insertString(2, "0", null); }
            catch (javax.swing.text.BadLocationException ex) { throw new AssertionError(ex); }
            assertEquals("100", level.getText());
            assertEquals(Experience.getXpForLevel(100), fields.getTargetXp());
        });
    }
}
