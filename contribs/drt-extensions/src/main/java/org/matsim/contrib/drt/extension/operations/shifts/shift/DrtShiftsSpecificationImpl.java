package org.matsim.contrib.drt.extension.operations.shifts.shift;

import java.util.Map;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.common.collections.SpecificationContainer;

/**
 * @author nkuehnel / MOIA
 */
public class DrtShiftsSpecificationImpl implements DrtShiftsSpecification {

  private final SpecificationContainer<DrtShift, DrtShiftSpecification> container =
      new SpecificationContainer<>();

  @Override
  public Map<Id<DrtShift>, DrtShiftSpecification> getShiftSpecifications() {
    return container.getSpecifications();
  }

  @Override
  public void addShiftSpecification(DrtShiftSpecification specification) {
    container.addSpecification(specification);
  }

  @Override
  public void replaceShiftSpecification(DrtShiftSpecification specification) {
    container.replaceSpecification(specification);
  }

  @Override
  public void removeShiftSpecification(Id<DrtShift> shiftId) {
    container.removeSpecification(shiftId);
  }
}
