package playground.boescpa.ivtBaseline.preparation;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.facilities.*;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;
import playground.boescpa.ivtBaseline.preparation.secondaryFacilityCreation.CreationOfCrossBorderFacilities;
import playground.boescpa.lib.tools.FacilityUtils;
import playground.boescpa.lib.tools.fileCreation.F2LCreator;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Script to create a runnable scenario from basic scenario files.
 *
 * @author boescpa
 */
public class PreparationScript {

    private static final String BACKUP_FOLDER = File.separator + "origScenario";
    private static final String TEMP_FOLDER = File.separator + "temp";

    // SCENARIO
    protected static final String FACILITIES = File.separator + IVTConfigCreator.FACILITIES;
    protected static final String HOUSEHOLD_ATTRIBUTES = File.separator + IVTConfigCreator.HOUSEHOLD_ATTRIBUTES;
    protected static final String HOUSEHOLDS = File.separator + IVTConfigCreator.HOUSEHOLDS;
    protected static final String POPULATION = File.separator + IVTConfigCreator.POPULATION;
    protected static final String POPULATION_ATTRIBUTES = File.separator + IVTConfigCreator.POPULATION_ATTRIBUTES;
	protected static final String FREIGHT_FACILITIES = File.separator + "freightFacilities.xml.gz";
	protected static final String FREIGHT_POPULATION = File.separator + "freightPopulation.xml.gz";
	protected static final String FREIGHT_POPULATION_ATTRIBUTES = File.separator + "freightPopulationAttributes.xml.gz";
	protected static final String CB_FACILITIES = File.separator + "cbFacilities.xml.gz";
	protected static final String CB_POPULATION = File.separator + "cbPopulation.xml.gz";
	protected static final String CB_POPULATION_ATTRIBUTES = File.separator + "cbPopulationAttributes.xml.gz";
    // RESOURCES
    protected static final String NETWORK = File.separator + IVTConfigCreator.NETWORK;
    protected static final String SCHEDULE = File.separator + IVTConfigCreator.SCHEDULE;
    protected static final String VEHICLES = File.separator + IVTConfigCreator.VEHICLES;
    private static final String NETWORK_ONLYCAR = File.separator + "network_onlyCar.xml.gz";
    private static final String SECONDARY_FACILITIES = File.separator + "SecondaryFacilitiesInclBorder.xml.gz";
    // OTHER
    protected static final String CONFIG = File.separator + "defaultIVTConfig.xml";
	private static final String LC_CONFIG = File.separator + "lcIVTConfig.xml";
    private static final String FACILITIES2LINKS = File.separator + IVTConfigCreator.FACILITIES2LINKS;

    private final static Logger log = Logger.getLogger(PreparationScript.class);

    private static String pathScenario;
    private static String pathResources;
    private static String backupFolder;
    private static String tempFolder;

    private static String pathFacilities;
    private static String pathPopulation;
    private static String pathHouseholdAttributes;
    private static String pathPopulationAttributes;
    private static String pathHouseholds;
	private static String pathFreightFacilities;
	private static String pathFreightPopulation;
	private static String pathFreightPopulationAttributes;
	private static String pathCBFacilities;
	private static String pathCBPopulation;
	private static String pathCBPopulationAttributes;
    private static String pathConfig;
    private static String pathLCConfig;

