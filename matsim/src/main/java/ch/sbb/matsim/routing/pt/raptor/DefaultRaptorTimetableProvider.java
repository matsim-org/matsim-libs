package ch.sbb.matsim.routing.pt.raptor;

import org.matsim.api.core.v01.population.Person;
import org.matsim.utils.objectattributes.attributable.Attributes;

/** Routes every request on the times of the given (unupdated) transit schedule. */
public class DefaultRaptorTimetableProvider implements RaptorTimetableProvider {

	@Override
	public RaptorTimetable getTimetable(SwissRailRaptorData data, double departureTime, Person person, Attributes routingAttributes) {
		return data.scheduleTimetable;
	}
}
