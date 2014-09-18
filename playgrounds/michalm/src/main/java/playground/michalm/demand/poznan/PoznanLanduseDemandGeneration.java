/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.michalm.demand.poznan;

import static playground.michalm.demand.poznan.PoznanLanduseDemandGeneration.ActivityType.EDUCATION;
import static playground.michalm.demand.poznan.PoznanLanduseDemandGeneration.ActivityType.HOME;
import static playground.michalm.demand.poznan.PoznanLanduseDemandGeneration.ActivityType.NON_HOME;
import static playground.michalm.demand.poznan.PoznanLanduseDemandGeneration.ActivityType.OTHER;
import static playground.michalm.demand.poznan.PoznanLanduseDemandGeneration.ActivityType.SHOPPING;
import static playground.michalm.demand.poznan.PoznanLanduseDemandGeneration.ActivityType.WORK;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;

import javax.naming.ConfigurationException;
import javax.xml.parsers.ParserConfigurationException;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.dvrp.run.VrpConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.matrices.Matrix;
import org.xml.sax.SAXException;

import pl.poznan.put.util.random.RandomUtils;
import pl.poznan.put.util.random.WeightedRandomSelectionTable;
import playground.michalm.demand.ActivityCreator;
import playground.michalm.demand.DefaultActivityCreator;
import playground.michalm.demand.DefaultActivityCreator.GeometryProvider;
import playground.michalm.demand.DefaultActivityCreator.PointAcceptor;
import playground.michalm.demand.DefaultPersonCreator;
import playground.michalm.demand.ODDemandGenerator;
import playground.michalm.demand.PersonCreator;
import playground.michalm.util.matrices.MatrixUtils;
import playground.michalm.util.visum.VisumODMatrixReader;
import playground.michalm.zone.Zone;
import playground.michalm.zone.Zones;
import playground.michalm.zone.util.SubzoneUtils;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;


public class PoznanLanduseDemandGeneration
{
    static enum ActivityType
    {
        HOME, WORK, EDUCATION, SHOPPING, OTHER, NON_HOME;
    }


    static enum ActivityPair
    {
        D_I(HOME, OTHER), //
        D_N(HOME, EDUCATION), //
        D_P(HOME, WORK), //
        D_Z(HOME, SHOPPING), //
        I_D(OTHER, HOME), //
        N_D(EDUCATION, HOME), //
        P_D(WORK, HOME), //
        Z_D(SHOPPING, HOME), //
        NZD(NON_HOME, NON_HOME);//

        final ActivityType from, to;


        private ActivityPair(ActivityType from, ActivityType to)
        {
            this.from = from;
            this.to = to;
        }
    }


    private static class ZoneLanduseValidation
    {
        private final boolean residential;
        private final boolean industrial;


        private ZoneLanduseValidation(boolean residential, boolean industrial)
        {
            this.residential = residential;
            this.industrial = industrial;
        }
    }


    private static final double PEOPLE_PER_VEHICLE = 1.2;
    private static final double RESIDENTIAL_TO_INDUSTRIAL_WORK = 0.33;
    private static final double RESIDENTIAL_TO_SHOP_SHOPPING = 0.1;


    public class LanduseLocationGeneratorStrategy
        implements GeometryProvider, PointAcceptor
    {
        @Override
        public Geometry getGeometry(Zone zone, String actType)
        {
            ActivityType activityType = ActivityType.valueOf(actType);

            if (isActivityConstrained(activityType)) {
                Polygon polygon = selectionTable.select(zone.getId(), activityType);

                if (polygon != null) {
                    return polygon; // randomly selected subzone
                }
            }

            return zone.getMultiPolygon(); // whole zone
        }


        @Override
        public boolean acceptPoint(Zone zone, String actType, Point point)
        {
            ActivityType activityType = ActivityType.valueOf(actType);

            if (isActivityConstrained(activityType)) {
                if (selectionTable.contains(zone.getId(), activityType)) {
                    return true;
                }

                for (Polygon p : forestByZone.get(zone.getId())) {
                    if (p.contains(point)) {
                        return false; // inside forest
                    }
                }
            }

            return true;
        }
    }


