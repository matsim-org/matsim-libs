/* *********************************************************************** *
 * project: org.matsim.*
 * CommuterDemandWriter
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.jbischoff.taxi.berlin.demand;

import java.io.*;
import java.util.*;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.*;
import org.matsim.contrib.taxi.TaxiUtils;
import org.matsim.contrib.zone.Zone;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.*;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.tabularFileParser.*;

import com.vividsolutions.jts.geom.*;

import playground.michalm.berlin.BerlinZoneUtils;


/**
 * @author jbischoff
 */

public class TaxiDemandWriter
{
    private static final Logger log = Logger.getLogger(TaxiDemandWriter.class);
    private Map<String, Geometry> municipalityMap;
    private Population population;
    private NetworkImpl network;
    private Scenario scenario;

    private Random rnd = new Random(17);
    private final static String DATADIR = "C:/local_jb/data/";
    private final static String NETWORKFILE = DATADIR + "network/berlin_brb.xml.gz";

    private final static double SCALEFACTOR = 1.0;
    static int fromTXL = 0;
    static int toTXL = 0;

    private Map<Id<Zone>, Integer> oMap = new HashMap<>();
    private Map<Id<Zone>, Integer> dMap = new HashMap<>();


    public static void main(String[] args)
    {
        LorShapeReader lsr = new LorShapeReader();
        lsr.readShapeFile(DATADIR + "OD/shp_merged/Planungsraum.shp", "SCHLUESSEL");
        lsr.readShapeFile(DATADIR + "OD/shp_merged/gemeinden.shp", "NR");
        for (int i = 16; i < 17; i++) {
            TaxiDemandWriter tdw = new TaxiDemandWriter();
            tdw.setMunicipalityMap(lsr.getShapeMap());
            tdw.writeDemand(DATADIR + "/OD/201304" + i + "/", "OD_201304" + i);
            //			tdw.writeODbyZone(DATADIR+"OD/201304"+i+"/od.csv");
        }

        //		for (int i = 25; i <29 ; i++){
        //			TaxiDemandWriter tdw = new TaxiDemandWriter();
        //			tdw.setMunicipalityMap(lsr.getShapeMap());
        //			tdw.writeDemand(DATADIR+"OD/kw9/201302"+i+"/", "OD_201302"+i);
        ////			tdw.writeODbyZone(DATADIR+"OD/201304"+i+"/od.csv");
        //		}
        //		
        //		for (int i = 1; i <4 ; i++){
        //			TaxiDemandWriter tdw = new TaxiDemandWriter();
        //			tdw.setMunicipalityMap(lsr.getShapeMap());
        //			tdw.writeDemand(DATADIR+"OD/kw9/2013030"+i+"/", "OD_2013030"+i);
        ////			tdw.writeODbyZone(DATADIR+"OD/201304"+i+"/od.csv");
        //		}

        System.out.println("trips from TXL " + TaxiDemandWriter.fromTXL);
        System.out.println("trips to TXL " + TaxiDemandWriter.toTXL);
    }


    private void writeODbyZone(String outputFileName)
    {
        Set<Id<Zone>> allZones = new TreeSet<>();
        allZones.addAll(this.dMap.keySet());
        allZones.addAll(this.oMap.keySet());
        BufferedWriter bw = IOUtils.getBufferedWriter(outputFileName);
        try {
            for (Id<Zone> zoneId : allZones) {
                int o = getFromMapOrReturnZero(this.oMap, zoneId);
                int d = getFromMapOrReturnZero(dMap, zoneId);
                String s = zoneId.toString() + "\t" + o + "\t" + d + "\n";
                bw.append(s);
            }

            bw.flush();
            bw.close();
        }
        catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }


    private int getFromMapOrReturnZero(Map<Id<Zone>, Integer> odMap, Id<Zone> zoneId)
    {
        int rv = 0;
        if (odMap.containsKey(zoneId))
            rv = odMap.get(zoneId);
        return rv;
    }


    private void setMunicipalityMap(Map<String, Geometry> municipalityMap)
    {
        this.municipalityMap = municipalityMap;
    }


    TaxiDemandWriter()
    {
        scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        ;
        new MatsimNetworkReader(scenario.getNetwork()).readFile(NETWORKFILE);
        this.network = (NetworkImpl)scenario.getNetwork();
    }


    private void writeDemand(String dirname, String fileNamePrefix)
    {

        population = scenario.getPopulation();
        //		generatePopulation(dirname, fileNamePrefix);
        generatePopulation4to4();
        log.info("Population size: " + population.getPersons().size());
        PopulationWriter populationWriter = new PopulationWriter(scenario.getPopulation(),
                scenario.getNetwork());
        populationWriter.write(dirname + fileNamePrefix + "_SCALE_" + SCALEFACTOR + "_"
                + "plans4to3.xml.gz");
        //		populationWriter.write(dirname+fileNamePrefix+"_SCALE_"+SCALEFACTOR+"_"+"plans.xml.gz");

    }


