package com.kinnaratsudio.kecakplugins.idempiere.hashvariable;

import com.kinnaratsudio.kecakplugins.idempiere.commons.IdempiereMixin;
import com.kinnaratsudio.kecakplugins.idempiere.exception.IdempiereClientException;
import org.joget.apps.app.model.DefaultHashVariablePlugin;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.dao.FormDataDao;
import org.joget.apps.form.model.Form;
import org.joget.commons.util.LogUtil;
import org.joget.plugin.base.PluginManager;
import org.joget.workflow.model.WorkflowAssignment;
import org.joget.workflow.model.WorkflowProcess;
import org.joget.workflow.model.service.WorkflowManager;
import org.joget.workflow.model.service.WorkflowUserManager;
import org.joget.workflow.util.WorkflowUtil;
import org.springframework.context.ApplicationContext;

import java.util.*;
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

        final String currentUser;
        final String key;
        if (split.length == 1) {
            currentUser = WorkflowUtil.getCurrentUsername();
            key = split[0];
        } else if (split.length == 2) {
            currentUser = split[0];
            key = split[1];
        } else if (split.length == 3) {
            final WorkflowAssignment wfAssignment = (WorkflowAssignment) this.getProperty("workflowAssignment");
            final String mode = split[0];
            switch (mode) {
                case "participant":
                    currentUser = getUserParticipant(wfAssignment, split[1]);
                    break;
                default:
                    currentUser = split[1];
            }

            key = split[2];
        } else {
            LogUtil.warn(getClassName(), "Error syntax [" + String.join(".", getPrefix(), variableKey) + "]");
            return "";
        }

        try {
            final Form formLogin = getLoginForm();
            return Optional.ofNullable(formDataDao.find(formLogin, "where e.customProperties.username = ?", new String[]{currentUser}, null, null, null, 1))
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
        syntax.add(this.getPrefix() + ".userId");
        syntax.add(this.getPrefix() + ".password");
        syntax.add(this.getPrefix() + ".lang");
        syntax.add(this.getPrefix() + ".clientId");
        syntax.add(this.getPrefix() + ".roleId");
        syntax.add(this.getPrefix() + ".orgId");
        syntax.add(this.getPrefix() + ".warehouseId");
        syntax.add(this.getPrefix() + ".stage");
        syntax.add(this.getPrefix() + ".USERNAME.baseUrl");
        syntax.add(this.getPrefix() + ".USERNAME.user");
        syntax.add(this.getPrefix() + ".USERNAME.userId");
        syntax.add(this.getPrefix() + ".USERNAME.lang");
        syntax.add(this.getPrefix() + ".USERNAME.clientId");
        syntax.add(this.getPrefix() + ".USERNAME.roleId");
        syntax.add(this.getPrefix() + ".USERNAME.orgId");
        syntax.add(this.getPrefix() + ".USERNAME.warehouseId");
        syntax.add(this.getPrefix() + ".USERNAME.stage");

        syntax.add(this.getPrefix() + ".participant.PARTICIPANT.baseUrl");
        syntax.add(this.getPrefix() + ".participant.PARTICIPANT.user");
        syntax.add(this.getPrefix() + ".participant.PARTICIPANT.userId");
        syntax.add(this.getPrefix() + ".participant.PARTICIPANT.lang");
        syntax.add(this.getPrefix() + ".participant.PARTICIPANT.clientId");
        syntax.add(this.getPrefix() + ".participant.PARTICIPANT.roleId");
        syntax.add(this.getPrefix() + ".participant.PARTICIPANT.orgId");
        syntax.add(this.getPrefix() + ".participant.PARTICIPANT.warehouseId");
        syntax.add(this.getPrefix() + ".participant.PARTICIPANT.stage");
        return syntax;
    }

    /**
     * @param wfAssignment
     * @param participantId
     * @return
     */
    protected String getUserParticipant(WorkflowAssignment wfAssignment, String participantId) {
        final WorkflowManager workflowManager = (WorkflowManager) AppUtil.getApplicationContext().getBean("workflowManager");
        final WorkflowProcess process = workflowManager.getProcess(wfAssignment.getProcessDefId());
        return Optional.ofNullable(participantId)
                .map(s -> s.split(";,"))
                .map(Arrays::stream)
                .orElseGet(Stream::empty)
                .map(s -> WorkflowUtil.getAssignmentUsers(process.getPackageId(), wfAssignment.getProcessDefId(), wfAssignment.getProcessId(), wfAssignment.getProcessVersion(), wfAssignment.getActivityId(), "", s))
                .peek(c -> LogUtil.info(getClassName(), "Participant ID [" + participantId + "] is assigned to [" + String.join(";", c) + "]"))
                .flatMap(Collection::stream)
                .findFirst()
                .orElse(WorkflowUserManager.ROLE_ANONYMOUS);
    }
}
