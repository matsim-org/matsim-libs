package playground.jbischoff.taxi.berlin.supply;

import java.util.ArrayList;
import java.util.Collections;
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
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;

import playground.jbischoff.taxi.berlin.demand.LorShapeReader;
import playground.jbischoff.taxi.berlin.demand.TaxiDemandWriter;
import playground.michalm.berlin.BerlinZoneUtils;
import playground.michalm.zone.Zone;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;


public class BerlinVehicleGeneratorV2
{

    private List<VData> vdata;
    private Queue<VData> vqueue;
    private Map<Integer, Integer> taxisOverTime;
    private Map<String, Geometry> shapedata;
    private static Logger log = Logger.getLogger(BerlinVehicleGeneratorV2.class);
    private Random rnd = new Random(42);
    private Scenario scenario;
    private static final String DATADIR = "C:/local_jb/data/";
    private final static String NETWORKFILE = DATADIR + "network/berlin_brb.xml.gz";
    private final static int PAXPERCAR = 4;
    private final static double EVSHARE = 1.0;

    private NetworkImpl network;


    public static void main(String[] args)
    {
        BerlinVehicleGeneratorV2 bvr = new BerlinVehicleGeneratorV2();
        bvr.readInputData();
        bvr.createTaxis(28, 52);
        List<Vehicle> taxiList = bvr.createVehicles();
        new VehicleWriter(taxiList).write(DATADIR + "/OD/demandperLorHr/taxis4to4_EV" + EVSHARE
                + ".xml");

    }


    private List<Vehicle> createVehicles()
    {
        List<Vehicle> vehicles = new ArrayList<Vehicle>();

        for (VData v : vdata) {
            String vid = v.vid.toString();
            if (rnd.nextDouble() < EVSHARE)
                vid = "e" + vid;
            vehicles.add(createTaxiFromLor(v.lor, Id.create(vid, Vehicle.class), v.t0, v.t1));
        }
        return vehicles;
    }


    private void createTaxis(int starthr, int endhr)
    {
        System.out.println(this.taxisOverTime);
        vqueue = new LinkedList<VData>();
        vdata = new ArrayList<VData>();
        for (int i = starthr; i <= endhr; i++) {
            List<TaxiDemandPerLor> demandPerLor;
            DemandPerLorParser dpl = new DemandPerLorParser();
            Integer day = (i / 24) + 15;
            Integer hr = i % 24;
            String hrstring = String.format("%02d", hr);

            String filename = DATADIR + "OD/demandperLorHr/OD_201304" + day.toString() + hrstring
                    + "_OD.csv";
            BerlinVehicleGenerator.read(filename, dpl);
            demandPerLor = dpl.getDemand();

            int currentVeh = this.taxisOverTime.get(i);
            if (i == starthr)
                addStartVehicles(currentVeh, demandPerLor, hr);

            else {
                int lastVeh = this.taxisOverTime.get(i - 1);
                if (currentVeh > lastVeh) {
                    addTaxis(currentVeh - lastVeh, demandPerLor, hr);
                    System.out.println(currentVeh + ":" + lastVeh + " a:  " + vqueue.size());

                }
                else if (currentVeh < lastVeh) {
                    if (! (hr > 24))
                        removeTaxis(lastVeh - currentVeh, hr);
                    System.out.println(currentVeh + ":" + lastVeh + " r:  " + vqueue.size());
                }

            }
        }

        removeTaxis(vqueue.size(), endhr % 24 + 1);
    }


    private void removeTaxis(int amount, int hr)
    {
        for (int i = 0; i < amount; i++) {

            VData v = vqueue.remove();
            int offset = rnd.nextInt(3600);
            if (v.t0 / 3600 > hr)
                offset = offset + 24 * 3600;
            v.t1 = hr * 3600 + offset;
            vdata.add(v);

        }
    }


    private void addTaxis(int amount, List<TaxiDemandPerLor> demandPerLor, Integer hr)
    {
        int amountOfTrips = this.calculateTripAmount(demandPerLor);

        int count = 0;
        for (TaxiDemandPerLor dpl : demandPerLor) {
            if (dpl.getFromLor() == 0)
                continue;
            double shareOfTripsFromHere = (double)dpl.getFromLor() / (double)amountOfTrips;
            long taxiAmountFromHere = Math.round(amount * shareOfTripsFromHere);
            for (int i = 0; i < taxiAmountFromHere; i++) {
                Id<Vehicle> vid = Id.create("t_" + dpl.getFromId() + "_" + (hr % 24) + "_" + i, Vehicle.class);
                VData v = new VData();
                v.vid = vid;
                v.lor = dpl.getFromId();
                int start = (hr % 24) * 3600 + rnd.nextInt(3600);
                v.t0 = start;
                vqueue.add(v);
                count++;
            }
        }
        Collections.sort(demandPerLor);

        TaxiDemandPerLor dpl[] = { Collections.max(demandPerLor), demandPerLor.get(1) };
        int i = 0;
        int rest = amount - count;
        while (rest > 0) {
            Id<Vehicle> vid = Id.create("t_" + dpl[i % 2].getFromId() + "_" + (hr % 24) + "_x" + i, Vehicle.class);
            VData v = new VData();
            v.lor = dpl[i % 2].getFromId();
            v.vid = vid;
            v.t0 = (hr % 24) * 3600 + rnd.nextInt(3600);
            i++;
            rest--;
            vqueue.add(v);

        }
        System.out.println( (hr % 24) + ": " + i);

    }


