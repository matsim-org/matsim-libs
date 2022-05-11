package org.matsim.contrib.drt.extension.shifts.shift;

import org.matsim.api.core.v01.Id;

import java.util.Map;

/**
 * @author nkuehnel / MOIA
 */
public interface DrtShiftsSpecification {
	Map<Id<DrtShift>, DrtShiftSpecification> getShiftSpecifications();

	void addShiftSpecification(DrtShiftSpecification specification);

	void replaceShiftSpecification(DrtShiftSpecification specification);

	void removeShiftSpecification(Id<DrtShift> vehicleId);
}
