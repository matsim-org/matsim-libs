package org.matsim.application.prepare.freight.tripGeneration;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.LanduseOptions;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;
import picocli.CommandLine;

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

    @CommandLine.Option(names = "--nuts", description = "Path to desired network file", required = true)
    // TODO Currently we have problem reading shp from URL... Shp needs to be downloaded to local disk.
    private Path shpPath;

    @CommandLine.Option(names = "--output", description = "Output path", required = true)
    private Path output;

    @CommandLine.Option(names = "--truck-load", defaultValue = "16.0", description = "Average load of truck")
    private double averageTruckLoad;

    @CommandLine.Option(names = "--working-days", defaultValue = "260", description = "Number of working days in a year")
    private int workingDays;

    @CommandLine.Option(names = "--sample", defaultValue = "1", description = "Scaling factor of the freight traffic (0, 1)")
    private double sample;

    @CommandLine.Mixin
    private LanduseOptions landuse = new LanduseOptions();

    @Override
    public Integer call() throws Exception {
        Network network = NetworkUtils.readNetwork(networkPath);
        log.info("Network successfully loaded!");

        log.info("preparing freight agent generator...");
        FreightAgentGenerator freightAgentGenerator = new FreightAgentGenerator(network, shpPath, landuse, averageTruckLoad, workingDays, sample);
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

        PopulationWriter populationWriter = new PopulationWriter(outputPopulation);
        populationWriter.write(output.toString());
        return 0;
    }

    public static void main(String[] args) {
        new GenerateFreightPlans().execute(args);
    }
}
