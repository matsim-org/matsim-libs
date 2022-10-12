package org.matsim.contrib.drt.extension.operations.operationFacilities;

import org.matsim.api.core.v01.Id;

import java.util.Map;

/**
 * @author nkuehnel / MOIA
 */
public interface OperationFacilitiesSpecification {

	Map<Id<OperationFacility>, OperationFacilitySpecification> getOperationFacilitySpecifications();

	void addOperationFacilitySpecification(OperationFacilitySpecification specification);

	void replaceOperationFacilitySpecification(OperationFacilitySpecification specification);

	void removeOperationFacilitySpecification(Id<OperationFacility> operationFacilityId);
}
