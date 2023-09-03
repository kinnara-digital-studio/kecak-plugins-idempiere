package com.kinnaratsudio.kecakplugins.idempiere.form;

import com.kinnaratsudio.kecakplugins.idempiere.commons.IdempiereMixin;
import com.kinnaratsudio.kecakplugins.idempiere.exception.IdempiereClientException;
import org.joget.apps.app.model.AppDefinition;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.dao.FormDataDao;
import org.joget.apps.form.model.*;
import org.joget.apps.form.service.FormUtil;
import org.joget.commons.util.LogUtil;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class IdempiereLoginUserOptionsBinder extends FormBinder implements FormLoadOptionsBinder, FormAjaxOptionsBinder, IdempiereMixin {
    public final static String LABEL = "iDempiere Login User Options Binder";
    @Override
    public boolean useAjax() {
        return true;
    }

    @Override
    public FormRowSet loadAjaxOptions(String[] dependencyValues) {
        try {
            final FormDataDao formDataDao = (FormDataDao) AppUtil.getApplicationContext().getBean("formDataDao");
            final Form form = getLoginForm();
            return Optional.ofNullable(formDataDao.find(form, null, null, null, null, null, null))
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
            LogUtil.error(getClassName(), e, e.getMessage());
            return null;
        }
    }

    @Override
    public FormRowSet load(Element element, String primaryKey, FormData formData) {
        setFormData(formData);
        return loadAjaxOptions(null);
    }

    @Override
    public String getName() {
        return LABEL;
    }

    @Override
    public String getVersion() {
        return getClass().getPackage().getImplementationVersion();
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
        return null;
    }

    @Override
    public String getPropertyOptions() {
        return null;
    }
}
