package org.matsim.contrib.drt.extension.shifts.shift;

import org.matsim.api.core.v01.Scenario;

/**
 * @author nkuehnel, fzwick
 */
public class DrtShiftUtils {

    public static final String DRT_SHIFTS = "drtShifts";

    public static DrtShifts getOrCreateShifts(Scenario scenario) {
        // based on carriers in freight contrib, nk nov'20
        DrtShifts shifts = (DrtShifts) scenario.getScenarioElement(DRT_SHIFTS);
        if (shifts == null) {
            shifts = new DrtShiftsImpl();
            scenario.addScenarioElement(DRT_SHIFTS, shifts);
        }
        return shifts;
    }

    public static DrtShifts getShifts(Scenario scenario) {
        // based on carriers in freight contrib, nk nov'20
        return (DrtShifts) scenario.getScenarioElement(DRT_SHIFTS);
    }
}
