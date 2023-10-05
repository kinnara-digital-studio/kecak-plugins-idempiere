package com.kinnaratsudio.kecakplugins.idempiere.permission;

import com.kinnaratsudio.kecakplugins.idempiere.commons.IdempiereMixin;
import com.kinnaratsudio.kecakplugins.idempiere.exception.IdempiereClientException;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.dao.FormDataDao;
import org.joget.apps.form.model.Form;
import org.joget.apps.form.model.FormRowSet;
import org.joget.apps.userview.model.UserviewPermission;
import org.joget.commons.util.LogUtil;
import org.joget.directory.model.User;
import org.joget.plugin.base.PluginManager;

import java.util.Collection;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Stream;

/**
 * Permission for users listed in table app_fd_idempiere_login
 */
public class IdempiereLoginUserPermission extends UserviewPermission implements IdempiereMixin {
    public static String LABEL = "iDempiere Login User Permission";
    @Override
    public boolean isAuthorize() {
        final FormDataDao formDataDao = (FormDataDao) AppUtil.getApplicationContext().getBean("formDataDao");
        try {
            final Form form = getLoginForm();
            final String username = Optional.ofNullable(getCurrentUser())
                    .map(User::getUsername)
                    .orElse("");
            final FormRowSet rowSet = formDataDao.find(form, "where e.customProperties.username = ?", new Object[] { username }, null, null, null, null);
            return Optional.ofNullable(rowSet)
                    .map(Collection::stream)
                    .orElseGet(Stream::empty)
                    .anyMatch(r -> "true".equalsIgnoreCase(r.getProperty("active")));
        } catch (IdempiereClientException e) {
            LogUtil.error(getClass().getName(), e, e.getMessage());
            return false;
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
}
