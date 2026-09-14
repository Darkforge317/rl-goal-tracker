package com.darkforge317.goaltracker.presets;

import com.darkforge317.goaltracker.models.Goal;
import com.darkforge317.goaltracker.models.task.ItemTask;
import com.darkforge317.goaltracker.models.task.ManualTask;
import com.darkforge317.goaltracker.models.task.QuestTask;
import com.darkforge317.goaltracker.models.task.SkillLevelTask;
import com.darkforge317.goaltracker.utils.ReorderableList;
import lombok.Getter;
import net.runelite.api.Quest;
import net.runelite.api.Skill;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static net.runelite.api.gameval.ItemID.*;

public class GoalPresetRepository {

    @Getter
    public static final class Preset {
        private final String name;
        private final String description;
        private final List<Goal> goals;
        public Preset(String name, String description, List<Goal> goals) {
            this.name = name;
            this.description = description;
            this.goals = goals;
        }

        @Override public String toString() { return name; }
    }

    public static List<Preset> getAll() {
        List<Preset> list = new ArrayList<>();
        list.add(buildEarlyIronman());
        list.add(buildMidIronman());
        list.add(buildLateIronman());
        list.add(buildFullVoidArmor());
        list.add(buildFastTravelUnlocks());
        list.add(buildFreeToPlayQuests());
        list.add(buildAllBarrowsGear());
        return list;
    }


    private static Preset buildEarlyIronman() {
        Goal early = Goal.builder()
                .description("Early Ironman Progression")
                .tasks(ReorderableList.from(
                        // Core Early Skills (unlock movement & teleports)
                        SkillLevelTask.builder().skill(Skill.AGILITY).targetSkillLevel(30).build(),
                        SkillLevelTask.builder().skill(Skill.MAGIC).targetSkillLevel(37).build(), // Falador/Camelot teles
                        SkillLevelTask.builder().skill(Skill.THIEVING).targetSkillLevel(20).build(),
                        // Early set-up gear
                        ItemTask.builder().itemId(GRACEFUL_HOOD).itemName("Graceful hood").quantity(1).build(),
                        ItemTask.builder().itemId(GRACEFUL_TOP).itemName("Graceful top").quantity(1).build(),
                        ItemTask.builder().itemId(GRACEFUL_LEGS).itemName("Graceful legs").quantity(1).build(),
                        ItemTask.builder().itemId(GRACEFUL_GLOVES).itemName("Graceful gloves").quantity(1).build(),
                        ItemTask.builder().itemId(GRACEFUL_BOOTS).itemName("Graceful boots").quantity(1).build(),
                        ItemTask.builder().itemId(GRACEFUL_CAPE).itemName("Graceful cape").quantity(1).build(),
                        // Foundational quests for stats/unlocks
                        QuestTask.builder().quest(Quest.DRUIDIC_RITUAL).build(),
                        QuestTask.builder().quest(Quest.WATERFALL_QUEST).build(),
                        QuestTask.builder().quest(Quest.PRIEST_IN_PERIL).build(),
                        QuestTask.builder().quest(Quest.TREE_GNOME_VILLAGE).build(),
                        QuestTask.builder().quest(Quest.THE_GRAND_TREE).build(),
                        // Birdhouse runs (Fossil Island access)
                        QuestTask.builder().quest(Quest.THE_DIG_SITE).build(),
                        QuestTask.builder().quest(Quest.BONE_VOYAGE).build(),
                        SkillLevelTask.builder().skill(Skill.HUNTER).targetSkillLevel(5).build(),
                        SkillLevelTask.builder().skill(Skill.CRAFTING).targetSkillLevel(8).build(), // clockwork
                        SkillLevelTask.builder().skill(Skill.CONSTRUCTION).targetSkillLevel(16).build(), // clockmaker's bench

                        // Seaweed runs (Giant seaweed patches on Fossil Island)
                        SkillLevelTask.builder().skill(Skill.FARMING).targetSkillLevel(23).build()
                ))
                .build();
        return new Preset("Early Ironman Progression", "Stats, gear, and quest goals for early game Ironman.", Arrays.asList(early));
    }

