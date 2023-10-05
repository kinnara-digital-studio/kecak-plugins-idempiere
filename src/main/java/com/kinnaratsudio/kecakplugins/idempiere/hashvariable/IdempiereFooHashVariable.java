package com.kinnaratsudio.kecakplugins.idempiere.hashvariable;

import org.joget.apps.app.model.DefaultHashVariablePlugin;
import org.joget.apps.app.service.AppUtil;
import org.joget.commons.util.LogUtil;
import org.joget.plugin.base.PluginManager;
import org.joget.workflow.util.WorkflowUtil;

import javax.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Stream;

public class IdempiereFooHashVariable extends DefaultHashVariablePlugin {
    @Override
    public String getPrefix() {
        return "iDempiereFoo";
    }

    @Override
    public String processHashVariable(String key) {
        final HttpServletRequest request = WorkflowUtil.getHttpServletRequest();
        LogUtil.info(getClass().getName(), "processHashVariable : key ["+key+"]");
        Optional.ofNullable((Map<String, String[]>) request.getParameterMap())
                .map(Map::entrySet)
                .map(Collection::stream)
                .orElseGet(Stream::empty)
                .forEach(e -> {
                    LogUtil.info(getClass().getName(), "load : parameterMap [" + e.getKey() + "] [" + String.join(",",e.getValue())+"]");
                });
        return key;
    }

    @Override
    public String getName() {
        return "iDempiere Foo Hash Variable";
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
        return getName();
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
