package playground.lsieber.scenario.reducer;

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

        Network modifiedNetwork = settings.createNetworkCutter().cut(network, settings);

        modifiedNetwork = NetworkCutterUtils.modeFilter(modifiedNetwork, settings.modes);
        if (settings.networkCleaner) {
            new NetworkCleaner().run(modifiedNetwork);
        }

        final File fileExportGz = new File(settings.workingDirectory, settings.NETWORKUPDATEDNAME + ".xml.gz");
        final File fileExport = new File(settings.workingDirectory, settings.NETWORKUPDATEDNAME + ".xml");
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
        System.out.println("saved converted network to: " + settings.workingDirectory + settings.NETWORKUPDATEDNAME + ".xml");

        network = modifiedNetwork;
    }
}
