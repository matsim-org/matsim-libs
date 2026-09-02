package org.matsim.simwrapper.dashboard;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.application.ApplicationUtils;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.ShpOptions;
import org.matsim.contrib.emissions.HbefaRoadTypeMapping;
import org.matsim.contrib.emissions.HbefaVehicleCategory;
import org.matsim.contrib.emissions.OsmHbefaMapping;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.simwrapper.Dashboard;
import org.matsim.simwrapper.SimWrapper;
import org.matsim.simwrapper.SimWrapperConfigGroup;
import org.matsim.vehicles.MatsimVehicleWriter;
import picocli.CommandLine;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.matsim.contrib.emissions.HbefaVehicleCategory.PASSENGER_CAR;
import static org.matsim.simwrapper.dashboard.MobilityToGridScenariosUtils.addEngineInformationToVehicleTypes;

@CommandLine.Command(name = "air-pollution-hbefa-types", description = "Run AirPollutionAnalysis with different combinations of HBEFA vehicle types.")
public class RunAirpollutionAnalysisWithDifferentHbefaVehicleTypes implements MATSimAppCommand {
	private static final Logger log = LogManager.getLogger(RunAirpollutionAnalysisWithDifferentHbefaVehicleTypes.class);

	private static final String BEFORE = "_before_emissions.xml";
	private static final String AFTER = "_after_emissions.xml";
	private static final String XML = ".xml";

	@CommandLine.Parameters(arity = "1..*", description = "Path to run output directories for which air pollution analysis should be run.")
	private List<Path> inputPaths;
	@CommandLine.Option(names = "--private-car-technology", description = "Set HBEFA 4.1 technology aka fuel type for car vehicle type.")
	MobilityToGridScenariosUtils.Hbefa41Technology carTechnology = MobilityToGridScenariosUtils.Hbefa41Technology.PETROL_4S;
	@CommandLine.Option(names = "--private-car-vehicle-category", description = "Set HBEFA 4.1 vehicle category aka fuel type for car vehicle type." +
		"Can be used to switch off emission calc for car by using NON_HBEFA_VEHICLE.")
	HbefaVehicleCategory carVehicleCategory = PASSENGER_CAR;
	@CommandLine.Mixin
	private final ShpOptions shp = new ShpOptions();
	@CommandLine.Option(names = "--commercial-hbefa-vehicle-categories", split = ",", description = "hbefa vehicle categories for commercial vehicles.")
	Map<String, HbefaVehicleCategory> commercialVehicleCategories = new HashMap<>();
	@CommandLine.Option(names = "--context", description = "Set dashboard/analysis context to avoid overwriting analysis files.")
	private String context = "emissions";

	public static void main(String[] args) {
		new RunAirpollutionAnalysisWithDifferentHbefaVehicleTypes().execute(args);
	}

