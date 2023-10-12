package com.kinnaratsudio.kecakplugins.idempiere.form;

import com.kinnarastudio.commons.Try;
import com.kinnarastudio.idempiere.model.StandardResponse;
import com.kinnarastudio.idempiere.type.ServiceMethod;
import com.kinnaratsudio.kecakplugins.idempiere.exception.IdempiereClientException;
import org.joget.apps.form.model.*;
import org.joget.commons.util.LogUtil;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public interface IdempiereFormDeleteBinder extends FormDeleteBinder {

    String getDeletionService();

    ServiceMethod getDeletionServiceMethod();

    StandardResponse executeDeletionService(int primaryKey, String serviceType, ServiceMethod method, Map<String, String> parameters, Map<String, String> fieldInput) throws IdempiereClientException;

    Map<String, String> getDeletionParameters();

    Map<String, String> getDeletionFieldInput();
    default void delete(Element element, FormRowSet rowSet, FormData formData, boolean deleteGrid, boolean deleteSubform, boolean abortProcess, boolean deleteFiles, boolean hardDelete) {
        final boolean isDebug = isDebug();
        final String serviceType = getDeletionService();
        final ServiceMethod method = getDeletionServiceMethod();
        final Map<String, String> parameters = getDeletionParameters();
        final Map<String, String> fieldInput = getDeletionFieldInput();

        if (serviceType.isEmpty()) {
            LogUtil.warn(getClass().getName(), "Cannot delete, deletion service is not defined");
            return;
        }

        Optional.ofNullable(rowSet)
                .map(Collection::stream)
                .orElseGet(Stream::empty)
                .map(FormRow::getId)
                .map(Try.onFunction(Integer::valueOf, (NumberFormatException e) -> 0))
                .distinct()
                .forEach(Try.onConsumer(primaryKey -> {
                    if (isDebug) {
                        LogUtil.info(getClass().getName(), "Deleting Record ID [" + primaryKey + "] service [" + serviceType + "] method [" + method + "]");
                    }

                    final StandardResponse response = executeDeletionService(primaryKey, serviceType, method, parameters, fieldInput);
                    if (response.isError()) {
                        throw new IdempiereClientException("Error deleting data [" + primaryKey + "]");
                    }
                }, (Exception e) -> {
                    if(isDebug) {
                        LogUtil.error(getClass().getName(), e, e.getMessage());
                    } else {
                        LogUtil.warn(getClass().getName(), e.getMessage());
                    }
                }));
    }

    default boolean isDebug() {
        return false;
    }
}
