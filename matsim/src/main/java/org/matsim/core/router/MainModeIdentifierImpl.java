package org.matsim.core.router;

import java.util.List;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;

public final class MainModeIdentifierImpl implements MainModeIdentifier {
	@Override
	public String identifyMainMode(
			final List<PlanElement> tripElements) {
		String mode = ((Leg) tripElements.get( 0 )).getMode();
		return mode.equals( TransportMode.transit_walk ) ? TransportMode.pt : mode;
	}
}