	@Override
	public Integer call() throws Exception {
//		for the m2g combined scenarios we only need to run 100% BEV / 100% H2 and then have to interpolate between that and the required
//		we need:
//		1) base case: 10% BEV, 90% petrol
//		2) multimodal mass 90% BEV, 5% petrol, 5% H2
//		3) motorized hedonism 60% BEV, 1% petrol, 20% synthetic, 19% H2
//		4) stagnation 10% BEV, 90% petrol

		for (Path runDirectory : inputPaths) {
			log.info("Running on {}", runDirectory);

			String configPath = ApplicationUtils.matchInput("config.xml", runDirectory).toString();
			String networkPath = ApplicationUtils.matchInput("output_network.xml.gz", runDirectory).toString();
			String vehiclesPath = ApplicationUtils.matchInput("output_vehicles.xml.gz", runDirectory).toString();
			String transitVehiclesPath = ApplicationUtils.matchInput("output_transitVehicles.xml.gz", runDirectory).toString();
			String populationPath = ApplicationUtils.matchInput("output_plans.xml.gz", runDirectory).toString();

			Path tmp = null;
			Path drtVehiclesPath = null;
			Path tmpDrtVehiclesPath = null;

			try {
				tmp = Path.of(runDirectory + "/emissions-tmp");
				if (!Files.exists(tmp)) {
					Files.createDirectory(tmp);
				}

				drtVehiclesPath = ApplicationUtils.matchInput("drt_vehicles.xml", runDirectory);
				tmpDrtVehiclesPath = Path.of(tmp + "/drt_vehicles.xml.gz");
				Files.move(drtVehiclesPath, tmpDrtVehiclesPath);
			} catch (IllegalArgumentException e) {
				log.warn("No file with pattern drt_vehicles.xml found. Catched IllegalArgumentException.");
			}

			//			original output files need to be overwritten as AirPollutionAnalysis searches for "config.xml".
//			We will copy the original output files back to their old file names later. very clunky, but I see no alternative, if we want to keep our run output consistent.
//			copy old files to separate files
			Path beforeEmissionsConfigPath = getUniqueTargetPath(Path.of(configPath.split(XML)[0] + BEFORE));
			Path beforeEmissionsNetworkPath = getUniqueTargetPath(Path.of(networkPath.split(XML)[0] + BEFORE + ".gz"));
			Path beforeEmissionsVehiclesPath = getUniqueTargetPath(Path.of(vehiclesPath.split(XML)[0] + BEFORE + ".gz"));
			Path beforeEmissionsTransitVehiclesPath = getUniqueTargetPath(Path.of(transitVehiclesPath.split(XML)[0] + BEFORE + ".gz"));
			Files.copy(Path.of(configPath), beforeEmissionsConfigPath);
			Files.copy(Path.of(networkPath), beforeEmissionsNetworkPath);
			Files.copy(Path.of(vehiclesPath), beforeEmissionsVehiclesPath);
			Files.copy(Path.of(transitVehiclesPath), beforeEmissionsTransitVehiclesPath);

			Config config = ConfigUtils.loadConfig(configPath);
			SimWrapper sw = SimWrapper.create(config);

			SimWrapperConfigGroup simwrapperCfg = ConfigUtils.addOrGetModule(config, SimWrapperConfigGroup.class);
			if (shp.isDefined()){
//				use different shape file than in simwrapper config if provided.
				simwrapperCfg.defaultParams().setShp(shp.getShapeFile());
			}
			//skip default dashboards (we only want to run AirPollutionAnalysis and create the corresponding dashboard).
			simwrapperCfg.setDefaultDashboards(SimWrapperConfigGroup.DefaultDashboardsMode.disabled);

			sw.addDashboard(Dashboard.customize(new EmissionsDashboard(config.global().getCoordinateSystem())).context(context));

//			configure emissions config group
			//	To decrypt hbefa input files set MATSIM_DECRYPTION_PASSWORD as environment variable. ask VSP for access.
			String HBEFA_2020_PATH = "https://svn.vsp.tu-berlin.de/repos/public-svn/3507bb3997e5657ab9da76dbedbb13c9b5991d3e/0e73947443d68f95202b71a156b337f7f71604ae/";
			String HBEFA_FILE_COLD_DETAILED = HBEFA_2020_PATH + "82t7b02rc0rji2kmsahfwp933u2rfjlkhfpi2u9r20.enc";
			String HBEFA_FILE_WARM_DETAILED = HBEFA_2020_PATH + "944637571c833ddcf1d0dfcccb59838509f397e6.enc";
			String HBEFA_FILE_COLD_AVERAGE = HBEFA_2020_PATH + "r9230ru2n209r30u2fn0c9rn20n2rujkhkjhoewt84202.enc" ;
			String HBEFA_FILE_WARM_AVERAGE = HBEFA_2020_PATH + "7eff8f308633df1b8ac4d06d05180dd0c5fdf577.enc";

			EmissionsConfigGroup eConfig = ConfigUtils.addOrGetModule(config, EmissionsConfigGroup.class);
			eConfig.setDetailedColdEmissionFactorsFile(HBEFA_FILE_COLD_DETAILED);
			eConfig.setDetailedWarmEmissionFactorsFile(HBEFA_FILE_WARM_DETAILED);
			eConfig.setAverageColdEmissionFactorsFile(HBEFA_FILE_COLD_AVERAGE);
			eConfig.setAverageWarmEmissionFactorsFile(HBEFA_FILE_WARM_AVERAGE);
			eConfig.setHbefaTableConsistencyCheckingLevel(EmissionsConfigGroup.HbefaTableConsistencyCheckingLevel.consistent);
			eConfig.setDetailedVsAverageLookupBehavior(EmissionsConfigGroup.DetailedVsAverageLookupBehavior.tryDetailedThenTechnologyAverageThenAverageTable);
			eConfig.setEmissionsComputationMethod(EmissionsConfigGroup.EmissionsComputationMethod.StopAndGoFraction);

			config.network().setInputFile(networkPath);
			config.vehicles().setVehiclesFile(vehiclesPath);
			config.transit().setVehiclesFile(transitVehiclesPath);
			config.plans().setInputFile(populationPath);

			Scenario scenario = ScenarioUtils.loadScenario(config);

			// add hbefa link attributes.
//			the link attributes should already be there as OpenBerlinScenario adds them, but in theory this class could also be run on some kind of initial network.
			HbefaRoadTypeMapping roadTypeMapping = OsmHbefaMapping.build();
			roadTypeMapping.addHbefaMappings(scenario.getNetwork());

			String carFuelType = getCarFuelType();

			addEngineInformationToVehicleTypes(scenario, carFuelType, carVehicleCategory, commercialVehicleCategories);

//			write outputs with adapted files.
//			now we can write the prepared output to the usual output file paths.
			ConfigUtils.writeConfig(config, configPath);
			NetworkUtils.writeNetwork(scenario.getNetwork(), networkPath);
			new MatsimVehicleWriter(scenario.getVehicles()).writeFile(vehiclesPath);
			new MatsimVehicleWriter(scenario.getTransitVehicles()).writeFile(transitVehiclesPath);

			try {
				sw.generate(runDirectory, true);
				sw.run(runDirectory);
			} catch (IOException e) {
				InterruptedIOException ex = new InterruptedIOException("Simwrapper did not finish correctly.");
				ex.initCause(e);
				throw ex;
			}

//			after finishing the analysis we can
//			1) copy the transformed files to paths with _after_emissions suffix
//			2) copy the original files from paths with suffix _before_emissions to original paths
//			3) delete the files with _before_emissions suffix.
			Path afterEmissionsConfigPath = getUniqueTargetPath(Path.of(configPath.split(XML)[0] + AFTER));
			Path afterEmissionsNetworkPath = getUniqueTargetPath(Path.of(networkPath.split(XML)[0] + AFTER + ".gz"));
			Path afterEmissionsVehiclesPath = getUniqueTargetPath(Path.of(vehiclesPath.split(XML)[0] + AFTER + ".gz"));
			Path afterEmissionsTransitVehiclesPath = getUniqueTargetPath(Path.of(transitVehiclesPath.split(XML)[0] + AFTER + ".gz"));
			Files.copy(Path.of(configPath), afterEmissionsConfigPath);
			Files.copy(Path.of(networkPath), afterEmissionsNetworkPath);
			Files.copy(Path.of(vehiclesPath), afterEmissionsVehiclesPath);
			Files.copy(Path.of(transitVehiclesPath), afterEmissionsTransitVehiclesPath);
			Files.copy(beforeEmissionsConfigPath, Path.of(configPath), StandardCopyOption.REPLACE_EXISTING);
			Files.copy(beforeEmissionsNetworkPath, Path.of(networkPath), StandardCopyOption.REPLACE_EXISTING);
			Files.copy(beforeEmissionsVehiclesPath, Path.of(vehiclesPath), StandardCopyOption.REPLACE_EXISTING);
			Files.copy(beforeEmissionsTransitVehiclesPath, Path.of(transitVehiclesPath), StandardCopyOption.REPLACE_EXISTING);
			Files.delete(beforeEmissionsConfigPath);
			Files.delete(beforeEmissionsNetworkPath);
			Files.delete(beforeEmissionsVehiclesPath);
			Files.delete(beforeEmissionsTransitVehiclesPath);

//			also move the drt vehicles file back to its original place if available
//			delete tmp dir
			if (tmp != null && drtVehiclesPath != null && tmpDrtVehiclesPath != null) {
				Files.move(tmpDrtVehiclesPath, drtVehiclesPath);
				Files.delete(tmp);
			}
		}

		return 0;
	}

