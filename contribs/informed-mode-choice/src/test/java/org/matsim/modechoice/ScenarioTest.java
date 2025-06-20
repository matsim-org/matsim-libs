package org.matsim.modechoice;

import com.google.inject.Injector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.application.MATSimApplication;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.PersonVehicles;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

public class ScenarioTest {

	protected InformedModeChoiceConfigGroup group;
	protected Controler controler;
	protected Injector injector;

	@RegisterExtension
	public MatsimTestUtils utils = new MatsimTestUtils();

	@BeforeEach
	public void setUp() throws Exception {

		Config config = TestScenario.loadConfig(utils);

		group = ConfigUtils.addOrGetModule(config, InformedModeChoiceConfigGroup.class);

		prepareConfig(config);

		controler = MATSimApplication.prepare(TestScenario.class, config, getArgs());


		// We need to add a vehicle, it however does not affect the results
		// Vehicles are needed due to the NetworkRoutingInclAccessEgressModule
		Id<VehicleType> typeId = Id.create(1, VehicleType.class);
		controler.getScenario().getVehicles().addVehicleType(VehicleUtils.createVehicleType(typeId));
		controler.getScenario().getVehicles().addVehicle(VehicleUtils.createVehicle(Id.createVehicleId(1), controler.getScenario().getVehicles().getVehicleTypes().get(typeId)));

		PersonVehicles vehicles = new PersonVehicles();
		vehicles.addModeVehicle(TransportMode.car, Id.createVehicleId(1));
		vehicles.addModeVehicle(TransportMode.ride, Id.createVehicleId(1));
		vehicles.addModeVehicle(TransportMode.walk, Id.createVehicleId(1));
		vehicles.addModeVehicle("freight", Id.createVehicleId(1));
		for (Person p : controler.getScenario().getPopulation().getPersons().values()){
			VehicleUtils.insertVehicleIdsIntoPersonAttributes(p, vehicles.getModeVehicles());
		}

		injector = controler.getInjector();

	}

	protected void prepareConfig(Config config) {
	}

	protected String[] getArgs() {
		return new String[0];
	}

}
