package com.darkforge317.goaltracker.ui.components;

import net.runelite.api.Experience;
import org.junit.jupiter.api.Test;
import javax.swing.*;
import static org.junit.jupiter.api.Assertions.*;

class SkillTargetFieldsTest
{
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
            assertEquals(Experience.getLevelForXp(1000000), fields.getTargetLevel());
            xp.setText("200m");
            assertEquals(99, fields.getTargetLevel());
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
            assertThrows(IllegalArgumentException.class, fields::getTargetXp);
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
        for (String value : new String[]{"", "0", "100", "1k"})
            assertThrows(IllegalArgumentException.class, () -> SkillTargetFields.parseLevel(value));
    }
}
