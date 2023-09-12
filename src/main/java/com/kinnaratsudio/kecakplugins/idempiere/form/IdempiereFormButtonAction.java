package com.kinnaratsudio.kecakplugins.idempiere.form;

import com.kinnarastudio.idempiere.type.ServiceMethod;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.model.Form;
import org.joget.apps.form.model.FormButton;
import org.joget.apps.form.model.FormData;
import org.joget.plugin.base.PluginManager;

import javax.annotation.Nullable;
import java.util.ResourceBundle;

public class IdempiereFormButtonAction extends FormButton {
    public final static String LABEL = "iDempiere Form Action Button";

    @Override
    public FormData actionPerformed(Form form, FormData formData) {
        return null;
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

    protected boolean isDebug() {
        return "true".equalsIgnoreCase(getPropertyString("debug"));
    }
}
