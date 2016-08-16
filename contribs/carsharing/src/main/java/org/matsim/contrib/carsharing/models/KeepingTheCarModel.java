/**
 * 
 */
package org.matsim.contrib.carsharing.models;

import org.matsim.api.core.v01.population.Person;

/**
 * @author balacm *
 */
public interface KeepingTheCarModel {	

	public boolean keepTheCarDuringNextACtivity(double d, Person person, String type);

}
