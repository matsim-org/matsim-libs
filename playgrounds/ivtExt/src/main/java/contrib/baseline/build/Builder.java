package contrib.baseline.build;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ProcessBuilder.Redirect;
import java.util.LinkedList;
import java.util.List;

import contrib.baseline.modification.EndTimeDiluter;
import contrib.baseline.modification.FreespeedAdjustment;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.io.FileUtils;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.ControlerDefaultsModule;
import org.matsim.core.controler.Injector;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.StrategyManagerModule;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.NetworkRoutingModule;
import org.matsim.core.router.PlanRouter;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripRouterModule;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutility;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;

import com.google.inject.Key;
import com.google.inject.name.Names;

import contrib.baseline.RunIVTBaseline;
import contrib.baseline.build.BuildConfig.Scaling;
import contrib.baseline.build.BuildConfig.Scenario;
import contrib.baseline.preparation.FacilityUnifier;
import contrib.baseline.preparation.PreparationScript;
import contrib.baseline.preparation.ZHCutter;
import contrib.baseline.preparation.ZHCutter.ZHCutterConfigGroup;
import contrib.baseline.preparation.crossborderCreation.CreateCBPop;
import contrib.baseline.preparation.crossborderCreation.CreateSingleTripPopulationConfigGroup;
import contrib.baseline.preparation.freightCreation.CreateFreightTraffic;

public class Builder {
	private BuildConfig config;
	private boolean svnAvailable = false;
	private boolean md5Available = false;
	
	private File basePopulationDirectory;
	private File scenarioDirectory;
	private File resourcesDirectory;
	private File zurichDirectory;
	
	public Builder(BuildConfig config) throws InterruptedException, IOException {
		this.config = config;
		preparationChecks();
		fixPaths();
	}
	
	public void run() throws IOException, InterruptedException {
		// Validate or fetch scenario from SVN
		
		if (hasConfigChanged()) { // config has changed, so reset!
			System.out.println("Resetting working copy");
			FileUtils.cleanDirectory(new File(config.getWorkingDirectory()));
		}
		
		copyConfig();
		
		if (!validateCheckoutFromSVN()) {
			if (svnAvailable) {
				exportFromSVN();
				verifyStep(validateCheckoutFromSVN());
			} else {
				throw new RuntimeException("No SVN checkout available, but cannot download because not SVN is installed");
			}
		}
		
		if (!validateUnifyFacilties()) {
			unifyFacilties();
			verifyStep(validateUnifyFacilties());
		}
		
		if (!validateCrossBorderPopulation()) {
			createCrossBorderPopulation();
			verifyStep(validateCrossBorderPopulation());
		}
		
		if (!validateFreightPopulation()) {
			createFreightPopulation();
			verifyStep(validateFreightPopulation());
		}
		
		if (!validateScenarioCreation("Switzerland", scenarioDirectory)) {
			createFinalScenario();
			verifyStep(validateScenarioCreation("Switzerland", scenarioDirectory));
		}
		
		File scenarioSource;
		
		if (config.getScenario() == Scenario.ZURICH) {
			if (!validateScenarioCreation("Zurich", zurichDirectory)) {
				cutZurichScenario();
				verifyStep(validateScenarioCreation("Zurich", zurichDirectory));
			}
			
			scenarioSource = zurichDirectory;
		} else {
			scenarioSource = scenarioDirectory;
		}
		
		if (!validateScenarioCreation("Output", new File(config.getOutputDirectory()))) {
			copyScenario(scenarioSource);
			verifyStep(validateScenarioCreation("Output", new File(config.getOutputDirectory())));
		}
		
		if (config.getHashScenario()) hashScenario();
	}
	
	private void verifyStep(boolean validationResult) {
		if (!validationResult) throw new RuntimeException();
	}
	
