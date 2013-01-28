package org.matsim.core.router;

import java.util.List;

import org.matsim.api.core.v01.population.PlanElement;

public interface MainModeIdentifier {
	public String identifyMainMode( List<PlanElement> tripElements );
}