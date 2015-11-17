package playground.boescpa.baseline.preparation;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.FacilitiesWriter;
import org.matsim.facilities.MatsimFacilitiesReader;
import playground.boescpa.baseline.ConfigCreator;
import playground.boescpa.baseline.F2LCreator;

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
    protected static final String FACILITIES = File.separator + ConfigCreator.FACILITIES;
    protected static final String HOUSEHOLD_ATTRIBUTES = File.separator + ConfigCreator.HOUSEHOLD_ATTRIBUTES;
    protected static final String HOUSEHOLDS = File.separator + ConfigCreator.HOUSEHOLDS;
    protected static final String POPULATION = File.separator + ConfigCreator.POPULATION;
    protected static final String POPULATION_ATTRIBUTES = File.separator + ConfigCreator.POPULATION_ATTRIBUTES;
    // RESOURCES
    protected static final String NETWORK = File.separator + ConfigCreator.NETWORK;
    protected static final String SCHEDULE = File.separator + ConfigCreator.SCHEDULE;
    protected static final String VEHICLES = File.separator + ConfigCreator.VEHICLES;
    private static final String NETWORK_ONLYCAR = File.separator + "network_onlyCar.xml.gz";
    private static final String SECONDARY_FACILITIES = File.separator + "SecondaryFacilitiesInclBorder.xml.gz";
    // OTHER
    private static final String CONFIG = File.separator + "defaultIVTConfig.xml";
    private static final String FACILITIES2LINKS = File.separator + ConfigCreator.FACILITIES2LINKS;

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
    private static String pathConfig;

    public static void main(final String[] args) {
        pathScenario = args[0];
        pathResources = args[1];
        int prctScenario = Integer.parseInt(args[2]);

        backupFolder = pathScenario + BACKUP_FOLDER;
        tempFolder = pathScenario + TEMP_FOLDER;

        try {
            if (saveOriginalFiles() && testScenario()) {
                mergeFacilities();
                addRemainingLocationChoiceActivities();
                createF2L();
                repairActivityChains();
                setInitialFacilitiesForAllActivities();
                createPrefsForPopulation();
                createDefaultIVTConfig(prctScenario);
                // to finish the process copy all files together to the final scenario
                createNewScenario();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                Files.deleteIfExists(Paths.get(tempFolder));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    private static void createNewScenario() throws IOException {
        log.info(" ------- Create New Scenario ------- ");
        Files.move(Paths.get(pathConfig), Paths.get(pathScenario + CONFIG));
        Files.move(Paths.get(pathFacilities), Paths.get(pathScenario + FACILITIES));
        Files.move(Paths.get(pathHouseholds), Paths.get(pathScenario + HOUSEHOLDS));
        Files.move(Paths.get(pathHouseholdAttributes), Paths.get(pathScenario + HOUSEHOLD_ATTRIBUTES));
        Files.move(Paths.get(pathPopulation), Paths.get(pathScenario + POPULATION));
        Files.move(Paths.get(pathPopulationAttributes), Paths.get(pathScenario + POPULATION_ATTRIBUTES));

        Files.copy(Paths.get(pathResources + NETWORK), Paths.get(pathScenario + NETWORK));
        Files.copy(Paths.get(pathResources + SCHEDULE), Paths.get(pathScenario + SCHEDULE));
        Files.copy(Paths.get(pathResources + VEHICLES), Paths.get(pathScenario + VEHICLES));
    }

    private static void createDefaultIVTConfig(int prctScenario) {
        log.info(" ------- Create Default IVT Config ------- ");
        pathConfig = tempFolder + CONFIG;
        final String[] args = {
                pathConfig,
                Integer.toString(prctScenario)
        };
        PrefsCreator.main(args);
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
        replaceInFile(pathFacilities, "shopping", "shop");
        replaceInFile(pathPopulation, "shopping", "shop");
        replaceInFile(pathFacilities, "primary_work", "work");
        replaceInFile(pathPopulation, "primary_work", "work");
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
        MatsimNetworkReader networkReader = new MatsimNetworkReader(scenario);
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
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        MatsimFacilitiesReader facilitiesReader = new MatsimFacilitiesReader(scenario);
        facilitiesReader.readFile(pathFacilities);
        ActivityFacilities scenarioFacilities = scenario.getActivityFacilities();
        // load secondary facilities:
        scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        facilitiesReader = new MatsimFacilitiesReader(scenario);
        facilitiesReader.readFile(pathResources + SECONDARY_FACILITIES);
        ActivityFacilities secondaryFacilities = scenario.getActivityFacilities();
        // unify facilities:
        ActivityFacilities mergedFacilities =
                new FacilityUnifier().uniteFacilitiesByMergingBintoA(scenarioFacilities, secondaryFacilities, null);
        FacilitiesWriter facilitiesWriter = new FacilitiesWriter(mergedFacilities);
        facilitiesWriter.write(pathFacilities);
    }

    private static boolean testScenario() {
        log.info(" ------- Test Scenario Files ------- ");
        try {
            Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
            MatsimFacilitiesReader facilitiesReader = new MatsimFacilitiesReader(scenario);
            facilitiesReader.readFile(pathFacilities);
            MatsimPopulationReader plansReader = new MatsimPopulationReader(scenario);
            plansReader.readFile(pathPopulation);
            return true;
        } catch (Exception e) {
            log.fatal("Test of scenario input files failed.");
            e.printStackTrace();
            return false;
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

            Files.createDirectory(Paths.get(tempFolder));
            pathFacilities = tempFolder + FACILITIES;
            Files.copy(Paths.get(backupFolder + FACILITIES), Paths.get(pathFacilities));
            pathHouseholds = tempFolder + HOUSEHOLDS;
            Files.move(Paths.get(backupFolder + HOUSEHOLDS), Paths.get(pathHouseholds));
            pathHouseholdAttributes = tempFolder + HOUSEHOLD_ATTRIBUTES;
            Files.copy(Paths.get(backupFolder + HOUSEHOLD_ATTRIBUTES), Paths.get(pathHouseholdAttributes));
            pathPopulation = tempFolder + POPULATION;
            Files.copy(Paths.get(backupFolder + POPULATION), Paths.get(pathPopulation));
            pathPopulationAttributes = tempFolder + POPULATION_ATTRIBUTES;
            Files.move(Paths.get(backupFolder + POPULATION_ATTRIBUTES), Paths.get(pathPopulationAttributes));

            return true;
        } catch (NoSuchFileException e) {
            e.printStackTrace();
            return false;
        }
    }

}
