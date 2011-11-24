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

                int odFlow = odMatrix[i][j] / 2;

                for (int k = 0; k < 2 * odFlow; k++) {
                    // generatePlans for the OD pair

//                    if (!(k % 100 == 0 && i < 9 && j < 9)) {
//                        continue;
//                    }

                    
                    Plan plan = createPlan();

                    Coord oCoord = getRandomCoordInZone(oZone);
                    Activity act = createActivity(plan, "dummy", oCoord);
                    act.setEndTime(3600 * k / odFlow);

                    if (k % 100 == 0 && i < 9 && j < 9) {
                        plan.addLeg(pf.createLeg("taxi"));
                    }
                    else {
                        plan.addLeg(pf.createLeg(TransportMode.car));
                    }

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
            dirName = "D:\\PP-rad\\taxi\\mielec\\";
            networkFileName = dirName + "network.xml";
            zonesXMLFileName = dirName + "zones.xml";
            zonesShpFileName = dirName + "GIS\\zones_with_no_zone.SHP";
            odMatrixFileName = dirName + "odMatrix.dat";
            plansFileName = dirName + "plans.xml";
            idField = "NO";
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
