package org.matsim.modechoice.constraints;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;

public interface TripConstraint<T> {

	// or list of legs ?

	T getContext(Person person, Plan plan);

	boolean filter(T context, byte[] modes);

	// TODO: internal mode representation ?

	// pass whole plan model ?
	// put mapping into the plan model


}
