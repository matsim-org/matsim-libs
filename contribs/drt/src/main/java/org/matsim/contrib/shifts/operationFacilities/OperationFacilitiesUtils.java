package org.matsim.contrib.shifts.operationFacilities;

import org.matsim.api.core.v01.Scenario;

/**
 * @author nkuehnel
 */
public class OperationFacilitiesUtils {

    public static final String DRT_OPERATION_FACILITIES = "drtOperationFacilities";

    public static OperationFacilities getOrCreateShifts(Scenario scenario) {
        // based on carriers in freight contrib, nk nov'20
        OperationFacilities facilities = (OperationFacilities) scenario.getScenarioElement(DRT_OPERATION_FACILITIES);
        if (facilities == null) {
            facilities = new OperationFacilitiesImpl();
            scenario.addScenarioElement(DRT_OPERATION_FACILITIES, facilities);
        }
        return facilities;
    }

    public static OperationFacilities getFacilities(Scenario scenario) {
        // based on carriers in freight contrib, nk nov'20
        return (OperationFacilities) scenario.getScenarioElement(DRT_OPERATION_FACILITIES);
    }
}
