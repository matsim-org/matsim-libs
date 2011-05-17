package playground.michalm.demand;

import java.io.*;
import java.util.*;

import javax.naming.*;
import javax.xml.parsers.*;

import playground.michalm.demand.Zone.Act;
import playground.michalm.demand.Zone.Group;

import org.apache.log4j.*;
import org.geotools.feature.*;
import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.network.*;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.network.*;
import org.matsim.core.scenario.*;
import org.matsim.core.utils.geometry.geotools.*;
import org.matsim.core.utils.misc.*;
import org.xml.sax.*;

import cern.jet.random.*;
import cern.jet.random.engine.*;

import com.vividsolutions.jts.geom.*;


public class DemandGenerator
{
    private static final Logger log = Logger.getLogger(DemandGenerator.class);

    private Uniform uniform = new Uniform(new MersenneTwister(new Date()));

    private Scenario scenario;
    private PopulationFactory pf;

    private Map<Id, Zone> zones = new TreeMap<Id, Zone>();


    public DemandGenerator(String networkFileName, String zonesXMLFileName, String zonesShpFileName)
        throws IOException, SAXException, ParserConfigurationException

    {
        scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        pf = scenario.getPopulation().getFactory();
        MatsimNetworkReader nr = new MatsimNetworkReader(scenario);
        nr.readFile(networkFileName);

        ZoneXMLReader xmlReader = new ZoneXMLReader(scenario);
        xmlReader.parse(zonesXMLFileName);
        zones = xmlReader.getZones();

        ZoneShpReader shpReader = new ZoneShpReader(scenario, zones);
        shpReader.readZones(zonesShpFileName);
    }


    public void generate()
    {
        // activityPlaces - for random generation
        Distribution<Zone> sDistrib = new Distribution<Zone>();
        Distribution<Zone> wDistrib = new Distribution<Zone>();
        Distribution<Zone> lDistrib = new Distribution<Zone>();

        for (Zone z : zones.values()) {
            sDistrib.add(z, z.getActPlaces(Act.S).intValue());
            wDistrib.add(z, z.getActPlaces(Act.W).intValue());
            lDistrib.add(z, z.getActPlaces(Act.L).intValue());
        }

        Population popul = scenario.getPopulation();
        PopulationFactory pf = popul.getFactory();

        for (Zone z : zones.values()) {
            // S - students: h-s-h-l-h
            int sCount = z.getGroupSize(Group.S);
            for (int s = 0; s < sCount; s++) {
                Plan plan = createPlan();

                int timeShift = uniform.nextIntFromTo(-3600, 3600);

                Coord home = getRandomCoordInZone(z);
                Activity act = createActivity(plan, "h", home);
                act.setEndTime(8 * 3600 + timeShift);

                plan.addLeg(pf.createLeg(TransportMode.car));

                act = createActivity(plan, "s", sDistrib);
                act.setStartTime(9 * 3600 + timeShift);
                act.setEndTime(15 * 3600 + timeShift);

                plan.addLeg(pf.createLeg(TransportMode.car));

                act = createActivity(plan, "l", lDistrib);
                act.setStartTime(16 * 3600 + timeShift);
                act.setEndTime(18 * 3600 + timeShift);

                plan.addLeg(pf.createLeg(TransportMode.car));

                act = createActivity(plan, "h", home);
                act.setStartTime(19 * 3600 + timeShift);
            }

            // W - workers: h-w-l-h
            int wCount = z.getGroupSize(Group.W);
            for (int w = 0; w < wCount; w++) {
                Plan plan = createPlan();

                int timeShift = uniform.nextIntFromTo(-3600, 7200);

                if (uniform.nextDouble() > 0.5) {// 50%
                    Coord home = getRandomCoordInZone(z);
                    Activity act = createActivity(plan, "h", home);
                    act.setEndTime(7 * 3600 + timeShift);

                    plan.addLeg(pf.createLeg(TransportMode.car));

                    act = createActivity(plan, "w", wDistrib);
                    act.setStartTime(8 * 3600 + timeShift);
                    act.setEndTime(12 * 3600 + timeShift);

                    plan.addLeg(pf.createLeg(TransportMode.car));

                    act = createActivity(plan, "l", lDistrib);
                    act.setStartTime(13 * 3600 + timeShift);
                    act.setEndTime(14 * 3600 + timeShift);

                    plan.addLeg(pf.createLeg(TransportMode.car));

                    act = createActivity(plan, "w", wDistrib);
                    act.setStartTime(15 * 3600 + timeShift);
                    act.setEndTime(18 * 3600 + timeShift);

                    plan.addLeg(pf.createLeg(TransportMode.car));

                    act = createActivity(plan, "h", home);
                    act.setStartTime(19 * 3600 + timeShift);
                }
                else {
                    Coord home = getRandomCoordInZone(z);
                    Activity act = createActivity(plan, "h", home);
                    act.setEndTime(8 * 3600 + timeShift);

                    plan.addLeg(pf.createLeg(TransportMode.car));

                    act = createActivity(plan, "w", wDistrib);
                    act.setStartTime(9 * 3600 + timeShift);
                    act.setEndTime(17 * 3600 + timeShift);

                    plan.addLeg(pf.createLeg(TransportMode.car));

                    act = createActivity(plan, "l", home);
                    act.setStartTime(18 * 3600 + timeShift);
                    act.setEndTime(19 * 3600 + timeShift);

                    plan.addLeg(pf.createLeg(TransportMode.car));

                    act = createActivity(plan, "h", home);
                    act.setStartTime(20 * 3600 + timeShift);
                }
            }

            // O - others: h-l-h
            int oCount = z.getGroupSize(Group.O);
            for (int o = 0; o < oCount; o++) {
                Plan plan = createPlan();

                int timeShift = uniform.nextIntFromTo(-3600, 3600);

                Coord home = getRandomCoordInZone(z);
                Activity act = createActivity(plan, "h", home);
                act.setEndTime(11 * 3600 + timeShift);

                plan.addLeg(pf.createLeg(TransportMode.car));

                act = createActivity(plan, "l", lDistrib);
                act.setStartTime(12 * 3600 + timeShift);
                act.setEndTime(15 * 3600 + timeShift);

                plan.addLeg(pf.createLeg(TransportMode.car));

                act = createActivity(plan, "h", home);
                act.setStartTime(16 * 3600 + timeShift);
            }
        }
    }


