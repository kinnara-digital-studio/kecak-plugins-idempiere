package com.kinnaratsudio.kecakplugins.idempiere.datalist;

import com.kinnarastudio.commons.Try;
import com.kinnarastudio.commons.jsonstream.JSONCollectors;
import com.kinnarastudio.commons.jsonstream.JSONStream;
import com.kinnaratsudio.kecakplugins.idempiere.commons.IdempiereMixin;
import com.kinnaratsudio.kecakplugins.idempiere.exception.IdempiereClientException;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.datalist.model.DataList;
import org.joget.apps.datalist.model.DataListActionDefault;
import org.joget.apps.datalist.model.DataListActionResult;
import org.joget.commons.util.LogUtil;
import org.joget.commons.util.ResourceBundleUtil;
import org.joget.plugin.base.PluginManager;
import org.joget.workflow.util.WorkflowUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class IdempiereDatalistAction extends DataListActionDefault implements IdempiereMixin {
    public final static String LABEL = "iDempiere Action";

    @Override
    public String getLinkLabel() {
        String label = getPropertyString("label");
        if (label == null || label.isEmpty()) {
            label = "Row Action";
        }
        return label;    }

    @Override
    public String getHref() {
        return getPropertyString("href");
    }

    @Override
    public String getTarget() {
        return "post";
    }

    @Override
    public String getHrefParam() {
        return getPropertyString("hrefParam");
    }

    @Override
    public String getHrefColumn() {
        return getPropertyString("hrefColumn");
    }

    @Override
    public String getConfirmation() {
        String confirm = getPropertyString("confirmation");
        if (confirm == null || confirm.isEmpty()) {
            confirm = ResourceBundleUtil.getMessage("datalist.formrowdeletedatalistaction.pleaseConfirm");
        }
        return confirm;
    }

    @Override
    public void setProperties(Map<String, Object> properties) {
        super.setProperties(properties);
        properties.put("cssClasses", "btn-danger");
    }

    @Override
    public DataListActionResult executeAction(DataList dataList, String[] rowKeys) {
        DataListActionResult result = new DataListActionResult();
        result.setType(DataListActionResult.TYPE_REDIRECT);
        result.setUrl("REFERER");

        // only allow POST
        HttpServletRequest request = WorkflowUtil.getHttpServletRequest();
        if (request != null && !"POST".equalsIgnoreCase(request.getMethod())) {
            return null;
        }

        if (rowKeys != null && rowKeys.length > 0) {
            for (String key : rowKeys) {
                executeService(key);
            }
        }

        return result;
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
                "/properties/commons/AdvanceOptions.json",
                "/properties/datalist/IdempiereDataListAction.json"
        };

        return Arrays.stream(resources)
                .map(s -> AppUtil.readPluginResource(getClassName(), s, null, true, "/messages/Idempiere"))
                .map(Try.onFunction(JSONArray::new))
                .flatMap(a -> JSONStream.of(a, Try.onBiFunction(JSONArray::getJSONObject)))
                .collect(JSONCollectors.toJSONArray())
                .toString();
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
            final Integer warehouseId = getWarehouseId();
            final Integer stage = getStage();

            final JSONObject jsonPayload = generatePayload(method, serviceType, primaryKey, username, password, language, clientId, roleId, orgId, String.valueOf(warehouseId), String.valueOf(stage), null, null);

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

    protected Integer getWarehouseId() {
        try {
            return Integer.parseInt(getPropertyString("warehouseId"));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    protected Integer getStage() {
        try {
            return Integer.parseInt(getPropertyString("stage"));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    protected String getTable() {
        return getPropertyString("table");
    }

    protected String getTablePrimaryKey() {
        return getTable() + "_ID";
    }
}
