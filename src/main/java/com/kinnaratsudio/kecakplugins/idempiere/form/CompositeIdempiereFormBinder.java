package com.kinnaratsudio.kecakplugins.idempiere.form;

import com.kinnarastudio.commons.Try;
import com.kinnarastudio.commons.jsonstream.JSONCollectors;
import com.kinnarastudio.commons.jsonstream.JSONStream;
import com.kinnarastudio.idempiere.exception.WebServiceBuilderException;
import com.kinnarastudio.idempiere.exception.WebServiceRequestException;
import com.kinnarastudio.idempiere.exception.WebServiceResponseException;
import com.kinnarastudio.idempiere.model.*;
import com.kinnarastudio.idempiere.type.ServiceMethod;
import com.kinnarastudio.idempiere.webservice.CompositeInterfaceWebService;
import com.kinnarastudio.idempiere.webservice.ModelOrientedWebService;
import com.kinnaratsudio.kecakplugins.idempiere.commons.IdempiereMixin;
import com.kinnaratsudio.kecakplugins.idempiere.exception.IdempiereClientException;
import com.kinnaratsudio.kecakplugins.idempiere.webservice.IdempiereGetPropertyJson;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.model.*;
import org.joget.apps.form.service.FormUtil;
import org.joget.commons.util.LogUtil;
import org.joget.plugin.base.PluginManager;
import org.json.JSONArray;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class CompositeIdempiereFormBinder extends FormBinder implements IdempiereMixin, FormStoreElementBinder, IdempiereFormDeleteBinder {
    public final static String LABEL = "Composite iDempiere Form Store Binder";

    @Override
    public FormRowSet store(Element element, FormRowSet rowSet, FormData formData) {
        try {
            final boolean isDebug = isDebug();
            final FormRow row = Optional.ofNullable(rowSet)
                    .map(Collection::stream)
                    .orElseGet(Stream::empty)
                    .findFirst()
                    .orElseThrow(() -> new IdempiereClientException("No record to store"));

            final String baseUrl = getBaseUrl();
            final String username = getUsername();
            final String password = getPassword();
            final String language = getLanguage();
            final Integer clientId = getClientId();
            final Integer roleId = getRoleId();
            final Integer orgId = getOrgId();
            final Integer warehouseId = getWarehouseId();
            final int numberOfServices = getNumberOfServices();

            final CompositeInterfaceWebService.Builder builder = new CompositeInterfaceWebService.Builder();
            builder.setBaseUrl(baseUrl)
                    .setLoginRequest(new LoginRequest(username, password, language, clientId, roleId, orgId, warehouseId))
                    .setServiceType(getCompositeService());

            IntStream.range(1, numberOfServices + 1).boxed().forEach(i -> {
                final ServiceMethod method = getMethod(i);
                final String serviceType = getOperationService(i);
                final String tableName = getTable(i);
                final FieldEntry[] fieldEntries = getWebServiceInput(i).entrySet().stream()
                        .filter(e -> !e.getKey().isEmpty())
                        .map(e -> {
                            final String formField = e.getValue();
                            boolean isConstantValue = isStringConstant(formField);

                            String key = e.getKey();
                            String value = isConstantValue ? formField.replaceAll("^\"|\"$", "") : row.getProperty(formField);

                            return new FieldEntry(key, value);
                        })
                        .toArray(FieldEntry[]::new);

                final Map<String, String> webServiceParameters = getWebServiceParameters(i);

                final String recordIdVariable = webServiceParameters.get("recordIDVariable");

                final int recordId;
                if(method == ServiceMethod.CREATE_DATA || recordIdVariable != null) {
                    recordId = 0;
                } else if(method == ServiceMethod.SET_DOCUMENT_ACTION) {
                    recordId = Optional.ofNullable(webServiceParameters.get(ModelSetDocAction.PARAMETER_RECORD_ID))
                            .map(Try.onFunction(Integer::parseInt))
                            .orElseGet(() -> Optional.of(formData)
                                    .map(FormData::getPrimaryKeyValue)
                                    .map(Try.onFunction(Integer::parseInt))
                                    .orElse(0));
                } else {
                    recordId = Optional.ofNullable(webServiceParameters.get(ModelCrud.PARAMETER_RECORD_ID))
                            .map(Try.onFunction(Integer::parseInt))
                            .orElseGet(() -> Optional.of(formData)
                                    .map(FormData::getPrimaryKeyValue)
                                    .map(Try.onFunction(Integer::parseInt))
                                    .orElse(0));
                }

                final Integer offset = Optional.ofNullable(webServiceParameters.get("Offset"))
                        .map(Try.onFunction(Integer::parseInt))
                        .orElse(null);

                final Integer limit = Optional.ofNullable(webServiceParameters.get("Limit"))
                        .map(Try.onFunction(Integer::parseInt))
                        .orElse(null);

                final Model model;
                if(method == ServiceMethod.SET_DOCUMENT_ACTION) {
                    final String docAction = webServiceParameters.get("docAction");
                    model = new ModelSetDocAction(serviceType, tableName, recordId, recordIdVariable, docAction);
                } else {
                    model = new ModelCrud(serviceType, tableName, recordId, recordIdVariable, offset, limit, new DataRow(fieldEntries));
                }
                final Operation operation = new Operation(method, model);

                if (isDebug) {
                    LogUtil.info(getClass().getName(), "Operation method [" + method + "] table [" + tableName + "] entries [" + Arrays.stream(fieldEntries).map(e -> e.getColumn() + "=" + e.getValue()).collect(Collectors.joining(",")) + "]");
                }
                builder.addOperation(operation);
            });

            if (isIgnoreCertificateError()) {
                builder.ignoreSslCertificateError();
            }

            final CompositeInterfaceWebService webService = builder.build();
            if (isDebug) {
                LogUtil.info(getClass().getName(), "store : webService request [" + webService.getRequestPayload() + "]");
            }
            final CompositeResponses response = (CompositeResponses) webService.execute();

            if (response.isError()) {
                if (isDebug) {
                    LogUtil.info(getClass().getName(), "store : webService response [" + response.getResponsePayload() + "]");
                }

                final String errorMessage = Arrays.stream(response.getErrorMessages())
                        .findFirst()
                        .orElse("Unknown error");
                throw new WebServiceResponseException(errorMessage);
            }

            Integer[] recordIds = Optional.of(response)
                    .map(CompositeResponses::getResponses)
                    .map(Arrays::stream)
                    .orElseGet(Stream::empty)
                    .filter(r -> !r.isError())
                    .peek(r -> {
                        if (isDebug) {
                            LogUtil.info(getClass().getName(), "RecordID [" + r.getRecordId() + "] generated");
                        }
                    })
                    .map(StandardResponse::getRecordId)
                    .filter(n -> n > 0)
                    .toArray(Integer[]::new);

            Arrays.stream(recordIds).findFirst()
                    .map(String::valueOf)
                    .ifPresent(row::setId);

            final FormRowSet rowSetToStore = new FormRowSet();
            rowSetToStore.add(row);
            rowSetToStore.setMultiRow(false);
            return rowSetToStore;

        } catch (IdempiereClientException | WebServiceBuilderException | WebServiceRequestException |
                 WebServiceResponseException e) {

            final String parameterName = FormUtil.getElementParameterName(element);
            formData.addFormError(parameterName, e.getMessage());

            LogUtil.error(getClass().getName(), e, e.getMessage());
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
                "/properties/commons/CompositeInterface.json",
                "/properties/form/IdempiereFormDeleteBinder.json",
                "/properties/commons/AdvanceOptions.json",
        };

        return Arrays.stream(resources)
                .map(s -> AppUtil.readPluginResource(getClass().getName(), s, new Object[]{IdempiereGetPropertyJson.class.getName()}, true, "/messages/Idempiere"))
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

    protected ServiceMethod getMethod(int page) {
        Map<String, Object> map = (Map<String, Object>) getProperty("numberOfServices");
        Map<String, Object> prop = (Map<String, Object>) map.get("properties");
        return getMethod(String.valueOf(prop.get("method_" + page)));
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
        try {
            return Integer.valueOf(getPropertyString("warehouseId"));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    protected Integer getStage() {
        try {
            return Integer.valueOf(getPropertyString("stage"));
        } catch (NumberFormatException e) {
            return null;
        }
    }


    protected boolean isIgnoreCertificateError() {
        return "true".equalsIgnoreCase(getPropertyString("ignoreCertificateError"));
    }

    protected String getCompositeService() {
        return getPropertyString("service");
    }

    protected String getOperationService(int page) {
        Map<String, Object> map = (Map<String, Object>) getProperty("numberOfServices");
        Map<String, Object> prop = (Map<String, Object>) map.get("properties");
        return String.valueOf(prop.get("service_" + page));
    }

    protected String getTable(int page) {
        Map<String, Object> map = (Map<String, Object>) getProperty("numberOfServices");
        Map<String, Object> prop = (Map<String, Object>) map.get("properties");
        return String.valueOf(prop.get("table_" + page));
    }

    protected Map<String, String> getWebServiceParameters(int page) {
        Map<String, Object> map = (Map<String, Object>) getProperty("numberOfServices");
        Map<String, Object> prop = (Map<String, Object>) map.get("properties");
        Object[] parameters = (Object[]) prop.get("webServiceParameters_" + page);
        return Arrays.stream(parameters)
                .map(o -> (Map<String, String>) o)
                .filter(Try.toNegate(m -> m.getOrDefault("parameterName", "").isEmpty() || m.getOrDefault("parameterValue", "").isEmpty()))
                .collect(Collectors.toMap(m -> m.get("parameterName"), m -> m.get("parameterValue")));
    }

    protected int getNumberOfServices() {
        Map<String, Object> properties = (Map<String, Object>) getProperty("numberOfServices");
        return Integer.parseInt(properties.get("className").toString());
    }

    /**
     * Map<Web Service Parameter, Form Field>
     *
     * @param page
     * @return
     */
    protected Map<String, String> getWebServiceInput(int page) {
        Map<String, Object> map = (Map<String, Object>) getProperty("numberOfServices");
        Map<String, Object> prop = (Map<String, Object>) map.get("properties");
        Object[] webServiceInput = (Object[]) prop.get("webServiceInput_" + page);
        return Arrays.stream(webServiceInput)
                .map(o -> (Map<String, String>) o)
                .collect(Collectors.toMap(m -> m.getOrDefault("apiField", ""), m -> m.getOrDefault("formField", "")));
    }

    @Override
    public boolean isDebug() {
        return "true".equalsIgnoreCase(getPropertyString("debug"));
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

        final String docAction = parameters.get("docAction");

        final ModelOrientedWebService.Builder builder = new ModelOrientedWebService.Builder()
                .setBaseUrl(getBaseUrl())
                .setServiceType(serviceType)
                .setMethod(method)
                .setLoginRequest(new LoginRequest(username, password, language, clientId, roleId, orgId, warehouseId))
                .setTable(getDeleteTable())
                .setDocAction(docAction);

        if (primaryKey > 0) {
            builder.setRecordId(primaryKey);
        }

        if (isIgnoreCertificateError()) {
            builder.ignoreSslCertificateError();
        }

        try {
            final ModelOrientedWebService webService = builder.build();
            if (isDebug()) {
                LogUtil.info(getClass().getName(), "executeDeletionService : request [" + webService.getRequest().getRequestPayload() + "]");
            }

            StandardResponse response = (StandardResponse) webService.execute();

            if (isDebug()) {
                LogUtil.info(getClass().getName(), "executeDeletionService : response [" + response.getResponsePayload() + "]");
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

    protected String getDeleteTable() {
        return getPropertyString("table");
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
}
