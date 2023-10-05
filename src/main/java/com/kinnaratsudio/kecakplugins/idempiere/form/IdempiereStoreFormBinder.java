package com.kinnaratsudio.kecakplugins.idempiere.form;

import com.kinnarastudio.commons.Try;
import com.kinnarastudio.commons.jsonstream.JSONCollectors;
import com.kinnarastudio.commons.jsonstream.JSONStream;
import com.kinnarastudio.idempiere.exception.WebServiceBuilderException;
import com.kinnarastudio.idempiere.exception.WebServiceRequestException;
import com.kinnarastudio.idempiere.exception.WebServiceResponseException;
import com.kinnarastudio.idempiere.model.*;
import com.kinnarastudio.idempiere.type.ServiceMethod;
import com.kinnarastudio.idempiere.webservice.ModelOrientedWebService;
import com.kinnaratsudio.kecakplugins.idempiere.exception.IdempiereClientException;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.model.*;
import org.joget.apps.form.service.FormUtil;
import org.joget.commons.util.LogUtil;
import org.joget.plugin.base.PluginManager;
import org.joget.workflow.model.service.WorkflowManager;
import org.joget.workflow.util.WorkflowUtil;
import org.json.JSONArray;
import org.json.JSONException;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class IdempiereStoreFormBinder extends FormBinder implements FormStoreElementBinder, FormDeleteBinder {
    public final static String LABEL = "iDempiere Form Store Binder";

    @Override
    public FormRowSet store(Element element, FormRowSet rowSet, FormData formData) {
        try {
            final boolean isDebug = isDebug();

            final FormRow row = Optional.ofNullable(rowSet)
                    .map(Collection::stream)
                    .orElseGet(Stream::empty)
                    .findFirst()
                    .orElseThrow(() -> new IdempiereClientException("No record to store"));

            if (isDebug) {
                LogUtil.info(getClass().getName(), "store : row [" + row.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue()).collect(Collectors.joining(",")) + "]");
            }

            final StandardResponse response = executeService(row);

            if (response.isError()) {
                throw new IdempiereClientException(response.getErrorMessage());
            }

            final String recordId = String.valueOf(response.getRecordId());

            if (isDebug) {
                LogUtil.info(getClass().getName(), "Table [" + getTable() + "] RecordID [" + recordId + "]");
            }

            row.setId(recordId);

            final String activityId = formData.getActivityId();
            final String processId = formData.getProcessId();
            if (activityId != null || processId != null) {
                WorkflowManager workflowManager = (WorkflowManager) WorkflowUtil.getApplicationContext().getBean("workflowManager");

                // recursively find element(s) mapped to workflow variable
                Map<String, String> variableMap = new HashMap<>();
                variableMap = storeWorkflowVariables(element, row, variableMap);
                if (activityId != null) {
                    workflowManager.activityVariables(activityId, variableMap);
                } else {
                    workflowManager.processVariables(processId, variableMap);
                }
            }

            final FormRowSet rowSetToStore = new FormRowSet();
            rowSetToStore.add(row);
            rowSetToStore.setMultiRow(false);
            return rowSetToStore;

        } catch (JSONException | IdempiereClientException e) {
            LogUtil.error(getClassName(), e, e.getMessage());

            final Form rootForm = FormUtil.findRootForm(element);
            final String id = rootForm.getPropertyString(FormUtil.PROPERTY_ID);
            formData.addFormError(id, e.getMessage());

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
                "/properties/commons/WebServiceType.json",
                "/properties/commons/WebServiceParameters.json",
                "properties/form/IdempiereFormStoreBinder.json",
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
                .setDataRow(dataRow)
                .setLimit(1)    // supposed to be only 1 data
                .setLoginRequest(new LoginRequest(username, password, language, clientId, roleId, orgId, warehouseId))
                .setTable(getTable());

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

    protected StandardResponse executeService(FormRow row) throws IdempiereClientException {
        final String username = getUsername();
        final String password = getPassword();
        final ServiceMethod method = getMethod();
        final String serviceType = getService();
        final String language = getLanguage();
        final Integer clientId = getClientId();
        final Integer roleId = getRoleId();
        final Integer orgId = getOrgId();
        final Integer warehouseId = getWarehouseId();

        final String tablePrimaryKey = getTablePrimaryKey();

        final FieldEntry[] fieldEntries = row.entrySet().stream()
                // do not send ID field
                .filter(e -> {
                    final String propName = String.valueOf(e.getKey());
                    return !propName.isEmpty() && !"id".equalsIgnoreCase(propName) && !tablePrimaryKey.equals(propName);
                })
                .filter(e -> !String.valueOf(e.getValue()).isEmpty())
                .map(e -> new FieldEntry(String.valueOf(e.getKey()), String.valueOf(e.getValue())))
                .toArray(FieldEntry[]::new);

        final DataRow dataRow = new DataRow(new Field(fieldEntries));

        final ModelOrientedWebService.Builder builder = new ModelOrientedWebService.Builder()
                .setBaseUrl(getBaseUrl())
                .setServiceType(serviceType)
                .setMethod(method)
                .setLoginRequest(new LoginRequest(username, password, language, clientId, roleId, orgId, warehouseId))
                .setTable(getTable())
                .setDataRow(dataRow);

        Optional.ofNullable(row.getId())
                .map(Integer::valueOf)
                .filter(i -> i > 0)
                .ifPresent(builder::setRecordId);

        if (isIgnoreCertificateError()) {
            builder.ignoreSslCertificateError();
        }

        try {
            final ModelOrientedWebService webService = builder.build();
            if (isDebug()) {
                LogUtil.info(getClass().getName(), "executeService : request [" + webService.getRequestPayload() + "]");
            }
            return (StandardResponse) webService.execute();
        } catch (WebServiceBuilderException | WebServiceResponseException | WebServiceRequestException e) {
            throw new IdempiereClientException(e);
        }
    }

    /**
     * Copy from {@link org.joget.apps.form.lib.WorkflowFormBinder#storeWorkflowVariables(Element, FormRow, Map)}
     * <p>
     * Recursive into elements to retrieve workflow variable values to be stored.
     *
     * @param element
     * @param row         The current row of data
     * @param variableMap The variable name=value pairs to be stored.
     * @return
     */
    protected Map<String, String> storeWorkflowVariables(Element element, FormRow row, Map<String, String> variableMap) {
        String variableName = element.getPropertyString(AppUtil.PROPERTY_WORKFLOW_VARIABLE);
        if (variableName != null && !variableName.trim().isEmpty()) {
            String id = element.getPropertyString(FormUtil.PROPERTY_ID);
            String value = (String) row.get(id);
            if (value != null) {
                variableMap.put(variableName, value);
            }
        }
        for (Iterator<Element> i = element.getChildren().iterator(); i.hasNext(); ) {
            Element child = i.next();
            storeWorkflowVariables(child, row, variableMap);
        }
        return variableMap;
    }

    protected boolean isDebug() {
        return "true".equalsIgnoreCase(getPropertyString("debug"));
    }

    @Override
    public void delete(Element element, FormRowSet rowSet, FormData formData, boolean deleteGrid, boolean deleteSubform, boolean abortProcess, boolean deleteFiles, boolean hardDelete) {
        final boolean isDebug = isDebug();
        final String serviceType = getDeletionService();
        if (serviceType.isEmpty()) {
            return;
        }

        Optional.ofNullable(rowSet)
                .map(Collection::stream)
                .orElseGet(Stream::empty)
                .map(FormRow::getId)
                .map(Try.onFunction(Integer::valueOf, (NumberFormatException e) -> 0))
                .forEach(Try.onConsumer(primaryKey -> {
                    if (isDebug) {
                        LogUtil.info(getClass().getName(), "Deleting Record ID [" + primaryKey + "] service [" + serviceType + "]");
                    }

                    final WindowTabData response = executeService(primaryKey, serviceType, new DataRow(new FieldEntry[0]));
                    if (!response.isSucceed()) {
                        throw new IdempiereClientException("Error deleting data [" + primaryKey + "]");
                    }
                }));
    }
}
