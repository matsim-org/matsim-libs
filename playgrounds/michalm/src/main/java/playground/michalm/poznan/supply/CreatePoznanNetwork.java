package playground.michalm.poznan.supply;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.OsmNetworkReader;


public class CreatePoznanNetwork
{
    public static void main(String[] args)
    {
        String osmFile = "d:/GoogleDrive/Poznan/Osm_2015_02/Source/Poznan_2015_02_05_all.osm";
        String networkFile = "d:/PP-rad/poznan/test/Poznan_2015_02_05_all.xml";

        Config config = ConfigUtils.createConfig();
        Scenario scenario = ScenarioUtils.createScenario(config);
        Network network = scenario.getNetwork();

        CoordinateTransformation coordTrans = TransformationFactory.getCoordinateTransformation(
                TransformationFactory.WGS84, TransformationFactory.WGS84_UTM33N);

        OsmNetworkReader onr = new OsmNetworkReader(network, coordTrans);
        // road -- same attributes as residential
        onr.setHighwayDefaults(1, "motorway", 2, 140.0 / 3.6, 1.0, 2500, true);
        onr.setHighwayDefaults(1, "motorway_link", 1, 70.0 / 3.6, 1.0, 1700, true);
        onr.setHighwayDefaults(2, "trunk", 1, 120.0 / 3.6, 1.0, 2200);
        onr.setHighwayDefaults(2, "trunk_link", 1, 70.0 / 3.6, 1.0, 1600);
        onr.setHighwayDefaults(3, "primary", 1, 90.0 / 3.6, 1.0, 1800);
        onr.setHighwayDefaults(3, "primary_link", 1, 60.0 / 3.6, 1.0, 1600);
        onr.setHighwayDefaults(4, "secondary", 1, 60.0 / 3.6, 1.0, 1400);
        onr.setHighwayDefaults(4, "secondary_link", 1, 50.0 / 3.6, 1.0, 1200);
        onr.setHighwayDefaults(5, "tertiary", 1, 60.0 / 3.6, 1.0, 1000);
        onr.setHighwayDefaults(5, "tertiary_link", 1, 50.0 / 3.6, 1.0, 800);
        onr.setHighwayDefaults(6, "road", 1, 50.0 / 3.6, 1.0, 700);
        onr.setHighwayDefaults(6, "unclassified", 1, 50.0 / 3.6, 1.0, 700);
        onr.setHighwayDefaults(6, "residential", 1, 40.0 / 3.6, 1.0, 700);
        onr.setHighwayDefaults(6, "living_street", 1, 30.0 / 3.6, 1.0, 400);

        onr.parse(osmFile);

        new NetworkCleaner().run(network);
        new NetworkWriter(network).write(networkFile);
    }}
