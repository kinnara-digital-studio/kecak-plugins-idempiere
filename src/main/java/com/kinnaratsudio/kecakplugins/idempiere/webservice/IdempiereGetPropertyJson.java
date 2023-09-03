package com.kinnaratsudio.kecakplugins.idempiere.webservice;

import com.kinnarastudio.commons.jsonstream.JSONCollectors;
import com.kinnarastudio.commons.jsonstream.JSONStream;
import org.joget.apps.app.service.AppUtil;
import org.joget.commons.util.LogUtil;
import org.joget.plugin.base.ExtDefaultPlugin;
import org.joget.plugin.base.PluginManager;
import org.joget.plugin.base.PluginWebSupport;
import org.joget.workflow.util.WorkflowUtil;
import org.json.JSONArray;
import org.kecak.apps.exception.ApiException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.IntStream;

public class IdempiereGetPropertyJson extends ExtDefaultPlugin implements PluginWebSupport {
    public final static String LABEL = "iDempiere Get Property Json";
    @Override
    public String getName() {
        return LABEL;
    }

    @Override
    public String getVersion() {
        PluginManager pluginManager = (PluginManager) AppUtil.getApplicationContext().getBean("pluginManager");
        ResourceBundle resourceBundle = pluginManager.getPluginMessageBundle(getClass().getName(), "/messages/BuildNumber");
        String buildNumber = resourceBundle.getString("buildNumber");
        return buildNumber;
    }

    @Override
    public String getDescription() {
        return getClass().getPackage().getImplementationTitle();
    }

    @Override
    public void webService(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        boolean isAdmin = WorkflowUtil.isCurrentUserInRole("ROLE_ADMIN");
        if (!isAdmin) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        try {
            final int page = Integer.parseInt(getParameter(request, "value"));
            final JSONArray jsonProperties = IntStream.range(1, page + 1).boxed()
                    .map(i -> AppUtil.readPluginResource(getClass().getName(), "/properties/commons/CompositeWebServiceSecurity.json", new Object[]{i, i, i, i, i, i}, true, "messages/Idempiere"))
                    .map(JSONArray::new)
                    .flatMap(j -> JSONStream.of(j, JSONArray::getJSONObject))
                    .collect(JSONCollectors.toJSONArray());

            response.getWriter().write(jsonProperties.toString());
        } catch (ApiException e) {
            LogUtil.error(getClass().getName(), e, e.getMessage());
            response.sendError(e.getErrorCode(), e.getMessage());
        }
    }

    protected Optional<String> optParameter(HttpServletRequest request, String parameterName) {
        return Optional.of(parameterName).map(request::getParameter);
    }
    protected String getParameter(HttpServletRequest request, String parameterName) throws ApiException {
        return optParameter(request, parameterName).orElseThrow(() -> new ApiException(HttpServletResponse.SC_BAD_REQUEST, "Parameter [" +parameterName+ "] is not supplied"));
    }
}
