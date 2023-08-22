package com.kinnaratsudio.kecakplugins.idempiere.form;

import com.kinnarastudio.commons.Try;
import com.kinnarastudio.commons.jsonstream.JSONCollectors;
import com.kinnarastudio.commons.jsonstream.JSONStream;
import com.kinnaratsudio.kecakplugins.idempiere.commons.RestMixin;
import com.kinnaratsudio.kecakplugins.idempiere.exception.IdempiereClientException;
import com.kinnaratsudio.kecakplugins.idempiere.model.DataRowField;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.model.*;
import org.joget.apps.form.service.FormUtil;
import org.joget.commons.util.LogUtil;
import org.joget.plugin.base.PluginManager;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class IdempiereFormBinder extends FormBinder implements FormLoadElementBinder, FormStoreElementBinder, FormDataDeletableBinder, RestMixin {
    public final static String LABEL = "iDempiere Form Binder";

    @Override
    public String getFormId() {
        Form form = FormUtil.findRootForm(this.getElement());
        return form.getPropertyString("id");
    }

    @Override
    public String getTableName() {
        Form form = FormUtil.findRootForm(this.getElement());
        return form.getPropertyString("tableName");
    }

    @Override
    public FormRowSet load(Element element, String primaryKey, FormData formData) {
        try {
            final JSONObject jsonResponse = executeService(primaryKey);
            if (jsonResponse == null) return null;

            final JSONArray jsonField = jsonResponse
                    .getJSONObject("WindowTabData")
                    .getJSONObject("DataSet")
                    .getJSONObject("DataRow")
                    .getJSONArray("field");

            final FormRow row = JSONStream.of(jsonField, Try.onBiFunction(JSONArray::getJSONObject))
                    .filter(this::nonNilValue)
                    .collect(Collectors.toMap(Try.onFunction(j -> j.getString("@column")), Try.onFunction(j -> j.getString("val")), (ignore, accept) -> accept, FormRow::new));

            FormRowSet rowSet = new FormRowSet();
            rowSet.add(row);
            return rowSet;

        } catch (JSONException e) {
            LogUtil.error(getClassName(), e, e.getMessage());
            return null;
        }
    }

    @Override
    public FormRowSet store(Element element, FormRowSet rowSet, FormData formData) {
        try {
            final FormRow rowToStore = Optional.ofNullable(rowSet)
                    .map(Collection::stream)
                    .orElseGet(Stream::empty)
                    .findFirst()
                    .orElseThrow(() -> new IdempiereClientException("No record to store"));

            final JSONObject jsonResponse = executeService(rowToStore);
            if (jsonResponse == null) throw new IdempiereClientException("Error retriving response");

            final JSONObject jsonStarndardResponse = jsonResponse
                    .getJSONObject("StandardResponse");

            if(isError(jsonStarndardResponse)) {
                final String error = jsonStarndardResponse.getString("Error");
                final String id = element.getPropertyString(FormUtil.PROPERTY_ID);
                formData.addFormError(id, error);
            } else {
                final String recordId = jsonStarndardResponse.getString("@RecordID");
                rowToStore.setId(recordId);
            }

            return rowSet;

        } catch (JSONException | IdempiereClientException e) {
            LogUtil.error(getClassName(), e, e.getMessage());

            return rowSet;
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
                "/properties/commons/WebServiceSecurity.json",
                "/properties/commons/AdvanceOptions.json",
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

    protected String getMethod() {
        return getPropertyString("method");
    }

    protected String getService() {
        return getPropertyString("service");
    }

    protected String getLanguage() {
        return getPropertyString("language");
    }

    protected String getClientId() {
        return getPropertyString("clientId");
    }

    protected String getRoleId() {
        return getPropertyString("roleId");
    }

    protected String getOrgId() {
        return getPropertyString("orgId");
    }

    protected String getWarehouseId() {
        return getPropertyString("warehouseId");
    }

    protected String getStage() {
        return getPropertyString("stage");
    }

    protected String getTable() {
        return getPropertyString("table");
    }

    protected String getTablePrimaryKey() {
        return getTable() + "_ID";
    }

    protected JSONObject executeService(String primaryKey) {
        try {
            final StringBuilder url = new StringBuilder(getBaseUrl() + "/ADInterface/services/rest/model_adservice/" + getMethod());

            final Map<String, String> headers = new HashMap<>();
            headers.put("Accept", "application/json");

            final String username = getUsername();
            final String password = getPassword();
            final String method = getMethod();
            final String serviceType = getService();
            final String language = getLanguage();
            final String clientId = getClientId();
            final String roleId = getRoleId();
            final String orgId = getOrgId();
            final String warehouseId = getWarehouseId();
            final String stage = getStage();

            final JSONObject jsonPayload = generatePayload(method, serviceType, primaryKey, username, password, language, clientId, roleId, orgId, warehouseId, stage, null, null);

            final HttpUriRequest request = getHttpRequest(url.toString(), "POST", headers, jsonPayload.toString());

            // kirim request ke server
            final HttpClient client = getHttpClient(isIgnoreCertificateError());
            final HttpResponse response = client.execute(request);

            final int statusCode = getResponseStatus(response);
            if (getStatusGroupCode(statusCode) != 200) {
                throw new IdempiereClientException("Response code [" + statusCode + "] is not 200 (Success)");
            } else if (statusCode != 200) {
                LogUtil.warn(getClassName(), "Response code [" + statusCode + "] is considered as success");
            }

            if (!isJsonResponse(response)) {
                throw new IdempiereClientException("Content type is not JSON");
            }

            try (BufferedReader br = new BufferedReader(new InputStreamReader(response.getEntity().getContent()))) {
                final JSONObject jsonResponseBody = new JSONObject(br.lines().collect(Collectors.joining()));
                return jsonResponseBody;
            }
        } catch (IOException | IdempiereClientException | JSONException e) {
            LogUtil.error(getClassName(), e, e.getMessage());
            return null;
        }
    }

    protected JSONObject executeService(FormRow row) {
        try {
            final StringBuilder url = new StringBuilder(getBaseUrl() + "/ADInterface/services/rest/model_adservice/" + getMethod());

            final Map<String, String> headers = new HashMap<>();
            headers.put("Accept", "application/json");

            final String username = getUsername();
            final String password = getPassword();
            final String method = getMethod();
            final String serviceType = getService();
            final String language = getLanguage();
            final String clientId = getClientId();
            final String roleId = getRoleId();
            final String orgId = getOrgId();
            final String warehouseId = getWarehouseId();
            final String stage = getStage();

            final String tablePrimaryKey = getTablePrimaryKey();

            final DataRowField[] dataRowField = row.entrySet().stream()
                    // do not send ID field
                    .filter(e -> !tablePrimaryKey.equals(e.getKey()))
                    .map(e -> new DataRowField(String.valueOf(e.getKey()), String.valueOf(e.getValue())))
                    .toArray(DataRowField[]::new);

            final JSONObject jsonPayload = generatePayload(method, serviceType, row.getProperty(tablePrimaryKey), dataRowField, username, password, language, clientId, roleId, orgId, warehouseId, stage, null, null);

            final HttpUriRequest request = getHttpRequest(url.toString(), "POST", headers, jsonPayload.toString());

            // kirim request ke server
            final HttpClient client = getHttpClient(isIgnoreCertificateError());
            final HttpResponse response = client.execute(request);

            final int statusCode = getResponseStatus(response);
            if (getStatusGroupCode(statusCode) != 200) {
                throw new IdempiereClientException("Response code [" + statusCode + "] is not 200 (Success)");
            } else if (statusCode != 200) {
                LogUtil.warn(getClassName(), "Response code [" + statusCode + "] is considered as success");
            }

            if (!isJsonResponse(response)) {
                throw new IdempiereClientException("Content type is not JSON");
            }

            try (BufferedReader br = new BufferedReader(new InputStreamReader(response.getEntity().getContent()))) {
                final JSONObject jsonResponseBody = new JSONObject(br.lines().collect(Collectors.joining()));
                LogUtil.info(getClassName(), "executeService : jsonResponseBody [" + jsonResponseBody + "]");
                return jsonResponseBody;
            }
        } catch (IOException | IdempiereClientException | JSONException e) {
            LogUtil.error(getClassName(), e, e.getMessage());
            return null;
        }
    }

    protected boolean isError(JSONObject jsonStarndardResponse) {
        try {
            return jsonStarndardResponse.has("@IsError") && jsonStarndardResponse.getBoolean("@IsError");
        } catch (JSONException e) {
            return false;
        }
    }

    protected boolean nonNilValue(JSONObject jsonField) {
        try {
            return !jsonField.getJSONObject("val").getBoolean("@nil");
        } catch (JSONException e) {
            return true;
        }
    }
}
