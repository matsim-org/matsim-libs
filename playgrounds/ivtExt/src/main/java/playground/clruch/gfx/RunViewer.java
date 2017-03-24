package playground.clruch.gfx;

import java.io.File;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;

import playground.clruch.gfx.helper.SiouxFallstoWGS84;
import playground.sebhoerl.avtaxi.framework.AVConfigGroup;

public class RunViewer {
    /**
     * @param args
     *            Main program arguments
     */
    public static void main(String[] args) {

        File configFile = new File(args[0]);
        final File dir = configFile.getParentFile();

        final Network network;
        {
            DvrpConfigGroup dvrpConfigGroup = new DvrpConfigGroup();
            dvrpConfigGroup.setTravelTimeEstimationAlpha(0.05);
            Config config = ConfigUtils.loadConfig(configFile.toString(), new AVConfigGroup(), dvrpConfigGroup);
            Scenario scenario = ScenarioUtils.loadScenario(config);
            network = scenario.getNetwork();
        }

        CoordinateTransformation ct;
        // ct = new CH1903LV03PlustoWGS84(); // <- switzerland
        ct = new SiouxFallstoWGS84(); // <- sioux falls

        MatsimStaticDatabase db = MatsimStaticDatabase.of(network, ct);

        MatsimJMapViewer matsimJMapViewer = new MatsimJMapViewer(db);
        matsimJMapViewer.setTileGridVisible(false);

        MatsimViewer matsimViewer = new MatsimViewer(matsimJMapViewer);

        // basel
        // getJMapViewer().setDisplayPosition(new Point(), new Coordinate(47.55814, 7.58769), 11);

        // sioux falls
        // TODO obtain center from db
        // jFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        matsimViewer.setDisplayPosition(43.54469101104898, -96.72376155853271, 13);

        matsimViewer.jFrame.setSize(800, 900);
        matsimViewer.jFrame.setVisible(true);

    }

}
