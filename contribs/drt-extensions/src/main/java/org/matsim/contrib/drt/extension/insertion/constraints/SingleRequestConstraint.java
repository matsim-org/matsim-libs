package org.matsim.contrib.drt.extension.insertion.constraints;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdSet;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.drt.extension.insertion.DrtInsertionConstraint;
import org.matsim.contrib.drt.optimizer.insertion.InsertionDetourTimeCalculator.DetourTimeInfo;
import org.matsim.contrib.drt.optimizer.insertion.InsertionGenerator.Insertion;
import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.dvrp.passenger.PassengerRequestSubmittedEvent;
import org.matsim.contrib.dvrp.passenger.PassengerRequestSubmittedEventHandler;
import org.matsim.core.events.MobsimScopeEventHandler;

public class SingleRequestConstraint
		implements DrtInsertionConstraint, PassengerRequestSubmittedEventHandler, MobsimScopeEventHandler {
	private final IdSet<Person> observedPersons = new IdSet<>(Person.class);

	@Override
	public boolean checkInsertion(DrtRequest drtRequest, Insertion insertion, DetourTimeInfo detourTimeInfo) {
		for (Id<Person> passengerId : drtRequest.getPassengerIds()) {
			if (observedPersons.contains(passengerId)) {
				return false;
			}
		}

		return true;
	}

	@Override
	public void handleEvent(PassengerRequestSubmittedEvent event) {
		observedPersons.addAll(event.getPersonIds());
	}
}
