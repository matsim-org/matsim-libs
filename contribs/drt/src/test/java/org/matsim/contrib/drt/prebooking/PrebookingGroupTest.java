package org.matsim.contrib.drt.prebooking;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.drt.prebooking.logic.AttributeBasedPrebookingLogic;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.dvrp.passenger.PassengerGroupIdentifier;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.mobsim.framework.MobsimPassengerAgent;
import org.matsim.testcases.MatsimTestUtils;

import java.util.Optional;

import static org.junit.Assert.assertEquals;

public class PrebookingGroupTest {

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();


	static PrebookingParams installPrebooking(Controler controller) {
		return installPrebooking(controller, true);
	}

	static PrebookingParams installPrebooking(Controler controller, boolean installLogic) {
		DrtConfigGroup drtConfig = DrtConfigGroup.getSingleModeDrtConfig(controller.getConfig());
		drtConfig.addParameterSet(new PrebookingParams());

		if (installLogic) {
			AttributeBasedPrebookingLogic.install(controller, drtConfig);
		}
		controller.addOverridingModule(new AbstractDvrpModeModule(drtConfig.getMode()) {
			@Override
			public void install() {
				bindModal(PassengerGroupIdentifier.class).toInstance(new PassengerGroupIdentifier() {
					@Override
					public Optional<Id<PassengerGroup>> getGroupId(MobsimPassengerAgent agent) {
						return Optional.of(Id.create("group", PassengerGroup.class));
					}
				});
			}
		});

		return drtConfig.getPrebookingParams().get();
	}

	@Test
	public void oneRequestArrivingLate() {

		// copy of prebooking test but with two persons in one request
		PrebookingTestEnvironment environment = new PrebookingTestEnvironment(utils) //
				.addVehicle("vehicleA", 1, 1) //
				// 1800 indicated but only departing 2000
				.addRequest("personA", 0, 0, 5, 5, 2000.0, 0.0, 2000.0 - 200.0) //
				.addRequest("personB", 0, 0, 5, 5, 2000.0, 0.0, 2000.0 - 200.0) //
				.configure(600.0, 1.3, 600.0, 60.0) //
				.endTime(10.0 * 3600.0);

		Controler controller = environment.build();
		installPrebooking(controller);
		controller.run();

		PrebookingTestEnvironment.RequestInfo requestInfoA = environment.getRequestInfo().get("personA");
		assertEquals(0.0, requestInfoA.submissionTime, 1e-3);
		assertEquals(2060.0, requestInfoA.pickupTime, 1e-3);
		assertEquals(2271.0, requestInfoA.dropoffTime, 1e-3);

		PrebookingTestEnvironment.RequestInfo requestInfoB = environment.getRequestInfo().get("personB");
		assertEquals(0.0, requestInfoB.submissionTime, 1e-3);
		assertEquals(2060.0, requestInfoB.pickupTime, 1e-3);
		assertEquals(2271.0, requestInfoB.dropoffTime, 1e-3);

		// assert both persons are part of same drt request
		assertEquals(requestInfoA.drtRequestId, requestInfoB.drtRequestId);
	}
}

