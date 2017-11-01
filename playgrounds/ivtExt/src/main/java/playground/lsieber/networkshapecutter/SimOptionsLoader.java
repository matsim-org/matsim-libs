package playground.lsieber.networkshapecutter;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

import org.matsim.api.core.v01.network.Network;

import ch.ethz.idsc.queuey.datalys.MultiFileTools;
import ch.ethz.idsc.queuey.util.GlobalAssert;
import playground.clruch.ScenarioOptions;
import playground.clruch.data.ReferenceFrame;
import playground.clruch.utils.NetworkLoader;
import playground.clruch.utils.PropertiesExt;

public class SimOptionsLoader {

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

}
