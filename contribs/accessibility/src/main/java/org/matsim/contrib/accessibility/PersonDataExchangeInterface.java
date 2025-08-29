package org.matsim.contrib.accessibility;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.facilities.ActivityFacility;

import java.util.Map;

public interface PersonDataExchangeInterface extends DataExchangeInterface {

	void setPersonAccessibilities(Person person, Double timeOfDay, String mode, double accessibility);


}
