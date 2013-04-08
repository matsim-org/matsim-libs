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

import java.io.*;
import java.util.*;

import javax.naming.ConfigurationException;
import javax.xml.parsers.ParserConfigurationException;

import org.matsim.api.core.v01.*;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.xml.sax.SAXException;

import pl.poznan.put.util.random.*;
import playground.michalm.demand.*;
import playground.michalm.demand.DefualtLocationGenerator.GeometryProvider;
import playground.michalm.demand.DefualtLocationGenerator.PointAcceptor;
import playground.michalm.util.visum.VisumODMatrixReader;

import com.vividsolutions.jts.geom.*;


public class PoznanLanduseDemandGeneration
{
    private static final String HOME = "H";
    private static final String WORK = "W";
    private static final String EDUCATION = "E";
    private static final String SHOPPING = "S";
    private static final String OTHER = "O";

    private static final double RESIDENTIAL_TO_INDUSTRIAL_WORK = 0.33;
    private static final double RESIDENTIAL_TO_SHOP_SHOPPING = 0.1;


    public static class LanduseLocationGeneratorStrategy
        implements GeometryProvider, PointAcceptor
    {
        private final Map<String, Map<Id, WeightedRandomSelection<Geometry>>> selectionByZoneByActType;
        private final Map<Id, List<Geometry>> forestByZone;
        private final Set<Id> validatedZones;


        public LanduseLocationGeneratorStrategy(
                Map<String, Map<Id, WeightedRandomSelection<Geometry>>> selectionByZoneByActType,
                Map<Id, List<Geometry>> forestByZone, Set<Id> validatedZones)
        {
            this.selectionByZoneByActType = selectionByZoneByActType;
            this.forestByZone = forestByZone;
            this.validatedZones = validatedZones;
        }


        @Override
        public Geometry getGeometry(Zone zone, String actType)
        {
            if (actType != OTHER && validatedZones.contains(zone.getId())) {
                return selectionByZoneByActType.get(actType).get(zone.getId()).select();
            }

            return (Geometry)zone.getZonePolygon().getDefaultGeometry();
        }


        @Override
        public boolean acceptPoint(Zone zone, String actType, Point point)
        {
            if (actType == OTHER) {
                return true;
            }

            for (Geometry g : forestByZone.get(zone.getId())) {
                if (g.contains(point)) {
                    return false; // inside forest
                }
            }

            return true;// outside forest
        }
    }


    private Scenario scenario;
    private Map<Id, Zone> zones;
    private Set<Id> validatedZones;
    private Map<String, Map<Id, WeightedRandomSelection<Geometry>>> selectionByZoneByActType;
    private ODDemandGenerator dg;