    public void write(String plansfileName)
    {
        new PopulationWriter(scenario.getPopulation(), scenario.getNetwork())
                .writeV4(plansfileName);
        log.info("Generated population written to: " + plansfileName);
    }


    private Activity createActivity(Plan plan, String actType, Coord coord)
    {
        NetworkImpl network = (NetworkImpl)scenario.getNetwork();
        Link link = network.getNearestLink(coord);
        Activity act = pf.createActivityFromLinkId(actType, link.getId());
        plan.addActivity(act);
        return act;
    }


    private Activity createActivity(Plan plan, String actType, Distribution<Zone> zoneDistrib)
    {
        Zone zone = zoneDistrib.draw();
        return createActivity(plan, actType, getRandomCoordInZone(zone));
    }


    private Coord getRandomCoordInZone(Zone zone)
    {
        Feature ft = zone.getZonePolygon();

        Envelope bounds = ft.getBounds();
        double minX = bounds.getMinX();
        double maxX = bounds.getMaxX();
        double minY = bounds.getMinY();
        double maxY = bounds.getMaxY();

        Geometry geometry = ft.getDefaultGeometry();
        Point p = null;
        do {
            double x = uniform.nextDoubleFromTo(minX, maxX);
            double y = uniform.nextDoubleFromTo(minY, maxY);
            p = MGC.xy2Point(x, y);
        }
        while (!geometry.contains(p));

        return scenario.createCoord(p.getX(), p.getY());
    }


    private int id = 0;


    private Plan createPlan()
    {
        Person person = pf.createPerson(scenario.createId(Integer.toString(id++)));
        scenario.getPopulation().addPerson(person);

        Plan plan = pf.createPlan();
        person.addPlan(plan);
        return plan;
    }


    public static void main(String[] args)
        throws ConfigurationException, IOException, SAXException, ParserConfigurationException
    {
        String dirName;

        String networkFileName;
        String zonesXMLFileName;
        String zonesShpFileName;
        String plansFileName;

        if (args.length == 1 && args[0].equals("test")) {// for testing
        // dirName = "D:\\PP-dyplomy\\2010_11-mgr\\burkat_andrzej\\siec1\\";
        // networkFileName = dirName + "network.xml";
        // zonesXMLFileName = dirName + "zones1.xml";
        // zonesShpFileName = dirName + "zones1.shp";
        // plansFileName = dirName + "plans1.xml";

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

            dirName = "D:\\PP-dyplomy\\2010_11-mgr\\gintrowicz_marcin\\NSE\\";
            networkFileName = dirName + "network2.xml";
            zonesXMLFileName = dirName + "zones2.xml";
            zonesShpFileName = dirName + "zone.shp";
            plansFileName = dirName + "plans.xml";
        }
        else if (args.length == 5) {
            dirName = args[0];
            networkFileName = dirName + args[1];
            zonesXMLFileName = dirName + args[2];
            zonesShpFileName = dirName + args[3];
            plansFileName = dirName + args[4];
        }
        else {
            throw new IllegalArgumentException("Incorrect program arguments: "
                    + Arrays.toString(args));
        }

        DemandGenerator dg = new DemandGenerator(networkFileName, zonesXMLFileName,
                zonesShpFileName);
        dg.generate();
        dg.write(plansFileName);
    }
}
