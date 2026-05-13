package org.matsim.simwrapper.DbViewer;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.vehicles.Vehicle;

public class LinkTraversal {
	Id<Person> agentId;
	Id<Link> linkId;
	double timeStamp;
	Id<Vehicle> mode;
//	double departureTime;
//	List<String> linkSequence = new ArrayList<>();
//
//	public void appendLink(String linkId, double time) {
//		if (linkSequence.isEmpty()) departureTime = time;
//		linkSequence.add(linkId);
//	}
}
