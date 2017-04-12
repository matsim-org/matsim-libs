package playground.johannes.studies.drive;

import org.apache.log4j.Logger;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.joda.time.LocalTime;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.common.gis.CRSUtils;
import org.matsim.contrib.common.util.XORShiftRandom;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.FacilitiesReaderMatsimV1;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import playground.johannes.synpop.data.*;
import playground.johannes.synpop.data.io.PopulationIO;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * Created by johannesillenberger on 11.04.17.
 */
public class RequestGenerator {

    private static final Logger logger = Logger.getLogger(RequestGenerator.class);

    private static final String MODULE_NAME = "requestGenerator";

    private static final String FACILITY_FILE_PARAM = "facilityFile";

    private static final String PERSONS_FILE_PARAM = "personsFile";

    private static final String PROBA_PARAM = "requestProba";

    private static final String REQUESTS_FILE_PARAM = "requestsFile";

    private static final String SEPARATOR = ";";

    public static void main(String args[]) throws IOException, FactoryException {
        final Config config = new Config();
        ConfigUtils.loadConfig(config, args[0]);
        ConfigGroup group = config.getModules().get(MODULE_NAME);

        logger.info("Loading facilities...");
        Scenario scenario = ScenarioUtils.createScenario(config);
        FacilitiesReaderMatsimV1 reader = new FacilitiesReaderMatsimV1(scenario);
        reader.readFile(group.getValue(FACILITY_FILE_PARAM));

        logger.info("Loading persons...");
        Set<? extends Person> persons = PopulationIO.loadFromXML(group.getParams().get(PERSONS_FILE_PARAM), new PlainFactory());

        logger.info("Selecting trips...");
        double proba = Double.parseDouble(group.getParams().get(PROBA_PARAM));
        Random random = new XORShiftRandom();

        List<Segment> trips = new ArrayList<>(persons.size());
        for(Person person : persons) {
            Episode episode = person.getEpisodes().get(0);
            for(Segment leg : episode.getLegs()) {
                if(random.nextDouble() < proba) {
                    trips.add(leg);
                }
            }
        }
        logger.info(String.format("Selected %s trips.", trips.size()));

        logger.info("Sorting trips...");
        if(trips.size() > 1) {
            Collections.sort(trips, new Comparator<Segment>() {
                @Override
                public int compare(Segment segment, Segment t1) {
                    double time1 = Double.parseDouble(segment.getAttribute(CommonKeys.LEG_START_TIME));
                    double time2 = Double.parseDouble(t1.getAttribute(CommonKeys.LEG_START_TIME));
                    int r = Double.compare(time1, time2);
                    if (r == 0) {
                        if (segment.equals(t1)) return 0;
                        else return segment.hashCode() - t1.hashCode();
                    }
                    return r;
                }
            });
        }

        logger.info("Writing trips...");
        BufferedWriter writer = new BufferedWriter(new FileWriter(group.getParams().get(REQUESTS_FILE_PARAM)));
        writer.write("Id;Time;From_lat;From_lon;To_lat;To_lon");
        writer.newLine();

        MathTransform transform = CRS.findMathTransform(CRSUtils.getCRS(31467), DefaultGeographicCRS.WGS84);

        Map<Id<ActivityFacility>, ? extends ActivityFacility> facilities = scenario.getActivityFacilities().getFacilities();
        for(Segment leg : trips) {
            writer.write(leg.getEpisode().getPerson().getId().toString());
            writer.write(SEPARATOR);
            int startTime = (int) Double.parseDouble(leg.getAttribute(CommonKeys.LEG_START_TIME));
            LocalTime lt = LocalTime.MIDNIGHT.plusSeconds(startTime);
            writer.write(lt.toString("HH:mm:ss"));
            writer.write(SEPARATOR);

            String idStart = leg.previous().getAttribute(CommonKeys.ACTIVITY_FACILITY);
            String idEnd = leg.next().getAttribute(CommonKeys.ACTIVITY_FACILITY);

            ActivityFacility facStart = facilities.get(Id.create(idStart, ActivityFacility.class));
            ActivityFacility facEnd = facilities.get(Id.create(idEnd, ActivityFacility.class));

            double[] startCoord = new double[] { facStart.getCoord().getX(), facStart.getCoord().getY() };
            try {
                transform.transform(startCoord, 0, startCoord, 0, 1);
            } catch (TransformException e) {
                e.printStackTrace();
            }

            double[] endCoord = new double[] { facEnd.getCoord().getX(), facEnd.getCoord().getY() };
            try {
                transform.transform(endCoord, 0, endCoord, 0, 1);
            } catch (TransformException e) {
                e.printStackTrace();
            }
            writer.write(String.valueOf(startCoord[1]));
            writer.write(SEPARATOR);
            writer.write(String.valueOf(startCoord[0]));
            writer.write(SEPARATOR);
            writer.write(String.valueOf(endCoord[1]));
            writer.write(SEPARATOR);
            writer.write(String.valueOf(endCoord[0]));
            writer.newLine();
        }

        writer.close();
        logger.info("Done.");
    }
}