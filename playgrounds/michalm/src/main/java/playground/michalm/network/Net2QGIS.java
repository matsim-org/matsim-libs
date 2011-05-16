package playground.michalm.network;

import java.util.*;

import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.network.*;
import org.matsim.core.network.*;
import org.matsim.core.scenario.*;
import org.matsim.core.utils.geometry.transformations.*;
import org.matsim.core.utils.misc.*;
import org.matsim.utils.gis.matsim2esri.network.*;


public class Net2QGIS
{
    public static void main(String[] args)
    {
        String dirName;
        String netFileName;
        String outFileNameLs;
        String outFileNameP;
        String outFileNameN;

        if (args.length == 1 && args[0].equals("test")) {// for testing
            dirName = "D:\\PP-dyplomy\\2010_11-mgr\\gintrowicz_marcin\\Paj\\";
            netFileName = dirName + "network.xml";
            outFileNameLs = dirName + "linksLs.shp";
            outFileNameP = dirName + "linksP.shp";
            outFileNameN = dirName + "nodes.shp";
        }
        else if (args.length == 5) {
            dirName = args[0];
            netFileName = dirName + args[1];
            outFileNameLs = dirName + args[2];
            outFileNameP = dirName + args[3];
            outFileNameN = dirName + args[4];
        }
        else {
            throw new IllegalArgumentException("Incorrect program arguments: "
                    + Arrays.toString(args));
        }


        String coordSystem = TransformationFactory.WGS84_UTM33N;

        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());

        scenario.getConfig().global().setCoordinateSystem(coordSystem);

        Network network = scenario.getNetwork();
        new MatsimNetworkReader(scenario).readFile(netFileName);

        //links as lines
        FeatureGeneratorBuilderImpl builder = new FeatureGeneratorBuilderImpl(
                network, coordSystem);
        builder.setFeatureGeneratorPrototype(LineStringBasedFeatureGenerator.class);
        builder.setWidthCalculatorPrototype(LanesBasedWidthCalculator.class);
        new Links2ESRIShape(network, outFileNameLs, builder).write();

        //links as polygons
        builder = new FeatureGeneratorBuilderImpl(network, coordSystem);// necessary?
        builder.setFeatureGeneratorPrototype(PolygonFeatureGenerator.class);
        builder.setWidthCalculatorPrototype(LanesBasedWidthCalculator.class);
        new Links2ESRIShape(network, outFileNameP, builder).write();

        //nodes as points
        new Nodes2ESRIShape(network, outFileNameN, coordSystem).write();
    }
}
