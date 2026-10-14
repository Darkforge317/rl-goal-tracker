package com.darkforge317.goaltracker;

import com.darkforge317.goaltracker.models.Goal;
import com.darkforge317.goaltracker.models.task.ManualTask;
import com.darkforge317.goaltracker.models.task.SkillLevelTask;
import com.darkforge317.goaltracker.models.task.SkillXpTask;
import com.darkforge317.goaltracker.models.task.QuestTask;
import com.darkforge317.goaltracker.models.task.ItemTask;
import com.darkforge317.goaltracker.models.task.CombatLevelTask;
import com.darkforge317.goaltracker.models.task.QuestPointsTask;
import com.darkforge317.goaltracker.models.enums.Status;
import com.darkforge317.goaltracker.models.enums.TaskType;
import com.darkforge317.goaltracker.utils.ReorderableList;
import com.google.common.io.Resources;
import net.runelite.api.Quest;
import net.runelite.api.Skill;
import net.runelite.http.api.RuneLiteAPI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

//noinspection UnstableApiUsage
@SuppressWarnings("null")
public class GoalSerializerTest {
    /**
     * Normalizes all line endings to LF (\n) for consistent string comparisons across platforms.
     * 
     * @param dirtyString The string whose line-endings you want to normalize.
     * @return The normalized string with the requested line endings.
     */
    private String normalizeLineEndings(String dirtyString) {
        if (dirtyString == null) return null;

        // Replace and return LF line endings
        return dirtyString.replace("\r\n", "\n").replace("\r", "\n").trim();
    }

    GoalSerializer serializer;

    @BeforeEach
    void setUp() {
        serializer = new GoalSerializer(RuneLiteAPI.GSON);
    }

    @Test
    public void deserialize_should_parse_successfully() throws IOException {
        String json = Resources.toString(Resources.getResource("data.json"), Charset.defaultCharset());

        List<Goal> goals = serializer.deserialize(json);

        assertEquals(1, goals.size());
        assertEquals("My Goal", goals.get(0).getDescription());
        assertEquals(-1, goals.get(0).getDisplayOrder());
        assertEquals(1, goals.get(0).getTasks().size());

        assertEquals(ManualTask.class, goals.get(0).getTasks().get(0).getClass());

        ManualTask task = (ManualTask) goals.get(0).getTasks().get(0);

        assertEquals(TaskType.MANUAL, task.getType());
        assertEquals(Status.COMPLETED, task.getStatus());
        assertTrue(task.isNotified());
        assertTrue(task.isDone());
        assertEquals(0, task.getIndentLevel());
        assertEquals("Do all the things!", task.getDescription());
    }

