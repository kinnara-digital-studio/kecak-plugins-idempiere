package com.kinnaratsudio.kecakplugins.idempiere.model;

public class DataRow {
    private final Field[] fields;

    public DataRow(Field[] fields) {
        this.fields = fields;
    }

    public Field[] getFields() {
        return fields;
    }
}
