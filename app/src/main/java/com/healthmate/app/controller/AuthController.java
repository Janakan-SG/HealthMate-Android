package com.healthmate.app.controller;

import android.content.Context;

import com.healthmate.app.data.LocalDatabase;
import com.healthmate.app.model.User;

public class AuthController {

    private final LocalDatabase database;

    public AuthController(Context context) {
        database = LocalDatabase.getInstance(context);
    }

    public User login(String email, char[] password) throws Exception {
        return login(email, password, true);
    }

    /** Sign in; the session is stored only when "Remember me" is ticked. */
    public User login(String email, char[] password,
                      boolean remember) throws Exception {
        User user = database.authenticate(email, password);

        if (user != null) {
            if (remember) {
                database.saveSession(user.getEmail());
            } else {
                database.clearSession();
            }
        }

        return user;
    }

    public User register(String name, String email,
                         char[] password) throws Exception {
        User user = database.register(name, email, password);

        if (user != null) {
            database.saveSession(user.getEmail());
        }

        return user;
    }

    public void logout() {
        database.clearSession();
    }

    public String getSessionEmail() {
        return database.getSessionEmail();
    }

    /** Returns the remembered user, or null if there is no valid session. */
    public User getSessionUser() {
        String email = database.getSessionEmail();
        if (email == null) return null;
        try {
            return database.findUser(email);
        } catch (Exception error) {
            return null;
        }
    }
}