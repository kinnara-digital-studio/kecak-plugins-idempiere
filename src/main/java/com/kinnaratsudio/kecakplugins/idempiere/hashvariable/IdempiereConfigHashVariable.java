package com.kinnaratsudio.kecakplugins.idempiere.hashvariable;

import com.kinnaratsudio.kecakplugins.idempiere.commons.IdempiereMixin;
import com.kinnaratsudio.kecakplugins.idempiere.exception.IdempiereClientException;
import org.joget.apps.app.model.DefaultHashVariablePlugin;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.dao.FormDataDao;
import org.joget.apps.form.model.Form;
import org.joget.commons.util.LogUtil;
import org.joget.plugin.base.PluginManager;
import org.joget.workflow.util.WorkflowUtil;
import org.springframework.context.ApplicationContext;

import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Stream;

public class IdempiereConfigHashVariable extends DefaultHashVariablePlugin implements IdempiereMixin {
    public final static String LABEL = "iDempiere Configuration Hash Variable";
    @Override
    public String getPrefix() {
        return "idempiereConfig";
    }

    @Override
    public String processHashVariable(String variableKey) {
        final ApplicationContext applicationContext = AppUtil.getApplicationContext();
        final FormDataDao formDataDao = (FormDataDao) applicationContext.getBean("formDataDao");

        final String[] split = variableKey.split("\\.");
        final String currentUser = split.length < 2 ? WorkflowUtil.getCurrentUsername() : split[0];
        final String key = split.length < 2 ? variableKey : split[1];

        try {
            final Form formLogin = getLoginForm();
            return Optional.ofNullable(formDataDao.find(formLogin, "where e.customProperties.username = ?", new String[] {currentUser}, null, null, null, 1))
                    .map(Collection::stream)
                    .orElseGet(Stream::empty)
                    .findFirst()
                    .map(r -> r.getProperty("idm_" + key.toLowerCase(), ""))
                    .orElseThrow(() -> new IdempiereClientException("Login information for user [" + currentUser + "] key [" + variableKey + "] is not found"));
        } catch (IdempiereClientException e) {
            LogUtil.error(getClassName(), e, e.getMessage());
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
        Collection<String> syntax = new HashSet<>();
        syntax.add(this.getPrefix() + ".baseUrl");
        syntax.add(this.getPrefix() + ".user");
        syntax.add(this.getPrefix() + ".password");
        syntax.add(this.getPrefix() + ".lang");
        syntax.add(this.getPrefix() + ".clientId");
        syntax.add(this.getPrefix() + ".roleId");
        syntax.add(this.getPrefix() + ".orgId");
        syntax.add(this.getPrefix() + ".warehouseId");
        syntax.add(this.getPrefix() + ".stage");
        syntax.add(this.getPrefix() + ".USERNAME.baseUrl");
        syntax.add(this.getPrefix() + ".USERNAME.user");
        syntax.add(this.getPrefix() + ".USERNAME.lang");
        syntax.add(this.getPrefix() + ".USERNAME.clientId");
        syntax.add(this.getPrefix() + ".USERNAME.roleId");
        syntax.add(this.getPrefix() + ".USERNAME.orgId");
        syntax.add(this.getPrefix() + ".USERNAME.warehouseId");
        syntax.add(this.getPrefix() + ".USERNAME.stage");
        return syntax;
    }
}
