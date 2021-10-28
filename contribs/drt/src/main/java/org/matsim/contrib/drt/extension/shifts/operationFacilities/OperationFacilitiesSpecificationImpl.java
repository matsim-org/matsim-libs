package org.matsim.contrib.drt.extension.shifts.operationFacilities;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.common.collections.SpecificationContainer;

import java.util.Map;

/**
 * @author nkuehnel / MOIA
 */
public class OperationFacilitiesSpecificationImpl implements OperationFacilitiesSpecification {

	private final SpecificationContainer<OperationFacility, OperationFacilitySpecification> container = new SpecificationContainer<>();

	@Override
	public Map<Id<OperationFacility>, OperationFacilitySpecification> getOperationFacilitySpecifications() {
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
