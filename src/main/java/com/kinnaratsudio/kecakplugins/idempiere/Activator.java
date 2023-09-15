package com.kinnaratsudio.kecakplugins.idempiere;

import java.util.ArrayList;
import java.util.Collection;

import com.kinnaratsudio.kecakplugins.idempiere.datalist.IdempiereDataListBinder;
import com.kinnaratsudio.kecakplugins.idempiere.datalist.IdempiereDataListFilter;
import com.kinnaratsudio.kecakplugins.idempiere.datalist.IdempiereDatalistAction;
import com.kinnaratsudio.kecakplugins.idempiere.form.CompositeIdempiereFormBinder;
import com.kinnaratsudio.kecakplugins.idempiere.form.IdempiereLoginUserOptionsBinder;
import com.kinnaratsudio.kecakplugins.idempiere.form.IdempiereFormBinder;
import com.kinnaratsudio.kecakplugins.idempiere.form.IdempiereOptionsBinder;
import com.kinnaratsudio.kecakplugins.idempiere.hashvariable.IdempiereConfigHashVariable;
import com.kinnaratsudio.kecakplugins.idempiere.hashvariable.IdempiereWebServiceQueryDataHashVariable;
import com.kinnaratsudio.kecakplugins.idempiere.permission.IdempiereLoginUserPermission;
import com.kinnaratsudio.kecakplugins.idempiere.process.IdempiereWebServiceTool;
import com.kinnaratsudio.kecakplugins.idempiere.webservice.IdempiereGetPropertyJson;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public class Activator implements BundleActivator {

    protected Collection<ServiceRegistration> registrationList;

    public void start(BundleContext context) {
        registrationList = new ArrayList<ServiceRegistration>();

        //Register plugin here
        registrationList.add(context.registerService(IdempiereFormBinder.class.getName(), new IdempiereFormBinder(), null));
        registrationList.add(context.registerService(IdempiereOptionsBinder.class.getName(), new IdempiereOptionsBinder(), null));
        registrationList.add(context.registerService(IdempiereLoginUserOptionsBinder.class.getName(), new IdempiereLoginUserOptionsBinder(), null));
        registrationList.add(context.registerService(IdempiereDataListBinder.class.getName(), new IdempiereDataListBinder(), null));
        registrationList.add(context.registerService(IdempiereDatalistAction.class.getName(), new IdempiereDatalistAction(), null));
        registrationList.add(context.registerService(IdempiereDataListFilter.class.getName(), new IdempiereDataListFilter(), null));
        registrationList.add(context.registerService(IdempiereWebServiceTool.class.getName(), new IdempiereWebServiceTool(), null));
        registrationList.add(context.registerService(IdempiereConfigHashVariable.class.getName(), new IdempiereConfigHashVariable(), null));
        registrationList.add(context.registerService(IdempiereWebServiceQueryDataHashVariable.class.getName(), new IdempiereWebServiceQueryDataHashVariable(), null));
        registrationList.add(context.registerService(IdempiereGetPropertyJson.class.getName(), new IdempiereGetPropertyJson(), null));
        registrationList.add(context.registerService(CompositeIdempiereFormBinder.class.getName(), new CompositeIdempiereFormBinder(), null));
        registrationList.add(context.registerService(IdempiereLoginUserPermission.class.getName(), new IdempiereLoginUserPermission(), null));
    }

    public void stop(BundleContext context) {
        for (ServiceRegistration registration : registrationList) {
            registration.unregister();
        }
    }
}