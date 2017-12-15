package playground.lsieber.scenario.preparer;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Objects;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.network.io.NetworkWriter;

import ch.ethz.idsc.owly.data.GlobalAssert;
import ch.ethz.idsc.queuey.util.GZHandler;
import playground.clruch.options.ScenarioOptions;
import playground.lsieber.networkshapecutter.LinkModes;
import playground.lsieber.networkshapecutter.NetworkCutterUtils;
import playground.lsieber.networkshapecutter.NetworkCutters;

public enum NetworkPreparer {
    ;
    public static Network run(Network network, ScenarioOptions scenOptions) throws MalformedURLException, IOException {
        GlobalAssert.that(!Objects.isNull(network));

        // Cut Network as defined in the IDSC Settings
        NetworkCutters networkCutter = scenOptions.getNetworkCutter();
        Network modifiedNetwork = networkCutter.cut(network, scenOptions);

        // Filter out only the modes defines in the IDSC Settings

        LinkModes linkModes = scenOptions.getLinkModes();
        if (!linkModes.allModesAllowed) {
            modifiedNetwork = NetworkCutterUtils.modeFilter(modifiedNetwork, scenOptions.getLinkModes());
        }

        if (scenOptions.cleanNetwork()) {
            new NetworkCleaner().run(modifiedNetwork);
        }

        final File fileExportGz = new File(scenOptions.getPreparedNetworkName() + ".xml.gz");
        final File fileExport = new File(scenOptions.getPreparedNetworkName() + ".xml");
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
        System.out.println("saved converted network to: " + scenOptions.getPreparedNetworkName() + ".xml");

        NetworkCutterUtils.printNettworkCuttingInfo(network, modifiedNetwork);

        return modifiedNetwork;

    }

}
