package com.kinnaratsudio.kecakplugins.idempiere.model;

public class DataRowField {
    final private String column;
    final private String value;

    public DataRowField(String column, String value) {
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