    private Scenario scenario;
    private Map<Id<Zone>, Zone> zones;
    private Map<Id, ZoneLanduseValidation> zoneLanduseValidation;
    private EnumMap<ActivityPair, Double> prtCoeffs;

    private Map<Id, List<Polygon>> forestByZone;
    private Map<Id, List<Polygon>> industrialByZone;
    private Map<Id, List<Polygon>> residentialByZone;
    private Map<Id, List<Polygon>> schoolByZone;
    private Map<Id, List<Polygon>> shopByZone;

    private ODDemandGenerator dg;

    private final EnumSet<ActivityType> constrainedActivities = EnumSet.of(HOME, WORK, EDUCATION,
            SHOPPING);

    private WeightedRandomSelectionTable<Id, ActivityType, Polygon> selectionTable;


    public void generate(String dirName)
        throws ConfigurationException, SAXException, ParserConfigurationException, IOException
    {
        String networkFile = dirName + "network.xml";
        String zonesXmlFile = dirName + "zones.xml";
        String zonesShpFile = dirName + "GIS\\zones.SHP";

        String forestShpFile = dirName + "GIS\\forests.SHP";
        String industrialShpFile = dirName + "GIS\\industrial.SHP";
        String residentialShpFile = dirName + "GIS\\residential.SHP";
        String schoolShpFile = dirName + "GIS\\school.SHP";
        String shopShpFile = dirName + "GIS\\shop.SHP";

        String zonesWithLanduseFile = dirName + "GIS\\zones_with_correct_landuse";

        String put2PrtRatiosFile = dirName + "PuT_PrT_ratios";

        String odMatrixFilePrefix = dirName + "odMatricesByType\\";
        String plansFile = dirName + "plans.xml.gz";
        int randomSeed = RandomUtils.DEFAULT_SEED;

        RandomUtils.reset(randomSeed);

        scenario = ScenarioUtils.createScenario(VrpConfigUtils.createConfig());
        new MatsimNetworkReader(scenario).readFile(networkFile);
        zones = Zones.readZones(scenario, zonesXmlFile, zonesShpFile);

        readValidatedZones(zonesWithLanduseFile);

        forestByZone = getLandusePolygonsByZone(forestShpFile);
        industrialByZone = getLandusePolygonsByZone(industrialShpFile);
        residentialByZone = getLandusePolygonsByZone(residentialShpFile);
        schoolByZone = getLandusePolygonsByZone(schoolShpFile);
        shopByZone = getLandusePolygonsByZone(shopShpFile);

        initSelection();

        prtCoeffs = readPrtCoeffs(put2PrtRatiosFile);

        generateAll(odMatrixFilePrefix);

        dg.write(plansFile);
        // dg.writeTaxiCustomers(taxiFile);
    }


    private void readValidatedZones(String validatedZonesFile)
        throws FileNotFoundException
    {
        zoneLanduseValidation = new HashMap<Id, ZoneLanduseValidation>();
        Scanner scanner = new Scanner(new File(validatedZonesFile));

        scanner.nextLine();// skip the header

        while (scanner.hasNext()) {
            Id<Zone> zoneId = Id.create(scanner.next(), Zone.class);
            boolean residential = scanner.nextInt() != 0;
            boolean industrial = scanner.nextInt() != 0;

            zoneLanduseValidation.put(zoneId, new ZoneLanduseValidation(residential, industrial));
        }

        scanner.close();
    }


    private Map<Id, List<Polygon>> getLandusePolygonsByZone(String shpFile)
    {
        System.out.println("getLanduseByZone() for: " + shpFile);
        return SubzoneUtils.extractSubzonePolygons(zones, ShapeFileReader.getAllFeatures(shpFile));
    }