    private void generatePopulation(String dirname, String fileNamePrefix)
    {

        for (int i = 0; i < 24; i++) {
            String hrstring = String.format("%02d", i);
            DemandParser dp = new DemandParser();
            String currentFileName = dirname + fileNamePrefix + hrstring + "0000.dat";
            read(currentFileName, dp);
            generatePlansForZones(i, dp.getDemand());
            writeODbyZone(dirname + fileNamePrefix + hrstring + "_OD.csv");
            this.oMap.clear();
            this.dMap.clear();

        }

    }


    private void generatePopulation4to4()
    {

        for (int i = 4; i < 24; i++) {
            String hrstring = String.format("%02d", i);
            DemandParser dp = new DemandParser();
            String currentFileName = DATADIR + "/OD/20130416/OD_20130416" + hrstring + "0000.dat";
            read(currentFileName, dp);
            generatePlansForZones(i, dp.getDemand());
            this.oMap.clear();
            this.dMap.clear();

        }
        for (int i = 0; i < 3; i++) {
            String hrstring = String.format("%02d", i);
            DemandParser dp = new DemandParser();
            String currentFileName = DATADIR + "/OD/20130417/OD_20130417" + hrstring + "0000.dat";
            read(currentFileName, dp);
            generatePlansForZones(i + 24, dp.getDemand());
            this.oMap.clear();
            this.dMap.clear();

        }

    }


    private void read(String file, TabularFileHandler handler)
    {
        TabularFileParserConfig config = new TabularFileParserConfig();
        log.info("parsing " + file);
        config.setDelimiterTags(new String[] { "\t", " " });
        config.setFileName(file);
        new TabularFileParser().parse(config, handler);
        log.info("done. (parsing " + file + ")");
    }


    private void generatePlansForZones(int hr, List<TaxiDemandElement> hourlyTaxiDemand)
    {

        double actualAmount = 0.;
        double desiredAmount = 0.;
        for (TaxiDemandElement tde : hourlyTaxiDemand) {
            double amount = tde.getAmount() * SCALEFACTOR;
            desiredAmount += amount;
            amount = Math.floor(amount);
            for (int i = 0; i < amount; i++) {
                Person p;
                Id<Person> pId = Id.create("p" + tde.fromId + "_" + tde.toId + "_hr_" + hr + "_nr_" + i, Person.class);
                p = generatePerson(tde.fromId, tde.toId, pId, hr);
                if (p == null)
                    continue;
                population.addPerson(p);
                incMap(oMap, tde.fromId);
                incMap(dMap, tde.toId);
                actualAmount++;
            }

        }
        long extraAmount = Math.round(desiredAmount - actualAmount);
        Collections.sort(hourlyTaxiDemand);
        for (int i = 0; i <= extraAmount; i++) {
            try {
                TaxiDemandElement tde = hourlyTaxiDemand.get(i % 3);
                Person p;
                Id<Person> pId = Id.create("p" + tde.fromId + "_" + tde.toId + "_hr_" + hr
                        + "_nr_x" + i, Person.class);
                p = generatePerson(tde.fromId, tde.toId, pId, hr);
                if (p == null)
                    continue;
                population.addPerson(p);
                incMap(oMap, tde.fromId);
                incMap(dMap, tde.toId);
            }
            catch (IndexOutOfBoundsException e) {}
        }
    }


    private void incMap(Map<Id<Zone>, Integer> odMap, Id<Zone> fromId)
    {
        Integer val;
        if (odMap.containsKey(fromId))
            val = odMap.get(fromId);
        else
            val = 0;
        val++;
        odMap.put(fromId, val);
    }


    private Person generatePerson(Id from, Id to, Id pId, int hr)
    {
        Person p;
        Plan plan;
        p = population.getFactory().createPerson(pId);
        plan = generatePlan(from, to, hr);
        if (plan == null)
            return null;
        p.addPlan(plan);
        return p;
    }


