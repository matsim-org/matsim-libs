package playground.lsieber.networkshapecutter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;

import ch.ethz.idsc.queuey.datalys.MultiFileTools;
import ch.ethz.idsc.queuey.util.GlobalAssert;
import playground.clruch.ScenarioOptions;
import playground.clruch.data.LocationSpec;
import playground.clruch.data.ReferenceFrame;
import playground.clruch.utils.NetworkLoader;
import playground.clruch.utils.PropertiesExt;

public class Test {

    public static void main(String[] args) throws FileNotFoundException, IOException {
        /** Load Simulation Options */
        NetworkVisualisation nV = new NetworkVisualisation();

        // PropertiesExt simOptions = nV.getSimOptions();

        /** change the network */
        /** 1.a) Load Network */
        Network network = nV.getNetwork();

        /** 1.b) Select Modes */
        HashSet<String> modes = new HashSet<String>();
        modes.add("car");
        // modes.add("pt");

        /** 2. cut the Network */
        Network cutN = new NetworkCutterShape(new File("shapefiles/Export_Output_2.shp")).filter(network, modes);
        // Network cutN = new NetworkCutterRadius(LocationSpec.HOMBURGERTAL.center, LocationSpec.HOMBURGERTAL.radius).filter(network, modes);
        /** 3. update the Network in nV */
        nV.setNetwork(cutN);

        /** write to XML */
        new NetworkWriter(cutN).write("network.xml");

        /** display on Map */
        nV.run();

    }

}
