package playground.lsieber.networkshapecutter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Objects;

import org.matsim.api.core.v01.network.Network;

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
        /** 1. Load Network */
        Network network = nV.getNetwork();
        /** 2. cut the Network */
        Network cutN = new NetworkCutterShape(new File("shapefiles/Export_Output_2.shp")).filter(network);
        // Network cutN = new NetworkCutterRadius(LocationSpec.HOMBURGERTAL.center, LocationSpec.HOMBURGERTAL.radius).filter(network);
        /** 3. update the Network in nV */
        nV.setNetwork(cutN);

        /** write to XML */
        // new NetworkWriter(cutN).write("network.xml");

        /** display on Map */
        nV.run();

    }

}
