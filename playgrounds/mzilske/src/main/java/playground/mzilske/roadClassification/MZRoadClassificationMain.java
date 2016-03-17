package playground.mzilske.roadClassification;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigReader;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.counts.Counts;
import org.matsim.counts.CountsReaderMatsimV1;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;
import roadclassification.RunRoadClassification;

/**
 * Created by michaelzilske on 08/10/15.
 */
public class MZRoadClassificationMain {

    final static String BERLIN_PATH = "/Users/michaelzilske/shared-svn/studies/countries/de/berlin/";
    final static String sightingsDir = "/Users/michaelzilske/git/java8-matsim-playground/output/berlin/congested3/alternatives/only-heavy-users/sightings";

    public static void main(String[] args) {
        Config config = new Config();
        config.addCoreModules();
//        new ConfigReader(config).parse(BerlinRunUncongested3.class.getResourceAsStream("2kW.15.xml"));
        config.plans().setInputFile(BERLIN_PATH + "plans/baseplan_car_only.xml.gz");
        config.controler().setWritePlansInterval(0);
        config.controler().setDumpDataAtEnd(false);
        config.network().setInputFile(BERLIN_PATH + "counts/iv_counts/network.xml.gz");
        config.controler().setOutputDirectory("output/road-classification-new/run");
        config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
        config.qsim().setRemoveStuckVehicles(false);
        config.planCalcScore().setWriteExperiencedPlans(true);

        Scenario scenario = ScenarioUtils.loadScenario(config);

        final Counts allCounts = new Counts();
        new CountsReaderMatsimV1(allCounts).parse(sightingsDir + "/all_counts.xml.gz");

        scenario.addScenarioElement(Counts.ELEMENT_NAME, allCounts);

        final ObjectAttributes linkAttributes = new ObjectAttributes();
        new ObjectAttributesXmlReader(linkAttributes).parse("/Users/michaelzilske/git/java8-matsim-playground/output/berlin/road-categories/road-categories.xml");
        scenario.addScenarioElement("linkAttributes", linkAttributes);
        RunRoadClassification.optimize(scenario);
        System.out.println("Done.");
    }

}
