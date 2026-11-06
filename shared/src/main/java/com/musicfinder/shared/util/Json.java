package com.musicfinder.shared.util;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class Json {
    public static final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL, true);

    public static String toJson(Object obj) {
        try {
            return mapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new IllegalArgumentException("Không đổi được sang JSON: " + e.getMessage());
        }
    }

    public static <T> T fromJson(String json, Class<T> type) {
        try {
            return mapper.readValue(json, type);
        } catch (Exception e) {
            throw new IllegalArgumentException("JSON không hợp lệ: " + e.getMessage());
        }
    }

    public static <T> T convert(Object data, Class<T> type) {
        if (data == null) {
            throw new IllegalArgumentException("Thiếu dữ liệu (data)");
        }
        try {
            return mapper.convertValue(data, type);
        } catch (Exception e) {
            throw new IllegalArgumentException("Dữ liệu không hợp lệ: " + e.getMessage());
        }
    }
}
