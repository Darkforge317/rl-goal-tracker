package com.darkforge317.goaltracker.adapters;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import net.runelite.api.Quest;

import java.io.IOException;
import java.util.Arrays;

/**
 * Gson adapter for RuneLite Quests.
 * Serializes/deserializes quests by their unique ID.
 */
public final class QuestAdapter extends TypeAdapter<Quest> {

    @Override
    public void write(JsonWriter out, Quest src) throws IOException {
        if (src == null) out.nullValue();
        else out.value(src.getId());
    }

    @Override
    public Quest read(JsonReader in) throws IOException {
        if (in.peek() == JsonToken.NULL) {
            in.nextNull();
            return null;
        }
        int id = in.nextInt();
        return Arrays.stream(Quest.values()).filter(quest -> quest.getId() == id).findFirst().orElse(null);
    }
}
