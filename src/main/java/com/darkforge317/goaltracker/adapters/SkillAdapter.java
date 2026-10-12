package com.darkforge317.goaltracker.adapters;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import net.runelite.api.Skill;

import java.io.IOException;
import java.util.Arrays;

/**
 * Gson adapter for RuneLite Skills.
 * Serializes/deserializes skills by their display name.
 */
public final class SkillAdapter extends TypeAdapter<Skill> {
    @Override
    public void write(JsonWriter out, Skill src) throws IOException {
        if (src == null) out.nullValue();
        else out.value(src.getName());
    }

    @Override
    public Skill read(JsonReader in) throws IOException {
        if (in.peek() == JsonToken.NULL) {
            in.nextNull();
            return null;
        }
        String name = in.nextString();
        return Arrays.stream(Skill.values()).filter(skill -> skill.getName().equals(name)).findFirst().orElse(null);
    }
}
