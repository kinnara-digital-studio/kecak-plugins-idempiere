package com.kinnaratsudio.kecakplugins.idempiere.model;

public class DataSet {
    private final DataRow[] dataRows;

    public DataSet(DataRow[] dataRows) {
        this.dataRows = dataRows;
    }

    public DataRow[] getDataRows() {
        return dataRows;
    }
}
