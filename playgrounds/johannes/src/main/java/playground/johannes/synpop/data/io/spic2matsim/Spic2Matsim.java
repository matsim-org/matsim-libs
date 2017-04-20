package playground.johannes.synpop.data.io.spic2matsim;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.FacilitiesReaderMatsimV1;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;
import playground.johannes.synpop.data.Person;
import playground.johannes.synpop.data.PlainFactory;
import playground.johannes.synpop.data.io.PopulationIO;
import playground.johannes.synpop.processing.TaskRunner;
import playground.johannes.synpop.util.PopulationStats;

import java.util.Set;

/**
 * Created by johannesillenberger on 06.04.17.
 */
public class Spic2Matsim {

    private static final Logger logger = Logger.getLogger(Spic2Matsim.class);

    public static void main(String args[]) {
        String popInFile = args[0];
        String facFile = args[1];
        String popOutFile = args[2];

        Config config = ConfigUtils.createConfig();
        Scenario scenario = ScenarioUtils.createScenario(config);
        /*
        Load persons...
         */
        logger.info("Loading persons...");
        Set<? extends Person> persons = PopulationIO.loadFromXML(popInFile, new PlainFactory());
        /*
        Load facilities...
         */
        logger.info("Loading facilities...");
        FacilitiesReaderMatsimV1 facReader = new FacilitiesReaderMatsimV1(scenario);
        facReader.readFile(facFile);
        ActivityFacilities facilities = scenario.getActivityFacilities();
        /*
        Validate persons...
         */
        logger.info("Validating persons...");
        TaskRunner.run(new ActTimeValidator(), persons);
        TaskRunner.runLegTask(new LegModeValidator(), persons);
        /*
        Convert...
         */
        PopulationStats stats = new PopulationStats();
        stats.run(persons);
        logger.info(String.format("Converting persons %s persons, %s episodes, %s activities and %s legs",
                stats.getNumPersons(),
                stats.getNumEpisodes(),
                stats.getNumActivities(),
                stats.getNumLegs()));
        PersonConverter converter = new PersonConverter(scenario.getPopulation(), facilities);
        converter.convert(persons);

        /*
        Write matsim xml...
         */
        logger.info("Writing population...");
        PopulationWriter writer = new PopulationWriter(scenario.getPopulation());
        writer.write(popOutFile);

        logger.info("Writing person attributes...");
        int idx = popOutFile.lastIndexOf("/");
        String attFile = String.format("%s/attributes.xml.gz", popOutFile.substring(0, idx));
        ObjectAttributes attrs = scenario.getPopulation().getPersonAttributes();
        ObjectAttributesXmlWriter oaWriter = new ObjectAttributesXmlWriter(attrs);
        oaWriter.writeFile(attFile);
    }
}
