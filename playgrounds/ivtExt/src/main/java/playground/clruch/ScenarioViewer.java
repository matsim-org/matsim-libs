package playground.clruch;

import java.io.File;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;

import playground.clruch.gfx.MatsimJMapViewer;
import playground.clruch.gfx.MatsimStaticDatabase;
import playground.clruch.gfx.MatsimViewerFrame;
import playground.clruch.gfx.PointCloud;
import playground.clruch.gfx.ReferenceFrame;
import playground.sebhoerl.avtaxi.framework.AVConfigGroup;

/**
 * the viewer allows to connect to the scenario server
 */
public class ScenarioViewer {
    /**
     * @param args
     *            Main program arguments
     */
    public static void main(String[] args) {

        File configFile = new File(args[0]);

        Network network = null;
        // TODO potentially use MatsimNetworkReader to only read network?!
        try {
            DvrpConfigGroup dvrpConfigGroup = new DvrpConfigGroup();
            dvrpConfigGroup.setTravelTimeEstimationAlpha(0.05);
            Config config = ConfigUtils.loadConfig(configFile.toString(), new AVConfigGroup(), dvrpConfigGroup);
            // Network network2 = NetworkUtils.createNetwork(config);
            // MatsimNetworkReader reader = new MatsimNetworkReader(network2);
            // reader.putAttributeConverters(Collections.emptyMap());
            // reader.parse(new URL("network.xml"));
            // network = network2;
            Scenario scenario = ScenarioUtils.loadScenario(config);
            network = scenario.getNetwork();

        } catch (Exception e) {
            e.printStackTrace();
        }

        double[] bb = NetworkUtils.getBoundingBox(network.getNodes().values());
        System.out.println(bb[0] + " " + bb[1] + " " + bb[2] + " " + bb[3]);

        MatsimStaticDatabase.initializeSingletonInstance( //
                network, //
                ReferenceFrame.SIOUXFALLS);

        MatsimJMapViewer matsimJMapViewer = new MatsimJMapViewer(MatsimStaticDatabase.INSTANCE);
        // this is optional and should not cause problems if file does not exist.
        // temporary solution
        matsimJMapViewer.virtualNetworkLayer.setPointCloud(PointCloud.fromCsvFile( //
                new File("dummy_vn/voronoi_BoundaryPoints.csv"), // TODO <- extract name from xml file
                MatsimStaticDatabase.INSTANCE.referenceFrame.coords_toWGS84));
        
//        matsimJMapViewer.setTileGridVisible(false);

        MatsimViewerFrame matsimViewer = new MatsimViewerFrame(matsimJMapViewer);
        matsimViewer.setDisplayPosition(MatsimStaticDatabase.INSTANCE.getCenter(), 12);
        matsimViewer.jFrame.setSize(800, 900);
        matsimViewer.jFrame.setVisible(true);

    }

}
