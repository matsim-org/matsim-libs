package org.matsim.contrib.drt.optimizer.distributed;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Message;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.drt.optimizer.constraints.DrtRouteConstraints;
import org.matsim.contrib.dvrp.optimizer.Request;

import java.util.List;

public record RequestMessage(
	Id<Request> id,
	double submissionTime,
	DrtRouteConstraints constraints,
	List<Id<Person>> passengerIds,
	String mode,
	Id<Link> fromLink,
	Id<Link> toLink
) implements Message {
}