    @Test
    public void deserialize_should_support_other_task_types() throws IOException {
        String json = Resources.toString(Resources.getResource("complex.json"), Charset.defaultCharset());

        List<Goal> goals = serializer.deserialize(json);

        assertEquals(1, goals.size());
        assertEquals("My Goal", goals.get(0).getDescription());
        assertEquals(-1, goals.get(0).getDisplayOrder());
        assertEquals(6, goals.get(0).getTasks().size());

        assertEquals(ManualTask.class, goals.get(0).getTasks().get(0).getClass());
        assertEquals(0, goals.get(0).getTasks().get(0).getIndentLevel());
        assertEquals(SkillLevelTask.class, goals.get(0).getTasks().get(1).getClass());
        assertEquals(1, goals.get(0).getTasks().get(1).getIndentLevel());

        SkillLevelTask skillLevelTask = (SkillLevelTask) goals.get(0).getTasks().get(1);

        assertEquals(99, skillLevelTask.getTargetSkillLevel());
        assertEquals(Skill.ATTACK, skillLevelTask.getSkill());
        assertEquals(1, skillLevelTask.getIndentLevel());

        assertEquals(SkillXpTask.class, goals.get(0).getTasks().get(2).getClass());

        SkillXpTask skillXpTask = (SkillXpTask) goals.get(0).getTasks().get(2);

        assertEquals(1234, skillXpTask.getTargetSkillXp());
        assertEquals(Skill.ATTACK, skillXpTask.getSkill());
        assertEquals(2, skillXpTask.getIndentLevel());

        assertEquals(QuestTask.class, goals.get(0).getTasks().get(3).getClass());

        QuestTask questTask = (QuestTask) goals.get(0).getTasks().get(3);

        assertEquals(Quest.ANIMAL_MAGNETISM, questTask.getQuest());
        assertEquals(1, questTask.getIndentLevel());

        assertEquals(ItemTask.class, goals.get(0).getTasks().get(4).getClass());

        ItemTask itemTask = (ItemTask) goals.get(0).getTasks().get(4);

        assertEquals(314, itemTask.getItemId());
        assertEquals(4, itemTask.getAcquired());
        assertEquals(10, itemTask.getQuantity());
        assertEquals("Feather", itemTask.getItemName());
        assertEquals(0, itemTask.getIndentLevel());

        assertEquals(CombatLevelTask.class, goals.get(0).getTasks().get(5).getClass());
        CombatLevelTask combatTask = (CombatLevelTask) goals.get(0).getTasks().get(5);
        assertEquals(TaskType.COMBAT_LEVEL, combatTask.getType());
        assertEquals(90.5, combatTask.getTargetCombatLevel(), 0.000001);
        assertEquals(85.25, combatTask.getCurrentCombatLevel(), 0.000001);
    }

    @Test
    void serialization_shouldPreserveQuestPointTaskTargetAndCachedProgress()
    {
        Goal goal = Goal.builder().description("Quest cape").build();
        goal.getTasks().add(QuestPointsTask.builder()
            .targetQuestPoints(300)
            .currentQuestPoints(175)
            .status(Status.IN_PROGRESS)
            .build());

        List<Goal> loaded = serializer.deserialize(serializer.serialize(Arrays.asList(goal)));

        assertEquals(1, loaded.size());
        assertEquals(1, loaded.get(0).getTasks().size());
        assertTrue(loaded.get(0).getTasks().get(0) instanceof QuestPointsTask);
        QuestPointsTask task = (QuestPointsTask) loaded.get(0).getTasks().get(0);
        assertEquals(300, task.getTargetQuestPoints());
        assertEquals(175, task.getCurrentQuestPoints());
        assertEquals(Status.IN_PROGRESS, task.getStatus());
        assertEquals(TaskType.QUEST_POINTS, task.getType());
    }

    @Test
    public void deserialize_should_migrate_legacy_indentation_into_explicit_hierarchy() {
        String legacyJson = "[{\"description\":\"Legacy\",\"items\":["
                + "{\"description\":\"Root\",\"indent_level\":0,\"type\":\"manual\"},"
                + "{\"description\":\"Child\",\"indent_level\":1,\"type\":\"manual\"},"
                + "{\"description\":\"Grandchild\",\"indent_level\":2,\"type\":\"manual\"},"
                + "{\"description\":\"Sibling\",\"indent_level\":1,\"type\":\"manual\"}]}]";

        List<Goal> goals = serializer.deserialize(legacyJson);
        Goal goal = goals.get(0);
        ManualTask root = (ManualTask) goal.getTasks().get(0);
        ManualTask child = (ManualTask) goal.getTasks().get(1);
        ManualTask grandchild = (ManualTask) goal.getTasks().get(2);
        ManualTask sibling = (ManualTask) goal.getTasks().get(3);

        assertEquals(1, goal.getHierarchyVersion());
        assertNotNull(root.getTaskId());
        assertEquals(root.getTaskId(), child.getParentTaskId());
        assertEquals(child.getTaskId(), grandchild.getParentTaskId());
        assertEquals(root.getTaskId(), sibling.getParentTaskId());
        assertEquals(0, root.getIndentLevel());
        assertEquals(1, child.getIndentLevel());
        assertEquals(2, grandchild.getIndentLevel());
        assertEquals(1, sibling.getIndentLevel());
    }

