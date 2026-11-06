package com.musicfinder.shared.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.musicfinder.shared.enums.Action;
import com.musicfinder.shared.enums.Status;
import com.musicfinder.shared.request.Request;
import com.musicfinder.shared.util.Json;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data 
@NoArgsConstructor 
@AllArgsConstructor 
public class Response {
    public String requestId;
    public Action action;
    public Status status;
    public String message;
    public Object data;

    public static Response invalid(String message) {
        Response res = new Response();
        res.status = Status.INVALID_REQUEST;
        res.message = message;
        return res;
    }

    public static Response create(Request req, Status status, String message, Object data) {
        Response res = new Response();
        res.requestId = req.getRequestId();
        res.action = req.getAction();
        res.status = status;
        res.message = message;
        res.data = data;
        return res;
    }

    public static Response success(Request req, Object data) {
        return create(req, Status.SUCCESS, "OK", data);
    }

    public static Response error(Request req, Status status, String message) {
        return create(req, status, message, null);
    }

    @JsonIgnore
    public boolean isSuccess() {
        return status == Status.SUCCESS;
    }

    public <T> T dataAs(Class<T> type) {
        return Json.convert(data, type);
    }

    public String toJson() {
        return Json.toJson(this);
    }

    public static Response fromJson(String json) {
        return Json.fromJson(json, Response.class);
    }
}