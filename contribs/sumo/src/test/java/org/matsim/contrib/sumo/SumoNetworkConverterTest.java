package org.matsim.contrib.sumo;

import com.google.common.io.Resources;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.lanes.LanesReader;
import org.matsim.lanes.LanesToLinkAssignment;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.SortedMap;

public class SumoNetworkConverterTest {


	@Test
	void convert() throws Exception {

        Path input = Files.createTempFile("sumo", ".xml");
        Path output = Files.createTempFile("matsim", ".xml");

        Files.copy(Resources.getResource("osm.net.xml").openStream(), input, StandardCopyOption.REPLACE_EXISTING);

        SumoNetworkConverter converter = SumoNetworkConverter.newInstance(List.of(input), output, "EPSG:4326", "EPSG:4326");

        converter.call();

        Network network = NetworkUtils.readNetwork(output.toString());

        assert network.getNodes().size() == 21 : "Must contain 21 nodes";
        assert network.getNodes().containsKey(Id.createNodeId("251106770")) : "Must contain specific id";

        Path lanes = Path.of(output.toString().replace(".xml", "-lanes.xml"));

        Config config = ConfigUtils.createConfig();
        Scenario scenario = ScenarioUtils.createScenario(config);

        LanesReader reader = new LanesReader(scenario);
        reader.readFile(lanes.toString());

        SortedMap<Id<Link>, LanesToLinkAssignment> l2l = scenario.getLanes().getLanesToLinkAssignments();

        System.out.println(l2l);

        assert l2l.containsKey(Id.createLinkId("-160346478#3")) : "Must contain link id";

        Path geometry = Path.of(output.toString().replace(".xml", "-linkGeometries.csv"));

        assert Files.exists(geometry) : "Geometries must exist";

    }
}