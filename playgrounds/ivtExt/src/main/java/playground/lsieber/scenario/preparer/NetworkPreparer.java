package playground.lsieber.scenario.preparer;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.network.io.NetworkWriter;

import ch.ethz.idsc.queuey.util.GZHandler;
import playground.lsieber.networkshapecutter.NetworkCutterUtils;
import playground.lsieber.networkshapecutter.PrepSettings;

public enum NetworkPreparer {
    ;
    public static void run(Network network, PrepSettings settings) throws MalformedURLException, IOException {
        if (network == null)
            System.out.println("its the network");
        if (settings.locationSpec == null)
            System.out.println("its the ls");

        // Cut Network as defined in the IDSC Settings
        Network modifiedNetwork = settings.createNetworkCutter().cut(network, settings);


        
        // Filter out only the modes defines in the IDSC Settings

        if (!settings.modes.allModesAllowed) {
            modifiedNetwork = NetworkCutterUtils.modeFilter(modifiedNetwork, settings.modes);
        }
        
        NetworkCutterUtils.printNettworkCuttingInfo(network, modifiedNetwork);


        // Clean the network if defined in the IDSC Settings
        if (settings.networkCleaner) {
            new NetworkCleaner().run(modifiedNetwork);
        }

        final File fileExportGz = new File(settings.preparedScenarioDirectory, settings.NETWORKUPDATEDNAME + ".xml.gz");
        final File fileExport = new File(settings.preparedScenarioDirectory, settings.NETWORKUPDATEDNAME + ".xml");
        {
            // write the modified population to file
            NetworkWriter nw = new NetworkWriter(modifiedNetwork);
            nw.write(fileExportGz.toString());
        }
        // extract the created .gz file
        try {
            GZHandler.extract(fileExportGz, fileExport);
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("saved converted network to: " + settings.preparedScenarioDirectory + settings.NETWORKUPDATEDNAME + ".xml");

        NetworkCutterUtils.printNettworkCuttingInfo(network, modifiedNetwork);

        network = modifiedNetwork;

        settings.config.network().setInputFile(settings.NETWORKUPDATEDNAME + ".xml");
    }

}