	private void fixPaths() {
		String outputDirectoryPath = config.getOutputDirectory();
		
		if (outputDirectoryPath == null) {
			throw new RuntimeException("Output directory must be set!");
		}
		
		File outputDirectory = new File(outputDirectoryPath);
		
		if (!outputDirectory.exists()) {
			outputDirectory.mkdirs();
		}
		
		String workingDirectoryPath = config.getWorkingDirectory();
		File workingDirectory;
		
		if (workingDirectoryPath == null) {
			workingDirectory = new File(outputDirectory, "_build");
		} else {
			workingDirectory = new File(workingDirectoryPath);
		}
		
		if (!workingDirectory.exists()) {
			workingDirectory.mkdirs();
		}
		
		String svnDirectoryPath = config.getSVNDirectory();
		File svnDirectory;
		
		if (svnDirectoryPath == null) {
			svnDirectory = new File(workingDirectory, "_svn");
			svnDirectory.mkdir();
		} else {
			svnDirectory = new File(svnDirectoryPath);
		}
		
		config.setOutputDirectory(outputDirectory.getAbsolutePath());
		config.setWorkingDirectory(workingDirectory.getAbsolutePath());
		config.setSVNDirectory(svnDirectory.getAbsolutePath());
		
		basePopulationDirectory = new File(svnDirectory, "baseline2010/demand/base_populations/" + makePopulationDirectoryName());
		
		scenarioDirectory = new File(workingDirectory, "_scenario");
		resourcesDirectory = new File(workingDirectory, "_resources");
		zurichDirectory = new File(workingDirectory, "_zurich");
		
		//scenarioDirectory.mkdir();
		//resourcesDirectory.mkdir();
	}
	
	private void preparationChecks() throws InterruptedException, IOException {
		ProcessBuilder processBuilder = new ProcessBuilder("svn", "--version");
		
		if (processBuilder.start().waitFor() == 0) {
			System.out.println("SVN is available.");
			svnAvailable = true;
		} else {
			System.out.println("SVN is NOT available.");
		}
		
		processBuilder = new ProcessBuilder("md5sum", "--version");
		
		if (processBuilder.start().waitFor() == 0) {
			System.out.println("MD5 is available.");
			md5Available = true;
		} else {
			System.out.println("MD5 is NOT available.");
		}
		
		if (config.getHashScenario() && !md5Available) {
			throw new IllegalArgumentException("No hashing possible because MD5 is not available");
		}
	}
	
	private String makePopulationDirectoryName() {
		int year;
		
		switch (config.getPopulation()) {
		case POPULATION_2015:
			year = 2015; break;
		case POPULATION_2030:
			year = 2030; break;
		default:
			throw new IllegalStateException();
		}
		
		int scaling;
		
		switch (config.getScaling()) {
		case SCALING_1:
			scaling = 1; break;
		case SCALING_10:
			scaling = 10; break;
		case SCALING_100:
			scaling = 100; break;
		default:
			throw new IllegalStateException();
		}
		
		return String.format("%dprct_switzerland%d_randomSeed1235_weekday", scaling, year);
	}
	
	private void hashScenario() throws InterruptedException, IOException {
		if (md5Available) {
			System.out.println("Hashing scenario ...");
			
			List<String> command = new LinkedList<>();
			command.add("md5sum");
			
			for (String file : final_files) {
				command.add(file);
			}
			
			ProcessBuilder processBuilder = new ProcessBuilder(command);
			processBuilder.directory(new File(config.getOutputDirectory()));
			processBuilder.redirectOutput(new File(config.getOutputDirectory(), "scenario.md5"));
			processBuilder.start().waitFor();
			
			System.out.println("Done.");
		}
	}
	
	private void cutZurichScenario() throws IOException {
		System.out.println("Cutting Zurich scenario ...");
		
		// Prepare config
		Config zhConfig = makeZurichCutterConfig();
		
		File zhConfigPath = new File(config.getWorkingDirectory(), "_zh_config.xml");
		FileUtils.deleteQuietly(zhConfigPath);
		
		new ConfigWriter(zhConfig).write(zhConfigPath.getAbsolutePath());
		
		FileUtils.deleteQuietly(zurichDirectory);
		zurichDirectory.mkdir();
		
		// Cut
		String[] args = new String[1];
		args[0] = zhConfigPath.getAbsolutePath();
		ZHCutter.main(args);
		System.gc();
		
		FileUtils.copyFile(new File(scenarioDirectory, "ptStationCounts.csv"), new File(zurichDirectory, "ptStationCounts.csv"));
		
		System.out.println("Done.");
	}
	
