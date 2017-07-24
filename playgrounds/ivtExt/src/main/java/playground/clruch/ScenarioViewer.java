// code by clruch and jph
package playground.clruch;

import org.matsim.api.core.v01.network.Network;

import playground.clruch.data.ReferenceFrame;
import playground.clruch.gfx.MatsimMapComponent;
import playground.clruch.gfx.MatsimViewerFrame;
import playground.clruch.net.MatsimStaticDatabase;
import playground.clruch.netdata.VirtualNetworkGet;
import playground.clruch.utils.NetworkLoader;

/**
 * the viewer allows to connect to the scenario server
 */
public class ScenarioViewer {
    /**
     * @param args
     *            complete path to av_config.xml file, e.g.
     *            /media/bubba/data/ethz/2017_03_13_Sioux_LP_improved/av_config.xml
     */
    public static void main(String[] args) {
        // BEGIN: CUSTOMIZE -----------------------------------------------
        // set manually depending on the scenario:

        ReferenceFrame referenceFrame = ReferenceFrame.SIOUXFALLS;

        // END: CUSTOMIZE -------------------------------------------------

        Network network = NetworkLoader.loadNetwork(args);
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
