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
import org.joget.apps.form.lib.CheckBox;
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

public class IdempiereStoreFormBinder extends FormBinder implements FormStoreElementBinder, IdempiereFormDeleteBinder {
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

            if(row.getId() == null && formData.getPrimaryKeyValue() != null) {
                row.setId(formData.getPrimaryKeyValue());
            }

            if (isDebug) {
                LogUtil.info(getClass().getName(), "store : row [" + row.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue()).collect(Collectors.joining(",")) + "]");
            }

            final StandardResponse response = executeService(element, row, formData);

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
                "properties/form/IdempiereFormDeleteBinder.json",
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
        return getMethod(getPropertyString("method"));
    }

    protected ServiceMethod getMethod(String method) {
        switch (method) {
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

    protected boolean isIgnoreCertificateError() {
        return "true".equalsIgnoreCase(getPropertyString("ignoreCertificateError"));
    }

    protected String getTablePrimaryKey() {
        return getTable() + "_ID";
    }

    protected Map<String, String> getParameters() {
        Object[] webServiceParameters = getPropertyGrid("webServiceParameters");
        return Arrays.stream(webServiceParameters)
                .map(o -> (Map<String, String>) o)
                .collect(Collectors.toMap(m -> m.getOrDefault("parameterName", ""), m -> m.getOrDefault("parameterValue", "")));
    }

    protected StandardResponse executeService(Element rootElement, FormRow row, FormData formData) throws IdempiereClientException {
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
                .filter(e -> e.getValue() != null)
                .map(e -> {
                    final String elementId = String.valueOf(e.getKey());
                    final Element element = FormUtil.findElement(elementId, rootElement, formData);
                    final Collection<FormRow> options = FormUtil.getElementPropertyOptionsMap(element, formData);
                    final String value;
                    if(element instanceof CheckBox && options != null && options.size() == 1) { // basically a switch
                        value = Optional.of(options)
                                .map(Collection::stream)
                                .orElseGet(Stream::empty)
                                .findFirst()
                                .map(r -> r.getProperty(FormUtil.PROPERTY_VALUE))
                                .filter(e.getValue()::equals)
                                .map(s -> "Y")
                                .orElse("N");
                    } else {
                        value = String.valueOf(e.getValue());
                    }
                    return new FieldEntry(String.valueOf(e.getKey()), value);
                })
                .toArray(FieldEntry[]::new);

        final DataRow dataRow = new DataRow(new Field(fieldEntries));

        final Map<String, String> parameters = getParameters();

        final String docAction = parameters.get("docAction");

        final ModelOrientedWebService.Builder builder = new ModelOrientedWebService.Builder()
                .setBaseUrl(getBaseUrl())
                .setServiceType(serviceType)
                .setMethod(method)
                .setLoginRequest(new LoginRequest(username, password, language, clientId, roleId, orgId, warehouseId))
                .setTable(getTable())
                .setDocAction(docAction)
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
                LogUtil.info(getClass().getName(), "executeService : request [" + webService.getRequest().getRequestPayload() + "]");
            }
            return (StandardResponse) webService.execute();
        } catch (WebServiceBuilderException | WebServiceResponseException | WebServiceRequestException e) {
            throw new IdempiereClientException(e);
        }
    }

    /**
     * Copy from {@link org.joget.apps.form.lib.WorkflowFormBinder# storeWorkflowVariables(Element, FormRow, Map)}
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

    @Override
    public String getDeletionService() {
        return getPropertyString("deletionService");
    }

    @Override
    public ServiceMethod getDeletionServiceMethod() {
        return getMethod(getPropertyString("deletionMethod"));
    }

    @Override
    public StandardResponse executeDeletionService(int primaryKey, String serviceType, ServiceMethod method, Map<String, String> parameters, Map<String, String> fieldInput) throws IdempiereClientException {
        final String username = getUsername();
        final String password = getPassword();
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
                .setLoginRequest(new LoginRequest(username, password, language, clientId, roleId, orgId, warehouseId))
                .setTable(getTable());

        if (primaryKey > 0) {
            builder.setRecordId(primaryKey);
        }

        final String docAction = parameters.getOrDefault("docAction", "");
        LogUtil.info(getClass().getName(), "executeDeletionService : docAction ["+docAction+"]");
        if(!docAction.isEmpty()) {
            builder.setDocAction(docAction);
        }

        if (isIgnoreCertificateError()) {
            builder.ignoreSslCertificateError();
        }

        try {
            final ModelOrientedWebService webService = builder.build();
            if (isDebug()) {
                LogUtil.info(getClass().getName(), "executeService : request [" + webService.getRequest().getRequestPayload() + "]");
            }

            StandardResponse response = (StandardResponse) webService.execute();

            if (isDebug()) {
                LogUtil.info(getClass().getName(), "executeService : response [" + response.getResponsePayload() + "]");
            }

            return response;
        } catch (WebServiceBuilderException | WebServiceResponseException | WebServiceRequestException e) {
            throw new IdempiereClientException(e);
        }
    }

    @Override
    public Map<String, String> getDeletionParameters() {
        Object[] webServiceParameters = getPropertyGrid("deletionWebServiceParameters");
        return Arrays.stream(webServiceParameters)
                .map(o -> (Map<String, String>) o)
                .collect(Collectors.toMap(m -> m.getOrDefault("parameterName", ""), m -> m.getOrDefault("parameterValue", "")));
    }

    @Override
    public Map<String, String> getDeletionFieldInput() {
        Object[] webServiceInput = getPropertyGrid("deletionWebServiceInput");
        return Arrays.stream(webServiceInput)
                .map(o -> (Map<String, String>) o)
                .collect(Collectors.toMap(m -> m.getOrDefault("apiField", ""), m -> m.getOrDefault("formField", "")));
    }

    public boolean isDebug() {
        return "true".equalsIgnoreCase(getPropertyString("debug"));
    }
}