	private Config makeZurichCutterConfig() {
		Config zhConfig = ConfigUtils.createConfig(new ZHCutterConfigGroup(ZHCutterConfigGroup.GROUP_NAME));
		ZHCutterConfigGroup cg = (ZHCutterConfigGroup) zhConfig.getModule(ZHCutterConfigGroup.GROUP_NAME);
		
		cg.setCommuterTag("outAct");
		cg.setPathToInputScenarioFolder(scenarioDirectory.getAbsolutePath());
		cg.setPathToTargetFolder(zurichDirectory.getAbsolutePath());
		cg.setxCoordCenter(2683518.0);
		cg.setyCoordCenter(1246836.0);
		cg.setRadius(30000);
		
		return zhConfig;
	}
	
	final static String[] copy_results = new String[]{
		"cbPopulation.xml.gz",
		"cbPopulationAttributes.xml.gz",
		"cbFacilities.xml.gz",
		"freightPopulation.xml.gz",
		"freightPopulationAttributes.xml.gz",
		"freightFacilities.xml.gz"
	};
	
	final static String[] copy_demand = new String[]{
		"facilities.xml.gz",
		"household_attributes.xml.gz",
		"households.xml.gz",
		"population.xml.gz",
		"population_attributes.xml.gz"
	};
	
	static String[] final_files = new String[] {
		"defaultIVTConfig.xml",
		"facilities.xml.gz",
		"household_attributes.xml.gz",
		"households.xml.gz",
		"mmNetwork.xml.gz",
		"mmSchedule.xml.gz",
		"mmVehicles.xml.gz",
		"population.xml.gz",
		"population_attributes.xml.gz",
		"ptLinkCountsIdentified.csv",
		"ptStationCounts.csv"
	};
	
	private boolean validateCopyScenario() {
		System.out.println("Validating copy of final scenario.");
		
		boolean valid = true;
		
		for (String file : final_files) {
			valid &= new File(config.getOutputDirectory(), file).exists();
		}
		
		if (valid) {
			System.out.println("Final scenario has been copied.");
		} else {
			System.out.println("Final scenario has not been copied.");
		}
		
		return valid;
	}
	
	private void copyScenario(File source) throws IOException {
		System.out.println("Copying final scenario ...");
		
		for (String file : final_files) {
			FileUtils.copyFile(new File(source, file), new File(config.getOutputDirectory(), file));
		}
		
		System.out.println("Done.");
	}
	
	private boolean validateScenarioCreation(String name, File path) {
		System.out.println("Validating final " + name + " scenario ...");
		boolean valid = true;
		
		for (String file : final_files) {
			valid &= new File(path, file).exists();
		}
		
		if (valid) {
			System.out.println("Scenario " + name + " exists.");
		} else {
			System.out.println("Scenario " + name + " does not exist.");
		}
		
		return valid;
	}

	private void applyModifications() throws IOException {
        System.out.println("Diluting population end times ...");

        FileUtils.moveFile(
                new File(scenarioDirectory, "population.xml.gz"),
                new File(scenarioDirectory, "population_original.xml.gz")
        );

        new EndTimeDiluter().dilutePopulation(
                new File(scenarioDirectory, "population_original.xml.gz").getAbsolutePath(),
                new File(scenarioDirectory, "population.xml.gz").getAbsolutePath()
        );

        System.gc();

        FileUtils.deleteQuietly(new File(scenarioDirectory, "population_original.xml.gz"));

        System.out.println("Adjusting freespeeds ...");

        FileUtils.moveFile(
                new File(scenarioDirectory, "mmNetwork.xml.gz"),
                new File(scenarioDirectory, "mmNetwork_original.xml.gz")
        );

        new FreespeedAdjustment().adjustSpeeds(
                new File(scenarioDirectory, "mmNetwork_original.xml.gz").getAbsolutePath(),
                new File(scenarioDirectory, "mmNetwork.xml.gz").getAbsolutePath()
        );

        FileUtils.deleteQuietly(new File(scenarioDirectory, "mmNetwork_original.xml.gz"));
    }
	
