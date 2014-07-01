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

package playground.jbischoff.taxi.berlin.supply;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Random;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.data.VehicleImpl;
import org.matsim.contrib.dvrp.data.file.VehicleWriter;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;
import org.matsim.matrices.Matrices;
import org.matsim.matrices.Matrix;
import org.matsim.matrices.MatsimMatricesReader;

import pl.poznan.put.util.random.WeightedRandomSelection;
import playground.jbischoff.taxi.berlin.demand.TaxiDemandWriter;
import playground.michalm.zone.Zone;
import playground.michalm.zone.Zones;

import com.vividsolutions.jts.geom.Point;


public class BerlinTaxiVehicleCreatorV3
{
    private static final String DATADIR = "C:/local_jb/data/";
    private static final String TAXISOVERTIME = DATADIR + "taxi_berlin/2013/vehicles/taxisweekly.csv";
    private static final String STATUSMATRIX = DATADIR + "taxi_berlin/2014/status/statusMatrixAvg.xml";
    private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private final static double EVSHARE = 0.0;
    private final static double MAXTIME = 14.0 * 3600;

    private static final Logger log = Logger.getLogger(BerlinTaxiVehicleCreatorV3.class);
    private static final double PAXPERCAR = 4;
    private static final Random RND = new Random(42);
    private static final String NETWORKFILE = DATADIR
            + "scenarios/2014_05_basic_scenario_v3/berlin_brb.xml";
    private static final String ZONESSHP = DATADIR + "shp_merged/zones.shp";
    private static final String ZONESXML = DATADIR + "shp_merged/zones.xml";
    private CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(
            "EPSG:25833", TransformationFactory.DHDN_GK4);
    private NetworkImpl network;
    private Map<Id, Zone> zones;
    List<Vehicle> vehicles;

    /**
     * @param args
     */
    private Map<Date, Integer> taxisOverTime;
    private Map<Date, Integer> taxisOverTimeHourlyAverage = new TreeMap<Date, Integer>();
    private WeightedRandomSelection<Id> wrs;


    public static void main(String[] args)
        throws ParseException
    {
        BerlinTaxiVehicleCreatorV3 btv = new BerlinTaxiVehicleCreatorV3();
        btv.readTaxisOverTime(TAXISOVERTIME);
        btv.createAverages(SDF.parse("2013-04-16 04:00:00"), SDF.parse("2013-04-17 04:00:00"));
        btv.prepareNetwork();
        btv.prepareMatrices();
        btv.createVehicles();
        btv.writeVehicles();
    }


    private void writeVehicles()
    {
        new VehicleWriter(vehicles).write(DATADIR
                + "scenarios/2014_05_basic_scenario_v3/taxis4to4_EV" + EVSHARE + ".xml");

    }


    private void prepareNetwork()
    {
        Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        new MatsimNetworkReader(sc).readFile(NETWORKFILE);
        this.network = (NetworkImpl)sc.getNetwork();
        this.zones = Zones.readZones(sc, ZONESXML, ZONESSHP);
   
    }


    @SuppressWarnings("deprecation")
    private void createVehicles()
    {
        Queue<Vehicle> vehiclesQueue = new LinkedList<Vehicle>();
        vehicles = new ArrayList<Vehicle>();
        Integer startDate = null;
        Date currentDate;
        int currentHr = 0;
        for (Entry<Date, Integer> e : taxisOverTimeHourlyAverage.entrySet()) {
            currentDate = e.getKey();
            if (startDate == null) {
                startDate = currentDate.getDate();
            }
            int dayOffset = currentDate.getDate() - startDate;
            currentHr = currentDate.getHours() + 24 * dayOffset;

            removeOverTimeVehicles(vehiclesQueue, currentHr);
            int diff = e.getValue() - vehiclesQueue.size();
            if (diff > 0) {
                for (int i = 0; i < diff; i++) {
                    Id lorId = this.wrs.select();
                    String vehId = "t_" + lorId + "_" + currentHr + "_" + i;
                    if (RND.nextDouble() < EVSHARE)
                        vehId = "e" + vehId;
                    int startTime = currentHr * 3600 + RND.nextInt(3600);
                    Vehicle veh = createTaxiFromLor(lorId, new IdImpl(vehId), startTime);
                    vehiclesQueue.add(veh);
                }

            }
            else if (diff < 0) {
                for (int i = 0; i > diff; i--) {
                    Vehicle veh = vehiclesQueue.poll();
                    int endTime = currentHr * 3600 + RND.nextInt(3600);
                    veh.setT1(endTime);
                    vehicles.add(veh);
                }
            }

        }
        for (int i = 0; i < vehiclesQueue.size(); i++) {
            Vehicle veh = vehiclesQueue.poll();
            int endTime = currentHr * 3600 + RND.nextInt(3600);
            veh.setT1(endTime);
            vehicles.add(veh);
        }
    }


