package playground.jbischoff.taxi.berlin.supply;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

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
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;

import playground.jbischoff.taxi.berlin.demand.LorShapeReader;
import playground.jbischoff.taxi.berlin.demand.TaxiDemandWriter;
import playground.michalm.berlin.BerlinZoneUtils;
import playground.michalm.zone.Zone;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;


public class BerlinVehicleGenerator
{

    private Map<String, Geometry> shapedata;
    private List<TaxiDemandPerLor> demandPerLor;
    private static Logger log = Logger.getLogger(BerlinVehicleGenerator.class);
    private Random rnd = new Random(42);
    private Scenario scenario;
    private final static String NETWORKFILE = "/Users/jb/shared-svn/projects/sustainability-w-michal-and-dlr/data/network/berlin_brb.xml.gz";
    private final static int PAXPERCAR = 4;
    private final static int T0 = 0;
    private final static int T1 = 30 * 60 * 60;
    private final static int AMOUNTOFTAXIS = 3000;
    private NetworkImpl network;

    public static void main(String[] args)
    {

        for (int i = 15; i < 22; i++) {
            BerlinVehicleGenerator bvr = new BerlinVehicleGenerator();

            bvr.readInputData("/Users/jb/shared-svn/projects/sustainability-w-michal-and-dlr/data/OD/201304"
                    + i + "/od.csv");
            List<Vehicle> vehicleList = bvr.createVehicles(AMOUNTOFTAXIS);
            new VehicleWriter(vehicleList)
                    .write("/Users/jb/shared-svn/projects/sustainability-w-michal-and-dlr/data/OD/201304"
                            + i + "/vehicles.xml");
        }
    }


    private List<Vehicle> createVehicles(int taxiAmount)
    {
        List<Vehicle> vehicles = new ArrayList<Vehicle>();
        int amountOfTrips = this.calculateTripAmount();

        for (TaxiDemandPerLor dpl : this.demandPerLor) {
            if (dpl.getFromLor() == 0)
                continue;
            double shareOfTripsFromHere = (double)dpl.getFromLor() / (double)amountOfTrips;
            long taxiAmountFromHere = Math.round(taxiAmount * shareOfTripsFromHere);
            for (int i = 0; i < taxiAmountFromHere; i++) {
                Id<Vehicle> vid = Id.create("t_" + dpl.getFromId() + "_" + i, Vehicle.class);
                vehicles.add(createTaxiFromLor(dpl.getFromId(), vid));

            }
        }

        return vehicles;
    }


    private Vehicle createTaxiFromLor(Id<Zone> lorId, Id<Vehicle> vid)
    {
        Link link = getRandomLinkInLor(lorId);
        Vehicle v = new VehicleImpl(vid, link, PAXPERCAR, T0, T1);
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


    private int calculateTripAmount()
    {

        int amount = 0;
        for (TaxiDemandPerLor tdpl : this.demandPerLor) {
            amount += tdpl.getFromLor();
        }

        return amount;
    }


    private void readInputData(String tabularDemandPerLor)
    {
        LorShapeReader lsr = new LorShapeReader();
        lsr.readShapeFile(
                "/Users/jb/shared-svn/projects/sustainability-w-michal-and-dlr/data/OD/shp_merged/Planungsraum.shp",
                "SCHLUESSEL");
        lsr.readShapeFile(
                "/Users/jb/shared-svn/projects/sustainability-w-michal-and-dlr/data/OD/shp_merged/gemeinden.shp",
                "NR");
        this.shapedata = lsr.getShapeMap();
        DemandPerLorParser dpl = new DemandPerLorParser();
        this.read(tabularDemandPerLor, dpl);
        this.demandPerLor = dpl.getDemand();

        scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        ;
        new MatsimNetworkReader(scenario).readFile(NETWORKFILE);
        this.network = (NetworkImpl)scenario.getNetwork();

    }


    public static void read(String file, TabularFileHandler handler)
    {
        TabularFileParserConfig config = new TabularFileParserConfig();
        log.info("parsing " + file);
        config.setDelimiterTags(new String[] { "\t" });
        config.setFileName(file);
        new TabularFileParser().parse(config, handler);
        log.info("done. (parsing " + file + ")");
    }

}


class TaxiDemandPerLor
    implements Comparable<TaxiDemandPerLor>
{
    private Id<Zone> lorNo;
    private int toLor;
    private int fromLor;


    TaxiDemandPerLor(Id<Zone> from, int toLor, int fromLor)
    {
        this.lorNo = from;
        this.toLor = toLor;
        this.fromLor = fromLor;
    }


    public Id<Zone> getFromId()
    {
        return lorNo;
    }


    public int getToLor()
    {
        return toLor;
    }


    public int getFromLor()
    {
        return fromLor;
    }


    @Override
    public int compareTo(TaxiDemandPerLor arg0)
    {
        Integer i = fromLor;
        return i.compareTo(arg0.getFromLor());
    }

}


class DemandPerLorParser
    implements TabularFileHandler
{

    private List<TaxiDemandPerLor> demand = new ArrayList<TaxiDemandPerLor>();


    @Override
    public void startRow(String[] row)
    {
        demand.add(new TaxiDemandPerLor(Id.create(row[0], Zone.class), Integer.parseInt(row[1]), Integer
                .parseInt(row[2])));
    }


    public List<TaxiDemandPerLor> getDemand()
    {
        return demand;
    }

}
