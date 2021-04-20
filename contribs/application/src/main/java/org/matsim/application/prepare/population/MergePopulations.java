package org.matsim.application.prepare.population;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.application.MATSimAppCommand;
import org.matsim.core.population.PopulationUtils;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.List;

@CommandLine.Command(
        name = "merge-populations",
        description = "Merge multiple populations together"
)
public class MergePopulations implements MATSimAppCommand {

    private static final Logger log = LogManager.getLogger(MergePopulations.class);

    @CommandLine.Parameters(arity = "2..*", paramLabel = "INPUT", description = "Paths of populations to merge")
    private List<Path> paths;

    @CommandLine.Option(names = "--output", description = "Path to output", required = true)
    private Path output;

    @Override
    public Integer call() throws Exception {


        Population population = PopulationUtils.readPopulation(paths.get(0).toString());

        for (int i = 1; i < paths.size(); i++) {
            log.info("Reading population file {}:{}", i, paths.get(i));
            Population otherPopulation = PopulationUtils.readPopulation(paths.get(i).toString());
            for (Person person : otherPopulation.getPersons().values()) {
                population.addPerson(person);
            }
        }

        // Write new population file
        // Write population
        System.out.println("Writing population file...");
        PopulationWriter pw = new PopulationWriter(population);
        pw.write(output.toString());

        return 0;
    }
}
