package org.matsim.contrib.drt.extension.shifts.operationFacilities;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.ev.infrastructure.Charger;

import java.util.Optional;

/**
 * @author nkuehnel / MOIA
 */
public interface OperationFacilitySpecification extends Identifiable<OperationFacility> {

	int getCapacity();

	Optional<Id<Charger>> getCharger();

	OperationFacilityType getType();

	Id<Link> getLinkId();

	Coord getCoord();
}