	private String getCarFuelType() {
		String carFuelType;
		if (carTechnology == MobilityToGridScenariosUtils.Hbefa41Technology.PETROL_4S) {
			carFuelType = "petrol (4S)";
		} else if (carTechnology == MobilityToGridScenariosUtils.Hbefa41Technology.DIESEL) {
			carFuelType = "diesel";
		} else if (carTechnology == MobilityToGridScenariosUtils.Hbefa41Technology.ELECTRICITY) {
			carFuelType = "electricity";
		} else {
			log.error("Invalid HBEFA 4.1 emission concept: {}.", carTechnology);
			throw new IllegalStateException("");
		}
		return carFuelType;
	}

	private static Path getUniqueTargetPath(Path targetPath) {
		int counter = 1;
		Path uniquePath = targetPath;

		// Add a suffix if the file already exists
		while (Files.exists(uniquePath)) {
			String originalPath = targetPath.toString();
			int dotIndex = originalPath.lastIndexOf(".");
			if (dotIndex == -1) {
				uniquePath = Path.of(originalPath + "_" + counter);
			} else {
				uniquePath = Path.of(originalPath.substring(0, dotIndex) + "_" + counter + originalPath.substring(dotIndex));
			}
			counter++;
		}

		return uniquePath;
	}
}
