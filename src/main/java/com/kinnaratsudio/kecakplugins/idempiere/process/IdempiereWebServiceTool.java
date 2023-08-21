package com.kinnaratsudio.kecakplugins.idempiere.process;

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
import org.joget.commons.util.LogUtil;
import org.joget.plugin.base.DefaultApplicationPlugin;
import org.joget.plugin.base.PluginManager;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class IdempiereWebServiceTool extends DefaultApplicationPlugin implements RestMixin {
    public final static String LABEL = "Idempiere Web Service Tool";

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
    public Object execute(Map map) {
        final String primaryKey = getRecordId();
        final DataRowField[] dataRow = getDataRow();
        JSONObject jsonResponse = executeService(primaryKey, dataRow);
        return null;
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
                "/properties/process/IdempiereWebServiceTool.json"
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

    protected JSONObject executeService(String primaryKey, final DataRowField[] dataRowField) {
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

            final JSONObject jsonPayload = generatePayload(method, serviceType, primaryKey, dataRowField, username, password, language, clientId, roleId, orgId, warehouseId, stage, null, null);

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

    protected String getRecordId() {
        return getPropertyString("recordId");
    }

    protected DataRowField[] getDataRow() {
        return Arrays.stream(getPropertyGrid("dataRow"))
                .map(m -> {
                    final String column = m.get("column");
                    final String value = m.get("value");
                    return new DataRowField(column, value);
                })
                .toArray(DataRowField[]::new);
    }
}
