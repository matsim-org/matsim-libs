package playground.vsp.drt.accessibilityOrientedDrt.run;

import com.google.common.base.Preconditions;
import com.google.inject.Singleton;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.application.MATSimAppCommand;
import org.matsim.contrib.drt.analysis.afterSimAnalysis.DrtVehicleStoppingTaskWriter;
import org.matsim.contrib.drt.extension.preplanned.optimizer.WaitForStopTask;
import org.matsim.contrib.drt.optimizer.constraints.ConstraintSetChooser;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.DrtControlerCreator;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.dvrp.passenger.PassengerRequestValidator;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeQSimModule;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.PopulationUtils;
import picocli.CommandLine;
import playground.vsp.drt.accessibilityOrientedDrt.analysis.ServiceAnalysis;
import playground.vsp.drt.accessibilityOrientedDrt.optimizer.HeterogeneousRequestValidator;
import playground.vsp.drt.accessibilityOrientedDrt.optimizer.PersonAttributeBasedConstraintSelector;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static playground.vsp.drt.accessibilityOrientedDrt.alternativeMode.AlternativeModeTripData.*;
import static playground.vsp.drt.accessibilityOrientedDrt.optimizer.PassengerAttribute.*;

public class RunHeterogeneousDrt implements MATSimAppCommand {
	@CommandLine.Option(names = "--config", description = "path to config file", required = true)
	private String configPath;

	@CommandLine.Option(names = "--output", description = "output root directory", required = true)
	private String outputRootDirectory;

	@CommandLine.Option(names = "--fleet-sizing", description = "a triplet: [from max interval]. ", arity = "1..*", defaultValue = "300 600 10")
	private List<Integer> fleetSizing;

	@CommandLine.Option(names = "--alternative-data", description = "path to alternative mode data", required = true)
	private Path alternativeDataPath;

	@CommandLine.Option(names = "--max-alpha", description = "max travel time alpha", defaultValue = "1.5")
	private double maxTravelTimeAlpha;

	@CommandLine.Option(names = "--max-beta", description = "max travel time beta", defaultValue = "900")
	private double maxTravelTimeBeta;

	@CommandLine.Option(names = "--max-wait-time", description = "max travel time beta", defaultValue = "600")
	private double maxWaitingTime;

	private static final Logger log = LogManager.getLogger(RunHeterogeneousDrt.class);

	private final Map<String, String> personAttributeMap = new LinkedHashMap<>();

	// Person ID (String) → travel time of the alternative mode
	private final Map<String, Boolean> alternativeModeIsAGoodOption = new HashMap<>();

	public static void main(String[] args) {
		new RunHeterogeneousDrt().execute(args);
	}

	@Override
	public Integer call() throws Exception {
		// Decoding fleet sizing sequence
		Preconditions.checkArgument(fleetSizing.size() == 3);
		int fleetFrom = fleetSizing.get(0);
		int fleetMax = fleetSizing.get(1);
		int fleetInterval = fleetSizing.get(2);

		Config configForGettingData = ConfigUtils.loadConfig(configPath, new MultiModeDrtConfigGroup(), new DvrpConfigGroup());

		// Reading person attributes
		Population inputPlans = PopulationUtils.readPopulation(Path.of(configPath).getParent().toString() + "/" + configForGettingData.plans().getInputFile());
		for (Person person : inputPlans.getPersons().values()) {
			personAttributeMap.put(person.getId().toString(), person.getAttributes().getAttribute(ATTRIBUTE_NAME).toString());
		}

		// Read alternative mode data
		log.info("Reading alternative mode data...");
		try (CSVParser parser = new CSVParser(Files.newBufferedReader(alternativeDataPath),
			CSVFormat.TDF.withFirstRecordAsHeader())) {
			for (CSVRecord record : parser.getRecords()) {
				String personId = record.get("id");
				double alternativeTravelTime = Double.parseDouble(record.get(ACTUAL_TOTAL_TRAVEL_TIME));
				double directTravelTime = Double.parseDouble(record.get(DIRECT_CAR_TRAVEL_TIME));
				double maxAllowedTravelTime = directTravelTime * maxTravelTimeAlpha + maxTravelTimeBeta;
				alternativeModeIsAGoodOption.put(personId, alternativeTravelTime <= maxAllowedTravelTime);
			}
		}

		for (int fleetSize = fleetFrom; fleetSize <= fleetMax; fleetSize += fleetInterval) {
			String fleetSizeFolder = outputRootDirectory + "/" + fleetSize + "-veh";


			Config config = ConfigUtils.loadConfig(configPath, new MultiModeDrtConfigGroup(), new DvrpConfigGroup());
			config.controller().setOutputDirectory(fleetSizeFolder);

			// Currently we only focus on single DRT mode
			DrtConfigGroup drtConfigGroup = DrtConfigGroup.getSingleModeDrtConfig(config);
			drtConfigGroup.setVehiclesFile("./vehicles/" + fleetSize + "-8_seater-drt-vehicles.xml");
			PersonAttributeBasedConstraintSelector.prepareDrtConstraint(drtConfigGroup,
				maxTravelTimeAlpha, maxTravelTimeBeta, maxWaitingTime,
				1.2, 450, 300);

			Controler controler = DrtControlerCreator.createControler(config, false);

			// Adding the New Request Validator //TODO
			controler.addOverridingQSimModule(new AbstractDvrpModeQSimModule(drtConfigGroup.getMode()) {
				@Override
				protected void configureQSim() {
					bindModal(PassengerRequestValidator.class).toProvider(
						modalProvider(getter -> new HeterogeneousRequestValidator(personAttributeMap, alternativeModeIsAGoodOption))).asEagerSingleton();
				}
			});
			// Adding person attribute-based constraint selector
			controler.addOverridingModule(new AbstractDvrpModeModule(drtConfigGroup.getMode()) {
				@Override
				public void install() {
					bindModal(ConstraintSetChooser.class).toProvider(
						() -> new PersonAttributeBasedConstraintSelector(drtConfigGroup)).in(Singleton.class);
				}
			});

			controler.run();

			// Plot DRT stopping tasks
			new DrtVehicleStoppingTaskWriter(Path.of(fleetSizeFolder)).addingCustomizedTaskToAnalyze(WaitForStopTask.TYPE).run(WaitForStopTask.TYPE);

			// perform analysis
			// analyze system travel behavior


			// check if fleet size is adequate, and stop simulation when fleet size is adequate
			boolean fleetSizeIsAdequate = ServiceAnalysis.isServiceSatisfactory(fleetSizeFolder, maxWaitingTime);
			if (fleetSizeIsAdequate) {
				break;
			}
		}
		return 0;
	}

}
