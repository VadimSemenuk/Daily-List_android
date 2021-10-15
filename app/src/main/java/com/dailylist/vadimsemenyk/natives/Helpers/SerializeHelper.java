package com.dailylist.vadimsemenyk.natives.Helpers;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;
import java.util.Calendar;

public class SerializeHelper {
    static public class DateTimeSerializer implements JsonSerializer<Calendar> {
        @Override
        public JsonElement serialize(Calendar src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.getTimeInMillis());
        }
    }

    static public class DateTimeDeserializer implements JsonDeserializer<Calendar> {
        public Calendar deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            Calendar dateTime = Calendar.getInstance();
            dateTime.setTimeInMillis(json.getAsJsonPrimitive().getAsLong());
            return dateTime;
        }
    }
}