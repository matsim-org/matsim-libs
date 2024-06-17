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

	@CommandLine.Option(names = "--lookupTable", description = "Path to desired lookupTable (csv-file)",
			defaultValue = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/german-wide-freight/v2/processed-data/complete-lookup-table.csv")
	private String lookupTablePath;

	@CommandLine.Option(names = "--nuts", description = "Path to desired network file", required = true)
    // TODO Change this to URL pointing to SVN--> need to update the Location calculator
    private Path shpPath;

    @CommandLine.Option(names = "--output", description = "Output folder path", required = true)
    private Path output;

    @CommandLine.Option(names = "--truck-load", defaultValue = "13.0", description = "Average load of truck")
    private double averageTruckLoad;

    @CommandLine.Option(names = "--working-days", defaultValue = "260", description = "Number of working days in a year")
    private int workingDays;

    @CommandLine.Option(names = "--sample", defaultValue = "1", description = "Scaling factor of the freight traffic (0, 1)")
    private double sample;

    @CommandLine.Mixin
    private LanduseOptions landuse = new LanduseOptions();

	/**
	 * Generates a freight-agent population. Following args must be set for this method to work: <br>
	 * {@code --data} <i>(optional)</i> - Path to raw data (ketten 2010) <br>
	 * {@code --network} <i>(optional)</i> - Path to desired network file <br>
	 * {@code --lookupTable} <i>(optional)</i> - Path to desired lookupTable <br>
	 * {@code --nuts} - Path to desired NUTS shapefile <br>
	 * {@code --output} - Output folder path <br>
	 * {@code --truck-load} <i>(optional)</i> - Average load of truck (default=13.0) <br>
	 * {@code --working-days} <i>(optional)</i> - Number of working days in a year (default=260) <br>
	 * {@code --sample} <i>(optional)</i> - Scaling factor of the freight traffic from 0.0 to 1.0 (default=1.0) <br>
	 * @return 0 if succeeded, writes output as .tsv-file containing the coordinates of the origin and destination
	 * @throws Exception
	 */
    @Override
    public Integer call() throws Exception {
        Network network = NetworkUtils.readNetwork(networkPath);
        log.info("Network successfully loaded!");

        log.info("preparing freight agent generator...");
        FreightAgentGenerator freightAgentGenerator = new FreightAgentGenerator(network, shpPath, lookupTablePath, landuse, averageTruckLoad, workingDays, sample);
        log.info("Freight agent generator successfully created!");

        log.info("Reading trip relations...");
        List<TripRelation> tripRelations = TripRelation.readTripRelations(dataPath);
        log.info("Trip relations successfully loaded. There are " + tripRelations.size() + " trip relations");

        log.info("Start generating population...");
        Population outputPopulation = PopulationUtils.createPopulation(ConfigUtils.createConfig());
        for (int i = 0; i < tripRelations.size(); i++) {
            List<Person> persons = freightAgentGenerator.generateRoadFreightAgents(tripRelations.get(i), Integer.toString(i));
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

        String outputPlansPath = output.toString() + "/german_freight.25pct.plans.xml.gz";
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
