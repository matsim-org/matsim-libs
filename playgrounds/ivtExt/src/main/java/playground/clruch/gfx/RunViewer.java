package playground.clruch.gfx;

import java.io.File;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.CH1903LV03PlustoWGS84;

import playground.sebhoerl.avtaxi.framework.AVConfigGroup;

public class RunViewer {
    /**
     * @param args
     *            Main program arguments
     */
    public static void main(String[] args) {

        File configFile = new File(args[0]);
        final File dir = configFile.getParentFile();

        DvrpConfigGroup dvrpConfigGroup = new DvrpConfigGroup();
        dvrpConfigGroup.setTravelTimeEstimationAlpha(0.05);

        Config config = ConfigUtils.loadConfig(configFile.toString(), new AVConfigGroup(), dvrpConfigGroup);
        Scenario scenario = ScenarioUtils.loadScenario(config);
        Network network = scenario.getNetwork();
        System.out.println("links " + network.getLinks().size());
        final Population population = scenario.getPopulation();
        System.out.println(population.getPersons().size());

        CoordinateTransformation coordinateTransformation = new CH1903LV03PlustoWGS84();

        MatsimComponent matsimComponent = new MatsimComponent( //
                network, //
                coordinateTransformation //
        );
        MatsimViewer matsimViewer = new MatsimViewer(matsimComponent);
        matsimViewer.jFrame.setVisible(true);
    }

}
