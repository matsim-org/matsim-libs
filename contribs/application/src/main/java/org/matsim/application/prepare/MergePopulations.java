package org.matsim.application.prepare;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.application.MATSimAppCommand;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
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

        Config config = ConfigUtils.createConfig();
        config.plans().setInputFile(paths.get(0).toString());
        Scenario scenario = ScenarioUtils.loadScenario(config);
        Population population = scenario.getPopulation();

        for (int i = 1; i < paths.size(); i++) {

            log.info("Reading population file {}:{}", i, paths.get(i));

            Config otherConfig = ConfigUtils.createConfig();
            otherConfig.plans().setInputFile(paths.get(i).toString());
            Scenario freightScenario = ScenarioUtils.loadScenario(otherConfig);
            Population freightOnlyPlans = freightScenario.getPopulation();

            for (Person person : freightOnlyPlans.getPersons().values()) {
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
