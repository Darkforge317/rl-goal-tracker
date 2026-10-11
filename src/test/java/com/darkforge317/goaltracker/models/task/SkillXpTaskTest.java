package com.darkforge317.goaltracker.models.task;

import com.darkforge317.goaltracker.models.enums.TaskType;
import net.runelite.api.Skill;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SkillXpTaskTest {
    @Test
    void toString_shouldReturnTheXPAndSkillName() {
        SkillXpTask task = SkillXpTask.builder().skill(Skill.ATTACK).targetSkillXp(1234).build();

        assertEquals("1234 Attack XP", task.toString());
    }

    @Test
    void levelTitlesSurvivePersistenceAndUseXpForCompletion() {
        for (double level : new double[]{111, 92.5, 127}) {
            SkillXpTask task = SkillXpTask.builder().skill(Skill.FISHING)
                .targetSkillXp(com.darkforge317.goaltracker.ui.components.SkillTargetFields.xpForLevel(level))
                .targetLevel(level).build();
            com.google.gson.Gson gson = new com.google.gson.Gson();
            SkillXpTask restored = gson.fromJson(gson.toJson(task), SkillXpTask.class);
            String title = java.math.BigDecimal.valueOf(level).stripTrailingZeros().toPlainString() + " Fishing";
            assertEquals(title, restored.toString());
            assertEquals(title, restored.getDisplayName());
            restored.setCurrentSkillXp(restored.getTargetSkillXp() - 1);
            org.junit.jupiter.api.Assertions.assertFalse(restored.hasReachedTargetXp());
            restored.setCurrentSkillXp(restored.getTargetSkillXp());
            org.junit.jupiter.api.Assertions.assertTrue(restored.hasReachedTargetXp());
            restored.setTargetLevel(null);
            assertEquals(restored.getTargetSkillXp() + " Fishing XP", restored.toString());
        }
    }

    @Test
    void getType_shouldReturnSkill() {
        SkillXpTask task = SkillXpTask.builder().build();

        assertEquals(TaskType.SKILL_XP, task.getType());
    }
}