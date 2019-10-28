package ch.sbb.matsim.routing.pt.raptor;

import org.matsim.api.core.v01.population.Person;
import org.matsim.facilities.Facility;

import java.util.List;

/**
 * Find potential stops for access or egress, based on a start or end coordinate.
 *
 * @author mrieser / Simunto GmbH
 */
@FunctionalInterface
public interface RaptorStopFinder {

	public enum Direction { ACCESS, EGRESS }

	List<InitialStop> findStops(Facility facility, Person person, double departureTime, RaptorParameters parameters, SwissRailRaptorData data, Direction type);

}