    public void generate()
        throws ConfigurationException, SAXException, ParserConfigurationException, IOException
    {
        String dirName = "D:\\eTaxi\\Poznan_MATSim\\";
        String networkFile = dirName + "network.xml";
        String zonesXmlFile = dirName + "zones.xml";
        String zonesShpFile = dirName + "GIS\\zones_with_no_zone.SHP";

        String forestShpFile = dirName + "GIS\\forests.SHP";
        String industrialShpFile = dirName + "GIS\\industrial.SHP";
        String residentialShpFile = dirName + "GIS\\residential.SHP";
        String schoolShpFile = dirName + "GIS\\school.SHP";
        String shopShpFile = dirName + "GIS\\shop.SHP";

        String validatedZonesFile = dirName + "GIS\\zones_with_correct_landuse";

        String odMatrixFilePrefix = dirName + "odMatricesByType\\";
        String plansFile = dirName + "plans.xml.gz";
        String idField = "NO";
        int randomSeed = RandomUtils.DEFAULT_SEED;

        RandomUtils.reset(randomSeed);

        scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        new MatsimNetworkReader(scenario).readFile(networkFile);
        zones = Zone.readZones(scenario, zonesXmlFile, zonesShpFile, idField);

        readValidatedZones(validatedZonesFile);

        System.out.println("getLanduseByZone(forestShpFile)");
        Map<Id, List<Geometry>> forestByZone = getLanduseByZone(forestShpFile);
        System.out.println("getLanduseByZone(industrialShpFile)");
        Map<Id, List<Geometry>> industrialByZone = getLanduseByZone(industrialShpFile);
        System.out.println("getLanduseByZone(residentialShpFile)");
        Map<Id, List<Geometry>> residentialByZone = getLanduseByZone(residentialShpFile);
        System.out.println("getLanduseByZone(schoolShpFile)");
        Map<Id, List<Geometry>> schoolByZone = getLanduseByZone(schoolShpFile);
        System.out.println("getLanduseByZone(shopShpFile)");
        Map<Id, List<Geometry>> shopByZone = getLanduseByZone(shopShpFile);

        selectionByZoneByActType = new HashMap<String, Map<Id, WeightedRandomSelection<Geometry>>>();

        Map<Id, WeightedRandomSelection<Geometry>> homeSelectionByZone = initSelectionByZone(HOME);
        Map<Id, WeightedRandomSelection<Geometry>> workSelectionByZone = initSelectionByZone(WORK);
        Map<Id, WeightedRandomSelection<Geometry>> educationSelectionByZone = initSelectionByZone(EDUCATION);
        Map<Id, WeightedRandomSelection<Geometry>> shoppingSelectionByZone = initSelectionByZone(SHOPPING);

        for (Id zoneId : validatedZones) {
            WeightedRandomSelection<Geometry> homeSelection = initSelection(zoneId,
                    homeSelectionByZone);
            WeightedRandomSelection<Geometry> workSelection = initSelection(zoneId,
                    workSelectionByZone);
            WeightedRandomSelection<Geometry> educationSelection = initSelection(zoneId,
                    educationSelectionByZone);
            WeightedRandomSelection<Geometry> shoppingSelection = initSelection(zoneId,
                    shoppingSelectionByZone);

            for (Geometry g : industrialByZone.get(zoneId)) {
                workSelection.add(g, g.getArea());
            }

            for (Geometry g : residentialByZone.get(zoneId)) {
                double area = g.getArea();
                homeSelection.add(g, area);
                workSelection.add(g, RESIDENTIAL_TO_INDUSTRIAL_WORK * area);
                shoppingSelection.add(g, RESIDENTIAL_TO_SHOP_SHOPPING * area);
            }

            for (Geometry g : schoolByZone.get(zoneId)) {
                educationSelection.add(g, g.getArea());
            }

            for (Geometry g : shopByZone.get(zoneId)) {
                shoppingSelection.add(g, g.getArea());
            }
        }

        LanduseLocationGeneratorStrategy strategy = new LanduseLocationGeneratorStrategy(
                selectionByZoneByActType, forestByZone, validatedZones);
        LocationGenerator lg = new DefualtLocationGenerator(scenario, strategy, strategy);
        dg = new ODDemandGenerator(scenario, lg, zones);

        generate(odMatrixFilePrefix + "D_I", HOME, OTHER);
        generate(odMatrixFilePrefix + "D_N", HOME, EDUCATION);
        generate(odMatrixFilePrefix + "D_P", HOME, WORK);
        generate(odMatrixFilePrefix + "D_Z", HOME, SHOPPING);
        generate(odMatrixFilePrefix + "I_D", OTHER, HOME);
        generate(odMatrixFilePrefix + "N_D", EDUCATION, HOME);
        generate(odMatrixFilePrefix + "P_D", WORK, HOME);
        generate(odMatrixFilePrefix + "Z_D", SHOPPING, HOME);
        generate(odMatrixFilePrefix + "NZD", OTHER, OTHER);

        dg.write(plansFile);
        // dg.writeTaxiCustomers(taxiFile);
    }


    private void generate(String filePrefix, String actTypeFrom, String actTypeTo)
        throws FileNotFoundException
    {
        for (int i = 0; i < 24; i++) {
            String odMatrixFile = filePrefix + "_" + i + "-" + (i + 1);
            System.out.println("Generation for " + odMatrixFile);
            double[][] odMatrix = VisumODMatrixReader.readMatrixFile(new File(odMatrixFile));
            dg.generateSinglePeriod(odMatrix, actTypeFrom, actTypeTo, 1, 1, 0, i * 3600);
        }
    }


    private void readValidatedZones(String validatedZonesFile)
        throws FileNotFoundException
    {
        validatedZones = new HashSet<Id>();
        Scanner scanner = new Scanner(new File(validatedZonesFile));

        while (scanner.hasNext()) {
            validatedZones.add(scenario.createId(scanner.next()));
        }

        scanner.close();
    }


    private WeightedRandomSelection<Geometry> initSelection(Id zoneId,
            Map<Id, WeightedRandomSelection<Geometry>> selectionByZone)
    {
        WeightedRandomSelection<Geometry> selection = new WeightedRandomSelection<Geometry>();
        selectionByZone.put(zoneId, selection);
        return selection;
    }


    private Map<Id, WeightedRandomSelection<Geometry>> initSelectionByZone(String actType)
    {
        Map<Id, WeightedRandomSelection<Geometry>> selectionByZone = new HashMap<Id, WeightedRandomSelection<Geometry>>();
        selectionByZoneByActType.put(actType, selectionByZone);
        return selectionByZone;
    }


    private Map<Id, List<Geometry>> getLanduseByZone(String shpFile)
    {
        return LanduseByZoneUtils.buildMap(zones, ShapeFileReader.getAllFeatures(shpFile));
    }


    public static void main(String[] args)
        throws ConfigurationException, IOException, SAXException, ParserConfigurationException
    {
        new PoznanLanduseDemandGeneration().generate();
    }
}
