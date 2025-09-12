package org.matsim.simwrapper.dashboard;

import com.google.common.collect.Iterables;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.application.MATSimApplication;
import org.matsim.contrib.emissions.HbefaRoadTypeMapping;
import org.matsim.contrib.emissions.HbefaVehicleCategory;
import org.matsim.contrib.emissions.OsmHbefaMapping;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.NetworkUtils;
import org.matsim.counts.*;
import org.matsim.examples.ExamplesUtils;
import org.matsim.simwrapper.Dashboard;
import org.matsim.simwrapper.SimWrapper;
import org.matsim.simwrapper.SimWrapperConfigGroup;
import org.matsim.simwrapper.TestScenario;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.EngineInformation;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.SplittableRandom;

public class ScenarioComparisonDashboardTest {

	private static final String HBEFA_2020_PATH = "https://svn.vsp.tu-berlin.de/repos/public-svn/3507bb3997e5657ab9da76dbedbb13c9b5991d3e/0e73947443d68f95202b71a156b337f7f71604ae/";
	private static final String HBEFA_FILE_COLD_DETAILED = HBEFA_2020_PATH + "82t7b02rc0rji2kmsahfwp933u2rfjlkhfpi2u9r20.enc";
	private static final String HBEFA_FILE_WARM_DETAILED = HBEFA_2020_PATH + "944637571c833ddcf1d0dfcccb59838509f397e6.enc";
	private static final String HBEFA_FILE_COLD_AVERAGE = HBEFA_2020_PATH + "r9230ru2n209r30u2fn0c9rn20n2rujkhkjhoewt84202.enc";
	private static final String HBEFA_FILE_WARM_AVERAGE = HBEFA_2020_PATH + "7eff8f308633df1b8ac4d06d05180dd0c5fdf577.enc";

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void generate() {
		Assumptions.assumeTrue(System.getenv("MATSIM_DECRYPTION_PASSWORD") != null);

		Config config = TestScenario.loadConfig(utils);

		generateDummyCounts(config);

		SimWrapperConfigGroup simWrapperConfigGroup = ConfigUtils.addOrGetModule(config, SimWrapperConfigGroup.class);
		simWrapperConfigGroup.setSampleSize(0.01);

		SimWrapperConfigGroup.ContextParams contextParams = simWrapperConfigGroup.defaultParams();
		contextParams.setMapCenter("12,48.95");
		contextParams.setMapZoomLevel(9.0);

		EmissionsConfigGroup emissionsConfig = ConfigUtils.addOrGetModule(config, EmissionsConfigGroup.class);

		emissionsConfig.setAverageColdEmissionFactorsFile(HBEFA_FILE_COLD_AVERAGE);
		emissionsConfig.setDetailedColdEmissionFactorsFile(HBEFA_FILE_COLD_DETAILED);
		emissionsConfig.setAverageWarmEmissionFactorsFile(HBEFA_FILE_WARM_AVERAGE);
		emissionsConfig.setDetailedWarmEmissionFactorsFile(HBEFA_FILE_WARM_DETAILED);
		emissionsConfig.setHbefaTableConsistencyCheckingLevel(EmissionsConfigGroup.HbefaTableConsistencyCheckingLevel.consistent);

		emissionsConfig.setDetailedVsAverageLookupBehavior(EmissionsConfigGroup.DetailedVsAverageLookupBehavior.tryDetailedThenTechnologyAverageThenAverageTable);


		SimWrapper sw = SimWrapper.create(config)
			.addDashboard(new EmissionsDashboard(config.global().getCoordinateSystem()))
			.addDashboard(new ImpactAnalysisDashboard(Set.of("car")));

		// Set base path to output folder of base scenario that you wish to compare a policy with. Example:
		sw.getConfigGroup().setBasePath("/home/brendan/git/matsim-libs/contribs/simwrapper/test/output/org/matsim/simwrapper/dashboard/EmissionsDashboardTest/generate");

		Controler controler = MATSimApplication.prepare(new TestScenario(sw), config);

		Scenario scenario = controler.getScenario();

		for (Link link : scenario.getNetwork().getLinks().values()) {
			link.setFreespeed(30);
		}

		prepareVehicleTypes(scenario);
		prepareHbefaNetwork(scenario);

		controler.addOverridingModule(new CountsModule());
		controler.run();

		Assertions.assertThat(Path.of(utils.getOutputDirectory(), "analysis", "emissions"))
			.isDirectoryContaining("glob:**emissions_total.csv");


		Assertions.assertThat(Path.of(utils.getOutputDirectory(), "analysis", "population"))
			.isDirectoryContaining("glob:**trip_stats.csv");

	}

	public void generateDummyCounts(Config config) {

		SplittableRandom random = new SplittableRandom(1234);

		URL context = ExamplesUtils.getTestScenarioURL("kelheim");
		Network network = NetworkUtils.readNetwork(context + config.network().getInputFile());

		List<? extends Link> links = List.copyOf(network.getLinks().values());
		int size = links.size();

		Counts<Link> counts = new Counts<>();

		for (int i = 0; i <= 100; i++) {
			Link link = links.get(random.nextInt(size));

			if (counts.getMeasureLocations().containsKey(link.getId()))
				continue;

			MeasurementLocation<Link> station = counts.createAndAddMeasureLocation(link.getId(), link.getId().toString() + "_count_station");

			Measurable carVolume = station.createVolume(TransportMode.car);
			Measurable freightVolume = station.createVolume(TransportMode.truck);

			for (int hour = 0; hour < 24; hour++) {
				carVolume.setAtHour(hour, random.nextInt(500));
				freightVolume.setAtHour(hour, random.nextInt(100));
			}
		}

		try {
			Files.createDirectories(Path.of(utils.getPackageInputDirectory()));
			String absolutPath = Path.of(utils.getPackageInputDirectory()).normalize().toAbsolutePath() + "/dummy_counts.xml";

			config.counts().setInputFile(absolutPath);
			new CountsWriter(counts).write(absolutPath);
		} catch (IOException e) {
			e.printStackTrace();
		}
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

