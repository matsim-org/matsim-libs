package org.matsim.simwrapper.dashboard;

import com.google.common.collect.Iterables;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.application.MATSimApplication;
import org.matsim.contrib.emissions.HbefaRoadTypeMapping;
import org.matsim.contrib.emissions.HbefaVehicleCategory;
import org.matsim.contrib.emissions.OsmHbefaMapping;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.NetworkUtils;
import org.matsim.simwrapper.SimWrapper;
import org.matsim.simwrapper.TestScenario;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.EngineInformation;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

import java.nio.file.Path;

public class EmissionsDashboardTest {

	private static final String HBEFA_2020_PATH = "https://svn.vsp.tu-berlin.de/repos/public-svn/3507bb3997e5657ab9da76dbedbb13c9b5991d3e/0e73947443d68f95202b71a156b337f7f71604ae/";
	private static final String HBEFA_FILE_COLD_DETAILED = HBEFA_2020_PATH + "82t7b02rc0rji2kmsahfwp933u2rfjlkhfpi2u9r20.enc";
	private static final String HBEFA_FILE_WARM_DETAILED = HBEFA_2020_PATH + "944637571c833ddcf1d0dfcccb59838509f397e6.enc";
	private static final String HBEFA_FILE_COLD_AVERAGE = HBEFA_2020_PATH + "r9230ru2n209r30u2fn0c9rn20n2rujkhkjhoewt84202.enc";
	private static final String HBEFA_FILE_WARM_AVERAGE = HBEFA_2020_PATH + "7eff8f308633df1b8ac4d06d05180dd0c5fdf577.enc";

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void generate() {

		// This test can only run if the password is set
		Assumptions.assumeTrue(System.getenv("MATSIM_DECRYPTION_PASSWORD") != null);

		Path out = Path.of(utils.getOutputDirectory(), "analysis", "emissions");

		Config config = TestScenario.loadConfig(utils);

		EmissionsConfigGroup emissionsConfig = ConfigUtils.addOrGetModule(config, EmissionsConfigGroup.class);

		emissionsConfig.setAverageColdEmissionFactorsFile(HBEFA_FILE_COLD_AVERAGE);
		emissionsConfig.setDetailedColdEmissionFactorsFile(HBEFA_FILE_COLD_DETAILED);
		emissionsConfig.setAverageWarmEmissionFactorsFile(HBEFA_FILE_WARM_AVERAGE);
		emissionsConfig.setDetailedWarmEmissionFactorsFile(HBEFA_FILE_WARM_DETAILED);
		emissionsConfig.setHbefaTableConsistencyCheckingLevel(EmissionsConfigGroup.HbefaTableConsistencyCheckingLevel.consistent);

		emissionsConfig.setDetailedVsAverageLookupBehavior(EmissionsConfigGroup.DetailedVsAverageLookupBehavior.tryDetailedThenTechnologyAverageThenAverageTable);

		SimWrapper sw = SimWrapper.create()
			.addDashboard(new EmissionsDashboard());

		Controler controler = MATSimApplication.prepare(new TestScenario(sw), config);

		Scenario scenario = controler.getScenario();

		prepareVehicleTypes(scenario);
		prepareHbefaNetwork(scenario);

		controler.run();

		Assertions.assertThat(out)
			.isDirectoryContaining("glob:**emissions_total.csv")
			.isDirectoryContaining("glob:**emissions_grid_per_day.xyt.csv");

	}

	/**
	 * we set all vehicles to average except for KEXI vehicles, i.e. drt. Drt vehicles are set to electric light commercial vehicles.
	 *
	 * @param scenario scenario object for which to prepare vehicle types
	 */
	private void prepareVehicleTypes(Scenario scenario) {

		Iterable<VehicleType> allTypes = Iterables.concat(scenario.getVehicles().getVehicleTypes().values(), scenario.getTransitVehicles().getVehicleTypes().values());

		for (VehicleType type : allTypes) {
			EngineInformation engineInformation = type.getEngineInformation();
			VehicleUtils.setHbefaTechnology(engineInformation, "average");
			VehicleUtils.setHbefaSizeClass(engineInformation, "average");
			if (scenario.getTransitVehicles().getVehicleTypes().containsKey(type.getId())) {
				// consider transit vehicles as non-hbefa vehicles, i.e. ignore them
				VehicleUtils.setHbefaVehicleCategory(engineInformation, HbefaVehicleCategory.NON_HBEFA_VEHICLE.toString());
			} else if (type.getId().toString().equals("car")) {
				VehicleUtils.setHbefaVehicleCategory(engineInformation, HbefaVehicleCategory.PASSENGER_CAR.toString());
				VehicleUtils.setHbefaEmissionsConcept(engineInformation, "average");
			} else if (type.getId().toString().equals("conventional_vehicle") || type.getId().toString().equals("autonomous_vehicle")) {
				VehicleUtils.setHbefaVehicleCategory(engineInformation, HbefaVehicleCategory.LIGHT_COMMERCIAL_VEHICLE.toString());
				VehicleUtils.setHbefaEmissionsConcept(engineInformation, "electricity");
			} else if (type.getId().toString().equals("freight")) {
				VehicleUtils.setHbefaVehicleCategory(engineInformation, HbefaVehicleCategory.NON_HBEFA_VEHICLE.toString());
				VehicleUtils.setHbefaEmissionsConcept(engineInformation, "average");
			}
		}
	}

	/**
	 * changes/adds link attributes of the network in the given scenario.
	 *
	 * @param scenario for which to prepare the network
	 */
	private void prepareHbefaNetwork(Scenario scenario) {
		HbefaRoadTypeMapping roadTypeMapping = OsmHbefaMapping.build();
//		the type attribute in our network has the prefix "highway" for all links but pt links. we need to delete that because OsmHbefaMapping does not handle that.
		for (Link link : scenario.getNetwork().getLinks().values()) {
			//pt links can be disregarded
			if (!link.getAllowedModes().contains("pt")) {
				NetworkUtils.setType(link, NetworkUtils.getType(link).replaceFirst("highway\\.", ""));
			}
		}
		roadTypeMapping.addHbefaMappings(scenario.getNetwork());
	}
}
