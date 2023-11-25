package org.matsim.contrib.drt.extension.operations.shifts.shift;

import java.util.Map;
import org.matsim.api.core.v01.Id;

/**
 * @author nkuehnel / MOIA
 */
public interface DrtShiftsSpecification {
  Map<Id<DrtShift>, DrtShiftSpecification> getShiftSpecifications();

  void addShiftSpecification(DrtShiftSpecification specification);

  void replaceShiftSpecification(DrtShiftSpecification specification);

  void removeShiftSpecification(Id<DrtShift> vehicleId);
}
