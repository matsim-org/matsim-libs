package playground.michalm.barcelona;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.*;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.OsmNetworkReader;


public class CreateBarcelonaNetwork
{
    public static void main(String[] args)
    {
        String dir = "d:/PP-rad/Barcelona/data/network/";
        String osmFile = dir + "barcelona_osm.xml";
        String networkFile = dir + "barcelona_network.xml";

        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        Network network = scenario.getNetwork();

        CoordinateTransformation coordTrans = TransformationFactory.getCoordinateTransformation(
                TransformationFactory.WGS84, TransformationFactory.WGS84_UTM31N);

        OsmNetworkReader onr = new OsmNetworkReader(network, coordTrans);
        onr.setHighwayDefaults(1, "motorway", 2, 80. / 3.6, 1., 2500, true);
        onr.setHighwayDefaults(1, "motorway_link", 1, 50. / 3.6, 1., 1700, true);
        onr.setHighwayDefaults(3, "primary", 1, 50. / 3.6, 1., 1800);
        onr.setHighwayDefaults(3, "primary_link", 1, 30. / 3.6, 1., 1600);
        onr.setHighwayDefaults(4, "secondary", 1, 50. / 3.6, 1., 1400);
        onr.setHighwayDefaults(4, "secondary_link", 1, 30. / 3.6, 1., 1200);
        onr.setHighwayDefaults(5, "tertiary", 1, 50. / 3.6, 1., 1000);
        onr.setHighwayDefaults(5, "tertiary_link", 1, 30. / 3.6, 1., 800);
        onr.setHighwayDefaults(6, "residential", 1, 30. / 3.6, 1., 700);
        onr.setHighwayDefaults(6, "living_street", 1, 30. / 3.6, 1., 400);
        onr.parse(osmFile);

        new NetworkCleaner().run(network);
        new NetworkWriter(network).write(networkFile);
    }
}
