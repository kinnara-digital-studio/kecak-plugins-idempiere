package com.kinnaratsudio.kecakplugins.idempiere;

import java.util.ArrayList;
import java.util.Collection;

import com.kinnaratsudio.kecakplugins.idempiere.datalist.IdempiereDataListBinder;
import com.kinnaratsudio.kecakplugins.idempiere.datalist.IdempiereRowDeleteDatalistAction;
import com.kinnaratsudio.kecakplugins.idempiere.form.IdempiereFormBinder;
import com.kinnaratsudio.kecakplugins.idempiere.form.IdempiereOptionsBinder;
import com.kinnaratsudio.kecakplugins.idempiere.hashvariable.IdempiereConfigHashVariable;
import com.kinnaratsudio.kecakplugins.idempiere.process.IdempiereWebServiceTool;
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
        registrationList.add(context.registerService(IdempiereDataListBinder.class.getName(), new IdempiereDataListBinder(), null));
        registrationList.add(context.registerService(IdempiereRowDeleteDatalistAction.class.getName(), new IdempiereRowDeleteDatalistAction(), null));
        registrationList.add(context.registerService(IdempiereWebServiceTool.class.getName(), new IdempiereWebServiceTool(), null));
        registrationList.add(context.registerService(IdempiereConfigHashVariable.class.getName(), new IdempiereConfigHashVariable(), null));
    }

    public void stop(BundleContext context) {
        for (ServiceRegistration registration : registrationList) {
            registration.unregister();
        }
    }
}