    private void initSelection()
    {
        selectionTable = WeightedRandomSelectionTable.createWithArrayTable(
                zoneLanduseValidation.keySet(), constrainedActivities);

        for (Entry<Id, ZoneLanduseValidation> e : zoneLanduseValidation.entrySet()) {
            Id zoneId = e.getKey();
            ZoneLanduseValidation validation = e.getValue();
            Zone zone = zones.get(zoneId);

            List<Polygon> industrialPolygons = validation.industrial ? //
                    industrialByZone.get(zoneId) : //
                    Zones.getPolygons(zone);

            for (Polygon p : industrialPolygons) {
                selectionTable.add(zoneId, WORK, p, p.getArea());
            }

            Iterable<Polygon> residentialPolygons = validation.residential ? //
                    residentialByZone.get(zoneId) : //
                    Zones.getPolygons(zone);

            for (Polygon p : residentialPolygons) {
                double area = p.getArea();
                selectionTable.add(zoneId, HOME, p, area);
                selectionTable.add(zoneId, WORK, p, RESIDENTIAL_TO_INDUSTRIAL_WORK * area);
                selectionTable.add(zoneId, SHOPPING, p, RESIDENTIAL_TO_SHOP_SHOPPING * area);
            }

            for (Polygon p : schoolByZone.get(zoneId)) {
                selectionTable.add(zoneId, EDUCATION, p, p.getArea());
            }

            for (Polygon p : shopByZone.get(zoneId)) {
                selectionTable.add(zoneId, SHOPPING, p, p.getArea());
            }
        }
    }


    static EnumMap<ActivityPair, Double> readPrtCoeffs(String put2PrtRatiosFile)
        throws FileNotFoundException
    {
        EnumMap<ActivityPair, Double> prtCoeffs = new EnumMap<ActivityPair, Double>(
                ActivityPair.class);
        Scanner scanner = new Scanner(new File(put2PrtRatiosFile));

        while (scanner.hasNext()) {
            ActivityPair pair = ActivityPair.valueOf(scanner.next());
            double putShare = scanner.nextDouble();
            double prtShare = scanner.nextDouble();
            double coeff = prtShare / (putShare + prtShare) / PEOPLE_PER_VEHICLE;

            if (prtCoeffs.put(pair, coeff) != null) {
                scanner.close();
                throw new RuntimeException("Pair respecified: " + pair);
            }
        }

        scanner.close();

        if (prtCoeffs.size() != ActivityPair.values().length) {
            throw new RuntimeException("Not all pairs have been specified");
        }

        return prtCoeffs;
    }


    private void generateAll(String odMatrixFilePrefix)
        throws FileNotFoundException
    {
        LanduseLocationGeneratorStrategy strategy = new LanduseLocationGeneratorStrategy();
        ActivityCreator ac = new DefaultActivityCreator(scenario, strategy, strategy);
        PersonCreator pc = new DefaultPersonCreator(scenario);
        dg = new ODDemandGenerator(scenario, zones, true, ac, pc);

        for (ActivityPair ap : ActivityPair.values()) {
            generate(odMatrixFilePrefix, ap);
        }
    }


    private void generate(String filePrefix, ActivityPair actPair)
        throws FileNotFoundException
    {
        double flowCoeff = prtCoeffs.get(actPair);
        String actTypeFrom = actPair.from.name();
        String actTypeTo = actPair.to.name();

        for (int i = 0; i < 24; i++) {
            String odMatrixFile = filePrefix + actPair.name() + "_" + i + "-" + (i + 1);
            System.out.println("Generation for " + odMatrixFile);
            double[][] visumODMatrix = VisumODMatrixReader.readMatrixFile(new File(odMatrixFile));
            Matrix odMatrix = MatrixUtils
                    .createSparseMatrix("m" + i, zones.keySet(), visumODMatrix);

            dg.generateSinglePeriod(odMatrix, actTypeFrom, actTypeTo, TransportMode.car, i * 3600,
                    3600, flowCoeff);
        }
    }


    private boolean isActivityConstrained(ActivityType activityType)
    {
        return constrainedActivities.contains(activityType);
    }


    public static void main(String[] args)
        throws ConfigurationException, IOException, SAXException, ParserConfigurationException
    {
        new PoznanLanduseDemandGeneration().generate("d:\\michalm\\eTaxi\\Poznan_MATSim\\");
    }
}
