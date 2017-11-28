package playground.lsieber.oldCode;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;

import ch.ethz.idsc.queuey.datalys.MultiFileTools;
import ch.ethz.idsc.queuey.util.GlobalAssert;
import playground.clruch.data.ReferenceFrame;
import playground.clruch.options.ScenarioOptions;
import playground.clruch.utils.NetworkLoader;

@Deprecated //TODO  why is this still needed?
public class SimOptionsLoader {

    Config config;
    public static Network LoadNetworkBasedOnSimOptions() throws IOException {
        ScenarioOptions simOptions = loadSimOptions();
        return NetworkLoaderlukas(simOptions);
    }

    public static Network LoadNetworkBasedOnSimOptions(ScenarioOptions simOptions) throws IOException {
        return NetworkLoaderlukas(simOptions);
    }

    public static ScenarioOptions loadSimOptions() throws IOException {
        // TODO: checkout this Class: NetworkLoader()
        File workingDirectory = MultiFileTools.getWorkingDirectory();
        return ScenarioOptions.load(workingDirectory);

    }

    public static Network NetworkLoaderlukas(ScenarioOptions simOptions) throws IOException {
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
        ScenarioOptions simOptions = loadSimOptions();
        return PopulationLoaderLukas(simOptions);
    }

    public static Population LoadPopulationBasedOnSimOptions(ScenarioOptions simOptions) throws IOException {
        return PopulationLoaderLukas(simOptions);
    }

    public static Population PopulationLoaderLukas(ScenarioOptions simOptions) throws IOException {
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
