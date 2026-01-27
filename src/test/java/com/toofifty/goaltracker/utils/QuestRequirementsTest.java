package com.toofifty.goaltracker.utils;

import com.toofifty.goaltracker.models.task.QuestTask;
import com.toofifty.goaltracker.models.task.SkillLevelTask;
import com.toofifty.goaltracker.models.task.Task;
import net.runelite.api.Quest;
import net.runelite.api.Skill;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class QuestRequirementsTest
{
    @Test
    void testGetRequirements()
    {
        List<Task> requirements = QuestRequirements.getRequirements(Quest.FAIRYTALE_I__GROWING_PAINS, 0);
        assertNotNull(requirements, "Requirements list should not be null");
        // Recursive requirements: LOST_CITY + its 2 skills, NATURE_SPIRIT + its 3 reqs, Farming 18 = 8 total
        assertEquals(8, requirements.size(), "Fairytale I should have 8 requirements (including recursive sub-quest requirements)");

        // Direct requirements at indent 1, sub-requirements at indent 2
        long indent1Count = requirements.stream().filter(t -> t.getIndentLevel() == 1).count();
        long indent2Count = requirements.stream().filter(t -> t.getIndentLevel() == 2).count();
        assertEquals(3, indent1Count, "Should have 3 direct requirements at indent 1");
        assertEquals(5, indent2Count, "Should have 5 sub-requirements at indent 2");

        // With recursive requirements: 4 quests (LOST_CITY, NATURE_SPIRIT, PRIEST_IN_PERIL, THE_RESTLESS_GHOST)
        // and 4 skills (Farming 18, Crafting 31, Woodcutting 36, Prayer 18)
        long questCount = requirements.stream().filter(t -> t instanceof QuestTask).count();
        long skillCount = requirements.stream().filter(t -> t instanceof SkillLevelTask).count();
        assertEquals(4, questCount, "Should contain 4 QuestTask requirements (including sub-quest prereqs)");
        assertEquals(4, skillCount, "Should contain 4 SkillLevelTask requirements (including sub-quest prereqs)");

        // Verify direct quest requirements are present
        boolean hasLostCity = requirements.stream().anyMatch(t -> t instanceof QuestTask && ((QuestTask) t).getQuest() == Quest.LOST_CITY);
        boolean hasNatureSpirit = requirements.stream().anyMatch(t -> t instanceof QuestTask && ((QuestTask) t).getQuest() == Quest.NATURE_SPIRIT);
        assertTrue(hasLostCity, "Lost City should be a requirement");
        assertTrue(hasNatureSpirit, "Nature Spirit should be a requirement");

        // Verify direct skill requirement (Farming 18)
        boolean hasFarming18 = requirements.stream().anyMatch(t -> t instanceof SkillLevelTask && ((SkillLevelTask) t).getSkill() == Skill.FARMING && ((SkillLevelTask) t).getLevel() == 18);
        assertTrue(hasFarming18, "Farming 18 should be a requirement");
    }

    @Test
    void testRequirementsAreFreshInstances()
    {
        List<Task> first = QuestRequirements.getRequirements(Quest.FAIRYTALE_I__GROWING_PAINS, 0);
        List<Task> second = QuestRequirements.getRequirements(Quest.FAIRYTALE_I__GROWING_PAINS, 0);

        assertEquals(first.size(), second.size(), "Both calls should return the same number of items");
        for (int i = 0; i < first.size(); i++) {
            assertNotSame(first.get(i), second.get(i), "Each requirement should be a fresh instance on each call");
            assertEquals(first.get(i).getClass(), second.get(i).getClass(), "Classes should match between calls");
            assertEquals(first.get(i).getIndentLevel(), second.get(i).getIndentLevel(), "Indent levels should match between calls");
        }
    }
}