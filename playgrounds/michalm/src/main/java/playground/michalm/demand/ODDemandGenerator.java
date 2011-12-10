package playground.michalm.demand;

import java.io.*;
import java.util.*;

import javax.naming.*;
import javax.xml.parsers.*;

import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.population.*;
import org.xml.sax.*;

import pl.poznan.put.util.array2d.*;


public class ODDemandGenerator
    extends DemandGenerator
{
    // private static final Logger log = Logger.getLogger(ODDemandGenerator.class);

    private int zoneCount;
    private int[][] odMatrix;


    public ODDemandGenerator(String networkFileName, String zonesXMLFileName,
            String zonesShpFileName, String odMatrixFileName, String idField)
        throws IOException, SAXException, ParserConfigurationException
    {
        super(networkFileName, zonesXMLFileName, zonesShpFileName, idField);

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

                int odFlow = odMatrix[i][j] / 2;// artificially reduced

                for (int k = 0; k < 2 * odFlow; k++) {// "2 * odFlow" because for 2 hours
                    // generatePlans for the OD pair

                    // if (!(k % 100 == 0 && i < 9 && j < 9)) {
                    // continue;
                    // }

                    Plan plan = createPlan();

                    Coord oCoord = getRandomCoordInZone(oZone);
                    Activity act = createActivity(plan, "dummy", oCoord);

                    double timeStep = 3600. / odFlow;

                    act.setEndTime((int)uniform.nextDoubleFromTo(k * timeStep, (k + 1) * timeStep));

                    // x% probability of choosing taxi for internal travels
                    // (i.e. between internal zones: 0-8)
                    double taxiProbability = 0.01;
                    if (i < 9 && j < 9 && uniform.nextDoubleFromTo(0, 1) < taxiProbability) {
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
            dirName = "D:\\PP-rad\\taxi\\mielec-nowe-OD\\";
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