	private void createFinalScenario() throws IOException {
		System.out.println("Creating final scenario ...");
		
		prepareScenarioAndResources();
		
		List<String> command = new LinkedList<>();
		command.add(scenarioDirectory.getAbsolutePath());
		command.add(resourcesDirectory.getAbsolutePath());

		switch (config.getScaling()) {
			case SCALING_1:
				command.add("1"); break;
			case SCALING_10:
				command.add("10"); break;
			case SCALING_100:
				command.add("100"); break;
			default:
				throw new IllegalStateException();
		}
		
		String[] arguments = new String[command.size()];
		PreparationScript.main(command.toArray(arguments));
		System.gc();

        applyModifications();

		System.out.println("Done creating final scenario.");
	}
	
	private void prepareScenarioAndResources() throws IOException {
		System.out.println("  Preparing scenario and resources ...");
		
		FileUtils.deleteQuietly(scenarioDirectory);
		FileUtils.deleteQuietly(resourcesDirectory);
		
		scenarioDirectory.mkdir();
		resourcesDirectory.mkdir();
		
		for (String name : copy_results) {
			FileUtils.copyFile(new File(config.getWorkingDirectory(), name), new File(scenarioDirectory, name));
		}
		
		for (String name : copy_demand) {
			FileUtils.copyFile(new File(basePopulationDirectory, name), new File(scenarioDirectory, name));
		}
		
		FileUtils.copyFile(
				new File(config.getSVNDirectory(), "baseline2010/supply/base_multimodal_supply/CH1903Plus_HAFAS2015/mmNetwork.xml.gz"), 
				new File(resourcesDirectory, "mmNetwork.xml.gz")); 
		
		FileUtils.copyFile(
				new File(config.getSVNDirectory(), "baseline2010/supply/base_multimodal_supply/CH1903Plus_HAFAS2015/mmSchedule.xml.gz"), 
				new File(resourcesDirectory, "mmSchedule.xml.gz")); 
		
		FileUtils.copyFile(
				new File(config.getSVNDirectory(), "baseline2010/supply/base_multimodal_supply/CH1903Plus_HAFAS2015/network_onlyCar.xml.gz"), 
				new File(resourcesDirectory, "network_onlyCar.xml.gz"));
		
		FileUtils.copyFile(
				new File(config.getSVNDirectory(), "baseline2010/supply/base_pt_counts/ptLinkCountsIdentified.csv"), 
				new File(scenarioDirectory, "ptLinkCountsIdentified.csv"));
		
		FileUtils.copyFile(
				new File(config.getSVNDirectory(), "baseline2010/supply/base_pt_counts/ptStationCounts.csv"), 
				new File(scenarioDirectory, "ptStationCounts.csv"));
		
		String vehicles;		
		switch (config.getScaling()) {
		case SCALING_1:
			vehicles = "mmVehicles_1Prct.xml.gz"; break;
		case SCALING_10:
			vehicles = "mmVehicles10Prct.xml.gz"; break;
		case SCALING_100:
			vehicles = "mmVehicles.xml.gz"; break;
		default:
			throw new IllegalStateException();
		}
		
		FileUtils.copyFile(
				new File(config.getSVNDirectory(), "baseline2010/supply/base_multimodal_supply/CH1903Plus_HAFAS2015/" + vehicles), 
				new File(resourcesDirectory, "mmVehicles.xml.gz"));
		
		FileUtils.copyFile(
				new File(config.getSVNDirectory(), "baseline2010/supply/base_secondary_activity_facilities/SecondaryFacilitiesInclBorder.xml.gz"), 
				new File(resourcesDirectory, "SecondaryFacilitiesInclBorder.xml.gz"));
		
		System.out.println("  Done.");
	}
	
