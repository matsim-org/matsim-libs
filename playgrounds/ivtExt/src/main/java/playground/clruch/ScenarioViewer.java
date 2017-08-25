// code by clruch and jph
package playground.clruch;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import org.matsim.api.core.v01.network.Network;

import playground.clruch.data.ReferenceFrame;
import playground.clruch.gfx.MatsimMapComponent;
import playground.clruch.gfx.MatsimViewerFrame;
import playground.clruch.net.MatsimStaticDatabase;
import playground.clruch.netdata.VirtualNetworkGet;
import playground.clruch.utils.NetworkLoader;

/** the viewer allows to connect to the scenario server or to view saved
 * simulation results. */
public class ScenarioViewer {

    /** @param args a java Properties type file with the options for the viewer, an example file
     *            can be created with DefaultOptions.saveViewerDefault();
     * @throws FileNotFoundException
     * @throws IOException */
    public static void main(String[] args) throws FileNotFoundException, IOException {

        Properties viewerOptions = new Properties(DefaultOptions.getViewerDefault());
        if (args.length > 0 && new File(args[0]).exists()) {
            viewerOptions.load(new FileInputStream(new File(args[0])));
        }else DefaultOptions.saveViewerDefault();

        ReferenceFrame referenceFrame = ReferenceFrame.fromString(//
                viewerOptions.getProperty("ReferenceFrame"));

        Network network = NetworkLoader.loadNetwork(new File(viewerOptions.getProperty("av_config.xml")));
        MatsimStaticDatabase.initializeSingletonInstance(network, referenceFrame);
        MatsimMapComponent matsimJMapViewer = new MatsimMapComponent(MatsimStaticDatabase.INSTANCE);

        // this is optional and should not cause problems if file does not exist.
        // temporary solution
        matsimJMapViewer.virtualNetworkLayer.setVirtualNetwork(VirtualNetworkGet.readDefault(network));

        MatsimViewerFrame matsimViewer = new MatsimViewerFrame(matsimJMapViewer);
        matsimViewer.setDisplayPosition(MatsimStaticDatabase.INSTANCE.getCenter(), 12);
        matsimViewer.jFrame.setSize(900, 900);
        matsimViewer.jFrame.setVisible(true);
    }

}
