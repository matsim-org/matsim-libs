package org.matsim.contrib.carsharing.models;

import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;

public interface ChooseVehicleType {

	String getPreferredVehicleType(Plan plan, Leg currentLeg);

}