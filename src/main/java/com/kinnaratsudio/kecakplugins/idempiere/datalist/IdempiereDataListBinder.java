package com.kinnaratsudio.kecakplugins.idempiere.datalist;

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
import org.joget.apps.datalist.model.*;
import org.joget.commons.util.LogUtil;
import org.joget.plugin.base.PluginManager;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class IdempiereDataListBinder extends DataListBinderDefault implements RestMixin {
    public final static String LABEL = "iDempiere DataList Binder";

    @Override
    public DataListColumn[] getColumns() {
        try {
            final JSONObject jsonResponse = executeService(null, getProperties(), null, null, null, null, 1);

            final JSONObject jsonDataSet = jsonResponse
                    .getJSONObject("WindowTabData")
                    .getJSONObject("DataSet");

            final JSONArray jsonDataRow;
            final Object objDataRow = jsonDataSet.get("DataRow");
            if (objDataRow instanceof JSONObject) {
                jsonDataRow = new JSONArray();
                jsonDataRow.put(objDataRow);
            } else if (objDataRow instanceof JSONArray) {
                jsonDataRow = (JSONArray) objDataRow;
            } else {
                throw new IdempiereClientException("Unable to parse columns");
            }

            return JSONStream.of(jsonDataRow, Try.onBiFunction(JSONArray::getJSONObject))
                    .findFirst()
                    .map(Try.onFunction(jsonRow -> {
                        final JSONArray jsonField = jsonRow.getJSONArray("field");

                        return JSONStream.of(jsonField, Try.onBiFunction(JSONArray::getJSONObject))
                                .map(Try.onFunction(f -> f.getString("@column")))
                                .map(s -> new DataListColumn(s, s, false))
                                .toArray(DataListColumn[]::new);
                    }))
                    .orElseGet(() -> new DataListColumn[0]);
        } catch (JSONException | IdempiereClientException | IOException e) {
            LogUtil.error(getClassName(), e, e.getMessage());
            return new DataListColumn[0];
        }
    }

    @Override
    public String getPrimaryKeyColumnName() {
        return getPropertyString("primaryKeyColumn");
    }

    @Override
    public DataListCollection<Map<String, String>> getData(DataList dataList, Map properties, @Nullable DataListFilterQueryObject[] filterQueryObjects, String sort, @Nullable Boolean desc, @Nullable Integer start, @Nullable Integer rows) {
        try {
            final JSONObject jsonResponse = executeService(dataList, properties, filterQueryObjects, sort, desc, start, rows);
            if(jsonResponse == null) return null;

            final JSONObject jsonDataSet = jsonResponse
                    .getJSONObject("WindowTabData")
                    .getJSONObject("DataSet");

            final Object objDataRow = jsonDataSet.get("DataRow");

            final JSONArray jsonDataRow;
            if(objDataRow instanceof JSONObject) {
                jsonDataRow = new JSONArray();
                jsonDataRow.put(objDataRow);
            } else if(objDataRow instanceof JSONArray) {
                jsonDataRow = (JSONArray) objDataRow;
            } else return null;

            return JSONStream.of(jsonDataRow, Try.onBiFunction(JSONArray::getJSONObject))
                    .map(Try.onFunction(json -> json.getJSONArray("field")))
                    .map(jsonArrayRow -> JSONStream.of(jsonArrayRow, Try.onBiFunction(JSONArray::getJSONObject))
                            .collect(Collectors.toMap(Try.onFunction(json -> json.getString("@column")), Try.onFunction(json -> json.getString("val")))))
                    .collect(Collectors.toCollection(DataListCollection::new));

        } catch (JSONException | IdempiereClientException | IOException e) {
            LogUtil.error(getClassName(), e, e.getMessage());
            return null;
        }
    }

    @Override
    public int getDataTotalRowCount(DataList dataList, Map properties, DataListFilterQueryObject[] filterQueryObjects) {
        try {
            final JSONObject jsonResponse = executeService(dataList, properties, filterQueryObjects, null, null, null, 1);
            if(jsonResponse == null) {
                return 0;
            }
            return jsonResponse.getJSONObject("WindowTabData").getInt("@TotalRows");
        } catch (JSONException | IdempiereClientException | IOException e) {
            LogUtil.error(getClassName(), e, e.getMessage());
            return 0;
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
                "/properties/datalist/IdempiereDataListBinder.json"
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

    protected JSONObject executeService(DataList dataList, Map properties, @Nullable DataListFilterQueryObject[] filterQueryObjects, String sortIgnored, @Nullable Boolean descIgnored, @Nullable Integer start, @Nullable Integer rows) throws IdempiereClientException, JSONException, IOException {
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

        final DataRowField[] filters = Optional.ofNullable(dataList)
                .map(DataList::getFilters)
                .map(Arrays::stream)
                .orElseGet(Stream::empty)
                .map(f -> {
                    final String name = f.getName();
                    return Optional.ofNullable(filterQueryObjects)
                            .map(Arrays::stream)
                            .orElseGet(Stream::empty)
                            .filter(q -> q.getQuery().contains("(" + name + ")"))
                            .map(DataListFilterQueryObject::getValues)
                            .flatMap(Arrays::stream)
                            .map(s -> s.split(";"))
                            .flatMap(Arrays::stream)
                            .filter(Try.toNegate(String::isEmpty))
                            .map(s -> new DataRowField(name, s))
                            .toArray(DataRowField[]::new);
                })
                .flatMap(Arrays::stream)
                .toArray(DataRowField[]::new);

        final JSONObject jsonPayload = generatePayload(method, serviceType, filters, username, password, language, clientId, roleId, orgId, warehouseId, stage, start, rows);

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
    }
}
