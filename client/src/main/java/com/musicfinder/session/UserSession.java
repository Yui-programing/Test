package com.musicfinder.session;

import com.musicfinder.shared.response.payload.UserData;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class UserSession {
    private static final UserSession INSTANCE = new UserSession();

    private UserData currentUser;
    private final List<Consumer<UserData>> listeners = new ArrayList<>();

    private UserSession() {
    }

    public static UserSession getInstance() {
        return INSTANCE;
    }

    public synchronized UserData getCurrentUser() {
        return currentUser;
    }

    public synchronized void setCurrentUser(UserData user) {
        this.currentUser = user;
        notifyListeners();
    }

    public synchronized boolean isLoggedIn() {
        return currentUser != null && currentUser.userId() != null;
    }

    public synchronized Long getUserId() {
        return currentUser != null ? currentUser.userId() : null;
    }

    public synchronized String getUsername() {
        return currentUser != null ? currentUser.userName() : null;
    }

    public synchronized void clear() {
        this.currentUser = null;
        notifyListeners();
    }

    public synchronized void addListener(Consumer<UserData> listener) {
        listeners.add(listener);
        if (currentUser != null) {
            listener.accept(currentUser);
        }
    }

    public synchronized void removeListener(Consumer<UserData> listener) {
        listeners.remove(listener);
    }

    private void notifyListeners() {
        for (Consumer<UserData> listener : new ArrayList<>(listeners)) {
            try {
                listener.accept(currentUser);
            } catch (Exception ignored) {
            }
        }
    }
}
