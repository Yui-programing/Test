package com.musicfinder.shared.request;

import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.musicfinder.shared.enums.Action;
import com.musicfinder.shared.util.Json;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class Request {

    private String requestId;
    private Action action;
    private JsonNode data;

    public static Request of(Action action, Object payload) {

        Request request = new Request();

        request.requestId = UUID.randomUUID().toString();
        request.action = action;

        if (payload != null) {
            request.data = Json.mapper.valueToTree(payload);
        }

        return request;
    }

    public <T> T dataAs(Class<T> type) {

        if (data == null || data.isNull()) {
            throw new IllegalArgumentException("Thiếu dữ liệu (data)");
        }

        return Json.convert(data, type);
    }

    public String toJson() {
        return Json.toJson(this);
    }

    public static Request fromJson(String json) {
        return Json.fromJson(json, Request.class);
    }
}