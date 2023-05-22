package org.matsim.simwrapper.dashboard;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.application.MATSimApplication;
import org.matsim.contrib.emissions.HbefaVehicleCategory;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.counts.CountsModule;
import org.matsim.simwrapper.SimWrapper;
import org.matsim.simwrapper.TestScenario;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.EngineInformation;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

import java.util.List;

import static org.junit.Assert.*;

public class EmissionsDashboardTest {

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();


	@Test
	public void generate() {

		Config config = TestScenario.loadConfig(utils);

		EmissionsConfigGroup emissionsConfig = ConfigUtils.addOrGetModule(config, EmissionsConfigGroup.class);

		emissionsConfig.setAverageColdEmissionFactorsFile("/Users/friedrichvoelkers/Downloads/EFA_ColdStart_Vehcat_2020_Average.csv");
		emissionsConfig.setAverageWarmEmissionFactorsFile("/Users/friedrichvoelkers/Downloads/EFA_HOT_Vehcat_2020_Average.csv");
		emissionsConfig.setDetailedVsAverageLookupBehavior(EmissionsConfigGroup.DetailedVsAverageLookupBehavior.directlyTryAverageTable);


		SimWrapper sw = SimWrapper.create()
				.addDashboard(new EmissionsDashboard());

		Controler controler = MATSimApplication.prepare(new TestScenario(sw), config);

		Scenario scenario = controler.getScenario();

		prepareVehicleTypes(scenario);

		controler.addOverridingModule(new CountsModule());
		controler.run();

	}

	/**
	 * we set all vehicles to average except for KEXI vehicles, i.e. drt. Drt vehicles are set to electric light commercial vehicles.
	 * @param scenario scenario object for which to prepare vehicle types
	 */
	private void prepareVehicleTypes(Scenario scenario) {
		for (VehicleType type : scenario.getVehicles().getVehicleTypes().values()) {
			EngineInformation engineInformation = type.getEngineInformation();
			VehicleUtils.setHbefaTechnology(engineInformation, "average");
			VehicleUtils.setHbefaSizeClass(engineInformation, "average");
			if (scenario.getTransitVehicles().getVehicleTypes().containsKey(type.getId())) {
				// consider transit vehicles as non-hbefa vehicles, i.e. ignore them
				VehicleUtils.setHbefaVehicleCategory( engineInformation, HbefaVehicleCategory.NON_HBEFA_VEHICLE.toString());
			} else if (type.getId().toString().equals("car")){
				VehicleUtils.setHbefaVehicleCategory(engineInformation, HbefaVehicleCategory.PASSENGER_CAR.toString());
				VehicleUtils.setHbefaEmissionsConcept(engineInformation, "average");
			} else if (type.getId().toString().equals("conventional_vehicle") || type.getId().toString().equals("autonomous_vehicle")){
				VehicleUtils.setHbefaVehicleCategory(engineInformation, HbefaVehicleCategory.LIGHT_COMMERCIAL_VEHICLE.toString());
				VehicleUtils.setHbefaEmissionsConcept(engineInformation, "electricity");
			} else if (type.getId().toString().equals("freight")){
				VehicleUtils.setHbefaVehicleCategory(engineInformation, HbefaVehicleCategory.HEAVY_GOODS_VEHICLE.toString());
				VehicleUtils.setHbefaEmissionsConcept(engineInformation, "average");
			}
		}
	}
}
