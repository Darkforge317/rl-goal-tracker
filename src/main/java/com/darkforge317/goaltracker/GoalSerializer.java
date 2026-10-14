package com.darkforge317.goaltracker;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.darkforge317.goaltracker.adapters.QuestAdapter;
import com.darkforge317.goaltracker.adapters.SkillAdapter;
import com.darkforge317.goaltracker.adapters.TaskAdapter;
import com.darkforge317.goaltracker.models.Goal;
import com.darkforge317.goaltracker.models.task.Task;
import com.darkforge317.goaltracker.utils.ReorderableList;
import com.darkforge317.goaltracker.utils.TaskHierarchyUtils;
import net.runelite.api.Quest;
import net.runelite.api.Skill;

import javax.inject.Inject;
import java.util.List;

/**
 * Handles JSON serialization and deserialization of Goals.
 * Uses custom adapters for Task, Skill, and Quest types with consistent field naming.
 */
public final class GoalSerializer
{	
    private final Gson gson;

    @Inject
    public GoalSerializer(Gson gson)
    {
        this.gson = gson;
    }

    public ReorderableList<Goal> deserialize(String serialized)
    {
        if (serialized == null || serialized.trim().isEmpty())
        {
            return new ReorderableList<>();
        }

        ReorderableList<Goal> goals = ReorderableList.from(this.getBuilder().fromJson(serialized, Goal[].class));
        for (Goal goal : goals)
        {
            if (goal == null) continue;
            if (goal.getHierarchyVersion() < 1)
            {
                TaskHierarchyUtils.initializeFromIndent(goal.getTasks());
                goal.setHierarchyVersion(1);
            }
            else
            {
                TaskHierarchyUtils.ensureTaskIds(goal.getTasks());
                TaskHierarchyUtils.normalizeIndentLevels(goal.getTasks());
            }
        }
        return goals;
    }

    public String serialize(List<Goal> goals)
    {
        return this.serialize(goals, false);
    }

    public String serialize(List<Goal> goals, boolean prettyPrinting)
    {
        return this.getBuilder(prettyPrinting).toJson(goals);
    }

    private Gson getBuilder() {
        return this.getBuilder(false);
    }

    private Gson getBuilder(boolean prettyPrinting) {
        var builder = gson.newBuilder()
            .setFieldNamingStrategy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .registerTypeAdapter(Skill.class, new SkillAdapter())
            .registerTypeAdapter(Quest.class, new QuestAdapter());

        if (prettyPrinting) {
            builder.setPrettyPrinting();
        }

        Gson taskGson = builder.create();
        return taskGson.newBuilder()
            .registerTypeAdapter(Task.class, new TaskAdapter(taskGson))
            .create();
    }
}
