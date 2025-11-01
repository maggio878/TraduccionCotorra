package com.example.traduccioncotorra.DB;

import android.content.Context;

public class UserConfigDAO {
    private ManagerDB managerDB;
    private static final String TABLE_NAME = "UserConfig";

    public UserConfigDAO(Context context) {
        managerDB = new ManagerDB(context);
    }
}
