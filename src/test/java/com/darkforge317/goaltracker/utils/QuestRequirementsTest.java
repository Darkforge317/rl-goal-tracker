package com.darkforge317.goaltracker.utils;

import com.darkforge317.goaltracker.models.task.QuestTask;
import com.darkforge317.goaltracker.models.task.SkillLevelTask;
import com.darkforge317.goaltracker.models.task.CombatLevelTask;
import com.darkforge317.goaltracker.models.task.QuestPointsTask;
import com.darkforge317.goaltracker.models.task.Task;
import net.runelite.api.Quest;
import net.runelite.api.Skill;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class QuestRequirementsTest
{
    @Test
    void testGetRequirements()
    {
        List<Task> requirements = QuestRequirements.getRequirements(Quest.FAIRYTALE_I__GROWING_PAINS, 0);
        assertNotNull(requirements, "Requirements list should not be null");
        // Recursive requirements: LOST_CITY + its 2 skills, NATURE_SPIRIT + 2 quest prereqs, Farming 18 = 7 total
        assertEquals(7, requirements.size(), "Fairytale I should have 7 requirements (including recursive sub-quest requirements)");

        // Direct requirements at indent 1, sub-requirements at indent 2
        long indent1Count = requirements.stream().filter(t -> t.getIndentLevel() == 1).count();
        long indent2Count = requirements.stream().filter(t -> t.getIndentLevel() == 2).count();
        assertEquals(3, indent1Count, "Should have 3 direct requirements at indent 1");
        assertEquals(4, indent2Count, "Should have 4 sub-requirements at indent 2");

        // With recursive requirements: 4 quests (LOST_CITY, NATURE_SPIRIT, PRIEST_IN_PERIL, THE_RESTLESS_GHOST)
        // and 3 skills (Farming 18, Crafting 31, Woodcutting 36)
        long questCount = requirements.stream().filter(t -> t instanceof QuestTask).count();
        long skillCount = requirements.stream().filter(t -> t instanceof SkillLevelTask).count();
        assertEquals(4, questCount, "Should contain 4 QuestTask requirements (including sub-quest prereqs)");
        assertEquals(3, skillCount, "Should contain 3 SkillLevelTask requirements (including sub-quest prereqs)");

        // Verify direct quest requirements are present
        boolean hasLostCity = requirements.stream().anyMatch(t -> t instanceof QuestTask && ((QuestTask) t).getQuest() == Quest.LOST_CITY);
        boolean hasNatureSpirit = requirements.stream().anyMatch(t -> t instanceof QuestTask && ((QuestTask) t).getQuest() == Quest.NATURE_SPIRIT);
        assertTrue(hasLostCity, "Lost City should be a requirement");
        assertTrue(hasNatureSpirit, "Nature Spirit should be a requirement");

        // Verify direct skill requirement (Farming 18)
        boolean hasFarming18 = requirements.stream().anyMatch(t -> t instanceof SkillLevelTask && ((SkillLevelTask) t).getSkill() == Skill.FARMING && ((SkillLevelTask) t).getTargetSkillLevel() == 18);
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

    @Test
    void dreamMentorRequirements_useCombatLevelInsteadOfDefence()
    {
        List<Task> requirements = QuestRequirements.getRequirements(Quest.DREAM_MENTOR, 0);

        assertTrue(requirements.stream().anyMatch(task -> task instanceof CombatLevelTask
            && ((CombatLevelTask) task).getTargetCombatLevel() == 85));
        assertFalse(requirements.stream().anyMatch(task -> task instanceof SkillLevelTask
            && ((SkillLevelTask) task).getSkill() == Skill.DEFENCE
            && ((SkillLevelTask) task).getTargetSkillLevel() == 85));
    }


    @Test
    void questPointRequirements_areRepresentedAndCopiedWithIndent()
    {
        List<Task> requirements = QuestRequirements.getRequirements(Quest.DRAGON_SLAYER_I, 0);

        assertEquals(1, requirements.size());
        assertTrue(requirements.get(0) instanceof QuestPointsTask);
        QuestPointsTask task = (QuestPointsTask) requirements.get(0);
        assertEquals(32, task.getTargetQuestPoints());
        assertEquals(1, task.getIndentLevel());
        assertNotSame(task, QuestRequirements.getRequirements(Quest.DRAGON_SLAYER_I, 0).get(0));
    }

    @Test
    void correctedQuestRequirements_includePointGatesSkillsAndQuestBranches()
    {
        List<Task> legends = QuestRequirements.getRequirements(Quest.LEGENDS_QUEST, 0);
        assertHasQuestPoints(legends, 107);
        assertHasSkillLevel(legends, Skill.HERBLORE, 45);
        assertHasSkillLevel(legends, Skill.MAGIC, 56);
        assertHasSkillLevel(legends, Skill.SMITHING, 50);
        assertHasQuest(legends, Quest.WATERFALL_QUEST);
        assertFalse(hasSkillLevel(legends, Skill.FISHING, 50));

        List<Task> dragonSlayerTwo = QuestRequirements.getRequirements(Quest.DRAGON_SLAYER_II, 0);
        assertHasQuestPoints(dragonSlayerTwo, 200);
        assertHasSkillLevel(dragonSlayerTwo, Skill.CRAFTING, 62);
        assertHasSkillLevel(dragonSlayerTwo, Skill.MINING, 68);
        assertHasSkillLevel(dragonSlayerTwo, Skill.SMITHING, 70);
        assertHasSkillLevel(dragonSlayerTwo, Skill.CONSTRUCTION, 50);
        assertHasSkillLevel(dragonSlayerTwo, Skill.HITPOINTS, 50);
        assertHasQuest(dragonSlayerTwo, Quest.ANIMAL_MAGNETISM);
        assertHasQuest(dragonSlayerTwo, Quest.GHOSTS_AHOY);
        assertHasQuest(dragonSlayerTwo, Quest.CLIENT_OF_KOUREND);
        assertFalse(hasSkillLevel(dragonSlayerTwo, Skill.HUNTER, 60));

        List<Task> dreamMentor = QuestRequirements.getRequirements(Quest.DREAM_MENTOR, 0);
        assertHasQuest(dreamMentor, Quest.EADGARS_RUSE);
        assertTrue(dreamMentor.stream().anyMatch(t -> t instanceof CombatLevelTask
                && ((CombatLevelTask) t).getTargetCombatLevel() == 85));
    }

    @Test
    void missingQuestChainsAreAvailableForGeneratedPrerequisites()
    {
        List<Task> kourendQuest = QuestRequirements.getRequirements(Quest.THE_QUEEN_OF_THIEVES, 0);
        assertHasQuest(kourendQuest, Quest.CLIENT_OF_KOUREND);
        assertHasQuest(kourendQuest, Quest.X_MARKS_THE_SPOT);
        assertHasSkillLevel(kourendQuest, Skill.THIEVING, 20);

        List<Task> newerQuest = QuestRequirements.getRequirements(Quest.THE_RED_REEF, 0);
        assertHasQuest(newerQuest, Quest.TROUBLED_TORTUGANS);
        assertHasQuest(newerQuest, Quest.PANDEMONIUM);
        assertHasSkillLevel(newerQuest, Skill.SAILING, 52);
        assertHasSkillLevel(newerQuest, Skill.SMITHING, 48);
    }

    @Test
    void questsMissingFromTheOriginalMap_haveGeneratedRequirements()
    {
        Quest[] newlyMappedQuests = {
            Quest.BELOW_ICE_MOUNTAIN, Quest.BLACK_KNIGHTS_FORTRESS, Quest.CLIENT_OF_KOUREND,
            Quest.CURSE_OF_THE_EMPTY_LORD, Quest.THE_ENCHANTED_KEY, Quest.ENTER_THE_ABYSS,
            Quest.FAMILY_PEST, Quest.THE_GENERALS_SHADOW, Quest.GETTING_AHEAD,
            Quest.LAIR_OF_TARN_RAZORLOR, Quest.MAGE_ARENA_I, Quest.MAGE_ARENA_II,
            Quest.MAKING_FRIENDS_WITH_MY_ARM, Quest.THE_QUEEN_OF_THIEVES,
            Quest.RAG_AND_BONE_MAN_II, Quest.SKIPPY_AND_THE_MOGRES, Quest.TALE_OF_THE_RIGHTEOUS,
            Quest.THE_FROZEN_DOOR, Quest.HOPESPEARS_WILL, Quest.THE_GARDEN_OF_DEATH,
            Quest.THE_ASCENT_OF_ARCEUUS, Quest.THE_DEPTHS_OF_DESPAIR, Quest.THE_FORSAKEN_TOWER,
            Quest.INTO_THE_TOMBS, Quest.HIS_FAITHFUL_SERVANTS, Quest.BARBARIAN_TRAINING,
            Quest.A_RUFF_SITUATION, Quest.PRYING_TIMES, Quest.CURRENT_AFFAIRS,
            Quest.TROUBLED_TORTUGANS, Quest.THE_RED_REEF, Quest.FALLEN_FROM_GRACE, Quest.CRAB_QUEST
        };

        for (Quest quest : newlyMappedQuests)
        {
            assertFalse(QuestRequirements.getRequirements(quest, 0).isEmpty(),
                    "Expected generated prerequisites for " + quest);
        }
    }

    private static void assertHasQuest(List<Task> tasks, Quest expected)
    {
        assertTrue(tasks.stream().filter(QuestTask.class::isInstance).map(QuestTask.class::cast)
                .anyMatch(task -> task.getQuest() == expected), "Expected quest requirement " + expected);
    }

    private static void assertHasQuestPoints(List<Task> tasks, int target)
    {
        assertTrue(tasks.stream().filter(QuestPointsTask.class::isInstance).map(QuestPointsTask.class::cast)
                .anyMatch(task -> task.getTargetQuestPoints() == target), "Expected " + target + " Quest points");
    }

    private static void assertHasSkillLevel(List<Task> tasks, Skill skill, int target)
    {
        assertTrue(hasSkillLevel(tasks, skill, target), "Expected " + skill + " " + target);
    }

    private static boolean hasSkillLevel(List<Task> tasks, Skill skill, int target)
    {
        return tasks.stream().filter(SkillLevelTask.class::isInstance).map(SkillLevelTask.class::cast)
                .anyMatch(task -> task.getSkill() == skill && task.getTargetSkillLevel() == target);
    }

    @Test
    void bloodMoonRisesRequirements_matchWikiQuestHierarchyAndSkillLevels()
    {
        List<Task> requirements = QuestRequirements.getRequirements(Quest.THE_BLOOD_MOON_RISES, 0);
        Map<Quest, Integer> actualQuestIndentLevels = new HashMap<>();
        requirements.stream()
            .filter(QuestTask.class::isInstance)
            .map(QuestTask.class::cast)
            .forEach(task -> actualQuestIndentLevels.put(task.getQuest(), task.getIndentLevel()));

        Map<Quest, Integer> expectedQuestIndentLevels = new HashMap<>();
        expectedQuestIndentLevels.put(Quest.A_NIGHT_AT_THE_THEATRE, 1);
        expectedQuestIndentLevels.put(Quest.SINS_OF_THE_FATHER, 1);
        expectedQuestIndentLevels.put(Quest.VAMPYRE_SLAYER, 2);
        expectedQuestIndentLevels.put(Quest.A_TASTE_OF_HOPE, 2);
        expectedQuestIndentLevels.put(Quest.DARKNESS_OF_HALLOWVALE, 3);
        expectedQuestIndentLevels.put(Quest.IN_AID_OF_THE_MYREQUE, 4);
        expectedQuestIndentLevels.put(Quest.IN_SEARCH_OF_THE_MYREQUE, 5);
        expectedQuestIndentLevels.put(Quest.NATURE_SPIRIT, 6);
        expectedQuestIndentLevels.put(Quest.PRIEST_IN_PERIL, 7);
        expectedQuestIndentLevels.put(Quest.THE_RESTLESS_GHOST, 7);
        assertEquals(expectedQuestIndentLevels, actualQuestIndentLevels);

        Map<Skill, Integer> expectedSkillLevels = new HashMap<>();
        expectedSkillLevels.put(Skill.SLAYER, 74);
        expectedSkillLevels.put(Skill.WOODCUTTING, 74);
        expectedSkillLevels.put(Skill.SMITHING, 72);
        expectedSkillLevels.put(Skill.COOKING, 72);
        expectedSkillLevels.put(Skill.FLETCHING, 70);
        expectedSkillLevels.put(Skill.MINING, 66);
        expectedSkillLevels.put(Skill.HUNTER, 65);
        expectedSkillLevels.put(Skill.CRAFTING, 64);
        expectedSkillLevels.put(Skill.HERBLORE, 64);
        expectedSkillLevels.put(Skill.MAGIC, 57);
        Map<Skill, Integer> actualSkillLevels = new HashMap<>();
        requirements.stream()
            .filter(task -> task instanceof SkillLevelTask && task.getIndentLevel() == 1)
            .map(SkillLevelTask.class::cast)
            .forEach(task -> actualSkillLevels.put(task.getSkill(), task.getTargetSkillLevel()));
        assertEquals(expectedSkillLevels, actualSkillLevels);
    }
}