    private Plan generatePlan(Id from, Id to, int hr)
    {
        Plan plan = population.getFactory().createPlan();
        Coord fromCoord;
        Coord toCoord;
        if (from.equals(BerlinZoneUtils.TXL_LOR_ID)) {

            fromCoord = BerlinZoneUtils.createFromTxlCoord();
            TaxiDemandWriter.fromTXL++;
        }
        else {
            fromCoord = this.shoot(from);
        }
        if (to.equals(BerlinZoneUtils.TXL_LOR_ID)) {

            toCoord = BerlinZoneUtils.createToTxlCoord();
            TaxiDemandWriter.toTXL++;
            if (from.equals(BerlinZoneUtils.TXL_LOR_ID)) {
                fromCoord = this.shoot(from);
                toCoord = this.shoot(to);
                //quite a lot of trips are TXL to TXL
            }
        }
        else {
            toCoord = this.shoot(to);
        }

        if (from.equals(BerlinZoneUtils.SXF_LOR_ID)) {

            fromCoord = BerlinZoneUtils.createSxfCentroid();
            //TaxiDemandWriter.fromTXL++; // SXF?, michalm
        }
        else {
            fromCoord = this.shoot(from);
        }
        if (to.equals(BerlinZoneUtils.SXF_LOR_ID)) {

            toCoord = BerlinZoneUtils.createSxfCentroid();
            //TaxiDemandWriter.toTXL++; // SXF?, michalm
            if (from.equals(BerlinZoneUtils.SXF_LOR_ID)) {
                fromCoord = this.shoot(from);
                toCoord = this.shoot(to);
                //quite a lot of trips are TXL to TXL
            }
        }
        else {
            toCoord = this.shoot(to);
        }

        if (fromCoord == null)
            return null;
        if (toCoord == null)
            return null;

        Link fromLink = network.getNearestLinkExactly(fromCoord);
        Link toLink = network.getNearestLinkExactly(toCoord);

        double activityStart = Math.round(hr * 3600. + rnd.nextDouble() * 3600.);
        //		if (hr == 27 )  activityStart = Math.round(hr * 3600. + rnd.nextDouble() * 1200.);
        plan.addActivity(this.addActivity("home", 0.0, activityStart, fromLink));
        plan.addLeg(this.addLeg(activityStart, TaxiUtils.TAXI_MODE, fromLink, toLink));
        plan.addActivity(this.addActivity("work", toLink));

        return plan;
    }


    private Activity addActivity(String type, Double start, Double end, Link link)
    {

        Activity activity = population.getFactory().createActivityFromLinkId(type, link.getId());
        activity.setStartTime(start);
        activity.setEndTime(end);
        return activity;
    }


    private Activity addActivity(String type, Link link)
    {

        Activity activity = population.getFactory().createActivityFromLinkId(type, link.getId());

        return activity;
    }


    private Leg addLeg(double departure, String mode, Link fromLink, Link toLink)
    {
        Leg leg = population.getFactory().createLeg(mode);
        leg.setDepartureTime(departure);
        leg.setRoute(new LinkNetworkRouteImpl(fromLink.getId(), toLink.getId()));
        return leg;
    }


    private Coord shoot(Id zoneId)
    {
        Point point;
        //		log.info("Zone" + zoneId);
        if (this.municipalityMap.containsKey(zoneId.toString())) {
            point = getRandomPointInFeature(this.rnd, this.municipalityMap.get(zoneId.toString()));
            Coord coord = new Coord(point.getX(), point.getY());
            return BerlinZoneUtils.ZONE_TO_NETWORK_COORD_TRANSFORMATION.transform(coord);
        }
        else {
            log.error(zoneId.toString() + "does not exist in shapedata");
            return null;
        }
    }


    public static Point getRandomPointInFeature(Random rnd, Geometry g)
    {
        Point p = null;
        double x, y;
        do {
            x = g.getEnvelopeInternal().getMinX() + rnd.nextDouble()
                    * (g.getEnvelopeInternal().getMaxX() - g.getEnvelopeInternal().getMinX());
            y = g.getEnvelopeInternal().getMinY() + rnd.nextDouble()
                    * (g.getEnvelopeInternal().getMaxY() - g.getEnvelopeInternal().getMinY());
            p = MGC.xy2Point(x, y);
        }
        while (!g.contains(p));
        return p;
    }

}


class TaxiDemandElement
    implements Comparable<TaxiDemandElement>
{
    Id<Zone> fromId;
    Id<Zone> toId;
    int amount;


    TaxiDemandElement(Id<Zone> from, Id<Zone> to, int amount)
    {
        this.fromId = from;
        this.toId = to;
        this.amount = amount;
    }


    public Id<Zone> getFromId()
    {
        return fromId;
    }


    public Id<Zone> getToId()
    {
        return toId;
    }


    public int getAmount()
    {
        return amount;
    }


    @Override
    public int compareTo(TaxiDemandElement arg0)
    {
        Integer amo = amount;
        return amo.compareTo(arg0.getAmount());
    }

}


class DemandParser
    implements TabularFileHandler
{

    List<TaxiDemandElement> demand = new ArrayList<TaxiDemandElement>();


    @Override
    public void startRow(String[] row)
    {
        demand.add(new TaxiDemandElement(Id.create(row[0], Zone.class), Id.create(row[1], Zone.class), Integer
                .parseInt(row[2])));
    }


    public List<TaxiDemandElement> getDemand()
    {
        return demand;
    }

}