    private static Preset buildMidIronman() {
        Goal mid = Goal.builder()
                .description("Mid Ironman Progression")
                .tasks(ReorderableList.from(
                        // Midgame Skill Targets
                        SkillLevelTask.builder().skill(Skill.ATTACK).targetSkillLevel(60).build(),
                        SkillLevelTask.builder().skill(Skill.STRENGTH).targetSkillLevel(60).build(),
                        SkillLevelTask.builder().skill(Skill.DEFENCE).targetSkillLevel(60).build(),
                        SkillLevelTask.builder().skill(Skill.RANGED).targetSkillLevel(60).build(),
                        SkillLevelTask.builder().skill(Skill.MAGIC).targetSkillLevel(55).build(), // High Alch
                        SkillLevelTask.builder().skill(Skill.PRAYER).targetSkillLevel(43).build(), // Protect prayers
                        SkillLevelTask.builder().skill(Skill.AGILITY).targetSkillLevel(60).build(),
                        // Midgame Gear & Upgrades
                        ItemTask.builder().itemId(DRAGON_SCIMITAR).itemName("Dragon scimitar").quantity(1).build(),
                        ItemTask.builder().itemId(DRAGON_PARRYINGDAGGER).itemName("Dragon defender").quantity(1).build(),
                        ItemTask.builder().itemId(BARBASSAULT_PENANCE_FIGHTER_TORSO).itemName("Fighter torso").quantity(1).build(),
                        ItemTask.builder().itemId(XBOWS_CROSSBOW_RUNITE).itemName("Rune crossbow").quantity(1).build(),
                        ItemTask.builder().itemId(IBANSTAFF_UPGRADED).itemName("Iban's staff (u)").quantity(1).build(),
                        ItemTask.builder().itemId(HUNDRED_GAUNTLETS_LEVEL_10).itemName("Barrows gloves").quantity(1).build(),
                        ItemTask.builder().itemId(ANMA_50_REWARD).itemName("Ava's accumulator").quantity(1).build(),
                        // Midgame Quests / Unlocks
                        QuestTask.builder().quest(Quest.RECIPE_FOR_DISASTER).build(),
                        QuestTask.builder().quest(Quest.FAIRYTALE_II__CURE_A_QUEEN).build(), // Fairy rings access (partial)
                        QuestTask.builder().quest(Quest.LUNAR_DIPLOMACY).build(),
                        QuestTask.builder().quest(Quest.ANIMAL_MAGNETISM).build(),
                        QuestTask.builder().quest(Quest.UNDERGROUND_PASS).build()
                ))
                .build();
        return new Preset("Mid Ironman Progression", "Stats, gear, and quest goals for mid game Ironman.", Arrays.asList(mid));
    }

