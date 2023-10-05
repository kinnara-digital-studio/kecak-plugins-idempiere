package com.kinnaratsudio.kecakplugins.idempiere.form;

import com.kinnaratsudio.kecakplugins.idempiere.commons.IdempiereMixin;
import com.kinnaratsudio.kecakplugins.idempiere.exception.IdempiereClientException;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.dao.FormDataDao;
import org.joget.apps.form.model.*;
import org.joget.apps.form.service.FormUtil;
import org.joget.commons.util.LogUtil;
import org.joget.plugin.base.PluginManager;
import org.joget.workflow.util.WorkflowUtil;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class IdempiereLoginUserOptionsBinder extends FormBinder implements FormLoadOptionsBinder, FormAjaxOptionsBinder, IdempiereMixin {
    public final static String LABEL = "iDempiere Login User Options Binder";
    @Override
    public boolean useAjax() {
        return true;
    }

    @Override
    public FormRowSet loadAjaxOptions(String[] tenantIds) {
        try {
            final FormDataDao formDataDao = (FormDataDao) AppUtil.getApplicationContext().getBean("formDataDao");
            final Form form = getLoginForm();
            final String where = tenantIds == null || tenantIds.length == 0 ? null : Arrays.stream(tenantIds).map(s -> "?").collect(Collectors.joining(",", "where e.customProperties.idm_clientid in (", ")"));
            return Optional.ofNullable(formDataDao.find(form, where, tenantIds, null, null, null, null))
                    .map(Collection::stream)
                    .orElseGet(Stream::empty)
                    .map(r -> {
                        final String value = r.getProperty("idm_userid");
                        final String label = r.getProperty("username");

                        final FormRow row = new FormRow();
                        row.setProperty(FormUtil.PROPERTY_VALUE, value);
                        row.setProperty(FormUtil.PROPERTY_LABEL, label);
                        return row;
                    })
                    .collect(Collectors.toCollection(FormRowSet::new));
        } catch (IdempiereClientException e) {
            LogUtil.error(getClass().getName(), e, e.getMessage());
            return null;
        }
    }

    @Override
    public FormRowSet load(Element element, String primaryKey, FormData formData) {
        setFormData(formData);
        try {
            final int tenantId = getTenantId();
            return loadAjaxOptions(new String[] {String.valueOf(tenantId)});
        } catch (IdempiereClientException e) {
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
        return null;
    }

    protected Integer getTenantId() throws IdempiereClientException {
        final FormDataDao formDataDao = (FormDataDao) AppUtil.getApplicationContext().getBean("formDataDao");
        final Form form = getLoginForm();

        final String username = WorkflowUtil.getCurrentUsername();
        return Optional.ofNullable(formDataDao.find(form, "where e.customProperties.username = ?", new Object[]{ username }, null, null, null, 1))
                .map(Collection::stream)
                .orElseGet(Stream::empty)
                .map(r -> r.getProperty("idm_clientid"))
                .filter(Objects::nonNull)
                .findFirst()
                .map(Integer::parseInt)
                .orElseThrow(() -> new IdempiereClientException("User [" + username + "] cannot be found in Login Configuration"));
    }
}
