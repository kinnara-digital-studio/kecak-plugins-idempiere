package com.kinnaratsudio.kecakplugins.idempiere.datalist;

import com.kinnarastudio.commons.Try;
import com.kinnarastudio.commons.jsonstream.JSONMapper;
import com.kinnarastudio.idempiere.exception.WebServiceBuilderException;
import com.kinnarastudio.idempiere.exception.WebServiceRequestException;
import com.kinnarastudio.idempiere.exception.WebServiceResponseException;
import com.kinnarastudio.idempiere.model.*;
import com.kinnarastudio.idempiere.type.ServiceMethod;
import com.kinnarastudio.idempiere.webservice.ModelOrientedWebService;
import com.kinnaratsudio.kecakplugins.idempiere.commons.IdempiereMixin;
import com.kinnaratsudio.kecakplugins.idempiere.exception.IdempiereClientException;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.datalist.lib.TextFieldDataListFilterType;
import org.joget.apps.datalist.model.DataList;
import org.joget.apps.datalist.model.DataListFilterQueryObject;
import org.joget.commons.util.LogUtil;
import org.joget.plugin.base.PluginManager;
import org.joget.workflow.util.WorkflowUtil;
import org.json.JSONArray;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class IdempiereDataListFilter extends TextFieldDataListFilterType implements IdempiereMixin {
    public final static String LABEL = "iDempiere Filter";

    @Override
    public String getTemplate(DataList datalist, String name, String label) {
        final FilterQueryType type = getOperationType();

        if (type == FilterQueryType.FOREIGN_KEY) {
            return "";
        }

        return super.getTemplate(datalist, name, label);
    }

    @Override
    public DataListFilterQueryObject getQueryObject(DataList dataList, String name) {
        final FilterQueryType type = getOperationType();
        final DataListFilterQueryObject queryObject = new IdempiereDataListFilterQueryObject(type);
        queryObject.setQuery(name);

        final String value = getValue(dataList, name, getPropertyString("defaultValue"));
        if (value == null || value.isEmpty()) {
            queryObject.setValues(null);
            return queryObject;
        }

        String[] values;
        if (type == FilterQueryType.OPTIONS) {
            try {
                final String primaryKey = getPrimaryKey();
                final WindowTabData tabData = executeService(getService(), getMethod(), getLabelField(), value);

                final String[] keys = Optional.of(tabData)
                        .map(WindowTabData::getDataRows)
                        .map(Arrays::stream)
                        .orElseGet(Stream::empty)
                        .map(DataRow::getFieldEntries)
                        .flatMap(Arrays::stream)
                        .filter(fe -> primaryKey.equalsIgnoreCase(fe.getColumn()))
                        .map(FieldEntry::getValue)
                        .map(String::valueOf)
                        .toArray(String[]::new);

                if (keys.length > 0) {
                    values = keys;
                } else {
                    values = new String[]{""};
                }

            } catch (IdempiereClientException e) {
                LogUtil.error(getClass().getName(), e, e.getMessage());
                values = new String[]{""};
            }

        } else {
            values = new String[]{value};
        }

        queryObject.setValues(values);
        return queryObject;
    }

    @Override
    public String getName() {
        return LABEL;
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
        return LABEL;
    }

    @Override
    public String getClassName() {
        return getClass().getName();
    }

    @Override
    public String getPropertyOptions() {
        final JSONArray jsonProperty = new JSONArray(super.getPropertyOptions());
        final JSONArray jsonIdempiereProperty = new JSONArray(AppUtil.readPluginResource(getClass().getName(), "/properties/datalist/IdempiereDataListFilter.json", null, true, "/messages/Idempiere"));
        return JSONMapper.concat(jsonProperty, jsonIdempiereProperty).toString();
    }

    protected FilterQueryType getOperationType() {
        switch (getPropertyString("operation")) {
            case "like":
                return FilterQueryType.LIKE;
            case "foreignKey":
                return FilterQueryType.FOREIGN_KEY;
            case "options":
                return FilterQueryType.OPTIONS;
            default:
                return FilterQueryType.EQUALS;
        }
    }

    protected String getBaseUrl() {
        return getPropertyString("baseUrl").replaceAll("/+$", "");
    }

    protected String getUsername() {
        return getPropertyString("username");
    }

    protected String getPassword() {
        return getPropertyString("password");
    }

    protected ServiceMethod getMethod() {
        switch (getPropertyString("method")) {
            case "create_data":
                return ServiceMethod.CREATE_DATA;
            case "create_update_data":
                return ServiceMethod.CREATE_OR_UPDATE_DATA;
            case "delete_data":
                return ServiceMethod.DELETE_DATA;
            case "read_data":
                return ServiceMethod.READ_DATA;
            case "update_data":
                return ServiceMethod.UPDATE_DATA;
            case "get_list":
                return ServiceMethod.GET_LIST;
            case "set_docaction":
                return ServiceMethod.SET_DOCUMENT_ACTION;
            default:
                return ServiceMethod.QUERY_DATA;
        }

    }

    protected String getService() {
        return getPropertyString("service");
    }

    protected String getLanguage() {
        return getPropertyString("language");
    }

    protected Integer getClientId() {
        return Integer.valueOf(getPropertyString("clientId"));
    }

    protected Integer getRoleId() {
        return Integer.valueOf(getPropertyString("roleId"));
    }

    protected Integer getOrgId() {
        return Integer.valueOf(getPropertyString("orgId"));
    }

    @Nullable
    protected Integer getWarehouseId() {
        try {
            return Integer.valueOf(getPropertyString("warehouseId"));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Nullable
    protected Integer getStage() {
        try {
            return Integer.valueOf(getPropertyString("stage"));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    protected String getTable() {
        return getPropertyString("table");
    }

    protected String getPrimaryKey() {
        return getTable() + "_ID";
    }

    protected boolean isIgnoreCertificateError() {
        return "true".equalsIgnoreCase(getPropertyString("ignoreCertificateError"));
    }

    protected Map<String, String> getWebServiceInput() {
        return Arrays.stream(getPropertyGrid("webServiceInput"))
                .collect(Collectors.toMap(m -> m.get("inputField"), m -> m.get("value")));
    }

    protected String getLabelField() {
        return getPropertyString("labelField");
    }

    protected WindowTabData executeService(String serviceType, ServiceMethod method, @Nullable String field, @Nullable String value) throws IdempiereClientException {
        final String username = getUsername();
        final String password = getPassword();
        final String language = getLanguage();
        final int clientId = getClientId();
        final int roleId = getRoleId();
        final int orgId = getOrgId();
        final Integer warehouseId = getWarehouseId();

        try {
            final ModelOrientedWebService.Builder builder = new ModelOrientedWebService.Builder()
                    .setBaseUrl(getBaseUrl())
                    .setServiceType(serviceType)
                    .setMethod(method)
                    .setLoginRequest(new LoginRequest(username, password, language, clientId, roleId, orgId, warehouseId))
                    .setTable(getTable());

            if (value != null && !value.isEmpty()) {
                final FieldEntry[] fieldEntries = new FieldEntry[]{
                        new FieldEntry(field, value)
                };
                final DataRow dataRow = new DataRow(fieldEntries);
                builder.setDataRow(dataRow);
            }

            if (isIgnoreCertificateError()) {
                builder.ignoreSslCertificateError();
            }

            final ModelOrientedWebService webService = builder.build();

            LogUtil.info(getClass().getName(), "executeService : request [" + webService.getRequest().getRequestPayload() + "]");
            return (WindowTabData) webService.execute();

        } catch (WebServiceRequestException | WebServiceBuilderException | WebServiceResponseException e) {
            throw new IdempiereClientException(e);
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
        FOREIGN_KEY,
        OPTIONS
    }
}
