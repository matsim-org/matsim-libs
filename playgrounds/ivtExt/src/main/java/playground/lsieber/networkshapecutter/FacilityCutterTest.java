package playground.lsieber.networkshapecutter;

import java.io.File;
import java.io.IOException;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.FacilitiesWriter;

import ch.ethz.idsc.queuey.datalys.MultiFileTools;
import playground.clruch.ScenarioOptions;
import playground.clruch.utils.PropertiesExt;

public class FacilityCutterTest {

    public static void main(String[] args) throws IOException {
        File workingDirectory = MultiFileTools.getWorkingDirectory();
        PropertiesExt simOptions = PropertiesExt.wrap(ScenarioOptions.load(workingDirectory));
        File file = new File(workingDirectory, simOptions.getString("simuConfig"));
        Config config = ConfigUtils.loadConfig(file.toString());
        Scenario scenario = ScenarioUtils.loadScenario(config);

        ActivityFacilities facilities = scenario.getActivityFacilities();

        facilities = new FacilityPopulationBasedCutter(scenario.getPopulation()).filter(facilities);
        // TODO Lets save it as .gz
        // new FacilitiesWriter(facilities).write("facilities.xml.gz");
        new FacilitiesWriter(facilities).write("facilities.xml");

    }

}
