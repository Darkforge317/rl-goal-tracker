package com.darkforge317.goaltracker.ui.components;

import net.runelite.api.Skill;
import net.runelite.client.game.SkillIconManager;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SkillSelectorTest
{
    @Test void startsEmptyAndSupportsSelectingAndClearing() throws Exception
    {
        SkillIconManager icons = mock(SkillIconManager.class);
        when(icons.getSkillImage(any(Skill.class), eq(true)))
            .thenReturn(new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB));
        SwingUtilities.invokeAndWait(() -> {
            SkillSelector selector = new SkillSelector(icons);
            assertNull(selector.getSelectedSkill());
            ComboBox<Skill> dropdown = (ComboBox<Skill>) selector.getComponent(0);
            assertEquals(-1, dropdown.getSelectedIndex());
            selector.setSelectedSkill(Skill.MINING);
            assertEquals(Skill.MINING, selector.getSelectedSkill());
            JPanel row = (JPanel) dropdown.getRenderer().getListCellRendererComponent(
                new JList<Skill>(), Skill.MINING, -1, false, false);
            assertNotNull(((JLabel) row.getComponent(0)).getIcon());
            selector.setSelectedSkill(null);
            assertNull(selector.getSelectedSkill());
            assertEquals(-1, dropdown.getSelectedIndex());
            if (Skill.OVERALL != null) {
                assertThrows(IllegalArgumentException.class, () -> selector.setSelectedSkill(Skill.OVERALL));
                SkillSelector overall = new SkillSelector(icons, true);
                overall.setSelectedSkill(Skill.OVERALL);
                assertEquals(Skill.OVERALL, overall.getSelectedSkill());
            }
        });
    }
}
