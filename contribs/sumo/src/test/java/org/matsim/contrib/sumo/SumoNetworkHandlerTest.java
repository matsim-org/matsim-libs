package org.matsim.contrib.sumo;

import com.google.common.io.Resources;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URL;

public class SumoNetworkHandlerTest {


	@Test
	void read() throws Exception {

        URL resource = Resources.getResource("osm.net.xml");

        SumoNetworkHandler handler = SumoNetworkHandler.read(new File(resource.toURI()));

        assert handler.edges.containsKey("-160346478#3"): "Must contain specific edge";
    }
}