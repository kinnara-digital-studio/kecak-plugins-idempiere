package com.kinnaratsudio.kecakplugins.idempiere.model;

import org.json.JSONObject;

public class Field {
    final private String column;
    final private String value;

    public Field(String column, String value) {
        this.column = column;
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public String getColumn() {
        return column;
    }
}
