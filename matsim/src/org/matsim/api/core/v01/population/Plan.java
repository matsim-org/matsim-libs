package org.matsim.api.core.v01.population;

import org.matsim.api.basic.v01.population.BasicPlan;
import org.matsim.api.basic.v01.population.PlanElement;

public interface Plan extends BasicPlan<PlanElement> {

	public Person getPerson();
	
}