    private static Preset buildLateIronman() {
        Goal late = Goal.builder()
                .description("Late Ironman Progression")
                .tasks(ReorderableList.from(
                        // Late Skill Targets
                        SkillLevelTask.builder().skill(Skill.ATTACK).targetSkillLevel(85).build(),
                        SkillLevelTask.builder().skill(Skill.STRENGTH).targetSkillLevel(85).build(),
                        SkillLevelTask.builder().skill(Skill.DEFENCE).targetSkillLevel(85).build(),
                        SkillLevelTask.builder().skill(Skill.MAGIC).targetSkillLevel(94).build(), // Vengeance/Ice Barrage
                        SkillLevelTask.builder().skill(Skill.PRAYER).targetSkillLevel(77).build(), // Rigour/Augury
                        SkillLevelTask.builder().skill(Skill.RANGED).targetSkillLevel(85).build(),
                        // Late Gear Goals
                        ItemTask.builder().itemId(ABYSSAL_TENTACLE).itemName("Abyssal tentacle").quantity(1).build(),
                        ItemTask.builder().itemId(TOXIC_BLOWPIPE).itemName("Toxic blowpipe").quantity(1).build(),
                        ItemTask.builder().itemId(ACB).itemName("Armadyl crossbow").quantity(1).build(),
                        ItemTask.builder().itemId(BOW_OF_FAERDHINEN).itemName("Bow of Faerdhinen").quantity(1).build(),
                        ItemTask.builder().itemId(BANDOS_CHESTPLATE).itemName("Bandos chestplate").quantity(1).build(),
                        ItemTask.builder().itemId(BANDOS_SKIRT).itemName("Bandos tassets").quantity(1).build(),
                        ItemTask.builder().itemId(TOTS_CHARGED).itemName("Trident of the seas").quantity(1).build(),
                        ItemTask.builder().itemId(ZENYTE_RING_ENCHANTED).itemName("Ring of suffering").quantity(1).build(),
                        ItemTask.builder().itemId(NZONE_ZENYTE_RING_ENCHANTED).itemName("Ring of suffering (i)").quantity(1).build(),
                        ItemTask.builder().itemId(NZONE_SALVE_AMULET_E).itemName("Salve amulet (ei)").quantity(1).build(),
                        ItemTask.builder().itemId(INFERNAL_CAPE).itemName("Infernal cape").quantity(1).build(),
                        // Late Quests / Diaries
                        QuestTask.builder().quest(Quest.SONG_OF_THE_ELVES).build(),
                        QuestTask.builder().quest(Quest.SINS_OF_THE_FATHER).build(),
                        QuestTask.builder().quest(Quest.MONKEY_MADNESS_II).build(),
                        QuestTask.builder().quest(Quest.DESERT_TREASURE_I).build()
                ))
                .build();
        return new Preset("Late Ironman Progression", "Stats, gear, and quest goals for late game Ironman.", Arrays.asList(late));
    }
    private static Preset buildFullVoidArmor() {
        Goal voidSet = Goal.builder()
                .description("Full Void Armor Set")
                .tasks(ReorderableList.from(
                        ItemTask.builder().itemId(PEST_VOID_KNIGHT_TOP).itemName("Void knight top").quantity(1).build(),
                        ItemTask.builder().itemId(PEST_VOID_KNIGHT_ROBES).itemName("Void knight robe").quantity(1).build(),
                        ItemTask.builder().itemId(PEST_VOID_KNIGHT_GLOVES).itemName("Void knight gloves").quantity(1).build(),
                        ItemTask.builder().itemId(GAME_PEST_MELEE_HELM).itemName("Void melee helm").quantity(1).build(),
                        ItemTask.builder().itemId(GAME_PEST_ARCHER_HELM).itemName("Void ranger helm").quantity(1).build(),
                        ItemTask.builder().itemId(GAME_PEST_MAGE_HELM).itemName("Void mage helm").quantity(1).build()
                ))
                .build();
        return new Preset("Full Void Armor", "All base Void pieces: top, robe, gloves, and all three helms.", Arrays.asList(voidSet));
    }
    private static Preset buildFreeToPlayQuests() {
        Goal f2p = Goal.builder()
                .description("All Free-to-Play Quests")
                .tasks(ReorderableList.from(
                        // Suggested quick-to-hard order
                        QuestTask.builder().quest(Quest.COOKS_ASSISTANT).build(),
                        QuestTask.builder().quest(Quest.SHEEP_SHEARER).build(),
                        QuestTask.builder().quest(Quest.ROMEO__JULIET).build(),
                        QuestTask.builder().quest(Quest.THE_RESTLESS_GHOST).build(),
                        QuestTask.builder().quest(Quest.IMP_CATCHER).build(),
                        QuestTask.builder().quest(Quest.DORICS_QUEST).build(),
                        QuestTask.builder().quest(Quest.GOBLIN_DIPLOMACY).build(),
                        QuestTask.builder().quest(Quest.ERNEST_THE_CHICKEN).build(),
                        QuestTask.builder().quest(Quest.THE_KNIGHTS_SWORD).build(),
                        QuestTask.builder().quest(Quest.PIRATES_TREASURE).build(),
                        QuestTask.builder().quest(Quest.PRINCE_ALI_RESCUE).build(),
                        QuestTask.builder().quest(Quest.BLACK_KNIGHTS_FORTRESS).build(),
                        QuestTask.builder().quest(Quest.VAMPYRE_SLAYER).build(),
                        QuestTask.builder().quest(Quest.DEMON_SLAYER).build(),
                        QuestTask.builder().quest(Quest.RUNE_MYSTERIES).build(),
                        QuestTask.builder().quest(Quest.MISTHALIN_MYSTERY).build(),
                        QuestTask.builder().quest(Quest.THE_CORSAIR_CURSE).build(),
                        // Note: Shield of Arrav needs a partner
                        QuestTask.builder().quest(Quest.SHIELD_OF_ARRAV).build(),
                        ManualTask.builder().description("Find a partner for Shield of Arrav (Phoenix/Black Arm) or coordinate in a clan chat").indentLevel(1).build(),
                        // Capstone F2P quest
                        QuestTask.builder().quest(Quest.DRAGON_SLAYER_I).build()
                ))
                .build();
        return new Preset("Free-to-Play Quests", "Every F2P quest in OSRS (20 total) as of today.", Arrays.asList(f2p));
    }
    private static Preset buildFastTravelUnlocks() {
        Goal travel = Goal.builder()
                .description("Fast Travel Unlocks (Quest-gated)")
                .tasks(ReorderableList.from(
                        // Spirit trees & gliders
                        QuestTask.builder().quest(Quest.TREE_GNOME_VILLAGE).build(),
                        QuestTask.builder().quest(Quest.THE_GRAND_TREE).build(),
                        // Fairy rings (partial completion allows use with dramen/lunar staff)
                        QuestTask.builder().quest(Quest.FAIRYTALE_II__CURE_A_QUEEN).build(),
                        // Balloons
                        QuestTask.builder().quest(Quest.ENLIGHTENED_JOURNEY).build(),
                        // City teleport spells unlocked by quests
                        QuestTask.builder().quest(Quest.WATCHTOWER).build(),
                        QuestTask.builder().quest(Quest.PLAGUE_CITY).build(),
                        QuestTask.builder().quest(Quest.EADGARS_RUSE).build(),
                        // Teleport items from quests
                        QuestTask.builder().quest(Quest.GHOSTS_AHOY).build(), // Ectophial
                        QuestTask.builder().quest(Quest.THE_DIG_SITE).build(), // Digsite pendant access
                        QuestTask.builder().quest(Quest.BONE_VOYAGE).build(), // Fossil Island travel
                        QuestTask.builder().quest(Quest.MONKEY_MADNESS_II).build(), // Royal seed pod
                        QuestTask.builder().quest(Quest.A_TASTE_OF_HOPE).build(), // Drakan's medallion teleports in Morytania
                        // Kourend memoirs teleports
                        QuestTask.builder().quest(Quest.CLIENT_OF_KOUREND).build(),
                        QuestTask.builder().quest(Quest.THE_DEPTHS_OF_DESPAIR).build(),
                        QuestTask.builder().quest(Quest.THE_QUEEN_OF_THIEVES).build(),
                        QuestTask.builder().quest(Quest.TALE_OF_THE_RIGHTEOUS).build(),
                        QuestTask.builder().quest(Quest.THE_FORSAKEN_TOWER).build(),
                        QuestTask.builder().quest(Quest.THE_ASCENT_OF_ARCEUUS).build(),
                        // Other travel networks
                        QuestTask.builder().quest(Quest.EAGLES_PEAK).build(), // Eagle transport
                        QuestTask.builder().quest(Quest.SHILO_VILLAGE).build(), // Brimhaven–Shilo carts
                        QuestTask.builder().quest(Quest.THE_GIANT_DWARF).build(), // Keldagrim mine carts
                        QuestTask.builder().quest(Quest.ANOTHER_SLICE_OF_HAM).build(), // Dorgesh-Kaan ↔ Keldagrim train
                        QuestTask.builder().quest(Quest.THE_FREMENNIK_TRIALS).build(), // Enchanted lyre to Rellekka
                        // Spellbook unlocks with many teleports
                        QuestTask.builder().quest(Quest.DESERT_TREASURE_I).build(), // Ancient Magicks
                        QuestTask.builder().quest(Quest.LUNAR_DIPLOMACY).build() // Lunar teleports
                ))
                .build();
        return new Preset(
                "Fast Travel Unlocks",
                "Quests that unlock major transportation methods: spirit trees, gliders, fairy rings, balloons, city teleports, carts, memoirs, seed pod, and more.",
                Arrays.asList(travel)
        );
    }