    private void removeOverTimeVehicles(Queue<Vehicle> vehiclesQueue, double currentHr)
    {
        boolean lastVehicleOverdue = true;
        while (lastVehicleOverdue) {
            Vehicle lastVeh = vehiclesQueue.peek();
            if (lastVeh == null)
                return;
            if ( (lastVeh.getT0() + MAXTIME) < currentHr * 3600) {

                log.info(lastVeh.getT0() + MAXTIME + " " + currentHr * 3600 + " " + currentHr);
                vehiclesQueue.remove(lastVeh);
                lastVeh.setT1(lastVeh.getT0() + MAXTIME);
                this.vehicles.add(lastVeh);
            }
            else {
                lastVehicleOverdue = false;
            }
        }

    }


    private Vehicle createTaxiFromLor(Id lorId, Id vid, int t0)
    {
        Link link;
        link = getRandomLinkInLor(lorId);
        Vehicle v = new VehicleImpl(vid, link, PAXPERCAR, t0, 0);
        return v;
    }


    private Link getRandomLinkInLor(Id lorId)
    {
        log.info(lorId);
        Id id = lorId;
        if (lorId.toString().length() == 7)
            id = new IdImpl("0" + lorId.toString());
        log.info(id);
        Point p = TaxiDemandWriter.getRandomPointInFeature(RND, this.zones.get(id)
                .getMultiPolygon());
        Coord coord = ct.transform(new CoordImpl(p.getX(), p.getY()));
        Link link = network.getNearestLinkExactly(coord);

        return link;
    }


    private void prepareMatrices()
    {
        Matrices statusMatrix = new Matrices();
        ScenarioImpl sc = (ScenarioImpl)ScenarioUtils.createScenario(ConfigUtils.createConfig());
        new MatsimMatricesReader(statusMatrix, sc).readFile(STATUSMATRIX);
        this.wrs = new WeightedRandomSelection<Id>();
        Matrix avestatus = statusMatrix.getMatrix("avg");

        for (Entry<Id, ArrayList<org.matsim.matrices.Entry>> fromLOR : avestatus.getFromLocations()
                .entrySet()) {
            Id lorId = fromLOR.getKey();
            double sum = 0;
            for (org.matsim.matrices.Entry v : fromLOR.getValue()) {
                sum += v.getValue();
            }
            this.wrs.add(lorId, sum);
        }

    }


    private void read(String file, TabularFileHandler handler)
    {
        TabularFileParserConfig config = new TabularFileParserConfig();
        log.info("parsing " + file);
        config.setDelimiterTags(new String[] { "\t" });
        config.setFileName(file);
        new TabularFileParser().parse(config, handler);
        log.info("done. (parsing " + file + ")");
    }


    @SuppressWarnings("deprecation")
    private void createAverages(Date start, Date end)
    {
        Date currentTime = start;
        int secs = 0;
        double sum = 0.;
        do {
            sum += this.taxisOverTime.get(currentTime);
            secs++;
            if ( (currentTime.getMinutes() == 59) && (currentTime.getSeconds() == 59)) {
                double average = sum / secs;
                this.taxisOverTimeHourlyAverage.put(currentTime, (int)Math.round(average));
                secs = 0;
                sum = 0;
            }
            else {}
            currentTime = getNextTime(currentTime);
        }
        while (end.after(currentTime));
        System.out.println(this.taxisOverTimeHourlyAverage);
    }


    private Date getNextTime(Date currentTime)
    {
        Calendar cal = Calendar.getInstance();
        cal.setTime(currentTime);
        cal.add(Calendar.SECOND, 1);
        return cal.getTime();
    }


    private void readTaxisOverTime(String taxisovertime2)
    {
        TaxisOverTimeParser topp = new TaxisOverTimeParser();
        read(TAXISOVERTIME, topp);
        this.taxisOverTime = topp.getTaxisOverTime();

    }

}


class TaxisOverTimeParser
    implements TabularFileHandler
{

    private Map<Date, Integer> taxisOverTime;
    SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");


    public TaxisOverTimeParser()
    {
        this.taxisOverTime = new TreeMap<Date, Integer>();
    }


    @Override
    public void startRow(String[] row)
    {
        try {
            this.taxisOverTime.put(SDF.parse(row[0]), Integer.parseInt(row[1]));
        }
        catch (NumberFormatException e) {
            e.printStackTrace();
        }
        catch (ParseException e) {
            e.printStackTrace();
        }
    }


    public Map<Date, Integer> getTaxisOverTime()
    {
        return taxisOverTime;
    }

}