package playground.michalm.demand;

import java.io.*;
import java.util.*;

import javax.naming.*;
import javax.xml.parsers.*;

import org.apache.log4j.*;
import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.population.*;
import org.xml.sax.*;

import pl.poznan.put.util.array2d.*;


public class ODDemandGenerator
    extends DemandGenerator
{
    private static final Logger log = Logger.getLogger(ODDemandGenerator.class);

    private int zoneCount;
    private int[][] odMatrix;


    public ODDemandGenerator(String networkFileName, String zonesXMLFileName,
            String zonesShpFileName, String odMatrixFileName, String idField)
        throws IOException, SAXException, ParserConfigurationException
    {
        super(networkFileName, zonesXMLFileName, zonesShpFileName,  idField);

        // read OD matrix
        zoneCount = fileOrderedZones.size();
        odMatrix = Array2DReader.getIntArray(new File(odMatrixFileName), zoneCount);

    }


    @Override
    public void generate()
    {
        Population popul = scenario.getPopulation();
        PopulationFactory pf = popul.getFactory();

        for (int i = 0; i < zoneCount; i++) {
            Zone oZone = fileOrderedZones.get(i);

            for (int j = 0; j < zoneCount; j++) {
                Zone dZone = fileOrderedZones.get(j);

                int odFlow = odMatrix[i][j];

                for (int k = 0; k < 2 * odFlow; k++) {
                    // generatePlans for the OD pair

                    Plan plan = createPlan();

                    Coord oCoord = getRandomCoordInZone(oZone);
                    Activity act = createActivity(plan, "dummy", oCoord);
                    act.setEndTime(3600 * k / odFlow);

                    plan.addLeg(pf.createLeg(TransportMode.car));

                    Coord dCoord = getRandomCoordInZone(dZone);
                    act = createActivity(plan, "dummy", dCoord);
                }
            }
        }
    }


    public static void main(String[] args)
        throws ConfigurationException, IOException, SAXException, ParserConfigurationException
    {
        String dirName;

        String networkFileName;
        String zonesXMLFileName;
        String zonesShpFileName;
        String odMatrixFileName;
        String plansFileName;
        String idField;

        if (args.length == 1 && args[0].equals("test")) {// for testing
            dirName = "d:\\PP-dyplomy\\PLANY\\";
            networkFileName = dirName + "network.xml";
            zonesXMLFileName = dirName + "zones.xml";
            zonesShpFileName = dirName + "zones_with_no_zone.SHP";
            odMatrixFileName = dirName + "odMatrix.dat";
            plansFileName = dirName + "plans.xml";
            idField = "NO";

            // dirName = "D:\\PP-dyplomy\\2010_11-mgr\\burkat_andrzej\\siec2\\";
            // networkFileName = dirName + "network2.xml";
            // zonesXMLFileName = dirName + "zones2.xml";
            // zonesShpFileName = dirName + "zones2.shp";
            // plansFileName = dirName + "plans2.xml";

            // dirName = "D:\\PP-dyplomy\\2010_11-mgr\\gintrowicz_marcin\\Paj\\";
            // networkFileName = dirName + "network.xml";
            // zonesXMLFileName = dirName + "zone.xml";
            // zonesShpFileName = dirName + "zone.shp";
            // plansFileName = dirName + "plans.xml";

            // dirName = "D:\\PP-dyplomy\\2010_11-mgr\\gintrowicz_marcin\\NSE\\";
            // networkFileName = dirName + "network2.xml";
            // zonesXMLFileName = dirName + "zones2.xml";
            // zonesShpFileName = dirName + "zone.shp";
            // plansFileName = dirName + "plans.xml";
        }
        else if (args.length == 7) {
            dirName = args[0];
            networkFileName = dirName + args[1];
            zonesXMLFileName = dirName + args[2];
            zonesShpFileName = dirName + args[3];
            odMatrixFileName = dirName + args[4];
            plansFileName = dirName + args[5];
            idField = args[6];
        }
        else {
            throw new IllegalArgumentException("Incorrect program arguments: "
                    + Arrays.toString(args));
        }

        ODDemandGenerator dg = new ODDemandGenerator(networkFileName, zonesXMLFileName,
                zonesShpFileName, odMatrixFileName, idField);
        dg.generate();
        dg.write(plansFileName);
    }

}