	private boolean validateFreightPopulation() {
		System.out.println("Validating freight population ...");
		
		File facilities = new File(config.getWorkingDirectory(), "freightFacilities.xml.gz");
		File population = new File(config.getWorkingDirectory(), "freightPopulation.xml.gz");
		File populationAttributes = new File(config.getWorkingDirectory(), "freightPopulationAttributes.xml.gz");
		
		if (facilities.exists() && population.exists() && populationAttributes.exists()) {
			System.out.println("Freight population exists.");
			return true;
		} else {
			System.out.println("Freight population does not exist.");
			return false;
		}
	}
	
	private void createFreightPopulation() throws IOException {
		System.out.println("Creating freight population ...");
		
		File freight = new File(config.getSVNDirectory(), "baseline2010/demand/freight_populations");
		
		File tempFacilities = new File(config.getWorkingDirectory(), "_freightFacilities.xml.gz");
		File tempPopulation = new File(config.getWorkingDirectory(), "_freightPopulation.xml.gz");
		File tempPopulationAttributes = new File(config.getWorkingDirectory(), "_freightPopulation_Attributes.xml.gz");
		
		File facilities = new File(config.getWorkingDirectory(), "freightFacilities.xml.gz");
		File population = new File(config.getWorkingDirectory(), "freightPopulation.xml.gz");
		File populationAttributes = new File(config.getWorkingDirectory(), "freightPopulationAttributes.xml.gz");
		
		List<String> command = new LinkedList<>();
		command.add(new File(freight, "BezirkskoordinatenInklAusland.txt").getAbsolutePath());
		command.add(new File(config.getWorkingDirectory(), "full_facilities.xml.gz").getAbsolutePath());
		command.add(new File(freight, "Lieferwagen.txt").getAbsolutePath());
		command.add(new File(freight, "LKW.txt").getAbsolutePath());
		command.add(new File(freight, "Sattelschlepper.txt").getAbsolutePath());
		command.add(new File(freight, "CumulativeProbabilityFreightDeparture.txt").getAbsolutePath());
		
		// Freight scaling acc. to demand/freight_populations/_README_Skalierungen.txt
		double freightScaling = scalingToDouble(config.getScaling());
		
		switch (config.getPopulation()) {
		case POPULATION_2015:
			freightScaling *= 1.05; break;
		case POPULATION_2030:
			freightScaling *= 1.18; break;
		default:
			throw new IllegalStateException();		
		}
		command.add(String.valueOf(freightScaling));
		command.add("1235");
		
		command.add(tempFacilities.getAbsolutePath());
		command.add(tempPopulation.getAbsolutePath());
		
		String[] arguments = new String[command.size()];
		CreateFreightTraffic.main(command.toArray(arguments));
		System.gc();
		
		FileUtils.deleteQuietly(facilities);
		FileUtils.deleteQuietly(population);
		FileUtils.deleteQuietly(populationAttributes);
		
		FileUtils.moveFile(tempFacilities, facilities);
		FileUtils.moveFile(tempPopulation, population);
		FileUtils.moveFile(tempPopulationAttributes, populationAttributes);
		System.out.println("Created freight population.");
	}
	
	private boolean validateCrossBorderPopulation() {
		System.out.println("Validating cross border population");
		
		File population = new File(config.getWorkingDirectory(), "cbPopulation.xml.gz");
		File populationAttributes = new File(config.getWorkingDirectory(), "cbPopulationAttributes.xml.gz");
		File populationFacilities = new File(config.getWorkingDirectory(), "cbFacilities.xml.gz");
		
		if (population.exists() && populationAttributes.exists() && populationFacilities.exists()) {
			System.out.println("Cross border population is available.");
			return true;
		} else {
			System.out.println("Cross border population does not exist.");
			return false;
		}
	}
	
