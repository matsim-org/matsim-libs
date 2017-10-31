package playground.lsieber.networkshapecutter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Objects;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.io.NetworkWriter;

import ch.ethz.idsc.queuey.datalys.MultiFileTools;
import ch.ethz.idsc.queuey.util.GlobalAssert;
import playground.clruch.ScenarioOptions;
import playground.clruch.data.LocationSpec;
import playground.clruch.data.ReferenceFrame;
import playground.clruch.gfx.MatsimMapComponent;
import playground.clruch.gfx.MatsimViewerFrame;
import playground.clruch.net.MatsimStaticDatabase;
import playground.clruch.netdata.VirtualNetworkGet;
import playground.clruch.utils.NetworkLoader;
import playground.clruch.utils.PropertiesExt;

public class Demo {

	public static void main(String[] args) throws FileNotFoundException, IOException {
		 // load options
        File workingDirectory = MultiFileTools.getWorkingDirectory();;
        PropertiesExt simOptions = PropertiesExt.wrap(ScenarioOptions.load(workingDirectory));
        File outputDirectory = new File(workingDirectory, simOptions.getString("visualizationFolder"));
        System.out.println("showing simulation results stored in folder: " + outputDirectory.getName());
        
        ReferenceFrame referenceFrame = simOptions.getReferenceFrame();
        /** reference frame needs to be set manually in IDSCOptions.properties file */
        GlobalAssert.that(Objects.nonNull(referenceFrame)); 
        GlobalAssert.that(Objects.nonNull(simOptions.getLocationSpec()));
        Network network = NetworkLoader.loadNetwork(new File(workingDirectory, simOptions.getString("simuConfig")));

        
        // cut the network
        Network networkPolyCutted = new NetworkCutter().filter(network, "shapefiles/Export_Output_2");
        Network networkRadiusCutted = new NetworkCutter().filter(network, LocationSpec.HOMBURGERTAL.center, LocationSpec.HOMBURGERTAL.radius);

        new NetworkWriter(networkPolyCutted).write("network_reduced_poly.xml");
        new NetworkWriter(networkRadiusCutted).write("network_reduced_radius.xml");
        //new NetworkWriter(network).write("network_complete.xml");
 
        /*TODO Write function to visualize a network and a population individually
         * @clruch: do we already have such a function???
         * 
         */
        
        
        // load viewer
        Network network_display = networkPolyCutted;
        
        MatsimStaticDatabase.initializeSingletonInstance(network_display, referenceFrame);
        MatsimMapComponent matsimJMapViewer = new MatsimMapComponent(MatsimStaticDatabase.INSTANCE);

        /** this is optional and should not cause problems if file does not exist. temporary solution */
        matsimJMapViewer.virtualNetworkLayer.setVirtualNetwork(VirtualNetworkGet.readDefault(network_display));


        MatsimViewerFrame matsimViewer = new MatsimViewerFrame(matsimJMapViewer, outputDirectory);
        matsimViewer.setDisplayPosition(MatsimStaticDatabase.INSTANCE.getCenter(), 12);
        matsimViewer.jFrame.setSize(900, 900);
        matsimViewer.jFrame.setVisible(true);        

	}

}
