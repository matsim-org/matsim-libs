package playground.lsieber.networkshapecutter;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

import org.matsim.api.core.v01.network.Network;

import ch.ethz.idsc.queuey.datalys.MultiFileTools;
import ch.ethz.idsc.queuey.util.GlobalAssert;
import playground.clruch.ScenarioOptions;
import playground.clruch.data.ReferenceFrame;
import playground.clruch.gfx.MatsimMapComponent;
import playground.clruch.gfx.MatsimViewerFrame;
import playground.clruch.net.MatsimStaticDatabase;
import playground.clruch.netdata.VirtualNetworkGet;
import playground.clruch.utils.NetworkLoader;
import playground.clruch.utils.PropertiesExt;

public class NetworkVisualisation {
    private PropertiesExt simOptions;
    private Network network;

    public NetworkVisualisation(Network network, PropertiesExt simOptions) throws IOException {
        this.simOptions = simOptions;
        this.network = network;
    }

    public NetworkVisualisation(Network network) throws IOException {
        this.simOptions = SimOptionsLoader.loadSimOptions();
        this.network = network;
    }

    public NetworkVisualisation(PropertiesExt simOptions) throws IOException {
        this.simOptions = simOptions;
        this.network = SimOptionsLoader.LoadNetworkBasedOnSimOptions(simOptions);
    }

    public NetworkVisualisation() throws IOException {
        this.simOptions = SimOptionsLoader.loadSimOptions();
        this.network = SimOptionsLoader.LoadNetworkBasedOnSimOptions();
    }

    public void run() throws IOException {
        displayNetwork();
    }

    public PropertiesExt getSimOptions() {
        return this.simOptions;
    }

    public Network getNetwork() {
        return this.network;
    }

    public void setNetwork(Network network) {
        this.network = network;
    }

    private void displayNetwork() throws IOException {

        File workingDirectory = MultiFileTools.getWorkingDirectory();
        File outputDirectory = new File(workingDirectory, simOptions.getString("visualizationFolder"));

        MatsimStaticDatabase.initializeSingletonInstance(network, simOptions.getReferenceFrame());
        MatsimMapComponent matsimJMapViewer = new MatsimMapComponent(MatsimStaticDatabase.INSTANCE);

        /** this is optional and should not cause problems if file does not exist. temporary solution */
        matsimJMapViewer.virtualNetworkLayer.setVirtualNetwork(VirtualNetworkGet.readDefault(network));

        // TODO possible without reading the output files?!
        MatsimViewerFrame matsimViewer = new MatsimViewerFrame(matsimJMapViewer, outputDirectory);
        matsimViewer.setDisplayPosition(MatsimStaticDatabase.INSTANCE.getCenter(), 12);
        matsimViewer.jFrame.setSize(900, 900);
        matsimViewer.jFrame.setVisible(true);
    }

}
