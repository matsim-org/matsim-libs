package playground.lsieber.oldCode;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;

import playground.clruch.data.LocationSpec;
import playground.clruch.prep.PopulationTools;
import playground.lsieber.networkshapecutter.LinkModes;
import playground.lsieber.networkshapecutter.NetworkCutterRadius;
import playground.lsieber.networkshapecutter.NetworkCutterUtils;
import playground.lsieber.networkshapecutter.NetworkVisualisation;

public class Test {

    public static void main(String[] args) throws FileNotFoundException, IOException {
        /** Load Simulation Options */

        NetworkVisualisation nV = new NetworkVisualisation();

        // PropertiesExt simOptions = nV.getSimOptions();

        /** change the network */
        /** 1.a) Load Network */
        Network network = nV.getNetwork();

        HashSet<String> allmodes = new HashSet<String>();

        for (Link link : network.getLinks().values()) {
            for (String mode : link.getAllowedModes()) {
                allmodes.add(mode);
            }
        }
        System.out.println("----------------------------------");
        System.out.println("--------------ALL MODES AVAILABLE--------------------");

        for (String m : allmodes) {
            System.out.println("Mode:" + m);
        }
        System.out.println("----------------------------------");
        System.out.println("----------------------------------");

        /** 1.b) Select Modes */
        HashSet<String> modes = new HashSet<String>();
        modes.add("car");
        modes.add("pt");
        modes.add("tram");
        modes.add("bus");
        LinkModes linkModes = new LinkModes(modes);
        /** 2. cut the Network */
        // Network cutN = new NetworkCutterShape(new File("shapefiles/TargetArea.shp")).filter(network, modes);
        Network cutN = NetworkCutterUtils
                .modeFilter(new NetworkCutterRadius(LocationSpec.BASEL_REGION.center, LocationSpec.BASEL_REGION.radius).process(network), linkModes);

        // Network cutN = new NetworkCutterRadius(LocationSpec.HOMBURGERTAL.center, LocationSpec.HOMBURGERTAL.radius).filter(network, modes);
        /** 3. update the Network in nV */
        nV.setNetwork(cutN);

        /** 4.a) load population */
        Population population = nV.getPopulation();

        // /** 5. Cut Population */
        Population cutP = population;

        System.out.println("Number of People before cutting:" + cutP.getPersons().size());
        PopulationTools.elminateOutsideNetwork(cutP, cutN);
        System.out.println("Number of People after cutting:" + cutP.getPersons().size());

        /** write to XML */
        new NetworkWriter(cutN).write("Homburgertal/network.xml");
        new PopulationWriter(cutP).write("Homburgertal/population.xml");
        // /** display on Map */
        nV.run();

        System.out.println("END END END");

    }

}