    public static void main(final String[] args) {
        pathScenario = args[0];
        pathResources = args[1];
        int prctScenario = Integer.parseInt(args[2]); // for example for a 1%-scenario enter here '1'

        backupFolder = pathScenario + BACKUP_FOLDER;
        tempFolder = pathScenario + TEMP_FOLDER;

        try {
            if (saveOriginalFiles() && testScenario(false)) {
                mergeFacilities();
                addRemainingLocationChoiceActivities();
                createF2L();
                repairActivityChains();
                setInitialFacilitiesForAllActivities();
                createPrefsForPopulation();
				mergeInSubpopulations();
                createDefaultIVTConfig(prctScenario);
				//createIVTLCConfig(prctScenario);
				testScenario(true);
                // to finish the process copy all files together to the final scenario
                createNewScenario();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
				Files.delete(Paths.get(pathFreightFacilities));
				Files.delete(Paths.get(pathFreightPopulation));
				Files.delete(Paths.get(pathFreightPopulationAttributes));
				Files.delete(Paths.get(pathCBFacilities));
				Files.delete(Paths.get(pathCBPopulation));
				Files.delete(Paths.get(pathCBPopulationAttributes));
				Files.deleteIfExists(Paths.get(tempFolder));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

	private static void createNewScenario() throws IOException {
        log.info(" ------- Create New Scenario ------- ");
        Files.move(Paths.get(pathConfig), Paths.get(pathScenario + CONFIG));
        Files.move(Paths.get(pathLCConfig), Paths.get(pathScenario + LC_CONFIG));
        Files.move(Paths.get(pathFacilities), Paths.get(pathScenario + FACILITIES));
        Files.move(Paths.get(pathHouseholds), Paths.get(pathScenario + HOUSEHOLDS));
        Files.move(Paths.get(pathHouseholdAttributes), Paths.get(pathScenario + HOUSEHOLD_ATTRIBUTES));
        Files.move(Paths.get(pathPopulation), Paths.get(pathScenario + POPULATION));
        Files.move(Paths.get(pathPopulationAttributes), Paths.get(pathScenario + POPULATION_ATTRIBUTES));

		Files.copy(Paths.get(pathResources + NETWORK), Paths.get(pathScenario + NETWORK));
        Files.copy(Paths.get(pathResources + SCHEDULE), Paths.get(pathScenario + SCHEDULE));
        Files.copy(Paths.get(pathResources + VEHICLES), Paths.get(pathScenario + VEHICLES));
    }

	/*private static void createIVTLCConfig(int prctScenario) {
		log.info(" ------- Create LC IVT Config ------- ");
		pathLCConfig = tempFolder + LC_CONFIG;
		final String[] args = {
				pathLCConfig,
				Integer.toString(prctScenario)
		};
		ChooseSecondaryFacilitiesConfigCreator.main(args);
	}*/

    private static void createDefaultIVTConfig(int prctScenario) {
        log.info(" ------- Create Default IVT Config ------- ");
        pathConfig = tempFolder + CONFIG;
        final String[] args = {
                pathConfig,
                Integer.toString(prctScenario)
        };
        IVTConfigCreator.main(args);
    }


	private static void mergeInSubpopulations() {
		log.info(" ------- Merge in the Sub-Populations ------- ");
		// read the scenario population
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		MatsimPopulationReader plansReader = new MatsimPopulationReader(scenario);
		plansReader.readFile(pathPopulation);
		Population scenarioPopulation = scenario.getPopulation();
		ObjectAttributesXmlReader attributesReader = new ObjectAttributesXmlReader(scenarioPopulation.getPersonAttributes());
		attributesReader.parse(pathPopulationAttributes);
		// add tag for main population
		/*for (Person p : scenarioPopulation.getPersons().values()) {
			scenarioPopulation.getPersonAttributes().putAttribute(p.getId().toString(), "subpopulation", "main_pop");
		}*/
		// add the freight population to the scenario population
		plansReader.readFile(pathFreightPopulation);
		attributesReader.parse(pathFreightPopulationAttributes);
		// add the cb population to the scenario population
		plansReader.readFile(pathCBPopulation);
		attributesReader.parse(pathCBPopulationAttributes);
		// write the new, merged population and its attributes:
		PopulationWriter writer = new PopulationWriter(scenarioPopulation);
		writer.write(pathPopulation);
		ObjectAttributesXmlWriter attributesWriter = new ObjectAttributesXmlWriter(scenarioPopulation.getPersonAttributes());
		attributesWriter.writeFile(pathPopulationAttributes);
	}

    private static void createPrefsForPopulation() {
        log.info(" ------- Create Prefs for Population ------- ");
        final String[] args = {
                pathPopulation,
                pathPopulationAttributes,
                pathPopulationAttributes
        };
        PrefsCreator.main(args);
    }

    private static void setInitialFacilitiesForAllActivities() {
        log.info(" ------- Set Initial Facilities for all Activities ------- ");
        final String[] args = {
                pathPopulation,
                pathFacilities,
                pathPopulation
        };
        FacilityAdder.main(args);
    }

    private static void repairActivityChains() throws IOException {
        renameActivitiesToIVTStandard();
        completeUnfinishedActivityChains();
    }

    private static void completeUnfinishedActivityChains() {
        log.info(" ------- Complete Unfinished Activity Chains ------- ");
        final String[] args = {
                pathPopulation,
                pathPopulation
        };
        ActivityChainRepairer.main(args);
    }

    private static void renameActivitiesToIVTStandard() throws IOException {
        log.info(" ------- Rename Activities to IVT Standard ------- ");
        replaceInFile(pathFacilities, "shopping", IVTConfigCreator.SHOP);
        replaceInFile(pathPopulation, "shopping", IVTConfigCreator.SHOP);
        replaceInFile(pathFacilities, "primary_work", IVTConfigCreator.WORK);
        replaceInFile(pathPopulation, "primary_work", IVTConfigCreator.WORK);
    }

    private static void replaceInFile(String pathFile, String stringToReplace, String replacingString) throws IOException {
        int numberOfReplacements = 0;
        BufferedReader reader = IOUtils.getBufferedReader(pathFile);
        List<String> content = new ArrayList<>();
        String line = reader.readLine();
        while (line != null) {
            if (line.contains(stringToReplace)) numberOfReplacements++;
            line = line.replaceAll(stringToReplace, replacingString);
            content.add(line);
            line = reader.readLine();
        }
        BufferedWriter writer = IOUtils.getBufferedWriter(pathFile);
        for (String writeLine : content) {
            writer.write(writeLine);
            writer.newLine();
        }
        writer.flush();
        writer.close();
        log.info("In file " + pathFile + " replaced " + stringToReplace + " by " + replacingString + " for " + numberOfReplacements);
    }

    private static void createF2L() {
        log.info(" ------- Create F2L ------- ");
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        MatsimFacilitiesReader facilitiesReader = new MatsimFacilitiesReader(scenario);
        facilitiesReader.readFile(pathFacilities);
        MatsimNetworkReader networkReader = new MatsimNetworkReader(scenario.getNetwork());
        networkReader.readFile(pathResources + NETWORK_ONLYCAR);
        F2LCreator.createF2L(scenario, pathScenario + FACILITIES2LINKS);
    }

    private static void addRemainingLocationChoiceActivities() {
        log.info(" ------- Add Location Choice Activities to Facilities ------- ");
        final String[] args = {
                pathFacilities,
                pathFacilities
        };
        ActivityAdder.main(args);
    }

    private static void mergeFacilities() {
        log.info(" ------- Merge Facilities ------- ");
        // load scenario facilities:
		ActivityFacilities scenarioFacilities = FacilityUtils.readFacilities(pathFacilities);
        // load secondary facilities:
        ActivityFacilities secondaryFacilities = FacilityUtils.readFacilities(pathResources + SECONDARY_FACILITIES);
		// load freight facilities:
		ActivityFacilities freightFacilities = FacilityUtils.readFacilities(pathFreightFacilities);
		// add cb-facilities:
		ActivityFacilities cbFacilities = FacilityUtils.readFacilities(pathCBFacilities);

        // unify facilities:
        ActivityFacilities partiallyMergedFacilities =
                new FacilityUnifier().uniteFacilitiesByMergingBintoA(scenarioFacilities, secondaryFacilities, null);
		ActivityFacilities fullyMergedFacilities =
				new FacilityUnifier().uniteFacilitiesByMergingBintoA(partiallyMergedFacilities, freightFacilities, null);
		// add cb-facilities:
		for (ActivityFacility facility : cbFacilities.getFacilities().values()) {
			if (facility.getId().toString().contains(CreationOfCrossBorderFacilities.BC_TAG)) {
				ActivityOption homeOption = facility.getActivityOptions().get("cbHome");
				homeOption.getOpeningTimes().clear();
				fullyMergedFacilities.getFacilities().get(facility.getId()).addActivityOption(homeOption);
			} else {
				fullyMergedFacilities.addActivityFacility(facility);
			}
		}

		// write unified facilities
		FacilitiesWriter facilitiesWriter = new FacilitiesWriter(fullyMergedFacilities);
        facilitiesWriter.write(pathFacilities);
    }

    private static boolean testScenario(boolean executeFinalScenarioTests) {
        log.info(" ------- Test Scenario Files ------- ");
        try {
            Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
            MatsimFacilitiesReader facilitiesReader = new MatsimFacilitiesReader(scenario);
            facilitiesReader.readFile(pathFacilities);
            MatsimPopulationReader plansReader = new MatsimPopulationReader(scenario);
            plansReader.readFile(pathPopulation);
			if (executeFinalScenarioTests) {
				testFacilityAssignment(scenario);
			}
            return true;
        } catch (Exception e) {
            log.fatal("Test of scenario input files failed.");
            e.printStackTrace();
            return false;
        }
    }

	private static void testFacilityAssignment(Scenario scenario) {
		boolean continueTest = true;
		for (Person p : scenario.getPopulation().getPersons().values()) {
			for (PlanElement planElement : p.getSelectedPlan().getPlanElements()) {
				if (planElement instanceof ActivityImpl) {
					ActivityImpl act = (ActivityImpl) planElement;
					if (act.getFacilityId() != null) {
						ActivityFacility activityFacility = scenario.getActivityFacilities().getFacilities().get(act.getFacilityId());
						if (!activityFacility.getActivityOptions().keySet().contains(act.getType())) {
							log.error("Assigned facility without appropriate activity type found. \n Agent Id: " + p.getId().toString()
									+ ", act: " + act.toString());
							continueTest = false;
						}
					} else {
						log.error("Activity without assigned facility found. Agent Id: " + p.getId().toString());
						continueTest = false;
					}
				}
				if (!continueTest) break;
			}
			if (!continueTest) break;
		}
	}

	private static boolean saveOriginalFiles() throws IOException {
        log.info(" ------- Save Original Files ------- ");
        try {
            Files.createDirectory(Paths.get(backupFolder));
            Files.move(Paths.get(pathScenario + FACILITIES), Paths.get(backupFolder + FACILITIES));
            Files.move(Paths.get(pathScenario + HOUSEHOLDS), Paths.get(backupFolder + HOUSEHOLDS));
            Files.move(Paths.get(pathScenario + HOUSEHOLD_ATTRIBUTES), Paths.get(backupFolder + HOUSEHOLD_ATTRIBUTES));
            Files.move(Paths.get(pathScenario + POPULATION), Paths.get(backupFolder + POPULATION));
            Files.move(Paths.get(pathScenario + POPULATION_ATTRIBUTES), Paths.get(backupFolder + POPULATION_ATTRIBUTES));
			Files.move(Paths.get(pathScenario + FREIGHT_FACILITIES), Paths.get(backupFolder + FREIGHT_FACILITIES));
			Files.move(Paths.get(pathScenario + FREIGHT_POPULATION), Paths.get(backupFolder + FREIGHT_POPULATION));
			Files.move(Paths.get(pathScenario + FREIGHT_POPULATION_ATTRIBUTES), Paths.get(backupFolder + FREIGHT_POPULATION_ATTRIBUTES));
			Files.move(Paths.get(pathScenario + CB_FACILITIES), Paths.get(backupFolder + CB_FACILITIES));
			Files.move(Paths.get(pathScenario + CB_POPULATION), Paths.get(backupFolder + CB_POPULATION));
			Files.move(Paths.get(pathScenario + CB_POPULATION_ATTRIBUTES), Paths.get(backupFolder + CB_POPULATION_ATTRIBUTES));

            Files.createDirectory(Paths.get(tempFolder));
            pathFacilities = tempFolder + FACILITIES;
            Files.copy(Paths.get(backupFolder + FACILITIES), Paths.get(pathFacilities));
            pathHouseholds = tempFolder + HOUSEHOLDS;
            Files.copy(Paths.get(backupFolder + HOUSEHOLDS), Paths.get(pathHouseholds));
            pathHouseholdAttributes = tempFolder + HOUSEHOLD_ATTRIBUTES;
            Files.copy(Paths.get(backupFolder + HOUSEHOLD_ATTRIBUTES), Paths.get(pathHouseholdAttributes));
            pathPopulation = tempFolder + POPULATION;
            Files.copy(Paths.get(backupFolder + POPULATION), Paths.get(pathPopulation));
            pathPopulationAttributes = tempFolder + POPULATION_ATTRIBUTES;
            Files.copy(Paths.get(backupFolder + POPULATION_ATTRIBUTES), Paths.get(pathPopulationAttributes));
			pathFreightFacilities = tempFolder + FREIGHT_FACILITIES;
			Files.copy(Paths.get(backupFolder + FREIGHT_FACILITIES), Paths.get(pathFreightFacilities));
			pathFreightPopulation = tempFolder + FREIGHT_POPULATION;
			Files.copy(Paths.get(backupFolder + FREIGHT_POPULATION), Paths.get(pathFreightPopulation));
			pathFreightPopulationAttributes = tempFolder + FREIGHT_POPULATION_ATTRIBUTES;
			Files.copy(Paths.get(backupFolder + FREIGHT_POPULATION_ATTRIBUTES), Paths.get(pathFreightPopulationAttributes));
			pathCBFacilities = tempFolder + CB_FACILITIES;
			Files.copy(Paths.get(backupFolder + CB_FACILITIES), Paths.get(pathCBFacilities));
			pathCBPopulation = tempFolder + CB_POPULATION;
			Files.copy(Paths.get(backupFolder + CB_POPULATION), Paths.get(pathCBPopulation));
			pathCBPopulationAttributes = tempFolder + CB_POPULATION_ATTRIBUTES;
			Files.copy(Paths.get(backupFolder + CB_POPULATION_ATTRIBUTES), Paths.get(pathCBPopulationAttributes));

            return true;
        } catch (NoSuchFileException e) {
            e.printStackTrace();
            return false;
        }
    }

}