    @Test
    public void serializeAndDeserialize_should_preserveExplicitParentRelationships() {
        ManualTask root = ManualTask.builder().description("Root").taskId("root-id").build();
        ManualTask child = ManualTask.builder().description("Child").taskId("child-id")
                .parentTaskId("root-id").indentLevel(1).build();
        Goal goal = Goal.builder().description("Hierarchy")
                .tasks(ReorderableList.from(Arrays.asList(root, child))).build();

        Goal restored = serializer.deserialize(serializer.serialize(Arrays.asList(goal))).get(0);
        assertEquals("root-id", restored.getTasks().get(0).getTaskId());
        assertEquals("root-id", restored.getTasks().get(1).getParentTaskId());
        assertEquals(1, restored.getTasks().get(1).getIndentLevel());
    }

    @Test
    public void serialize_should_convert_goals_to_json() throws IOException {
        //noinspection UnstableApiUsage
        String expectedJson = Resources.toString(Resources.getResource("data.json"), Charset.defaultCharset());

        List<Goal> goals = Arrays.asList(
            Goal.builder()
                .description("My Goal")
                .tasks(ReorderableList.from(Arrays.asList(
                    ManualTask.builder()
                    .description("Do all the things!")
                        .taskId("task-1")
                        .status(Status.COMPLETED)
                        .notified(true)
                        .indentLevel(0)
                        .build())))
                .build()
        );

        assertEquals(
            normalizeLineEndings(expectedJson),
            normalizeLineEndings(serializer.serialize(goals, true))
        );
    }

    @Test
    public void serialize_should_support_other_task_types() throws IOException {
        //noinspection UnstableApiUsage
        String expectedJson = Resources.toString(Resources.getResource("complex.json"), Charset.defaultCharset());

        List<Goal> goals = Arrays.asList(
                Goal.builder()
                        .description("My Goal")
                        .tasks(ReorderableList.from(Arrays.asList(
                                ManualTask.builder()
                                        .description("Do all the things!")
                                        .taskId("task-1")
                                        .status(Status.COMPLETED)
                                        .notified(true)
                                        .indentLevel(0)
                                        .build(),
                                SkillLevelTask.builder()
                                        .taskId("task-2")
                                        .parentTaskId("task-1")
                                        .status(Status.IN_PROGRESS)
                                        .notified(false)
                                        .indentLevel(1)
                                        .targetSkillLevel(99)
                                        .skill(Skill.ATTACK)
                                        .build(),
                                SkillXpTask.builder()
                                        .taskId("task-3")
                                        .parentTaskId("task-2")
                                        .status(Status.NOT_STARTED)
                                        .notified(false)
                                        .indentLevel(2)
                                        .targetSkillXp(1234)
                                        .skill(Skill.ATTACK)
                                        .build(),
                                QuestTask.builder()
                                        .taskId("task-4")
                                        .parentTaskId("task-1")
                                        .status(Status.NOT_STARTED)
                                        .notified(false)
                                        .indentLevel(1)
                                        .quest(Quest.ANIMAL_MAGNETISM)
                                        .build(),
                                ItemTask.builder()
                                        .taskId("task-5")
                                        .status(Status.NOT_STARTED)
                                        .notified(false)
                                        .indentLevel(0)
                                        .quantity(10)
                                        .acquired(4)
                                        .itemId(314)
                                        .itemName("Feather")
                                        .build(),
                                CombatLevelTask.builder()
                                        .taskId("task-6")
                                        .status(Status.NOT_STARTED)
                                        .indentLevel(0)
                                        .targetCombatLevel(90.5)
                                        .currentCombatLevel(85.25)
                                        .build()
                        ))).build()
        );

        assertEquals(
            normalizeLineEndings(expectedJson),
            normalizeLineEndings(serializer.serialize(goals, true))
        );
    }
}
