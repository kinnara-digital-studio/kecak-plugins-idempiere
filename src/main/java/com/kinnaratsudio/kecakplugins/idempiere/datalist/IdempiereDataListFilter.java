package com.kinnaratsudio.kecakplugins.idempiere.datalist;

import com.kinnarastudio.commons.jsonstream.JSONMapper;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.datalist.lib.TextFieldDataListFilterType;
import org.joget.apps.datalist.model.DataList;
import org.joget.apps.datalist.model.DataListFilterQueryObject;
import org.joget.plugin.base.PluginManager;
import org.json.JSONArray;

import java.util.ResourceBundle;

public class IdempiereDataListFilter extends TextFieldDataListFilterType {
    @Override
    public String getTemplate(DataList datalist, String name, String label) {
        final FilterQueryType type = getOperationType();

        if(type == FilterQueryType.FOREIGN_KEY) {
            return "";
        }

        return super.getTemplate(datalist, name, label);
    }

    @Override
    public DataListFilterQueryObject getQueryObject(DataList dataList, String name) {
        final FilterQueryType type = getOperationType();
        final DataListFilterQueryObject queryObject = new IdempiereDataListFilterQueryObject(type);
        queryObject.setQuery(name);
        queryObject.setValues(new String[]{getValue(dataList, name, getPropertyString("defaultValue"))});
        return queryObject;
    }

    @Override
    public String getName() {
        return "iDempiere DataList Filter";
    }

    @Override
    public String getVersion() {
        PluginManager pluginManager = (PluginManager) AppUtil.getApplicationContext().getBean("pluginManager");
        ResourceBundle resourceBundle = pluginManager.getPluginMessageBundle(getClassName(), "/messages/BuildNumber");
        String buildNumber = resourceBundle.getString("buildNumber");
        return buildNumber;
    }

    @Override
    public String getDescription() {
        return getClass().getPackage().getImplementationTitle();
    }

    @Override
    public String getLabel() {
        return "iDempiere Filter";
    }

    @Override
    public String getClassName() {
        return getClass().getName();
    }

    @Override
    public String getPropertyOptions() {
        final JSONArray jsonProperty =  new JSONArray(super.getPropertyOptions());
        final JSONArray jsonIdempiereProperty = new JSONArray(AppUtil.readPluginResource(getClass().getName(), "/properties/datalist/IdempiereDataListFilter.json"));
        return JSONMapper.concat(jsonProperty, jsonIdempiereProperty).toString();
    }

    protected FilterQueryType getOperationType() {
        switch (getPropertyString("operation")) {
            case "like":
                return FilterQueryType.LIKE;
            case "foreignKey":
                return FilterQueryType.FOREIGN_KEY;
            default:
                return FilterQueryType.EQUALS;
        }
    }

    /**
     * Indicator for filter type that current filter is for iDempiere
     */
    public final static class IdempiereDataListFilterQueryObject extends DataListFilterQueryObject {
        private final FilterQueryType type;


        public IdempiereDataListFilterQueryObject(FilterQueryType type) {
            this.type = type;
        }

        public FilterQueryType getType() {
            return type;
        }
    }

    public enum FilterQueryType {
        EQUALS,
        LIKE,
        FOREIGN_KEY
    }
}