	private void createCrossBorderPopulation() throws IOException {
		System.out.println("Creating cross border population...");
		
		// Prepare config
		Config cbConfig = makeCrossBorderConfig();
		
		File cbConfigPath = new File(config.getWorkingDirectory(), "_cb_config.xml");
		FileUtils.deleteQuietly(cbConfigPath);
		
		new ConfigWriter(cbConfig).write(cbConfigPath.getAbsolutePath());
		
		// Create population
		String[] args = new String[1];
		args[0] = cbConfigPath.getAbsolutePath();
		CreateCBPop.main(args);
		System.gc();
		
		// Move files
		
		File temporaryPopulation = new File(config.getWorkingDirectory(), makeCrossBorderName("_tmp_cbPopulation") + ".xml.gz");
		File temporaryPopulationAttributes = new File(config.getWorkingDirectory(), makeCrossBorderName("_tmp_cbPopulation") + "_Attributes.xml.gz");
		File temporaryPopulationFacilities = new File(config.getWorkingDirectory(), makeCrossBorderName("_tmp_cbPopulation") + "_Facilities.xml.gz");
		
		File population = new File(config.getWorkingDirectory(), "cbPopulation.xml.gz");
		File populationAttributes = new File(config.getWorkingDirectory(), "cbPopulationAttributes.xml.gz");
		File populationFacilities = new File(config.getWorkingDirectory(), "cbFacilities.xml.gz");
		
		FileUtils.deleteQuietly(population);
		FileUtils.deleteQuietly(populationAttributes);
		FileUtils.deleteQuietly(populationFacilities);
		
		FileUtils.moveFile(temporaryPopulation, population);
		FileUtils.moveFile(temporaryPopulationAttributes, populationAttributes);
		FileUtils.moveFile(temporaryPopulationFacilities, populationFacilities);

		System.out.println("Cross border population created.");
	}
	
	private String makeCrossBorderName(String base) {
		double scaling = scalingToDouble(config.getScaling());
		return base + "_1235_" + scaling;
	}
	
	private double scalingToDouble(Scaling scaling) {
		switch (config.getScaling()) {
		case SCALING_1: return 0.01;
		case SCALING_10: return 0.1;
		case SCALING_100: return 1.0;
		default: throw new IllegalStateException();
		}
	}
	
	private long scalingToLong(Scaling scaling) {
		switch (config.getScaling()) {
		case SCALING_1: return 1;
		case SCALING_10: return 10;
		case SCALING_100: return 100;
		default: throw new IllegalStateException();
		}
	}
	
	private Config makeCrossBorderConfig() {
		Config cbConfig = ConfigUtils.createConfig(new CreateSingleTripPopulationConfigGroup());
		CreateSingleTripPopulationConfigGroup cg = (CreateSingleTripPopulationConfigGroup) cbConfig.getModule(CreateSingleTripPopulationConfigGroup.GROUP_NAME);
		
		File cbPopulations = new File(config.getSVNDirectory(), "baseline2010/demand/cb_populations");
		
		cg.setPathToFacilities(new File(config.getWorkingDirectory(), "full_facilities.xml.gz").getAbsolutePath());
		cg.setPathToOriginsFile(cbPopulations.getAbsolutePath() + "/");
		cg.setPathToDestinationsFile(cbPopulations.getAbsolutePath() + "/");
		cg.setPathToCumulativeDepartureProbabilities(cbPopulations.getAbsolutePath() + "/");
		cg.setPathToCumulativeDurationProbabilities(cbPopulations.getAbsolutePath() + "/");
		
		File target = new File(config.getWorkingDirectory(), "_tmp_cbPopulation.xml.gz");
		cg.setPathToOutput(target.getAbsolutePath());
		
		cg.setRandomSeed(1235);
		cg.setSamplePercentage(scalingToDouble(config.getScaling()));
		
		cg.setDelimiter(";");
		cg.setMode("car");
		
		return cbConfig;
	}
	
	private boolean validateUnifyFacilties() {
		System.out.println("Validating facility unification ...");
		
		File facilities = new File(config.getWorkingDirectory(), "full_facilities.xml.gz");
		if (facilities.exists()) {
			System.out.println("Facilties exist.");
			return true;
		}
		
		System.out.println("Facilities do not exist.");
		return false;
	}
	
