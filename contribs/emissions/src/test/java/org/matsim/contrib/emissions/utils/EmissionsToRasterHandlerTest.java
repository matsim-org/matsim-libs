package org.matsim.contrib.emissions.utils;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

public class EmissionsToRasterHandlerTest {

    @Rule
    public MatsimTestUtils testUtils = new MatsimTestUtils();

    @Test
    public void writeEmissionsToNetcdf() throws MalformedURLException {

        URL equil = URI.create(ExamplesUtils.getTestScenarioURL("equil").toString() + "network.xml").toURL();
        Network network = NetworkUtils.readNetwork(equil.toString());

        var rasteredNetwork = new RasteredNetwork(network, 20);

        var handler = new EmissionsToRasterHandler(rasteredNetwork, 100);

        // read emissions events file

        // do something with the output

    }

}