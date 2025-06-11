package org.matsim.contrib.drt.routing;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.drt.optimizer.constraints.DrtRouteConstraints;
import org.matsim.utils.objectattributes.attributable.Attributes;

/**
 * @author nkuehnel / MOIA
 * @author Sebastian Hörl, IRT SystemX
 */
public interface DrtRouteConstraintsCalculator {

	DrtRouteConstraints calculateRouteConstraints( //
			double departureTime, //
			Link accessActLink, //
			Link egressActLink, //
			Person person, //
			Attributes tripAttributes, //
			double unsharedRideTime, //
			double unsharedDistance //
	);

}
