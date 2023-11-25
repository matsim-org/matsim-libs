package org.matsim.contrib.drt.extension.operations.operationFacilities;

import java.util.Map;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.common.collections.SpecificationContainer;

/**
 * @author nkuehnel / MOIA
 */
public class OperationFacilitiesSpecificationImpl implements OperationFacilitiesSpecification {

  private final SpecificationContainer<OperationFacility, OperationFacilitySpecification>
      container = new SpecificationContainer<>();

  @Override
  public Map<Id<OperationFacility>, OperationFacilitySpecification>
      getOperationFacilitySpecifications() {
    return container.getSpecifications();
  }

  @Override
  public void addOperationFacilitySpecification(OperationFacilitySpecification specification) {
    container.addSpecification(specification);
  }

  @Override
  public void replaceOperationFacilitySpecification(OperationFacilitySpecification specification) {
    container.replaceSpecification(specification);
  }

  @Override
  public void removeOperationFacilitySpecification(Id<OperationFacility> operationFacilityId) {
    container.removeSpecification(operationFacilityId);
  }
}
