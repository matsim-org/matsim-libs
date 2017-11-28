package playground.lsieber.networkshapecutter;

import java.io.File;
import java.io.IOException;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

import ch.ethz.idsc.queuey.datalys.MultiFileTools;
import playground.clruch.gfx.MatsimMapComponent;
import playground.clruch.gfx.MatsimViewerFrame;
import playground.clruch.net.MatsimStaticDatabase;
import playground.clruch.netdata.VirtualNetworkGet;
import playground.clruch.utils.ScenarioOptionsExt;
import playground.lsieber.oldCode.SimOptionsLoader;

public class NetworkVisualisation {
    private ScenarioOptionsExt simOptions;
    private Network network;
    private Population population;

    public NetworkVisualisation(Network network, ScenarioOptionsExt simOptions) throws IOException {
        this.simOptions = simOptions;
        this.network = network;
    }

    public NetworkVisualisation(Network network) throws IOException {
        this.simOptions = SimOptionsLoader.loadSimOptions();
        this.network = network;
    }

    public NetworkVisualisation(ScenarioOptionsExt simOptions) throws IOException {
        this.simOptions = simOptions;
        this.network = SimOptionsLoader.LoadNetworkBasedOnSimOptions(simOptions);
    }

    public NetworkVisualisation() throws IOException {
        this.simOptions = SimOptionsLoader.loadSimOptions();
        File workingDirectory = MultiFileTools.getWorkingDirectory();
        
        File file = new File(workingDirectory, simOptions.getString("simuConfig"));
        Config config = ConfigUtils.loadConfig(file.toString());
        
        Scenario scenario = ScenarioUtils.loadScenario(config);
        this.network = scenario.getNetwork();
        this.population = scenario.getPopulation();
    }

    public void run() throws IOException {
        displayNetwork();
    }

    public ScenarioOptionsExt getSimOptions() {
        return this.simOptions;
    }

    public Network getNetwork() {
        return this.network;
    }

    public void setNetwork(Network network) {
        this.network = network;
    }
    
    public Population getPopulation() {
        return this.population;
    }
    
    public void setPopulation(Population population) {
        this.population = population;
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
