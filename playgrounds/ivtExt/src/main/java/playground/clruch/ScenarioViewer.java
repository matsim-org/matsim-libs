package playground.clruch;

import java.io.File;

import playground.clruch.demo.utils.NetworkLoader;
import playground.clruch.gfx.MatsimJMapViewer;
import playground.clruch.gfx.MatsimViewerFrame;
import playground.clruch.gfx.PointCloud;
import playground.clruch.gfx.ReferenceFrame;
import playground.clruch.net.MatsimStaticDatabase;

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
        
        // set manually to display virtual network boundary (will be ignored if file doesn't exist)
        // this is optional and should not cause problems if file does not exist.
        
        File csvFile = new File("dummy_vn/voronoi_BoundaryPoints.csv");
        
        // END: CUSTOMIZE -------------------------------------------------

        MatsimStaticDatabase.initializeSingletonInstance(NetworkLoader.loadNetwork(args), referenceFrame);
        MatsimJMapViewer matsimJMapViewer = new MatsimJMapViewer(MatsimStaticDatabase.INSTANCE);
        // TODO temporary solution
        matsimJMapViewer.virtualNetworkLayer.setPointCloud(PointCloud.fromCsvFile( //
                csvFile, MatsimStaticDatabase.INSTANCE.referenceFrame.coords_toWGS84));

        MatsimViewerFrame matsimViewer = new MatsimViewerFrame(matsimJMapViewer);
        matsimViewer.setDisplayPosition(MatsimStaticDatabase.INSTANCE.getCenter(), 12);
        matsimViewer.jFrame.setSize(800, 900);
        matsimViewer.jFrame.setVisible(true);
    }

}
