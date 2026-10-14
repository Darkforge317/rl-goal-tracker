package com.darkforge317.goaltracker.adapters;

import com.darkforge317.goaltracker.models.enums.TaskType;
import com.darkforge317.goaltracker.models.task.ItemTask;
import com.darkforge317.goaltracker.models.task.CombatLevelTask;
import com.darkforge317.goaltracker.models.task.ManualTask;
import com.darkforge317.goaltracker.models.task.QuestTask;
import com.darkforge317.goaltracker.models.task.QuestPointsTask;
import com.darkforge317.goaltracker.models.task.SkillLevelTask;
import com.darkforge317.goaltracker.models.task.SkillXpTask;
import com.darkforge317.goaltracker.models.task.Task;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

/**
 * Gson adapter for polymorphic Task serialization.
 * Adds a "type" field and deserializes into the correct Task subclass.
 */
@Slf4j
public final class TaskAdapter extends TypeAdapter<Task>
{
    private final Gson gson;
    private final TypeAdapter<JsonElement> jsonElementAdapter;

    public TaskAdapter(Gson gson)
    {
        this.gson = gson;
        this.jsonElementAdapter = gson.getAdapter(JsonElement.class);
    }

    @Override
    public void write(JsonWriter out, Task src) throws IOException
    {
        if (src == null)
        {
            out.nullValue();
            return;
        }

        JsonObject json = gson.toJsonTree(src, src.getClass()).getAsJsonObject();
        json.addProperty("type", src.getType().toString());
        jsonElementAdapter.write(out, json);
    }

    @Override
    public Task read(JsonReader in) throws IOException
    {
        JsonElement json = jsonElementAdapter.read(in);
        if (json == null || json.isJsonNull()) return null;

        TaskType taskType = TaskType.fromString(json.getAsJsonObject().get("type").getAsString());
        switch (taskType)
        {
            case SKILL_XP:
                return gson.fromJson(json, SkillXpTask.class);
            case ITEM:
                return gson.fromJson(json, ItemTask.class);
            case MANUAL:
                return gson.fromJson(json, ManualTask.class);
            case QUEST:
                return gson.fromJson(json, QuestTask.class);
            case SKILL_LEVEL:
                return gson.fromJson(json, SkillLevelTask.class);
            case COMBAT_LEVEL:
                return gson.fromJson(json, CombatLevelTask.class);
            case QUEST_POINTS:
                return gson.fromJson(json, QuestPointsTask.class);
            default:
                log.error("Unknown task type: " + taskType);
                return null;
        }
    }
}
