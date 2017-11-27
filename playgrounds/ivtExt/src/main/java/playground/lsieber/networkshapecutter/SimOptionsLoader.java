package playground.lsieber.networkshapecutter;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

import ch.ethz.idsc.queuey.datalys.MultiFileTools;
import ch.ethz.idsc.queuey.util.GlobalAssert;
import playground.clruch.ScenarioOptions;
import playground.clruch.data.ReferenceFrame;
import playground.clruch.utils.NetworkLoader;
import playground.clruch.utils.PropertiesExt;

public class SimOptionsLoader {

    Config config;
    public static Network LoadNetworkBasedOnSimOptions() throws IOException {
        PropertiesExt simOptions = loadSimOptions();
        return NetworkLoaderlukas(simOptions);
    }

    public static Network LoadNetworkBasedOnSimOptions(PropertiesExt simOptions) throws IOException {
        return NetworkLoaderlukas(simOptions);
    }

    public static PropertiesExt loadSimOptions() throws IOException {
        // TODO: checkout this Class: NetworkLoader()
        File workingDirectory = MultiFileTools.getWorkingDirectory();
        return PropertiesExt.wrap(ScenarioOptions.load(workingDirectory));

    }

    public static Network NetworkLoaderlukas(PropertiesExt simOptions) throws IOException {
        File workingDirectory = MultiFileTools.getWorkingDirectory();
        File outputDirectory = new File(workingDirectory, simOptions.getString("visualizationFolder"));
        System.out.println("showing simulation results stored in folder: " + outputDirectory.getName());

        ReferenceFrame referenceFrame = simOptions.getReferenceFrame();
        /** reference frame needs to be set manually in IDSCOptions.properties file */
        GlobalAssert.that(Objects.nonNull(referenceFrame));
        GlobalAssert.that(Objects.nonNull(simOptions.getLocationSpec()));
        Network network = NetworkLoader.loadNetwork(new File(workingDirectory, simOptions.getString("simuConfig")));

        return network;

    }
    
    public static Population LoadPopulationBasedOnSimOptions() throws IOException {
        PropertiesExt simOptions = loadSimOptions();
        return PopulationLoaderLukas(simOptions);
    }

    public static Population LoadPopulationBasedOnSimOptions(PropertiesExt simOptions) throws IOException {
        return PopulationLoaderLukas(simOptions);
    }

    public static Population PopulationLoaderLukas(PropertiesExt simOptions) throws IOException {
        File workingDirectory = MultiFileTools.getWorkingDirectory();

        Population population = PopulationLoader.loadPopulation(new File(workingDirectory, simOptions.getString("simuConfig")));
        return population;
    }
    
    // TODO copied from ScenarioLoaderImpl. maybe this can be done nicer...
//    private void loadNetwork(Config config) {
//       this.config = config;
//        if ((this.config.network() != null) && (this.config.network().getInputFile() != null)) {
//            URL networkUrl = this.config.network().getInputFileURL(this.config.getContext());
//            if ( config.network().getInputCRS() == null ) {
//                MatsimNetworkReader reader = new MatsimNetworkReader(NetworkUtils.createNetwork(this.config));
//                reader.putAttributeConverters( attributeConverters );
//                reader.parse(networkUrl);
//            }
//            else {
//                final CoordinateTransformation transformation =
//                        TransformationFactory.getCoordinateTransformation(
//                                config.network().getInputCRS(),
//                                config.global().getCoordinateSystem() );
//                MatsimNetworkReader reader = new MatsimNetworkReader( transformation , NetworkUtils.createNetwork(this.config));
//                reader.putAttributeConverters( attributeConverters );
//                reader.parse(networkUrl);
//            }
//
//            if ((this.config.network().getChangeEventsInputFile()!= null) && this.config.network().isTimeVariantNetwork()) {
//                Network network = this.scenario.getNetwork();
//                List<NetworkChangeEvent> changeEvents = new ArrayList<>() ;
//                NetworkChangeEventsParser parser = new NetworkChangeEventsParser(network,changeEvents);
//                parser.parse(this.config.network().getChangeEventsInputFileUrl(config.getContext()));
//                NetworkUtils.setNetworkChangeEvents(network,changeEvents);
//            }
//        }
//    }

}