    private static Preset buildAllBarrowsGear() {
        Goal barrows = Goal.builder()
                .description("All Barrows Gear")
                .tasks(ReorderableList.from(
                        ItemTask.builder().itemId(BARROWS_AHRIM_HEAD).itemName("Ahrim's hood").quantity(1).build(),
                        ItemTask.builder().itemId(BARROWS_AHRIM_BODY).itemName("Ahrim's robetop").quantity(1).build(),
                        ItemTask.builder().itemId(BARROWS_AHRIM_LEGS).itemName("Ahrim's robeskirt").quantity(1).build(),
                        ItemTask.builder().itemId(BARROWS_AHRIM_WEAPON).itemName("Ahrim's staff").quantity(1).build(),
                        ItemTask.builder().itemId(BARROWS_DHAROK_HEAD).itemName("Dharok's helm").quantity(1).build(),
                        ItemTask.builder().itemId(BARROWS_DHAROK_BODY).itemName("Dharok's platebody").quantity(1).build(),
                        ItemTask.builder().itemId(BARROWS_DHAROK_LEGS).itemName("Dharok's platelegs").quantity(1).build(),
                        ItemTask.builder().itemId(BARROWS_DHAROK_WEAPON).itemName("Dharok's greataxe").quantity(1).build(),
                        ItemTask.builder().itemId(BARROWS_GUTHAN_HEAD).itemName("Guthan's helm").quantity(1).build(),
                        ItemTask.builder().itemId(BARROWS_GUTHAN_BODY).itemName("Guthan's platebody").quantity(1).build(),
                        ItemTask.builder().itemId(BARROWS_GUTHAN_LEGS).itemName("Guthan's chainskirt").quantity(1).build(),
                        ItemTask.builder().itemId(BARROWS_GUTHAN_WEAPON).itemName("Guthan's warspear").quantity(1).build(),
                        ItemTask.builder().itemId(BARROWS_KARIL_HEAD).itemName("Karil's coif").quantity(1).build(),
                        ItemTask.builder().itemId(BARROWS_KARIL_BODY).itemName("Karil's leathertop").quantity(1).build(),
                        ItemTask.builder().itemId(BARROWS_KARIL_LEGS).itemName("Karil's leatherskirt").quantity(1).build(),
                        ItemTask.builder().itemId(BARROWS_KARIL_WEAPON).itemName("Karil's crossbow").quantity(1).build(),
                        ItemTask.builder().itemId(BARROWS_TORAG_HEAD).itemName("Torag's helm").quantity(1).build(),
                        ItemTask.builder().itemId(BARROWS_TORAG_BODY).itemName("Torag's platebody").quantity(1).build(),
                        ItemTask.builder().itemId(BARROWS_TORAG_LEGS).itemName("Torag's platelegs").quantity(1).build(),
                        ItemTask.builder().itemId(BARROWS_TORAG_WEAPON).itemName("Torag's hammers").quantity(1).build(),
                        ItemTask.builder().itemId(BARROWS_VERAC_HEAD).itemName("Verac's helm").quantity(1).build(),
                        ItemTask.builder().itemId(BARROWS_VERAC_BODY).itemName("Verac's brassard").quantity(1).build(),
                        ItemTask.builder().itemId(BARROWS_VERAC_LEGS).itemName("Verac's plateskirt").quantity(1).build(),
                        ItemTask.builder().itemId(BARROWS_VERAC_WEAPON).itemName("Verac's flail").quantity(1).build()
                ))
                .build();
        return new Preset("All Barrows Gear", "Collect a full set of each Barrows brothers' equipment.", Arrays.asList(barrows));
    }
}