    private void addStartVehicles(int currentVeh, List<TaxiDemandPerLor> demandPerLor, int hr)
    {

        int amountOfTrips = this.calculateTripAmount(demandPerLor);
        int count = 0;

        for (TaxiDemandPerLor dpl : demandPerLor) {
            if (dpl.getFromLor() == 0)
                continue;
            double shareOfTripsFromHere = (double)dpl.getFromLor() / (double)amountOfTrips;
            long taxiAmountFromHere = Math.round(currentVeh * shareOfTripsFromHere);
            for (int i = 0; i < taxiAmountFromHere; i++) {
                Id<Vehicle> vid = Id.create("t_" + dpl.getFromId() + "_" + hr + "_" + i, Vehicle.class);
                VData v = new VData(vid, dpl.getFromId());
                int start = (hr % 24) * 3600 - 1800;
                int end = start + 7200 + rnd.nextInt(16 * 3600);
                v.t0 = start;
                v.t1 = end;
                vdata.add(v);
                count++;
            }
        }
        Collections.sort(demandPerLor);
        TaxiDemandPerLor dpl[] = { Collections.max(demandPerLor), demandPerLor.get(1) };
        int i = 0;
        int rest = currentVeh - count;
        while (rest > 0) {
            Id<Vehicle> vid = Id.create("t_" + dpl[i % 2].getFromId() + "_" + (hr % 24) + "_x" + i, Vehicle.class);
            VData v = new VData();
            i++;
            v.lor = dpl[i % 2].getFromId();
            v.vid = vid;
            int start = (hr % 24) * 3600 + rnd.nextInt(3600);
            int end = start + rnd.nextInt(8 * 3600);
            v.t0 = start;
            v.t1 = end;
            rest--;
            vdata.add(v);

        }
        System.out.println( (hr % 24) + ": " + i);

    }


    private Vehicle createTaxiFromLor(Id<Zone> lorId, Id<Vehicle> vid, int t0, int t1)
    {
        Link link;
        if (lorId.equals(BerlinZoneUtils.TXL_LOR_ID)) {

            link = network.getLinks().get(BerlinZoneUtils.FROM_TXL_LINK_ID);

        }
        else {
            link = getRandomLinkInLor(lorId);
        }

        Vehicle v = new VehicleImpl(vid, link, PAXPERCAR, t0, t1);

        return v;
    }


    private Link getRandomLinkInLor(Id<Zone> lorId)
    {
        Point p = TaxiDemandWriter.getRandomPointInFeature(this.rnd,
                this.shapedata.get(lorId.toString()));
        Coord coord = BerlinZoneUtils.ZONE_TO_NETWORK_COORD_TRANSFORMATION.transform(new CoordImpl(p.getX(), p.getY()));
        Link link = network.getNearestLinkExactly(coord);

        return link;
    }


    private int calculateTripAmount(List<TaxiDemandPerLor> demandPerLor)
    {

        int amount = 0;
        for (TaxiDemandPerLor tdpl : demandPerLor) {
            amount += tdpl.getFromLor();
        }

        return amount;
    }


    private void readInputData()
    {
        LorShapeReader lsr = new LorShapeReader();
        lsr.readShapeFile(DATADIR + "OD/shp_merged/Planungsraum.shp", "SCHLUESSEL");
        lsr.readShapeFile(DATADIR + "OD/shp_merged/gemeinden.shp", "NR");
        this.shapedata = lsr.getShapeMap();

        scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        ;
        new MatsimNetworkReader(scenario).readFile(NETWORKFILE);
        this.network = (NetworkImpl)scenario.getNetwork();
        TaxiTimeParser ttp = new TaxiTimeParser();
        BerlinVehicleGenerator.read(DATADIR + "OD/vehicles/weekly/taxisOverTimeWeek.csv", ttp);
        this.taxisOverTime = calculateAverageTaxisOverTime(ttp.getTaxisOverTime());

    }


    private Map<Integer, Integer> calculateAverageTaxisOverTime(Map<Integer, Integer> taxisOverTime2)
    {

        Map<Integer, Integer> average = new TreeMap<Integer, Integer>();

        int sum = 0;
        for (Entry<Integer, Integer> e : taxisOverTime2.entrySet()) {
            sum += e.getValue();
            if (e.getKey() % 3600 == 0) {
                average.put(e.getKey() / 3600, sum / 3600);
                sum = 0;
            }
        }

        return average;
    }
}


class TaxiTimeParser
    implements TabularFileHandler
{

    private Map<Integer, Integer> taxisOverTime = new TreeMap<Integer, Integer>();


    @Override
    public void startRow(String[] row)
    {
        taxisOverTime.put(Integer.parseInt(row[0]), Integer.parseInt(row[1]));
    }


    public Map<Integer, Integer> getTaxisOverTime()
    {
        return taxisOverTime;
    }

}


class VData
    implements Comparable<VData>
{

    Id<Vehicle> vid;
    Integer t0;
    Integer t1;
    Id<Zone> lor;


    VData(Id<Vehicle> vid, Id<Zone> lorId)
    {
        this.vid = vid;
        this.lor = lorId;
    }


    public VData()
    {
        // TODO Auto-generated constructor stub
    }


    @Override
    public int compareTo(VData arg0)
    {
        return t0.compareTo(arg0.t0);
    }

}
