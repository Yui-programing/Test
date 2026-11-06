package com.musicfinder.shared;

import com.musicfinder.shared.enums.Action;
import com.musicfinder.shared.request.Request;
import com.musicfinder.shared.request.payload.LoginPayload;

public class RequestTestMain {

    public static void main(String[] args) {

        LoginPayload payload = new LoginPayload("dong", "123456");

        Request request = Request.of(Action.LOGIN, payload);

        System.out.println(request.toJson());
    }
}