	private void unifyFacilties() throws IOException {
		System.out.println("Unifying facilities ...");
		
		File primary = new File(basePopulationDirectory, "facilities.xml.gz");
		File secondary = new File(config.getSVNDirectory(), "baseline2010/supply/base_secondary_activity_facilities/SecondaryFacilitiesInclBorder.xml.gz");
		
		File temporaryTarget = new File(config.getWorkingDirectory(), "_tmp_full_facilities.xml.gz");
		File target = new File(config.getWorkingDirectory(), "full_facilities.xml.gz");
		
		if (!primary.exists()) {
			throw new IllegalStateException("Original population facilities not available");
		}
		
		if (!secondary.exists()) {
			throw new IllegalStateException("Secondary activitiy facilities not available");
		}
		
		FileUtils.deleteQuietly(temporaryTarget);
		FileUtils.deleteQuietly(target);
		
		List<String> command = new LinkedList<>();
		command.add(primary.getAbsolutePath());
		command.add(secondary.getAbsolutePath());
		command.add(temporaryTarget.getAbsolutePath());
		command.add("false");
		command.add("false");
		
		String[] arguments = new String[command.size()];
		FacilityUnifier.main(command.toArray(arguments));
		
		FileUtils.moveFile(temporaryTarget, target);
		System.out.println("Unifiying facilities done.");
	}

	private boolean validateCheckoutFromSVN() {
		System.out.println("Validating SVN export ...");
		File svnPath = new File(config.getSVNDirectory(), "baseline2010");
		
		if (!svnPath.exists()) {
			System.out.println("Export not available");
			return false;
		}
		
		if (!svnPath.isDirectory()) {
			System.out.println("Checkout not available");
			return false;
		}
		
		System.out.println("Export seems valid");
		return true;
	}
	
	private void exportFromSVN() throws IOException, InterruptedException {
		System.out.println("Exporting from SVN ...");
		System.out.println("(this may take a while)");
		
		File svnPath = new File(config.getSVNDirectory());
		FileUtils.deleteQuietly(new File(svnPath, "_tmp"));
		
		List<String> command = new LinkedList<>();
		command.add("svn");
		command.add("export");
		command.add(config.getRepository());
		command.add("_tmp");
		
		ProcessBuilder processBuilder = new ProcessBuilder(command);
		processBuilder.directory(svnPath);
		
		Process process = processBuilder.start();
		if (process.waitFor() != 0) {
			IOUtils.copy(process.getInputStream(), System.err);
			throw new IllegalStateException("SVN export failure");
		}
		
		FileUtils.moveDirectory(new File(svnPath, "_tmp"), new File(svnPath, "baseline2010"));
		
		System.out.println("Export successful!");
	}
	
	private void copyConfig() {
		File outputConfig = new File(config.getOutputDirectory(), "build.xml");
		BuildConfig.saveConfig(outputConfig.getAbsolutePath(), config);
	}
	
	private boolean hasConfigChanged() {
		System.out.println("Checking if configuration has changed ...");
		
		File outputConfigPath = new File(config.getOutputDirectory(), "build.xml");
		
		if (outputConfigPath.exists()) {
			BuildConfig outputConfig = BuildConfig.loadConfig(outputConfigPath.getAbsolutePath());
			
			if (!hashConfig(config).equals(hashConfig(outputConfig))) {
				System.out.println("Configuration has been changed!");
				return true;
			}
		}
		
		System.out.println("No changes, OK!");
		return false;
	}
	
	private String hashConfig(BuildConfig config) {
		StringBuilder builder = new StringBuilder();
		builder.append(config.getOutputDirectory());
		builder.append(config.getWorkingDirectory());
		builder.append(config.getSVNDirectory());
		builder.append(config.getPopulation().toString());
		builder.append(config.getScaling().toString());
		builder.append(config.getScenario().toString());
		return builder.toString();
	}
	
	public static void main(String[] argv) throws IOException, InterruptedException {
		if (argv.length < 1) {
			System.out.println("First argument must be configuration file.");
			System.exit(1);
		}
		
		BuildConfig config = BuildConfig.loadConfig(argv[0]);
		
		//config.setOutputDirectory("/home/sebastian/build_switzerland");
		//config.setScaling(Scaling.SCALING_1);
		//config.setScenario(Scenario.ZURICH);
		//config.setHashScenario(true);
		
		Builder builder = new Builder(config);
		builder.run();
	}
}
