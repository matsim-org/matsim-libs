package org.matsim.contrib.vsp.pt.fare;

import com.google.inject.Inject;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.AgentWaitingForPtEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.router.StageActivityTypeIdentifier;
import org.matsim.pt.PtConstants;

import java.util.HashMap;
import java.util.Map;

public class ChainedPtFareHandler implements PtFareHandler {
	@Inject
	private EventsManager events;

	private final Map<Id<Person>, Coord> personDepartureCoordMap = new HashMap<>();
	private final Map<Id<Person>, Coord> personArrivalCoordMap = new HashMap<>();

	@Inject
	private ChainedPtFareCalculator fareCalculator;

	@Override
	public void handleEvent(ActivityStartEvent event) {
		if (event.getActType().equals(PtConstants.TRANSIT_ACTIVITY_TYPE)) {
			personDepartureCoordMap.computeIfAbsent(event.getPersonId(), c -> event.getCoord()); // The departure place is fixed to the place of
			// first pt interaction an agent has in the whole leg
			personArrivalCoordMap.put(event.getPersonId(), event.getCoord()); // The arrival stop will keep updating until the agent start a real
			// activity (i.e. finish the leg)
		}

		if (StageActivityTypeIdentifier.isStageActivity(event.getActType())) {
			return;
		}

		Id<Person> personId = event.getPersonId();
		if (!personDepartureCoordMap.containsKey(personId)) {
			return;
		}

		Coord from = personDepartureCoordMap.get(personId);
		Coord to = personArrivalCoordMap.get(personId);

		PtFareCalculator.FareResult fare = fareCalculator.calculateFare(from, to).orElseThrow();

		// charge fare to the person
		events.processEvent(new PersonMoneyEvent(event.getTime(), event.getPersonId(), -fare.fare(), PtFareConfigGroup.PT_FARE,
			fare.transactionPartner(), event.getPersonId().toString()));

		personDepartureCoordMap.remove(personId);
		personArrivalCoordMap.remove(personId);
	}

	@Override
	public void handleEvent(AgentWaitingForPtEvent event) {
		//TODO
	}

	@Override
	public void reset(int iteration) {
		personArrivalCoordMap.clear();
		personDepartureCoordMap.clear();
	}
}
