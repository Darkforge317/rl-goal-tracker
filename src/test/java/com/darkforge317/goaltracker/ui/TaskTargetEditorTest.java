package com.darkforge317.goaltracker.ui;

import org.junit.jupiter.api.Test;
import net.runelite.api.Skill;
import com.darkforge317.goaltracker.models.task.*;
import static org.junit.jupiter.api.Assertions.*;

class TaskTargetEditorTest
{
    @Test void switchesTargetTypeWithoutLosingHierarchy()
    {
        SkillLevelTask original = SkillLevelTask.builder().skill(Skill.MINING)
            .targetSkillLevel(70).currentSkillLevel(80).indentLevel(3).collapsed(true).build();
        Task result = TaskTargetEditor.retargetSkill(original, false, 1000000);
        assertTrue(result instanceof SkillXpTask);
        assertEquals(1000000, ((SkillXpTask) result).getTargetSkillXp());
        assertEquals(3, result.getIndentLevel());
        assertTrue(result.isCollapsed());
        Task back = TaskTargetEditor.retargetSkill(result, true, 90);
        assertEquals(Skill.MINING, ((SkillLevelTask) back).getSkill());
        assertEquals(90, ((SkillLevelTask) back).getTargetSkillLevel());
        assertEquals(3, back.getIndentLevel());
        assertTrue(back.isCollapsed());
    }

    @Test void onlySkillAndItemGoalsSupportEditing()
    {
        assertTrue(TaskTargetEditor.supports(SkillLevelTask.builder().build()));
        assertTrue(TaskTargetEditor.supports(SkillXpTask.builder().build()));
        assertTrue(TaskTargetEditor.supports(ItemTask.builder().build()));
        assertFalse(TaskTargetEditor.supports(ManualTask.builder().build()));
        assertFalse(TaskTargetEditor.supports(QuestTask.builder().build()));
    }

    @Test void quantityArrowsRespectBoundsAndAcceptSuffixes() throws Exception
    {
        javax.swing.SwingUtilities.invokeAndWait(() -> {
            javax.swing.JSpinner spinner = TaskTargetEditor.createQuantitySpinner(1);
            assertNull(spinner.getPreviousValue());
            assertEquals(2, spinner.getNextValue());
            javax.swing.JFormattedTextField field = ((javax.swing.JSpinner.NumberEditor) spinner.getEditor()).getTextField();
            field.setText("2K");
            assertDoesNotThrow(spinner::commitEdit);
            assertEquals(2000, spinner.getValue());
            spinner.setValue(Integer.MAX_VALUE);
            assertNull(spinner.getNextValue());
            field.setText("0");
            assertThrows(java.text.ParseException.class, spinner::commitEdit);
        });
    }

    @Test void acceptsTargetsAndCaseInsensitiveSuffixes()
    {
        assertEquals(99, TaskTargetEditor.parseTarget(" 99 ", 99));
        assertEquals(25000, TaskTargetEditor.parseTarget("25K", 200000000));
        assertEquals(200000000, TaskTargetEditor.parseTarget("200m", 200000000));
        assertEquals(Integer.MAX_VALUE, TaskTargetEditor.parseTarget("2147483647", Integer.MAX_VALUE));
    }

    @Test void rejectsEmptyInvalidAndOutOfRangeTargets()
    {
        for (String text : new String[]{"", "0", "-1", "100", "1.5", "abc", "1k", "9223372036854775808"})
        {
            assertThrows(IllegalArgumentException.class, () -> TaskTargetEditor.parseTarget(text, 99));
        }
        assertThrows(ArithmeticException.class, () -> TaskTargetEditor.parseTarget("9223372036854775807m", Integer.MAX_VALUE));
    }
}
