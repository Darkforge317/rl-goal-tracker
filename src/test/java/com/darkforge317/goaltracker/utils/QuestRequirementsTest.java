package com.darkforge317.goaltracker.utils;

import com.darkforge317.goaltracker.models.task.QuestTask;
import com.darkforge317.goaltracker.models.task.ManualTask;
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
        // Recursive requirements: LOST_CITY + its 2 skills, NATURE_SPIRIT + 2 quest prereqs = 6 total
        assertEquals(6, requirements.size(), "Fairytale I should have 6 requirements (including recursive sub-quest requirements)");

        // Direct requirements at indent 1, sub-requirements at indent 2
        long indent1Count = requirements.stream().filter(t -> t.getIndentLevel() == 1).count();
        long indent2Count = requirements.stream().filter(t -> t.getIndentLevel() == 2).count();
        assertEquals(2, indent1Count, "Should have 2 direct requirements at indent 1");
        assertEquals(4, indent2Count, "Should have 4 sub-requirements at indent 2");

        // With recursive requirements: 4 quests (LOST_CITY, NATURE_SPIRIT, PRIEST_IN_PERIL, THE_RESTLESS_GHOST)
        // and 2 skills (Crafting 31, Woodcutting 36)
        long questCount = requirements.stream().filter(t -> t instanceof QuestTask).count();
        long skillCount = requirements.stream().filter(t -> t instanceof SkillLevelTask).count();
        assertEquals(4, questCount, "Should contain 4 QuestTask requirements (including sub-quest prereqs)");
        assertEquals(2, skillCount, "Should contain 2 SkillLevelTask requirements (including sub-quest prereqs)");

        // Verify direct quest requirements are present
        boolean hasLostCity = requirements.stream().anyMatch(t -> t instanceof QuestTask && ((QuestTask) t).getQuest() == Quest.LOST_CITY);
        boolean hasNatureSpirit = requirements.stream().anyMatch(t -> t instanceof QuestTask && ((QuestTask) t).getQuest() == Quest.NATURE_SPIRIT);
        assertTrue(hasLostCity, "Lost City should be a requirement");
        assertTrue(hasNatureSpirit, "Nature Spirit should be a requirement");

        // Verify Fairytale I has no direct Farming requirement.
        boolean hasFarming18 = requirements.stream().anyMatch(t -> t instanceof SkillLevelTask && ((SkillLevelTask) t).getSkill() == Skill.FARMING && ((SkillLevelTask) t).getTargetSkillLevel() == 18);
        assertFalse(hasFarming18, "Fairytale I should not require Farming 18");
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
        assertHasQuest(legends, Quest.SHILO_VILLAGE);
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
        assertEquals(2, dragonSlayerTwo.stream()
                .filter(QuestTask.class::isInstance)
                .map(QuestTask.class::cast)
                .filter(task -> task.getQuest() == Quest.SHILO_VILLAGE)
                .count());
        assertTrue(dragonSlayerTwo.stream().filter(QuestTask.class::isInstance)
                .map(QuestTask.class::cast)
                .anyMatch(task -> task.getQuest() == Quest.SHILO_VILLAGE && task.getIndentLevel() == 2));
        assertTrue(dragonSlayerTwo.stream().filter(QuestTask.class::isInstance)
                .map(QuestTask.class::cast)
                .anyMatch(task -> task.getQuest() == Quest.SHILO_VILLAGE && task.getIndentLevel() == 3));

        List<Task> dreamMentor = QuestRequirements.getRequirements(Quest.DREAM_MENTOR, 0);
        assertHasQuest(dreamMentor, Quest.EADGARS_RUSE);
        assertHasQuest(dreamMentor, Quest.SHILO_VILLAGE);
        assertTrue(dreamMentor.stream().anyMatch(t -> t instanceof CombatLevelTask
                && ((CombatLevelTask) t).getTargetCombatLevel() == 85));

        List<Task> eadgarsRuse = QuestRequirements.getRequirements(Quest.EADGARS_RUSE, 0);
        assertHasQuest(eadgarsRuse, Quest.DRUIDIC_RITUAL);
        assertHasQuest(eadgarsRuse, Quest.TROLL_STRONGHOLD);
        assertHasQuest(eadgarsRuse, Quest.DEATH_PLATEAU);
        assertHasSkillLevel(eadgarsRuse, Skill.HERBLORE, 31);
    }

    @Test
    void confirmedQuestRequirementCandidates_matchWikiLevels()
    {
        List<Task> anotherSlice = QuestRequirements.getRequirements(Quest.ANOTHER_SLICE_OF_HAM, 0);
        assertHasSkillLevel(anotherSlice, Skill.ATTACK, 15);
        assertHasSkillLevel(anotherSlice, Skill.PRAYER, 25);
        assertHasQuest(anotherSlice, Quest.THE_GIANT_DWARF);
        assertHasQuest(anotherSlice, Quest.THE_DIG_SITE);

        List<Task> deathToTheDorgeshuun = QuestRequirements.getRequirements(Quest.DEATH_TO_THE_DORGESHUUN, 0);
        assertHasSkillLevel(deathToTheDorgeshuun, Skill.AGILITY, 23);
        assertHasSkillLevel(deathToTheDorgeshuun, Skill.THIEVING, 23);
        assertFalse(hasSkillLevelAtIndent(deathToTheDorgeshuun, Skill.THIEVING, 17, 1));
        assertFalse(hasSkillLevelAtIndent(deathToTheDorgeshuun, Skill.MINING, 23, 1));

        List<Task> elementalWorkshopTwo = QuestRequirements.getRequirements(Quest.ELEMENTAL_WORKSHOP_II, 0);
        assertHasSkillLevel(elementalWorkshopTwo, Skill.MAGIC, 20);
        assertHasSkillLevel(elementalWorkshopTwo, Skill.SMITHING, 30);
        assertFalse(hasSkillLevelAtIndent(elementalWorkshopTwo, Skill.DEFENCE, 20, 1));
        assertFalse(hasSkillLevelAtIndent(elementalWorkshopTwo, Skill.CRAFTING, 20, 1));
        assertFalse(hasSkillLevelAtIndent(elementalWorkshopTwo, Skill.MINING, 30, 1));

        List<Task> eyesOfGlouphrie = QuestRequirements.getRequirements(Quest.THE_EYES_OF_GLOUPHRIE, 0);
        assertHasSkillLevel(eyesOfGlouphrie, Skill.CONSTRUCTION, 5);
        assertHasSkillLevel(eyesOfGlouphrie, Skill.MAGIC, 46);
        assertFalse(hasSkillLevelAtIndent(eyesOfGlouphrie, Skill.FARMING, 5, 1));

        List<Task> golem = QuestRequirements.getRequirements(Quest.THE_GOLEM, 0);
        assertHasSkillLevel(golem, Skill.CRAFTING, 20);
        assertHasSkillLevel(golem, Skill.THIEVING, 25);
        assertFalse(hasSkillLevelAtIndent(golem, Skill.CRAFTING, 25, 1));
        assertFalse(hasSkillLevelAtIndent(golem, Skill.THIEVING, 20, 1));

        List<Task> aidOfTheMyreque = QuestRequirements.getRequirements(Quest.IN_AID_OF_THE_MYREQUE, 0);
        assertHasSkillLevel(aidOfTheMyreque, Skill.AGILITY, 25);
        assertHasSkillLevel(aidOfTheMyreque, Skill.CRAFTING, 25);
        assertHasSkillLevel(aidOfTheMyreque, Skill.MINING, 15);
        assertHasSkillLevel(aidOfTheMyreque, Skill.MAGIC, 7);
        assertFalse(hasSkillLevelAtIndent(aidOfTheMyreque, Skill.MINING, 25, 1));
        assertFalse(hasSkillLevelAtIndent(aidOfTheMyreque, Skill.FISHING, 15, 1));

        assertHasSkillLevel(QuestRequirements.getRequirements(Quest.WATCHTOWER, 0), Skill.MAGIC, 14);
        assertHasSkillLevel(QuestRequirements.getRequirements(Quest.BETWEEN_A_ROCK, 0), Skill.MINING, 40);
        assertHasQuest(QuestRequirements.getRequirements(Quest.BETWEEN_A_ROCK, 0), Quest.FISHING_CONTEST);
        assertFalse(hasSkillLevelAtIndent(QuestRequirements.getRequirements(Quest.BETWEEN_A_ROCK, 0), Skill.CRAFTING, 40, 1));

        List<Task> fairytaleTwo = QuestRequirements.getRequirements(Quest.FAIRYTALE_II__CURE_A_QUEEN, 0);
        assertHasSkillLevel(fairytaleTwo, Skill.FARMING, 49);
        assertHasSkillLevel(fairytaleTwo, Skill.HERBLORE, 57);
        assertHasSkillLevel(fairytaleTwo, Skill.THIEVING, 40);
        assertFalse(hasSkillLevelAtIndent(fairytaleTwo, Skill.FARMING, 37, 1));
        assertFalse(hasSkillLevelAtIndent(fairytaleTwo, Skill.MAGIC, 49, 1));

        List<Task> wanted = QuestRequirements.getRequirements(Quest.WANTED, 0);
        assertFalse(hasSkillLevelAtIndent(wanted, Skill.AGILITY, 13, 1));
        List<Task> zogreFleshEaters = QuestRequirements.getRequirements(Quest.ZOGRE_FLESH_EATERS, 0);
        assertFalse(hasSkillLevelAtIndent(zogreFleshEaters, Skill.FLETCHING, 30, 1));

        List<Task> fremennikIsles = QuestRequirements.getRequirements(Quest.THE_FREMENNIK_ISLES, 0);
        assertHasSkillLevel(fremennikIsles, Skill.CONSTRUCTION, 20);
        assertFalse(hasSkillLevelAtIndent(fremennikIsles, Skill.AGILITY, 40, 1));

        List<Task> greatBrainRobbery = QuestRequirements.getRequirements(Quest.THE_GREAT_BRAIN_ROBBERY, 0);
        assertHasQuest(greatBrainRobbery, Quest.CREATURE_OF_FENKENSTRAIN);
        assertHasQuest(greatBrainRobbery, Quest.RECIPE_FOR_DISASTER__PIRATE_PETE);
        assertHasQuest(greatBrainRobbery, Quest.CABIN_FEVER);
        assertHasSkillLevel(greatBrainRobbery, Skill.CONSTRUCTION, 30);
        assertHasSkillLevel(greatBrainRobbery, Skill.CRAFTING, 16);
        assertHasSkillLevel(greatBrainRobbery, Skill.PRAYER, 50);
        assertFalse(hasSkillLevelAtIndent(greatBrainRobbery, Skill.COOKING, 31, 1));
        assertFalse(hasSkillLevelAtIndent(greatBrainRobbery, Skill.CRAFTING, 45, 1));

        List<Task> deviousMinds = QuestRequirements.getRequirements(Quest.DEVIOUS_MINDS, 0);
        assertHasSkillLevel(deviousMinds, Skill.FLETCHING, 50);
        assertHasSkillLevel(deviousMinds, Skill.RUNECRAFT, 50);
        assertHasSkillLevel(deviousMinds, Skill.SMITHING, 65);
        assertFalse(hasSkillLevelAtIndent(deviousMinds, Skill.THIEVING, 50, 1));

        List<Task> cabinFever = QuestRequirements.getRequirements(Quest.CABIN_FEVER, 0);
        assertHasQuest(cabinFever, Quest.PIRATES_TREASURE);
        assertHasQuest(cabinFever, Quest.RUM_DEAL);
        assertHasSkillLevel(cabinFever, Skill.AGILITY, 42);
        assertHasSkillLevel(cabinFever, Skill.CRAFTING, 45);
        assertHasSkillLevel(cabinFever, Skill.RANGED, 40);
        assertHasSkillLevel(cabinFever, Skill.SMITHING, 50);
        assertFalse(hasSkillLevelAtIndent(cabinFever, Skill.COOKING, 50, 1));

        List<Task> rumDeal = QuestRequirements.getRequirements(Quest.RUM_DEAL, 0);
        assertHasQuest(rumDeal, Quest.ZOGRE_FLESH_EATERS);
        assertHasQuest(rumDeal, Quest.PRIEST_IN_PERIL);
        assertHasSkillLevel(rumDeal, Skill.CRAFTING, 42);
        assertHasSkillLevel(rumDeal, Skill.SLAYER, 42);
        assertFalse(hasSkillLevelAtIndent(rumDeal, Skill.CRAFTING, 30, 1));

        List<Task> myArmsBigAdventure = QuestRequirements.getRequirements(Quest.MY_ARMS_BIG_ADVENTURE, 0);
        assertHasSkillLevel(myArmsBigAdventure, Skill.FARMING, 29);
        assertHasSkillLevel(myArmsBigAdventure, Skill.WOODCUTTING, 10);
        assertFalse(hasSkillLevelAtIndent(myArmsBigAdventure, Skill.AGILITY, 15, 1));
        assertFalse(hasSkillLevelAtIndent(myArmsBigAdventure, Skill.COOKING, 10, 1));

        List<Task> kingsRansom = QuestRequirements.getRequirements(Quest.KINGS_RANSOM, 0);
        assertHasSkillLevel(kingsRansom, Skill.DEFENCE, 65);
        assertHasSkillLevel(kingsRansom, Skill.MAGIC, 45);
        assertFalse(hasSkillLevelAtIndent(kingsRansom, Skill.MAGIC, 65, 1));
        assertFalse(hasSkillLevelAtIndent(kingsRansom, Skill.DEFENCE, 45, 1));

        List<Task> olafsQuest = QuestRequirements.getRequirements(Quest.OLAFS_QUEST, 0);
        assertHasSkillLevel(olafsQuest, Skill.FIREMAKING, 40);
        assertHasSkillLevel(olafsQuest, Skill.WOODCUTTING, 50);
        assertFalse(hasSkillLevelAtIndent(olafsQuest, Skill.AGILITY, 40, 1));
        assertFalse(hasSkillLevelAtIndent(olafsQuest, Skill.FIREMAKING, 50, 1));
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

    private static boolean hasSkillLevelAtIndent(List<Task> tasks, Skill skill, int target, int indent)
    {
        return tasks.stream().filter(SkillLevelTask.class::isInstance).map(SkillLevelTask.class::cast)
                .anyMatch(task -> task.getSkill() == skill && task.getTargetSkillLevel() == target
                        && task.getIndentLevel() == indent);
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

        assertEquals(2, requirements.stream()
            .filter(QuestTask.class::isInstance)
            .map(QuestTask.class::cast)
            .filter(task -> task.getQuest() == Quest.A_TASTE_OF_HOPE)
            .count());
        assertEquals(2, requirements.stream()
            .filter(QuestTask.class::isInstance)
            .map(QuestTask.class::cast)
            .filter(task -> task.getQuest() == Quest.DARKNESS_OF_HALLOWVALE)
            .count());

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

    @Test
    void reviewedQuestRequirementCandidates_matchCurrentWikiRequirements()
    {
        List<Task> throne = QuestRequirements.getRequirements(Quest.THRONE_OF_MISCELLANIA, 0);
        assertHasQuest(throne, Quest.THE_FREMENNIK_TRIALS);
        assertFalse(hasSkillLevelAtIndent(throne, Skill.COOKING, 45, 1));

        List<Task> swanSong = QuestRequirements.getRequirements(Quest.SWAN_SONG, 0);
        assertHasQuestPoints(swanSong, 100);
        assertHasQuest(swanSong, Quest.GARDEN_OF_TRANQUILLITY);
        assertHasSkillLevel(swanSong, Skill.MAGIC, 66);
        assertHasSkillLevel(swanSong, Skill.FIREMAKING, 42);
        assertHasSkillLevel(swanSong, Skill.COOKING, 62);
        assertFalse(hasSkillLevelAtIndent(swanSong, Skill.WOODCUTTING, 62, 1));

        List<Task> hauntedMine = QuestRequirements.getRequirements(Quest.HAUNTED_MINE, 0);
        assertHasQuest(hauntedMine, Quest.PRIEST_IN_PERIL);
        assertFalse(hasSkillLevelAtIndent(hauntedMine, Skill.AGILITY, 15, 1));

        List<Task> ratcatchers = QuestRequirements.getRequirements(Quest.RATCATCHERS, 0);
        assertHasQuest(ratcatchers, Quest.GERTRUDES_CAT);
        assertFalse(hasSkillLevelAtIndent(ratcatchers, Skill.THIEVING, 33, 1));

        List<Task> desertTreasure = QuestRequirements.getRequirements(Quest.DESERT_TREASURE_I, 0);
        assertHasQuest(desertTreasure, Quest.THE_TOURIST_TRAP);
        assertHasQuest(desertTreasure, Quest.PRIEST_IN_PERIL);
        assertHasQuest(desertTreasure, Quest.WATERFALL_QUEST);
        assertHasSkillLevel(desertTreasure, Skill.SLAYER, 10);
        assertFalse(hasSkillLevelAtIndent(desertTreasure, Skill.HERBLORE, 10, 1));

        List<Task> slugMenace = QuestRequirements.getRequirements(Quest.THE_SLUG_MENACE, 0);
        assertHasQuest(slugMenace, Quest.SEA_SLUG);
        assertHasSkillLevel(slugMenace, Skill.RUNECRAFT, 30);
        assertHasSkillLevel(slugMenace, Skill.CRAFTING, 30);
        assertHasSkillLevel(slugMenace, Skill.SLAYER, 30);
        assertHasSkillLevel(slugMenace, Skill.THIEVING, 30);
        assertFalse(hasSkillLevelAtIndent(slugMenace, Skill.SMITHING, 30, 1));

        List<Task> myArms = QuestRequirements.getRequirements(Quest.MY_ARMS_BIG_ADVENTURE, 0);
        assertHasQuest(myArms, Quest.EADGARS_RUSE);
        assertHasQuest(myArms, Quest.THE_FEUD);

        List<Task> wanted = QuestRequirements.getRequirements(Quest.WANTED, 0);
        assertHasQuest(wanted, Quest.THE_LOST_TRIBE);
        assertHasQuest(wanted, Quest.PRIEST_IN_PERIL);
        assertHasQuest(wanted, Quest.ENTER_THE_ABYSS);
        assertHasQuestPoints(wanted, 32);
        assertFalse(hasSkillLevelAtIndent(wanted, Skill.AGILITY, 13, 1));

        List<Task> enlightenedJourney = QuestRequirements.getRequirements(Quest.ENLIGHTENED_JOURNEY, 0);
        assertHasQuestPoints(enlightenedJourney, 20);
        assertHasQuestPoints(QuestRequirements.getRequirements(Quest.TEARS_OF_GUTHIX, 0), 43);
        assertHasQuestPoints(QuestRequirements.getRequirements(Quest.RECIPE_FOR_DISASTER__CULINAROMANCER, 0), 175);

        List<Task> mourningPartOne = QuestRequirements.getRequirements(Quest.MOURNINGS_END_PART_I, 0);
        assertHasSkillLevel(mourningPartOne, Skill.RANGED, 60);
        assertHasQuest(mourningPartOne, Quest.BIG_CHOMPY_BIRD_HUNTING);
        assertHasQuest(mourningPartOne, Quest.SHEEP_HERDER);
        assertFalse(hasSkillLevelAtIndent(mourningPartOne, Skill.AGILITY, 60, 1));
        assertFalse(hasSkillLevelAtIndent(QuestRequirements.getRequirements(Quest.MOURNINGS_END_PART_II, 0),
                Skill.AGILITY, 60, 1));

        List<Task> regicide = QuestRequirements.getRequirements(Quest.REGICIDE, 0);
        assertFalse(hasSkillLevelAtIndent(regicide, Skill.FLETCHING, 25, 1));

        List<Task> monkeyMadnessTwo = QuestRequirements.getRequirements(Quest.MONKEY_MADNESS_II, 0);
        assertHasQuest(monkeyMadnessTwo, Quest.RECIPE_FOR_DISASTER__KING_AWOWOGEI);

        List<Task> heroesQuest = QuestRequirements.getRequirements(Quest.HEROES_QUEST, 0);
        assertHasQuestPoints(heroesQuest, 55);
        assertFalse(hasSkillLevelAtIndent(heroesQuest, Skill.AGILITY, 25, 1));

        List<Task> fremennikExiles = QuestRequirements.getRequirements(Quest.THE_FREMENNIK_EXILES, 0);
        assertHasQuest(fremennikExiles, Quest.LUNAR_DIPLOMACY);
        assertHasQuest(fremennikExiles, Quest.MOUNTAIN_DAUGHTER);
        assertHasSkillLevel(fremennikExiles, Skill.MINING, 60);

        List<Task> desertTreasureTwo = QuestRequirements.getRequirements(Quest.DESERT_TREASURE_II__THE_FALLEN_EMPIRE, 0);
        assertHasQuest(desertTreasureTwo, Quest.DESERT_TREASURE_I);
        assertHasQuest(desertTreasureTwo, Quest.SECRETS_OF_THE_NORTH);
        assertHasQuest(desertTreasureTwo, Quest.ENAKHRAS_LAMENT);
        assertHasQuest(desertTreasureTwo, Quest.TEMPLE_OF_THE_EYE);
        assertHasQuest(desertTreasureTwo, Quest.THE_GARDEN_OF_DEATH);
        assertHasQuest(desertTreasureTwo, Quest.BELOW_ICE_MOUNTAIN);
        assertHasQuest(desertTreasureTwo, Quest.HIS_FAITHFUL_SERVANTS);
        assertHasQuest(desertTreasureTwo, Quest.THE_GENERALS_SHADOW);
        assertHasQuest(desertTreasureTwo, Quest.DEVIOUS_MINDS);
        assertHasQuest(desertTreasureTwo, Quest.HAZEEL_CULT);
        assertHasSkillLevel(desertTreasureTwo, Skill.FIREMAKING, 75);
        assertHasSkillLevel(desertTreasureTwo, Skill.CONSTRUCTION, 60);

        List<Task> secretsOfTheNorth = QuestRequirements.getRequirements(Quest.SECRETS_OF_THE_NORTH, 0);
        assertHasQuest(secretsOfTheNorth, Quest.MAKING_FRIENDS_WITH_MY_ARM);
        assertHasQuest(secretsOfTheNorth, Quest.SWAN_SONG);
        assertHasQuest(secretsOfTheNorth, Quest.GARDEN_OF_TRANQUILLITY);
        assertHasQuest(secretsOfTheNorth, Quest.COLD_WAR);
        assertHasQuest(secretsOfTheNorth, Quest.ROMEO__JULIET);
        assertHasQuest(secretsOfTheNorth, Quest.THE_GENERALS_SHADOW);
        assertHasQuest(secretsOfTheNorth, Quest.FIGHT_ARENA);
        assertHasQuest(secretsOfTheNorth, Quest.DEVIOUS_MINDS);
        assertHasQuest(secretsOfTheNorth, Quest.DORICS_QUEST);
        assertHasQuest(secretsOfTheNorth, Quest.ENTER_THE_ABYSS);
        assertHasQuest(secretsOfTheNorth, Quest.HAZEEL_CULT);
        assertFalse(secretsOfTheNorth.stream().filter(QuestTask.class::isInstance)
                .map(QuestTask.class::cast).anyMatch(task -> task.getQuest() == Quest.MAKING_HISTORY));

        List<Task> whileGuthixSleeps = QuestRequirements.getRequirements(Quest.WHILE_GUTHIX_SLEEPS, 0);
        assertHasQuestPoints(whileGuthixSleeps, 180);
        assertHasQuest(whileGuthixSleeps, Quest.DEFENDER_OF_VARROCK);
        assertHasQuest(whileGuthixSleeps, Quest.THE_PATH_OF_GLOUPHRIE);
        assertHasQuest(whileGuthixSleeps, Quest.FIGHT_ARENA);
        assertHasQuest(whileGuthixSleeps, Quest.DREAM_MENTOR);
        assertHasQuest(whileGuthixSleeps, Quest.THE_HAND_IN_THE_SAND);
        assertHasQuest(whileGuthixSleeps, Quest.WANTED);
        assertHasQuest(whileGuthixSleeps, Quest.TEMPLE_OF_THE_EYE);
        assertHasQuest(whileGuthixSleeps, Quest.TEARS_OF_GUTHIX);
        assertHasQuest(whileGuthixSleeps, Quest.NATURE_SPIRIT);
        assertHasQuest(whileGuthixSleeps, Quest.A_TAIL_OF_TWO_CATS);
        assertHasSkillLevel(whileGuthixSleeps, Skill.THIEVING, 72);
        assertHasSkillLevel(whileGuthixSleeps, Skill.FIREMAKING, 49);
        assertTrue(whileGuthixSleeps.stream().filter(ManualTask.class::isInstance).map(ManualTask.class::cast)
                .anyMatch(task -> task.getDescription().equals("Access to the Warriors' Guild")
                        && task.getIndentLevel() == 1));
        assertFalse(whileGuthixSleeps.stream().filter(QuestTask.class::isInstance)
                .map(QuestTask.class::cast).anyMatch(task -> task.getQuest() == Quest.THE_CURSE_OF_ARRAV));

        List<Task> finalDawn = QuestRequirements.getRequirements(Quest.THE_FINAL_DAWN, 0);
        assertHasQuest(finalDawn, Quest.TWILIGHTS_PROMISE);
        assertFalse(finalDawn.stream().filter(QuestTask.class::isInstance).map(QuestTask.class::cast)
                .anyMatch(task -> task.getQuest() == Quest.TWILIGHTS_PROMISE && task.getIndentLevel() == 1));

        List<Task> tasteOfHope = QuestRequirements.getRequirements(Quest.A_TASTE_OF_HOPE, 0);
        assertHasSkillLevel(tasteOfHope, Skill.AGILITY, 45);
        assertHasSkillLevel(tasteOfHope, Skill.ATTACK, 40);
        assertHasSkillLevel(tasteOfHope, Skill.CRAFTING, 48);
        assertHasSkillLevel(tasteOfHope, Skill.HERBLORE, 40);
        assertHasSkillLevel(tasteOfHope, Skill.SLAYER, 38);

        List<Task> finalBranches = QuestRequirements.getRequirements(Quest.THE_FREMENNIK_EXILES, 0);
        assertHasQuest(finalBranches, Quest.HEROES_QUEST);
        assertHasQuest(finalBranches, Quest.THE_FREMENNIK_ISLES);

        List<Task> atFirstLight = QuestRequirements.getRequirements(Quest.AT_FIRST_LIGHT, 0);
        assertHasQuest(atFirstLight, Quest.EAGLES_PEAK);
        assertHasSkillLevel(atFirstLight, Skill.HUNTER, 46);
        assertHasQuest(QuestRequirements.getRequirements(Quest.PERILOUS_MOONS, 0), Quest.TWILIGHTS_PROMISE);
        assertHasQuest(QuestRequirements.getRequirements(Quest.SHADOWS_OF_CUSTODIA, 0), Quest.CHILDREN_OF_THE_SUN);
        assertHasQuest(QuestRequirements.getRequirements(Quest.SCRAMBLED, 0), Quest.CHILDREN_OF_THE_SUN);
    }

    @Test
    void lunarDiplomacyRequirements_matchUserVerifiedList()
    {
        List<Task> requirements = QuestRequirements.getRequirements(Quest.LUNAR_DIPLOMACY, 0);
        assertHasQuest(requirements, Quest.THE_FREMENNIK_TRIALS);
        assertHasQuest(requirements, Quest.LOST_CITY);
        assertHasQuest(requirements, Quest.RUNE_MYSTERIES);
        assertHasQuest(requirements, Quest.SHILO_VILLAGE);
        assertHasQuest(requirements, Quest.JUNGLE_POTION);
        assertHasSkillLevel(requirements, Skill.HERBLORE, 5);
        assertHasSkillLevel(requirements, Skill.CRAFTING, 61);
        assertHasSkillLevel(requirements, Skill.DEFENCE, 40);
        assertHasSkillLevel(requirements, Skill.FIREMAKING, 49);
        assertHasSkillLevel(requirements, Skill.MAGIC, 65);
        assertHasSkillLevel(requirements, Skill.MINING, 60);
        assertHasSkillLevel(requirements, Skill.WOODCUTTING, 55);
        assertFalse(hasSkillLevelAtIndent(requirements, Skill.AGILITY, 32, 1));
    }

    @Test
    void shieldOfArravRequiresFindingAPartner()
    {
        List<Task> requirements = QuestRequirements.getRequirements(Quest.SHIELD_OF_ARRAV, 0);

        assertTrue(requirements.stream().filter(ManualTask.class::isInstance).map(ManualTask.class::cast)
                .anyMatch(task -> task.getDescription().equals("Find a partner for the quest")
                        && task.getIndentLevel() == 1));
    }
}
