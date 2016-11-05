/**
 * 
 */
package org.matsim.contrib.carsharing.models;

import org.matsim.api.core.v01.population.Person;

/**
 * @author balacm
 *
 */
public class KeepingTheCarModelExample implements KeepingTheCarModel {

	
	public KeepingTheCarModelExample() {		
		
	}
	
	@Override
	public boolean keepTheCarDuringNextActivity(double durationOfActivity, Person person, String csType) {

		return false;
		//if (durationOfActivity < 2 *3600) {
			
		//	return 	MatsimRandom.getRandom().nextDouble() > durationOfActivity / (2.0 * 3600.0);

			
		//}		
		//return false;		
	}
}
