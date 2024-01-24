package com.kinnaratsudio.kecakplugins.idempiere.hashvariable;

import com.kinnarastudio.idempiere.exception.WebServiceBuilderException;
import com.kinnarastudio.idempiere.exception.WebServiceRequestException;
import com.kinnarastudio.idempiere.exception.WebServiceResponseException;
import com.kinnarastudio.idempiere.model.*;
import com.kinnarastudio.idempiere.type.ServiceMethod;
import com.kinnarastudio.idempiere.webservice.ModelOrientedWebService;
import com.kinnaratsudio.kecakplugins.idempiere.exception.IdempiereClientException;
import org.joget.apps.app.model.DefaultHashVariablePlugin;
import org.joget.apps.app.service.AppUtil;
import org.joget.commons.util.LogUtil;
import org.joget.plugin.base.PluginManager;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Usage : idempiereQueryData.SERVICE.COLUMN.id.VALUE
 * Usage : idempiereQueryData.SERVICE.COLUMN.Value.VALUE
 * Usage : idempiereQueryData.SERVICE.COLUMN.Name.VALUE
 * <p>
 * Returns RecordID for particular value or name
 */
public class IdempiereWebServiceQueryDataHashVariable extends DefaultHashVariablePlugin {
    public final static String LABEL = "iDempiere Query Data Hash Variable";

    @Override
    public String getPrefix() {
        return "idempiereQueryData";
    }

    @Override
    public String processHashVariable(String key) {
        try {
            final String[] split = key.split("\\.", 4);
            final String service = Arrays.stream(split).findFirst().orElseThrow(() -> new IdempiereClientException("Service is not supplied"));
            final String column = Arrays.stream(split).skip(1).findFirst().orElseThrow(() -> new IdempiereClientException("Column is not supplied"));
            final String filter = Arrays.stream(split).skip(2).findFirst().orElseThrow(() -> new IdempiereClientException("Method is not supplied"));
            final String value = Arrays.stream(split).skip(3).findFirst().orElseThrow(() -> new IdempiereClientException("Method value is not supplied"));

            ModelOrientedWebService.Builder builder = new ModelOrientedWebService.Builder();

            final String username = getUsername();
            final String password = getPassword();
            final String language = getLanguage();
            final int clientId = getClientId();
            final int roleId = getRoleId();
            final Integer orgId = getOrgId();
            final Integer warehouseId = getWarehouseId();

            builder.setServiceType(service)
                    .setBaseUrl(getBaseUrl())
                    .setLoginRequest(new LoginRequest(username, password, language, clientId, roleId, orgId, warehouseId))
                    .setMethod(getMethod())
                    .ignoreSslCertificateError();

            if (filter.equals("Id")) {
                builder.setRecordId(Integer.parseInt(value));
            } else {
                builder.setDataRow(new DataRow(new FieldEntry[]{
                        new FieldEntry(filter, value)
                }));
            }

            ModelOrientedWebService webService = builder.build();
            final WebServiceResponse response = webService.execute();

            if (response instanceof WindowTabData) {
                final DataRow[] dataRows = ((WindowTabData) response).getDataRows();
                return Optional.ofNullable(dataRows)
                        .map(Arrays::stream)
                        .orElseGet(Stream::empty)
                        .map(DataRow::getFieldEntries)
                        .flatMap(Arrays::stream)
                        .filter(fe -> column.equalsIgnoreCase(fe.getColumn()))
                        .map(FieldEntry::getValue)
                        .map(String::valueOf)
                        .collect(Collectors.joining(";"));
            }
            return "";
        } catch (IdempiereClientException | WebServiceBuilderException | WebServiceRequestException |
                 WebServiceResponseException e) {
            LogUtil.error(getClass().getName(), e, e.getMessage());
            return "";
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
        return null;
    }

    @Override
    public Collection<String> availableSyntax() {
        final String[] fields = new String[]{
                "Value",
                "Name"
        };

        final Collection<String> syntax = new ArrayList<>();
        syntax.add(getPrefix() + ".QUERY_SERVICE.GET_COLUMN.FIELD_INPUT_NAME.FIELD_INPUT_VALUE");
        for (String field : fields) {
            syntax.add(getPrefix() + ".QUERY_SERVICE.GET_COLUMN." + field + ".PARAM_VALUE");
        }

        return syntax;
    }

    protected String getBaseUrl() {
        return AppUtil.processHashVariable("#idempiereConfig.baseUrl#", null, null, null);
    }

    protected String getUsername() {
        return AppUtil.processHashVariable("#idempiereConfig.user#", null, null, null);
    }

    protected String getPassword() {
        return AppUtil.processHashVariable("#idempiereConfig.password#", null, null, null);
    }

    protected ServiceMethod getMethod() {
        return ServiceMethod.QUERY_DATA;
    }

    protected String getLanguage() {
        return AppUtil.processHashVariable("#idempiereConfig.lang#", null, null, null);
    }

    protected int getClientId() {
        return Integer.parseInt(AppUtil.processHashVariable("#idempiereConfig.clientId#", null, null, null));
    }

    protected int getRoleId() {
        return Integer.parseInt(AppUtil.processHashVariable("#idempiereConfig.roleId#", null, null, null));
    }

    protected Integer getOrgId() {
        try {
            return Integer.parseInt(AppUtil.processHashVariable("#idempiereConfig.orgId#", null, null, null));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    @Nullable
    protected Integer getWarehouseId() {
        try {
            return Integer.parseInt(AppUtil.processHashVariable("#idempiereConfig.warehouseId#", null, null, null));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Nullable
    protected Integer getStage() {
        try {
            return Integer.parseInt(AppUtil.processHashVariable("#idempiereConfig.stage#", null, null, null));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
