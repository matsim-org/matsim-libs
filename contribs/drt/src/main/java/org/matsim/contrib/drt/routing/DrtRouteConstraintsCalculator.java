package org.matsim.contrib.drt.routing;

import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.drt.optimizer.constraints.DrtOptimizationConstraintsSet;
import org.matsim.utils.objectattributes.attributable.Attributes;

/**
 * @author nkuehnel / MOIA
 */
public interface DrtRouteConstraintsCalculator {

    double getMaxTravelTime(DrtOptimizationConstraintsSet constraintsSet, double unsharedRideTime, Person person, Attributes tripAttributes);

    double getMaxRideTime(DrtOptimizationConstraintsSet constraintsSet, double unsharedRideTime, Person person, Attributes tripAttributes);
    
    double getMaxWaitTime(DrtOptimizationConstraintsSet constraintsSet, double unsharedRideTime, Person person, Attributes tripAttributes);
}
