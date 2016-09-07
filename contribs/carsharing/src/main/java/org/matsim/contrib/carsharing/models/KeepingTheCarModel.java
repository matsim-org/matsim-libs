/**
 * 
 */
package org.matsim.contrib.carsharing.models;

import org.matsim.api.core.v01.population.Person;

/**
 * @author balac *
 */
public interface KeepingTheCarModel {	

	public boolean keepTheCarDuringNextActivity(double d, Person person, String type);

}
