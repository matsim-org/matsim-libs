package org.matsim.urbanEV;


import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;

import static org.matsim.examples.ExamplesUtils.getTestScenarioURL;



public class OnePercentageConsumptionPerLink {

    public static void main(String[] args) {

        // read in the network
        Network network = NetworkUtils.createNetwork();

        new MatsimNetworkReader(network).readFile("C:/Users/admin/IdeaProjects/matsim-berlin/test/input/chessboard/network.xml");

        // iterate through all links
        for (Link l : network.getLinks().values()){
            //get current capacity
            double oldLength = l.getLength();
            double newLength = oldLength * 0.728803851494254;

            //set new capacity
            l.setLength(newLength);
        }
        new NetworkWriter(network).write("C:/Users/admin/IdeaProjects/matsim-berlin/test/input/1%network.xml");
    }
}