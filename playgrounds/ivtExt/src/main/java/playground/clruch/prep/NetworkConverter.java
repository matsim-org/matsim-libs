package playground.clruch.prep;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;

import playground.clruch.utils.GZHandler;

public class NetworkConverter {
    public static void main(String[] args) throws MalformedURLException {
        final File dir = new File(args[0]);
        if (!dir.isDirectory()) {
            new RuntimeException("not a directory: " + dir).printStackTrace();
            System.exit(-1);
        }
        final File fileImport = new File(dir, "network.xml");
        final File fileExportGz = new File(dir, "networkConverted.xml.gz");
        final File fileExport = new File(dir, "networkConverted.xml");

        // load the existing population file
        Config config = ConfigUtils.createConfig();
        config.network().setInputFile(fileImport.toString());
        Scenario scenario = ScenarioUtils.loadScenario(config);
        Network network = scenario.getNetwork();

        // increasing the first value goes right
        // increasing the second value goes north
        //Coord center = new Coord(2704366.0,1236061.0);
        //Coord center = new Coord(2684648.0,1251492.0);
        //Coord center = new Coord(2684648.0,1251492.0); Center in Kloten
        Coord center = new Coord(2683600.0,1251400.0);
        double radius = 9000;
        Network filteredNetwork = NetworkTools.elminateOutsideRadius(network, center, radius);
        
        
        {
            // write the modified population to file
            NetworkWriter nw = new NetworkWriter(filteredNetwork);
            nw.write(fileExportGz.toString());
        }

        // extract the created .gz file
        try {
            GZHandler.extract(fileExportGz, fileExport);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
