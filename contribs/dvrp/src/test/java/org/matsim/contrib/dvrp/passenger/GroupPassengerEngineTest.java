package org.matsim.contrib.dvrp.passenger;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.compress.utils.Sets;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.dvrp.examples.onetaxi.OneTaxiActionCreator;
import org.matsim.contrib.dvrp.examples.onetaxi.OneTaxiOptimizer;
import org.matsim.contrib.dvrp.examples.onetaxi.OneTaxiRequest;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.DvrpVehicleImpl;
import org.matsim.contrib.dvrp.fleet.Fleet;
import org.matsim.contrib.dvrp.fleet.ImmutableDvrpVehicleSpecification;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.contrib.dvrp.optimizer.VrpOptimizer;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeQSimModule;
import org.matsim.contrib.dvrp.run.DvrpModes;
import org.matsim.contrib.dvrp.run.MobsimTimerProvider;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentLogic;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentSourceQSimModule;
import org.matsim.contrib.dynagent.run.DynActivityEngine;
import org.matsim.core.events.MobsimScopeEventHandlingModule;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimBuilder;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

import java.util.List;
import java.util.Set;

import static org.matsim.contrib.dvrp.passenger.PassengerEngineTestFixture.*;

public class GroupPassengerEngineTest {

	private final PassengerEngineTestFixture fixture = new PassengerEngineTestFixture();

	private final Id<DvrpVehicle> VEHICLE_ID = Id.create("taxi1", DvrpVehicle.class);
	private final DvrpVehicle oneTaxi = new DvrpVehicleImpl(ImmutableDvrpVehicleSpecification.newBuilder()
			.id(VEHICLE_ID)
			.serviceBeginTime(0)
			.serviceEndTime(3600)
			.startLinkId(fixture.linkAB.getId())
			.capacity(1)
			.build(), fixture.linkAB);
	private final Fleet fleet = () -> ImmutableMap.of(oneTaxi.getId(), oneTaxi);

	@Test
	public void test_group() {
		double departureTime = 0;
		Id<Person> person1 = Id.createPersonId("1");
		Id<Person> person2 = Id.createPersonId("2");

		fixture.addPersonWithLeg(fixture.linkAB, fixture.linkBA, departureTime, person1);
		fixture.addPersonWithLeg(fixture.linkAB, fixture.linkBA, departureTime, person2);

		PassengerRequestValidator requestValidator = request -> Set.of();//valid
		createQSim(requestValidator, OneTaxiOptimizer.class).run();

		double pickupStartTime = 1;
		double pickupEndTime = pickupStartTime + OneTaxiOptimizer.PICKUP_DURATION;
		double taxiDepartureTime = pickupEndTime + 1;
		double taxiEntersLinkBATime = taxiDepartureTime + 1;
		double taxiArrivalTime = taxiEntersLinkBATime + (fixture.linkBA.getLength() / fixture.linkBA.getFreespeed());
		double dropoffEndTime = taxiArrivalTime + OneTaxiOptimizer.DROPOFF_DURATION;

		//1 second delay between pickupEndTime and taxiDepartureTime is not considered in schedules
		double scheduledDropoffTime = dropoffEndTime - pickupStartTime - 1;

		var requestId = Id.create("taxi_0", Request.class);
		fixture.assertPassengerEvents(
				List.of(person1, person2),
				new ActivityEndEvent(departureTime, person2, fixture.linkAB.getId(), null, START_ACTIVITY),
				new PersonDepartureEvent(departureTime, person2, fixture.linkAB.getId(), MODE, MODE),
				new ActivityEndEvent(departureTime, person1, fixture.linkAB.getId(), null, START_ACTIVITY),
				new PersonDepartureEvent(departureTime, person1, fixture.linkAB.getId(), MODE, MODE),
				new PassengerWaitingEvent(departureTime, MODE, requestId, List.of(person1, person2)),
				new PassengerRequestScheduledEvent(departureTime, MODE, requestId, List.of(person1, person2), VEHICLE_ID, 0,
						scheduledDropoffTime),
				new PersonEntersVehicleEvent(pickupStartTime, person1, Id.createVehicleId(VEHICLE_ID)),
				new PassengerPickedUpEvent(pickupStartTime, MODE, requestId, person1, VEHICLE_ID),
				new PersonEntersVehicleEvent(pickupStartTime, person2, Id.createVehicleId(VEHICLE_ID)),
				new PassengerPickedUpEvent(pickupStartTime, MODE, requestId, person2, VEHICLE_ID),
				new PassengerDroppedOffEvent(dropoffEndTime, MODE, requestId, person1, VEHICLE_ID),
				new PersonLeavesVehicleEvent(dropoffEndTime, person1, Id.createVehicleId(VEHICLE_ID)),
				new PersonArrivalEvent(dropoffEndTime, person1, fixture.linkBA.getId(), MODE),
				new ActivityStartEvent(dropoffEndTime, person1, fixture.linkBA.getId(), null, END_ACTIVITY),
				new PassengerDroppedOffEvent(dropoffEndTime, MODE, requestId, person2, VEHICLE_ID),
				new PersonLeavesVehicleEvent(dropoffEndTime, person2, Id.createVehicleId(VEHICLE_ID)),
				new PersonArrivalEvent(dropoffEndTime, person2, fixture.linkBA.getId(), MODE),
				new ActivityStartEvent(dropoffEndTime, person2, fixture.linkBA.getId(), null, END_ACTIVITY)
				);
	}


	private QSim createQSim(PassengerRequestValidator requestValidator, Class<? extends VrpOptimizer> optimizerClass) {
		return new QSimBuilder(fixture.config).useDefaults()
				.addOverridingModule(new MobsimScopeEventHandlingModule())
				.addQSimModule(new PassengerEngineQSimModule(MODE, PassengerEngineQSimModule.PassengerEngineType.WITH_GROUPS))
				.addQSimModule(new VrpAgentSourceQSimModule(MODE))
				.addQSimModule(new AbstractDvrpModeQSimModule(MODE) {
					@Override
					protected void configureQSim() {
						bindModal(Network.class).toInstance(fixture.network);
						bind(MobsimTimer.class).toProvider(MobsimTimerProvider.class).asEagerSingleton();

						//requests
						bindModal(PassengerRequestCreator.class).to(OneTaxiRequest.OneTaxiRequestCreator.class)
								.asEagerSingleton();
						bindModal(PassengerRequestValidator.class).toInstance(requestValidator);

						//supply
						addQSimComponentBinding(DynActivityEngine.COMPONENT_NAME).to(DynActivityEngine.class);
						bindModal(Fleet.class).toInstance(fleet);
						bindModal(VehicleType.class).toInstance(VehicleUtils.getDefaultVehicleType());
						bindModal(VrpOptimizer.class).to(optimizerClass).asEagerSingleton();
						bindModal(VrpAgentLogic.DynActionCreator.class).to(OneTaxiActionCreator.class)
								.asEagerSingleton();

						//groups
						bindModal(PassengerGroupIdentifier.class).toInstance(agent -> Id.create("group1", PassengerGroupIdentifier.PassengerGroup.class));
					}
				})
				.configureQSimComponents(components -> {
					components.addComponent(DvrpModes.mode(MODE));
					components.addNamedComponent(DynActivityEngine.COMPONENT_NAME);
				})
				.build(fixture.scenario, fixture.eventsManager);
	}
}
