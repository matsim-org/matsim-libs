package org.matsim.contrib.carsharing.models;

import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
/** 
 * @author balac
 */
public interface ChooseTheCompany {
	
	public String pickACompany(Plan plan, Leg leg, double now, String vehicleType);

}
