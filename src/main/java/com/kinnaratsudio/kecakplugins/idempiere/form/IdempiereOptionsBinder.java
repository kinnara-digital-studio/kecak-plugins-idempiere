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
import org.joget.apps.form.service.FormUtil;
import org.joget.commons.util.LogUtil;
import org.joget.plugin.base.PluginManager;
import org.json.JSONArray;

import java.util.Arrays;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class IdempiereOptionsBinder extends FormBinder implements FormLoadOptionsBinder, FormAjaxOptionsBinder {
    public final static String LABEL = "iDempiere Options Binder";

    @Override
    public boolean useAjax() {
        return true;
    }

    @Override
    public FormRowSet loadAjaxOptions(String[] dependencyValues) {

        try {
            final WindowTabData response = executeService();
            final String valueField = getValueField();
            final String labelField = getLabelField();
            return Arrays.stream(response.getDataRows())
                    .map(DataRow::getFieldEntries)
                    .map(fe -> {
                        final FormRow row = new FormRow();

                        Arrays.stream(fe).forEach(Try.onConsumer(fieldEntry -> {
                            final String column = fieldEntry.getColumn();
                            final String value = String.valueOf(fieldEntry.getValue());
                            if (column.equals(valueField)) {
                                row.setProperty(FormUtil.PROPERTY_VALUE, value);
                            } else if (column.equals(labelField)) {
                                row.setProperty(FormUtil.PROPERTY_LABEL, value);
                            }
                        }));

                        return row;
                    })
                    .collect(Collectors.toCollection(FormRowSet::new));
        } catch (IdempiereClientException e) {
            LogUtil.error(getClassName(), e, e.getMessage());
            return null;
        }
    }

    @Override
    public FormRowSet load(Element element, String primaryKeys, FormData formData) {
        setFormData(formData);
        return loadAjaxOptions(null);
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
                "/properties/form/IdempiereOptionsBinder.json",
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

    protected Integer getWarehouseId() {
        return Integer.valueOf(getPropertyString("warehouseId"));
    }

    protected Integer getStage() {
        return Integer.valueOf(getPropertyString("stage"));
    }

    protected String getTable() {
        return getPropertyString("table");
    }

    protected boolean isIgnoreCertificateError() {
        return "true".equalsIgnoreCase(getPropertyString("ignoreCertificateError"));
    }

    protected Map<String, String> getWebServiceInput() {
        return Arrays.stream(getPropertyGrid("webServiceInput"))
                .collect(Collectors.toMap(m -> m.get("field"), m -> m.get("value")));
    }
    protected String getValueField() {
        return getPropertyString("valueField");
    }

    protected String getLabelField() {
        return getPropertyString("labelField");
    }

    protected WindowTabData executeService() throws IdempiereClientException {
        final String username = getUsername();
        final String password = getPassword();
        final ServiceMethod method = getMethod();
        final String serviceType = getService();
        final String language = getLanguage();
        final int clientId = getClientId();
        final int roleId = getRoleId();
        final int orgId = getOrgId();
        final int warehouseId = getWarehouseId();
        final int stage = getStage();

        final FieldEntry[] fieldEntries = getWebServiceInput().entrySet().stream()
                .map(e -> new FieldEntry(e.getKey(), e.getValue()))
                .toArray(FieldEntry[]::new);

        final ModelOrientedWebService.Builder builder = new ModelOrientedWebService.Builder()
                .setBaseUrl(getBaseUrl())
                .setServiceType(serviceType)
                .setMethod(method)
                .setDataRow(new DataRow(fieldEntries))
                .setLoginRequest(new LoginRequest(username, password, language, clientId, roleId, orgId, warehouseId))
                .setTable(getTable());

        if(isIgnoreCertificateError()) {
            builder.ignoreSslCertificateError();
        }

        try {
            final ModelOrientedWebService webService = builder.build();
            return (WindowTabData) webService.execute();
        } catch (WebServiceBuilderException | WebServiceResponseException | WebServiceRequestException e) {
            throw new IdempiereClientException(e);
        }
    }
}
