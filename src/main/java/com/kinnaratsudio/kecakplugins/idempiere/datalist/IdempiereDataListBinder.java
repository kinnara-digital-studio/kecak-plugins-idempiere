package com.kinnaratsudio.kecakplugins.idempiere.datalist;

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
import org.joget.apps.datalist.model.*;
import org.joget.commons.util.LogUtil;
import org.joget.plugin.base.PluginManager;
import org.json.JSONArray;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class IdempiereDataListBinder extends DataListBinderDefault {
    public final static String LABEL = "iDempiere DataList Binder";

    @Override
    public DataListColumn[] getColumns() {
        try {
            final String serviceType = getService();
            final ServiceMethod method = getMethod();

            final WindowTabData response = executeService(serviceType, method, null, null, null, null, 1, true);

            return Optional.of(response)
                    .map(WindowTabData::getDataRows)
                    .map(Arrays::stream)
                    .orElseGet(Stream::empty)
                    .map(DataRow::getFieldEntries)
                    .flatMap(Arrays::stream)
                    .map(FieldEntry::getColumn)
                    .distinct()
                    .map(s -> new DataListColumn(s, s, false))
                    .toArray(DataListColumn[]::new);
        } catch (IdempiereClientException e) {
            LogUtil.error(getClassName(), e, e.getMessage());
            return new DataListColumn[0];
        }
    }

    @Override
    public String getPrimaryKeyColumnName() {
        if (!getPropertyString("primaryKeyColumn").isEmpty()) {
            return getPropertyString("primaryKeyColumn");
        } else if (!getTable().isEmpty()) {
            return getTable() + "_ID";
        } else {
            return "id";
        }
    }

    @Override
    public DataListCollection<Map<String, String>> getData(DataList dataList, Map properties, @Nullable DataListFilterQueryObject[] filterQueryObjects, String sort, @Nullable Boolean desc, @Nullable Integer start, @Nullable Integer rows) {
        try {

            final String serviceType = getService();
            final ServiceMethod method = getMethod();
            final WindowTabData response = executeService(serviceType, method, filterQueryObjects, sort, desc, start, rows, false);
            return Arrays.stream(response.getDataRows())
                    .map(dr -> Arrays.stream(dr.getFieldEntries())
                            .collect(Collectors.toMap(FieldEntry::getColumn, e -> String.valueOf(e.getValue()))))
                    .collect(Collectors.toCollection(DataListCollection::new));
        } catch (IdempiereClientException e) {
            LogUtil.error(getClassName(), e, e.getMessage());
            return null;
        }
    }

    @Override
    public int getDataTotalRowCount(DataList dataList, Map properties, DataListFilterQueryObject[] filterQueryObjects) {
        try {
            final String serviceType = getService();
            final ServiceMethod method = getMethod();
            final WindowTabData response = executeService(serviceType, method, filterQueryObjects, null, null, null, 1, false);
            return response.getTotalRows();
        } catch (IdempiereClientException e) {
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
                "/properties/commons/WebServiceType.json",
                "/properties/commons/WebServiceParameters.json",
                "/properties/commons/WebServiceFieldInput.json",
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

    protected boolean isIgnoreCertificateError() {
        return "true".equalsIgnoreCase(getPropertyString("ignoreCertificateError"));
    }

    protected Map<String, String> getWebServiceInput() {
        return Arrays.stream(getPropertyGrid("webServiceInput"))
                .collect(Collectors.toMap(m -> m.get("inputField"), m -> m.get("value")));
    }

    protected WindowTabData executeService(String serviceType, ServiceMethod method, @Nullable DataListFilterQueryObject[] filterQueryObjects, String sortIgnored, @Nullable Boolean descIgnored, @Nullable Integer start, @Nullable Integer rows, boolean isGetColumns) throws IdempiereClientException {
        final String username = getUsername();
        final String password = getPassword();
        final String language = getLanguage();
        final int clientId = getClientId();
        final int roleId = getRoleId();
        final int orgId = getOrgId();
        final Integer warehouseId = getWarehouseId();

        try {
            final Stream<FieldEntry> streamDefaultFilter = getWebServiceInput().entrySet().stream()
                    .map(e -> new FieldEntry(e.getKey(), e.getValue()));

            final Stream<FieldEntry> streamQueryObjects = Optional.ofNullable(filterQueryObjects)
                    .map(Arrays::stream)
                    .orElseGet(Stream::empty)
                    .filter(q -> q instanceof IdempiereDataListFilter.IdempiereDataListFilterQueryObject && q.getValues() != null)
                    .map(q -> {
                        final IdempiereDataListFilter.FilterQueryType type = ((IdempiereDataListFilter.IdempiereDataListFilterQueryObject) q).getType();
                        final String name = q.getQuery();

                        if (type == IdempiereDataListFilter.FilterQueryType.OPTIONS) {
                            final String[] value = Optional.of(q)
                                    .map(DataListFilterQueryObject::getValues)
                                    .map(Arrays::stream)
                                    .orElseGet(Stream::empty)
                                    .filter(Objects::nonNull)
                                    .map(s -> s.split(";"))
                                    .flatMap(Arrays::stream)
                                    .toArray(String[]::new);

                            return new FieldEntry[]{new FieldEntry(name, value)};

                        } else {
                            return Optional.of(q)
                                    .map(DataListFilterQueryObject::getValues)
                                    .map(Arrays::stream)
                                    .orElseGet(Stream::empty)
                                    .filter(Objects::nonNull)
                                    .map(s -> s.split(";"))
                                    .flatMap(Arrays::stream)
                                    .filter(Try.toNegate(String::isEmpty))
                                    .map(s -> {
                                        final String value;
                                        if (type == IdempiereDataListFilter.FilterQueryType.LIKE) {
                                            value = "%" + s + "%";
                                        } else {
                                            value = s;
                                        }
                                        return new FieldEntry(name, value);
                                    })
                                    .toArray(FieldEntry[]::new);
                        }
                    })
                    .flatMap(Arrays::stream);

            final FieldEntry[] fieldEntries = Stream.concat(streamDefaultFilter, streamQueryObjects)
                    .toArray(FieldEntry[]::new);

            final Field field = new Field(fieldEntries);
            final DataRow dataRow = new DataRow(field);
            final ModelOrientedWebService.Builder builder = new ModelOrientedWebService.Builder()
                    .setBaseUrl(getBaseUrl())
                    .setServiceType(serviceType)
                    .setMethod(method)
                    .setLoginRequest(new LoginRequest(username, password, language, clientId, roleId, orgId, warehouseId))
                    .setOffset(start)
                    .setLimit(rows)
                    .setTable(getTable());

            if (!isGetColumns) {
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
}
