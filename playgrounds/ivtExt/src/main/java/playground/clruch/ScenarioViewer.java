// code by clruch and jph
package playground.clruch;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Objects;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;

import ch.ethz.idsc.queuey.datalys.MultiFileTools;
import ch.ethz.idsc.queuey.util.GlobalAssert;
import playground.clruch.data.ReferenceFrame;
import playground.clruch.gfx.MatsimMapComponent;
import playground.clruch.gfx.MatsimViewerFrame;
import playground.clruch.net.MatsimStaticDatabase;
import playground.clruch.netdata.VirtualNetworkGet;
import playground.clruch.options.ScenarioOptions;
import playground.clruch.utils.NetworkLoader;

/** the viewer allows to connect to the scenario server or to view saved
 * simulation results. */
public class ScenarioViewer {

    public static void main(String[] args) throws FileNotFoundException, IOException {
        File workingDirectory = MultiFileTools.getWorkingDirectory();
        run(workingDirectory);
    }

    /** Execute in simulation folder to view past results or connect to simulation s
     * 
     * @param args not used
     * @throws FileNotFoundException
     * @throws IOException */
    public static void run(File workingDirectory) throws FileNotFoundException, IOException {

        // load options
        ScenarioOptions simOptions = ScenarioOptions.load(workingDirectory);
        Config config = ConfigUtils.loadConfig(simOptions.getSimulationConfigName());
        System.out.println(simOptions.getSimulationConfigName());
        File outputSubDirectory = new File(config.controler().getOutputDirectory());
        System.out.println(outputSubDirectory.getAbsolutePath());
        File outputDirectory = outputSubDirectory.getParentFile();
        // File outputDirectory = new File(workingDirectory, "/simulation_output" );

        System.out.println(outputSubDirectory.getAbsolutePath());
        System.out.println("showing simulation results stored in folder: " + outputDirectory.getName());

        ReferenceFrame referenceFrame = simOptions.getReferenceFrame();
        /** reference frame needs to be set manually in IDSCOptions.properties file */
        GlobalAssert.that(Objects.nonNull(referenceFrame));
        GlobalAssert.that(Objects.nonNull(simOptions.getLocationSpec()));
        Network network = NetworkLoader.loadNetwork(new File(workingDirectory, simOptions.getString("simuConfig")));
        System.out.println("INFO network loaded");
        System.out.println("INFO total links " + network.getLinks().size());
        System.out.println("INFO total nodes " + network.getNodes().size());

        // load viewer
        MatsimStaticDatabase.initializeSingletonInstance(network, referenceFrame);
        MatsimMapComponent matsimJMapViewer = new MatsimMapComponent(MatsimStaticDatabase.INSTANCE);

        /** this is optional and should not cause problems if file does not exist. temporary solution */
        matsimJMapViewer.virtualNetworkLayer.setVirtualNetwork(VirtualNetworkGet.readDefault(network));

        MatsimViewerFrame matsimViewer = new MatsimViewerFrame(matsimJMapViewer, outputDirectory);
        matsimViewer.setDisplayPosition(MatsimStaticDatabase.INSTANCE.getCenter(), 12);
        matsimViewer.jFrame.setSize(900, 900);
        matsimViewer.jFrame.setVisible(true);
    }

}
