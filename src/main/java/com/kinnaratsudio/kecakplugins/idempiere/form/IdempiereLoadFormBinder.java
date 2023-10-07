package com.kinnaratsudio.kecakplugins.idempiere.form;

import com.kinnarastudio.commons.Try;
import com.kinnarastudio.commons.jsonstream.JSONCollectors;
import com.kinnarastudio.commons.jsonstream.JSONStream;
import com.kinnarastudio.idempiere.exception.WebServiceBuilderException;
import com.kinnarastudio.idempiere.exception.WebServiceRequestException;
import com.kinnarastudio.idempiere.exception.WebServiceResponseException;
import com.kinnarastudio.idempiere.model.DataRow;
import com.kinnarastudio.idempiere.model.FieldEntry;
import com.kinnarastudio.idempiere.model.LoginRequest;
import com.kinnarastudio.idempiere.model.WindowTabData;
import com.kinnarastudio.idempiere.type.ServiceMethod;
import com.kinnarastudio.idempiere.webservice.ModelOrientedWebService;
import com.kinnaratsudio.kecakplugins.idempiere.exception.IdempiereClientException;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.model.*;
import org.joget.commons.util.LogUtil;
import org.joget.plugin.base.PluginManager;
import org.joget.workflow.util.WorkflowUtil;
import org.json.JSONArray;
import org.json.JSONException;

import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class IdempiereLoadFormBinder extends FormBinder implements FormLoadElementBinder {
    public final static String LABEL = "iDempiere Form Load Binder";

    @Override
    public FormRowSet load(Element form, String primaryKey, FormData formData) {
        if (isDebug()) {
            LogUtil.info(getClass().getName(), "load : primaryKey [" + primaryKey + "]");


            final HttpServletRequest request = WorkflowUtil.getHttpServletRequest();
            Optional.ofNullable((Map<String, String[]>) request.getParameterMap())
                    .map(Map::entrySet)
                    .map(Collection::stream)
                    .orElseGet(Stream::empty)
                    .forEach(e -> LogUtil.info(getClass().getName(), "load : parameterMap [" + e.getKey() + "] [" + String.join(",", e.getValue()) + "]"));

            Optional.ofNullable(formData.getRequestParams())
                    .map(Map::entrySet)
                    .map(Collection::stream)
                    .orElseGet(Stream::empty)
                    .forEach(e -> LogUtil.info(getClass().getName(), "load : formData.requestParams [" + e.getKey() + "] [" + String.join(",", e.getValue()) + "]"));
        }

        try {
            final Integer intPrimaryKey = Optional.ofNullable(primaryKey)
                    .map(Try.onFunction(Integer::valueOf, (NumberFormatException e) -> 0))
                    .orElse(0);

            final FieldEntry[] fieldEntries = getWebServiceInput().entrySet().stream()
                    .map(e -> {
                        final String column = e.getKey();
                        final String value = e.getValue();
                        if (value.startsWith("@")) {
                            return new FieldEntry(column, formData.getRequestParameter(value.replaceAll("^@", "")));
                        } else {
                            return new FieldEntry(column, value);
                        }
                    })
                    .toArray(FieldEntry[]::new);

            final DataRow dataRow = new DataRow(fieldEntries);
            final String serviceType = getService();
            final WindowTabData response = executeService(intPrimaryKey, serviceType, dataRow);

            if (!response.isSucceed()) {
                throw new IdempiereClientException("Error loading data [" + primaryKey + "]");
            }

            final FormRow row = Arrays.stream(response.getDataRows())
                    .findFirst()
                    .map(DataRow::getFieldEntries)
                    .map(Arrays::stream)
                    .orElseGet(Stream::empty)
                    .collect(Collectors.toMap(FieldEntry::getColumn, fe -> String.valueOf(fe.getValue()), (ignore, accept) -> accept, FormRow::new));

            row.setId(String.valueOf(intPrimaryKey));

            formData.clearFormErrors();

            final FormRowSet rowSet = new FormRowSet();
            rowSet.add(row);
            return rowSet;

        } catch (JSONException | IdempiereClientException e) {
            LogUtil.error(getClassName(), e, e.getMessage());
            return null;
        }
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
        final String[] resources = new String[]{
                "/properties/commons/LoginRequest.json",
                "/properties/commons/WebServiceType.json",
                "/properties/commons/WebServiceParameters.json",
                "/properties/commons/WebServiceFieldInput.json",
                "/properties/commons/AdvanceOptions.json"
        };

        return Arrays.stream(resources)
                .map(s -> AppUtil.readPluginResource(getClassName(), s, null, true, "/messages/Idempiere"))
                .map(Try.onFunction(JSONArray::new))
                .flatMap(a -> JSONStream.of(a, Try.onBiFunction(JSONArray::getJSONObject)))
                .collect(JSONCollectors.toJSONArray())
                .toString();
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

    protected String getDeletionService() {
        return getPropertyString("deletionService");
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

    protected boolean isIgnoreCertificateError() {
        return "true".equalsIgnoreCase(getPropertyString("ignoreCertificateError"));
    }

    protected Map<String, String> getWebServiceInput() {
        return Arrays.stream(getPropertyGrid("webServiceInput"))
                .collect(Collectors.toMap(m -> m.get("inputField"), m -> m.get("value")));
    }

    protected String getTablePrimaryKey() {
        return getTable() + "_ID";
    }

    protected WindowTabData executeService(Integer primaryKey, String serviceType, DataRow dataRow) throws IdempiereClientException {
        final String username = getUsername();
        final String password = getPassword();
        final ServiceMethod method = getMethod();
        final String language = getLanguage();
        final Integer clientId = getClientId();
        final Integer roleId = getRoleId();
        final Integer orgId = getOrgId();
        final Integer warehouseId = getWarehouseId();
        final Integer stage = getStage();

        final ModelOrientedWebService.Builder builder = new ModelOrientedWebService.Builder()
                .setBaseUrl(getBaseUrl())
                .setServiceType(serviceType)
                .setMethod(method)
                .setLimit(1)    // supposed to be only 1 data
                .setLoginRequest(new LoginRequest(username, password, language, clientId, roleId, orgId, warehouseId))
                .setTable(getTable());

        if (dataRow.getFieldEntries().length > 0) {
            builder.setDataRow(dataRow);
        }

        if (primaryKey != null && primaryKey > 0) {
            builder.setRecordId(primaryKey);
        }

        if (isIgnoreCertificateError()) {
            builder.ignoreSslCertificateError();
        }

        try {
            final ModelOrientedWebService webService = builder.build();
            if (isDebug()) {
                LogUtil.info(getClass().getName(), "executeService : request [" + webService.getRequestPayload() + "]");
            }
            WindowTabData response = (WindowTabData) webService.execute();


            if (isDebug()) {
                LogUtil.info(getClass().getName(), "executeService : response [" + response.getResponsePayload() + "]");
            }

            return response;
        } catch (WebServiceBuilderException | WebServiceResponseException | WebServiceRequestException e) {
            throw new IdempiereClientException(e);
        }
    }

    protected boolean isDebug() {
        return "true".equalsIgnoreCase(getPropertyString("debug"));
    }
}
