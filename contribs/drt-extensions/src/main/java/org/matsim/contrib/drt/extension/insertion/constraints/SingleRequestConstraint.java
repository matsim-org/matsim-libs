package org.matsim.contrib.drt.extension.insertion.constraints;

import org.matsim.api.core.v01.IdSet;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.drt.extension.insertion.DrtInsertionConstraint;
import org.matsim.contrib.drt.optimizer.insertion.InsertionDetourTimeCalculator.DetourTimeInfo;
import org.matsim.contrib.drt.optimizer.insertion.InsertionGenerator.Insertion;
import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.dvrp.passenger.PassengerRequestRejectedEvent;
import org.matsim.contrib.dvrp.passenger.PassengerRequestRejectedEventHandler;
import org.matsim.core.events.MobsimScopeEventHandler;

public class SingleRequestConstraint
		implements DrtInsertionConstraint, PassengerRequestRejectedEventHandler, MobsimScopeEventHandler {
	private final IdSet<Person> rejectedPersons = new IdSet<>(Person.class);

	@Override
	public void handleEvent(PassengerRequestRejectedEvent event) {
		rejectedPersons.add(event.getPersonId());
	}

	@Override
	public boolean checkInsertion(DrtRequest drtRequest, Insertion insertion, DetourTimeInfo detourTimeInfo) {
		return !rejectedPersons.contains(drtRequest.getPassengerId());
	}
}
