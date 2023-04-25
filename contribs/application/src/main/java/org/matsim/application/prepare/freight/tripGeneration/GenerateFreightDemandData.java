package org.matsim.application.prepare.freight.tripGeneration;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.*;
import org.matsim.application.MATSimAppCommand;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationUtils;
import picocli.CommandLine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@CommandLine.Command(
        name = "generate-freight-data",
        description = "Generate german wide freight data",
        showDefaultValues = true
)
public class GenerateFreightDemandData implements MATSimAppCommand {
    private static final Logger log = LogManager.getLogger(GenerateFreightDemandData.class);

    @CommandLine.Option(names = "--data", description = "Path to raw data (ketten 2010)",
            defaultValue = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/german-wide-freight/raw-data/ketten-2010.csv")
    private String dataPath;
    @CommandLine.Option(names = "--output", description = "Output folder path", required = true, defaultValue = "output/longDistanceFreightData/")
    private Path output;


    @Override
    public Integer call() throws Exception {

        log.info("Reading trip relations...");
        List<TripRelation> tripRelations = TripRelation.readTripRelations(dataPath);
        log.info("Trip relations successfully loaded. There are " + tripRelations.size() + " trip relations");

        log.info("Start generating population...");
        Population outputPopulation = PopulationUtils.createPopulation(ConfigUtils.createConfig());
		PopulationFactory populationFactory = PopulationUtils.getFactory();
		for (int i = 0; i < tripRelations.size(); i++) {
			Person person = populationFactory.createPerson(Id.createPersonId("freightData_" + i));
			LongDistanceFreightUtils.writeCommonAttributesV2(person, tripRelations.get(i), Integer.toString(i));
			outputPopulation.addPerson(person);

			if (i % 500000 == 0) {
				log.info("Processing: " + i + " out of " + tripRelations.size() + " entries have been processed");
			}
		}

		if (!Files.exists(output)) {
            Files.createDirectory(output);
        }

        String outputPlansPath = output.toString() + "/german_freightData.xml.gz";
        PopulationWriter populationWriter = new PopulationWriter(outputPopulation);
        populationWriter.write(outputPlansPath);

        return 0;
    }

    public static void main(String[] args) {
        new GenerateFreightDemandData().execute(args);
    }
}
