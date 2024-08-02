package org.matsim.application.prepare.freight.tripGeneration;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.*;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.LanduseOptions;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;
import picocli.CommandLine;

import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@CommandLine.Command(
        name = "generate-freight-plans",
        description = "Generate german wide freight population",
        showDefaultValues = true
)
public class GenerateFreightPlans implements MATSimAppCommand {
    private static final Logger log = LogManager.getLogger(GenerateFreightPlans.class);

    @CommandLine.Option(names = "--data", description = "Path to raw data (ketten 2010)",
            defaultValue = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/german-wide-freight/raw-data/ketten-2010.csv")
    private String dataPath;

    @CommandLine.Option(names = "--network", description = "Path to desired network file",
            defaultValue = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/german-wide-freight/v2/germany-europe-network.xml.gz")
    private String networkPath;

    @CommandLine.Option(names = "--nuts", description = "Path to NUTS file (available on SVN: )", required = true)
    // TODO Change this to URL pointing to SVN--> need to update the Location calculator
    private Path shpPath;

    @CommandLine.Option(names = "--output", description = "Output folder path", required = true)
    private Path output;

    @CommandLine.Option(names = "--truck-load", defaultValue = "13.0", description = "Average load of truck")
    private double averageTruckLoad;

    @CommandLine.Option(names = "--working-days", defaultValue = "260", description = "Number of working days in a year")
    private int workingDays;

    @CommandLine.Option(names = "--sample", defaultValue = "100", description = "Sample size of the freight plans (0, 100]")
    private double pct;

    @CommandLine.Mixin
    private LanduseOptions landuse = new LanduseOptions();

    @Override
    public Integer call() throws Exception {
        Network network = NetworkUtils.readNetwork(networkPath);
        log.info("Network successfully loaded!");

        log.info("preparing freight agent generator...");
        FreightAgentGenerator freightAgentGenerator = new FreightAgentGenerator(network, shpPath, landuse, averageTruckLoad, workingDays, pct / 100);
        log.info("Freight agent generator successfully created!");

        log.info("Reading trip relations...");
        List<TripRelation> tripRelations = TripRelation.readTripRelations(dataPath);
        log.info("Trip relations successfully loaded. There are " + tripRelations.size() + " trip relations");

        log.info("Start generating population...");
        Population outputPopulation = PopulationUtils.createPopulation(ConfigUtils.createConfig());
        for (int i = 0; i < tripRelations.size(); i++) {
            List<Person> persons = freightAgentGenerator.generateFreightAgents(tripRelations.get(i), Integer.toString(i));
            for (Person person : persons) {
                outputPopulation.addPerson(person);
            }

            if (i % 500000 == 0) {
                log.info("Processing: " + i + " out of " + tripRelations.size() + " entries have been processed");
            }
        }

        if (!Files.exists(output)) {
            Files.createDirectory(output);
        }

        String outputPlansPath = output.toString() + "/german_freight." + pct + "pct.plans.xml.gz";
        PopulationWriter populationWriter = new PopulationWriter(outputPopulation);
        populationWriter.write(outputPlansPath);

        // Write down tsv file for visualisation and analysis
        String freightTripTsvPath = output.toString() + "/freight_trips_data.tsv";
        CSVPrinter tsvWriter = new CSVPrinter(new FileWriter(freightTripTsvPath), CSVFormat.TDF);
        tsvWriter.printRecord("trip_id", "from_x", "from_y", "to_x", "to_y");
        for (Person person : outputPopulation.getPersons().values()) {
            List<PlanElement> planElements = person.getSelectedPlan().getPlanElements();
            Activity act0 = (Activity) planElements.get(0);
            Activity act1 = (Activity) planElements.get(2);
            Coord fromCoord = act0.getCoord();
            Coord toCoord = act1.getCoord();
            tsvWriter.printRecord(person.getId().toString(), fromCoord.getX(), fromCoord.getY(), toCoord.getX(), toCoord.getY());
        }
        tsvWriter.close();

        return 0;
    }

    public static void main(String[] args) {
        new GenerateFreightPlans().execute(args);
    }
}
