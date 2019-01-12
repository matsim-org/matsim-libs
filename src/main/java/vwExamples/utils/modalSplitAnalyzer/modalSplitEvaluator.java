/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

/**
 *
 */
package vwExamples.utils.modalSplitAnalyzer;

import com.vividsolutions.jts.geom.Geometry;
import org.apache.commons.lang.mutable.MutableInt;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.algorithms.PersonAlgorithm;
import org.matsim.core.population.io.StreamingPopulationReader;
import org.matsim.core.population.io.StreamingPopulationWriter;
import org.matsim.core.router.*;
import org.matsim.core.router.TripStructureUtils.Subtour;
import org.matsim.core.router.TripStructureUtils.Trip;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.io.IOUtils;
import org.opengis.feature.simple.SimpleFeature;
import parking.ParkingRouterNetworkRoutingModule;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

/**
 * @author axer
 */

public class modalSplitEvaluator {
    private Map<String, PersonValidator> groups = new HashMap<>();
    Set<String> zones = new HashSet<>();
    static Map<String, Geometry> zoneMap = new HashMap<>();
    String shapeFile = "C:\\Temp\\shp\\parking-bs.shp";
    String shapeFeature = "NO";
    StageActivityTypes stageActs;
    static String inFileName = "C:\\Temp\\output\\ptToDrt\\ptToDrt.output_plans.xml.gz";
    static String OutFileName = "C:\\Temp\\plans\\drt_old.xml.gz";

    static List<String> primaryActivies = new ArrayList<>();
    static List<String> primaryLegModes = new ArrayList<>();
    // Share of the mode that is tried to be shifted to a new mode
    static Map<String, Double> desiredModalShiftRatesMap = new HashMap<String, Double>();

    // Caution, the actual pt in our Berlin Model is overestimated. Thus, we need to
    // reduce it by factor 0.58, the get the correct amount of DRT users
    // Like Berlin
    static {
        desiredModalShiftRatesMap.put("car", 0.0);
        desiredModalShiftRatesMap.put("pt", 0.5);
    }

    public static void main(String[] args) {

        modalSplitEvaluator tde = new modalSplitEvaluator();
        tde.run(inFileName);

    }

    public void readShape(String shapeFile, String featureKeyInShapeFile) {
        Collection<SimpleFeature> features = ShapeFileReader.getAllFeatures(shapeFile);
        for (SimpleFeature feature : features) {
            String id = feature.getAttribute(featureKeyInShapeFile).toString();
            Geometry geometry = (Geometry) feature.getDefaultGeometry();
            zones.add(id);
            zoneMap.put(id, geometry);
        }
    }

    public double maxShiftNumbersPerMode(String mode, Map<String, Double> desiredModalShiftRatesMap,
                                         Map<String, Integer> modeTripsMap, Map<String, Double> modeTripsMapRelative) {

        double ratioOfModesToShiftInSim = 0;
        double newShareOfMode;
        double maxShiftableTripsPerMode = 0;

        double absTripNumber = 0;

        for (String modeToShift : desiredModalShiftRatesMap.keySet()) {
            ratioOfModesToShiftInSim += modeTripsMapRelative.get(modeToShift);
        }

        for (Integer modeTrips : modeTripsMap.values()) {
            absTripNumber += modeTrips;
        }

        // newShareOfMode = modeTripsMapRelative.get(mode) -
        // (desiredModalShiftRatesMap.get(mode) * ratioOfModesToShiftInSim);
        //
        // maxShiftableTripsPerMode=modeTripsMap.get(mode)-newShareOfMode*absTripNumber;

        maxShiftableTripsPerMode = modeTripsMap.get(mode) * desiredModalShiftRatesMap.get(mode);

        return maxShiftableTripsPerMode;

    }

    public void removeCarStagingActivities(Plan plan) {

        List<Integer> idxList = new ArrayList<Integer>();
        Activity nexAct = null;

        List<Leg> legList = PopulationUtils.getLegs(plan);

        for (Leg leg : legList) {

            // Next activity after this leg
            nexAct = PopulationUtils.getNextActivity(plan, leg);

            // If this nexAct is car_interaction, it can be deleted
            if (nexAct.getType().equals("car interaction")) {
                Leg prevLeg = PopulationUtils.getPreviousLeg(plan, nexAct);
                Leg nextLeg = PopulationUtils.getNextLeg(plan, nexAct);
                int elementIdx = PopulationUtils.getActLegIndex(plan, (PlanElement) nexAct);
                idxList.add(elementIdx);

                // Check if there is an access or egress walk

                // PopulationUtils.removeActivity(plan, elementIdx);

                if (prevLeg.getMode().equals("access_walk")) {
                    elementIdx = PopulationUtils.getActLegIndex(plan, (PlanElement) prevLeg);
                    // PopulationUtils.removeLeg(plan, elementIdx);
                    idxList.add(elementIdx);
                }

                if (nextLeg.getMode().equals("egress_walk")) {
                    elementIdx = PopulationUtils.getActLegIndex(plan, (PlanElement) nextLeg);
                    // PopulationUtils.removeLeg(plan, elementIdx);
                    idxList.add(elementIdx);
                }

            }

            if (nexAct.getType().equals("car parkingSearch")) {
                int elementIdx = PopulationUtils.getActLegIndex(plan, (PlanElement) nexAct);
                Leg nextLeg = PopulationUtils.getNextLeg(plan, nexAct);
                // PopulationUtils.removeActivity(plan, elementIdx);
                idxList.add(elementIdx);

                if (nextLeg.getMode().equals("car")) {
                    elementIdx = PopulationUtils.getActLegIndex(plan, (PlanElement) nextLeg);
                    // PopulationUtils.removeLeg(plan, elementIdx);
                    idxList.add(elementIdx);
                }

            }

        }
        if (!idxList.isEmpty()) {

            int actIdx = 0;
            for (Iterator<PlanElement> iter = plan.getPlanElements().iterator(); iter.hasNext(); ) {

                PlanElement element = iter.next();

                if (idxList.contains(actIdx)) {
                    //System.out.println(element);
                    iter.remove();
                }
                actIdx++;
            }
            //System.out.println("finished");
        }


    }

    public static boolean isWithinZone(Coord coord, Map<String, Geometry> zoneMap) {
        // Function assumes Shapes are in the same coordinate system like MATSim
        // simulation

        for (String zone : zoneMap.keySet()) {
            Geometry geometry = zoneMap.get(zone);
            if (geometry.intersects(MGC.coord2Point(coord))) {
                // System.out.println("Coordinate in "+ zone);
                return true;
            }
        }

        return false;
    }

    public static String getSubtourMode(Subtour subTour, Plan plan) {


        // ToDo: Inefficient way to get the legMode. Reduce loops!
        String subtourMode = null;
        List<Trip> trips = subTour.getTrips();

        MainModeIdentifier mainModeIdentifier = new MainModeIdentifierImpl();

        for (TripStructureUtils.Trip trip : trips) {
            final List<PlanElement> fullTrip =
                    plan.getPlanElements().subList(
                            plan.getPlanElements().indexOf(trip.getOriginActivity()) + 1,
                            plan.getPlanElements().indexOf(trip.getDestinationActivity()));
            subtourMode = mainModeIdentifier.identifyMainMode(fullTrip);
            return subtourMode;
        }

        return subtourMode;

    }

    public static List<Integer> getReplaceableTripIndices(Subtour subTour, Plan plan) {

        String subtourMode = null;
        List<Trip> trips = subTour.getTrips();
        String[] chainedModes = {TransportMode.car, TransportMode.bike};

        subtourMode = getSubtourMode(subTour, plan);
        List<Integer> replaceableTrips = new ArrayList<Integer>();

        // Dealing with a chained mode all trips need to be within the specified area
        if (Arrays.asList(chainedModes).contains(subtourMode) && (subtourMode != null)) {
            // System.out.println("Chained mode");

            for (Trip trip : trips) {

                //List<Leg> legList = trip.getLegsOnly();

                // Checks, if any leg of this subTour leaves a area (e.g. the service area)
                //for (Leg leg : legList) {

                if (isWithinZone(trip.getDestinationActivity().getCoord(), zoneMap)
                        && isWithinZone(trip.getOriginActivity().getCoord(), zoneMap)) {

                    replaceableTrips.add(trips.indexOf(trip));
                }
                // A trip of a chained mode leaves the specified area, drop all element of
                // replaceableTrips
                // and return a cleaned list
                else {
                    // System.out.println("Agent leaves area!");
                    replaceableTrips.clear();
                    //System.out.println(replaceableTrips);
                    return replaceableTrips;

                    //}

                }

            }
            //System.out.println(replaceableTrips);
            return replaceableTrips;

        }

        // Not dealing with a chained mode, get trip indices that are within specified
        // area
        else if (!Arrays.asList(chainedModes).contains(subtourMode) && (subtourMode != null)) {
            // System.out.println("Non Chained mode");

            for (Trip trip : trips) {

                List<Leg> legList = trip.getLegsOnly();

                // Checks, if any leg of this subTour leaves a area (e.g. the service area)
                //for (Leg leg : legList) {
                // Only add trip indices that are within the specified area
                if (isWithinZone(trip.getDestinationActivity().getCoord(), zoneMap)
                        && isWithinZone(trip.getOriginActivity().getCoord(), zoneMap)) {
                    replaceableTrips.add(trips.indexOf(trip));
                    //}

                }

            }
            return replaceableTrips;

        }

        return replaceableTrips;

    }

    public static boolean judgeLeg(Activity prevAct, Activity nextAct, Map<String, Geometry> zoneMap) {
        boolean prevActInZone = false;
        boolean nextActInZone = false;

        // if(isWithinSpecificZone(prevAct.getCoord(), zoneMap,zoneList)) prevActInZone=
        // true;
        // if(isWithinSpecificZone(nextAct.getCoord(), zoneMap,zoneList)) nextActInZone=
        // true;

        if (isWithinZone(prevAct.getCoord(), zoneMap))
            prevActInZone = true;
        if (isWithinZone(nextAct.getCoord(), zoneMap))
            nextActInZone = true;

        if ((prevActInZone == true) && (nextActInZone == true)) {
            // System.out.println("Leg in Zone: "+plan.getPerson().getId().toString());
            return true;
        } else
            return false;
    }

    public static boolean isPrimaryActivity(Activity act) {
        for (String validAct : primaryActivies) {
            if (act.getType().contains(validAct)) {
                // System.out.println(validAct +" found");
                return true;

            }
        }

        return false;

    }

    public static boolean isPrimaryLeg(Leg leg) {

        for (String validLeg : primaryLegModes) {
            if (validLeg.equals(leg.getMode())) {
                return true;
            }
        }

        return false;

    }

    public void run(String populationFile) {
        // We have three groups of agents
        readShape(shapeFile, shapeFeature);
        groups.put("LivesCity", new LivesCity());

        primaryActivies.add("home");
        primaryActivies.add("work");
        primaryActivies.add("school");
        primaryActivies.add("education");
        primaryActivies.add("leisure");
        primaryActivies.add("shopping");
        primaryActivies.add("other");

        primaryLegModes.add(TransportMode.car);
        primaryLegModes.add(TransportMode.drt);
        primaryLegModes.add(TransportMode.walk);
        primaryLegModes.add(TransportMode.ride);
        primaryLegModes.add(TransportMode.pt);
        primaryLegModes.add(TransportMode.bike);
        primaryLegModes.add(TransportMode.other);
        primaryLegModes.add(TransportMode.taxi);
        primaryLegModes.add(TransportMode.train);

        for (Entry<String, PersonValidator> e : groups.entrySet()) {
            // Iterate over each group
            Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
            StreamingPopulationReader spr = new StreamingPopulationReader(scenario);

            // A hash map that stores the mode as key and the traveled distances
            // ptSlow or any mode that contains pt will be casted to pt
            Map<String, TravelDistanceActivity> distanceActivityPerModeWithinTraffic = new HashMap<>();
            distanceActivityPerModeWithinTraffic.put(TransportMode.car, new TravelDistanceActivity(TransportMode.car));
            distanceActivityPerModeWithinTraffic.put(TransportMode.ride,
                    new TravelDistanceActivity(TransportMode.ride));
            distanceActivityPerModeWithinTraffic.put(TransportMode.bike,
                    new TravelDistanceActivity(TransportMode.bike));
            distanceActivityPerModeWithinTraffic.put(TransportMode.pt, new TravelDistanceActivity(TransportMode.pt));
            distanceActivityPerModeWithinTraffic.put(TransportMode.walk,
                    new TravelDistanceActivity(TransportMode.walk));
            distanceActivityPerModeWithinTraffic.put(TransportMode.drt, new TravelDistanceActivity(TransportMode.drt));

            Map<String, TravelDistanceActivity> distanceActivityPerModeAllTraffic = new HashMap<>();
            distanceActivityPerModeAllTraffic.put(TransportMode.car, new TravelDistanceActivity(TransportMode.car));
            distanceActivityPerModeAllTraffic.put(TransportMode.ride, new TravelDistanceActivity(TransportMode.ride));
            distanceActivityPerModeAllTraffic.put(TransportMode.bike, new TravelDistanceActivity(TransportMode.bike));
            distanceActivityPerModeAllTraffic.put(TransportMode.pt, new TravelDistanceActivity(TransportMode.pt));
            distanceActivityPerModeAllTraffic.put(TransportMode.walk, new TravelDistanceActivity(TransportMode.walk));
            distanceActivityPerModeAllTraffic.put(TransportMode.drt, new TravelDistanceActivity(TransportMode.drt));

            // The stream read gets a new PersonAlgorithm which does stuff for each person

            spr.addAlgorithm(new PersonAlgorithm() {

                @Override
                public void run(Person person) {

                    // This line ensures, that a person algorithm is operating on a person!
                    if (e.getValue().isValidPerson(person)) {

                        // System.out.println(person.getId());
                        // Work only with the selected plan of an agent
                        Plan plan = person.getSelectedPlan();

                        Leg prevLeg = null;
                        Activity prevAct = null;

                        // Convert pure transit_walks to walk
                        for (PlanElement pe : plan.getPlanElements()) {

                            if (pe instanceof Activity) {
                                if (prevLeg != null && prevAct != null) {
                                    // pt interaction walks are counted as regular walks
                                    if (!((Activity) pe).getType().equals("pt interaction")
                                            && !prevAct.getType().equals("pt interaction")
                                            && (prevLeg.getMode().equals(TransportMode.transit_walk))) {
                                        // System.out.println("Rewrite transit_walk to walk");
                                        prevLeg.setMode("walk");
                                    }

                                }
                                prevAct = (Activity) pe;
                            } else if (pe instanceof Leg) {
                                prevLeg = (Leg) pe;
                            }

                        }

                        // Remove transit acts from plan
                        // Cleaned plan without transit_walks and no TransitActs
                        // True = also remove access and egress walks
                        //new TransitActsRemover().run(plan, true);
                        //removeCarStagingActivities(plan);

                        // Activity lastAct = null;
                        // Leg lastLeg = null;
                        // double lastDistance = 0;

                        List<Leg> legList = PopulationUtils.getLegs(plan);

                        Activity preAct = null;
                        Activity nexAct = null;

                        Activity prePrimaryAct = null;
                        Activity nexPrimaryAct = null;

                        double totalLegDistance = 0.0;

                        String primaryLegMode = null;

                        for (Leg leg : legList) {
                            preAct = PopulationUtils.getPreviousActivity(plan, leg);
                            nexAct = PopulationUtils.getNextActivity(plan, leg);

                            // Update Primary Activities
                            if (isPrimaryActivity(preAct)) {
                                prePrimaryAct = preAct;
                            }

                            if (isPrimaryActivity(nexAct)) {
                                nexPrimaryAct = nexAct;
                            }

                            double legDistance = 0.0;

                            // check whether this leg is allowed to be used as a distance value
                            if (isPrimaryLeg(leg)) {
                                primaryLegMode = leg.getMode();


                                if (leg.getMode().equals(TransportMode.pt)) {
                                    legDistance = ((CoordUtils.calcEuclideanDistance(preAct.getCoord(),
                                            nexAct.getCoord()) * 1.3) / 1000.0);
                                } else {
                                    legDistance = leg.getRoute().getDistance() / 1000;
                                }

                                totalLegDistance += legDistance;
                            } else {
                                // System.out.println("Skip " + leg.getMode() );
                            }

                            // check whether we have reached a primary activity
                            if (isPrimaryActivity(nexAct) && !(primaryLegMode == null)) {
                                // Write entry to statistic
                                distanceActivityPerModeAllTraffic.get(primaryLegMode).addLeg(nexAct.getType(),
                                        (int) totalLegDistance);
                                // System.out.println("Leg " + primaryLegMode + " totalDistance = "
                                // +totalLegDistance + " activity = " + nexAct.getType() );
                                // Reset values - leg is finished

                                // Write entry to statistic, if trip is within the research area
                                if (judgeLeg(prePrimaryAct, nexPrimaryAct, zoneMap)) {
                                    distanceActivityPerModeWithinTraffic.get(primaryLegMode)
                                            .addLeg(nexPrimaryAct.getType(), (int) totalLegDistance);
                                }

                                totalLegDistance = 0.0;
                                primaryLegMode = null;

                            }
                        }

                    }
                }

            });

            spr.readFile(populationFile);
            // Iterate over distance distr. per mode a write a table

            for (TravelDistanceActivity tda : distanceActivityPerModeWithinTraffic.values()) {
                tda.writeTable(populationFile.replace(".xml.gz",
                        "_distanceStatsWithinTraffic_" + tda.mode + "_" + e.getKey() + ".csv"));
                // Within Traffic: Considers only traffic of residents within this zone or zones
                // where origin and destination is within zone or zones
                System.out.println("Within Traffic - Mode: " + tda.mode + " " + tda.getNumerOfTrips());
            }

            // Initialize absolute numbers of trips for within traffic simulation
            ModalShare withinTrafficModalShare = new ModalShare(distanceActivityPerModeWithinTraffic);

            System.out.println(withinTrafficModalShare.modeTripsMapRelative.toString());

            for (TravelDistanceActivity tda : distanceActivityPerModeAllTraffic.values()) {
                tda.writeTable(populationFile.replace(".xml.gz",
                        "_distanceStatsAllTraffic_" + tda.mode + "_" + e.getKey() + ".csv"));
                // All traffic: Considers all traffic of residents within this zone or zones
                System.out.println("All Traffic - Mode: " + tda.mode + " " + tda.getNumerOfTrips());
            }

            // Initialize absolute numbers of trips for all traffic in simulation
            ModalShare allTrafficModalShare = new ModalShare(distanceActivityPerModeAllTraffic);
            System.out.println(allTrafficModalShare.modeTripsMapRelative.toString());

            // Here we are reading the supplied plan file again and we are modifying the
            // plans
            StreamingPopulationReader spr2 = new StreamingPopulationReader(scenario);
            StreamingPopulationWriter spw = new StreamingPopulationWriter();
            spw.startStreaming(OutFileName);
            Map<String, MutableInt> initalModeTripMap = new HashMap<String, MutableInt>();
            Map<String, MutableInt> LogReplacedTripsMap = new HashMap<String, MutableInt>();
            Random p = new Random();

            // Initialize modeTripcounterMap with modes
            // We use the total trips of all inhabitants within the area
            for (String mode : allTrafficModalShare.modeTripsMap.keySet()) {
                // Add mode to map and initialize actual number of trips per mode
                initalModeTripMap.put(mode, new MutableInt());
                initalModeTripMap.get(mode).setValue(allTrafficModalShare.modeTripsMap.get(mode));
                LogReplacedTripsMap.put(mode, new MutableInt());
            }

            spr2.addAlgorithm(new PersonAlgorithm() {

                @Override
                public void run(Person person) {

//					String STAGE = PtConstants.TRANSIT_ACTIVITY_TYPE;
//					String STAGE = "car interaction";
//					StageActivityTypes stagesActivities = new StageActivityTypesImpl(STAGE);

                    // We are only modifying person that are living in area
                    // The value of e is the defined person validator that
                    if (e.getValue().isValidPerson(person)) {


                        Plan plan = person.getSelectedPlan();

//						removeCarStagingActivities(plan);

                        // System.out.println("Agent: "+person.getId().toString());

                        // We need to drop all transit acts
                        //new TransitActsRemover().run(plan,true);
                        //removeCarStagingActivities(plan);

                        //Collection<Subtour> subtours = TripStructureUtils.getSubtours(plan, stagesActivities);


                        StageActivityTypes blackList = new ParkingRouterNetworkRoutingModule.ParkingAccessEgressStageActivityTypes();

                        for (Subtour subTour : TripStructureUtils.getSubtours(plan, blackList)) {
                            List<Integer> replaceTripIndices = null;
                            replaceTripIndices = getReplaceableTripIndices(subTour, plan);
//							System.out.println(replaceTripIndices);
                            String SubtourMode = getSubtourMode(subTour, plan);


                            //System.out.println(SubtourMode);


                            // Replace mode names with regular modes
                            if (SubtourMode.equals("ptSlow")) {
                                SubtourMode = TransportMode.pt;
                            } else if (SubtourMode.equals("bicycle")) {
                                SubtourMode = TransportMode.bike;
                            } else if (SubtourMode.equals("ride")) {
                                SubtourMode = TransportMode.car;
                            }

//							System.out.println(SubtourMode);

                            if (desiredModalShiftRatesMap.keySet().contains(SubtourMode)) {

                                double limit = maxShiftNumbersPerMode(SubtourMode, desiredModalShiftRatesMap,
                                        allTrafficModalShare.modeTripsMap, allTrafficModalShare.modeTripsMapRelative);

                                if (limit >= LogReplacedTripsMap.get(SubtourMode).intValue()) {


                                    // Replace trips that are in general replaceable
                                    // Without considering a threshold share
                                    if ((replaceTripIndices.size() > 0) && p.nextDouble() < 0.50) {
                                        for (Integer idx : replaceTripIndices) {
                                            Trip trip = subTour.getTrips().get(idx);

                                            for (Leg l : trip.getLegsOnly()) {
                                                l.setRoute(null);
                                                l.setTravelTime(0.0);
                                            }

                                            TripRouter.insertTrip(plan, trip.getOriginActivity(),
                                                    Collections.singletonList(
                                                            PopulationUtils.createLeg(TransportMode.drt)),
                                                    trip.getDestinationActivity());
                                            initalModeTripMap.get(SubtourMode).decrement();
                                            initalModeTripMap.get(TransportMode.drt).increment();
                                            LogReplacedTripsMap.get(SubtourMode).increment();
                                        }

                                    }
                                    // System.out.println(LogReplacedTripsMap.toString());
                                }
                            }

                        }

                    }

                }
            });

            double shiftablePTtrips = maxShiftNumbersPerMode("pt", desiredModalShiftRatesMap,
                    allTrafficModalShare.modeTripsMap, allTrafficModalShare.modeTripsMapRelative);
            double shiftableCARtrips = maxShiftNumbersPerMode("car", desiredModalShiftRatesMap,
                    allTrafficModalShare.modeTripsMap, allTrafficModalShare.modeTripsMapRelative);

            System.out.println("PT trips: " + shiftablePTtrips);
            System.out.println("CAR trips: " + shiftableCARtrips);

            spr2.addAlgorithm(spw);
            spr2.readFile(populationFile);
            spw.closeStreaming();
            System.out.println(LogReplacedTripsMap.toString());

        }

    }

    class ModalShare {
        Integer totalTrips = 0;
        Map<String, Integer> modeTripsMap = new HashMap<>();
        Map<String, Double> modeTripsMapRelative = new HashMap<>();

        public ModalShare(Map<String, TravelDistanceActivity> distanceActivityPerMode) {
            for (TravelDistanceActivity tda : distanceActivityPerMode.values()) {
                modeTripsMap.put(tda.mode, tda.getNumerOfTrips());
                totalTrips += tda.getNumerOfTrips();
            }

            for (Entry<String, Integer> entry : modeTripsMap.entrySet()) {
                int trips = entry.getValue();
                double share = (double) trips / this.totalTrips;
                modeTripsMapRelative.put(entry.getKey(), share);
            }
        }

    }

    class TravelDistanceActivity {
        String mode;
        Map<String, int[]> activityDistanceBins = new TreeMap<>();

        public TravelDistanceActivity(String mode) {
            this.mode = mode;
        }

        public void addLeg(String activity, int distance) {
            // Extract the activity that related to this leg
            activity = activity.split("_")[0];
            if (!activityDistanceBins.containsKey(activity)) {
                activityDistanceBins.put(activity, new int[51]);
            }
            if (distance > 50)
                distance = 50;
            activityDistanceBins.get(activity)[distance]++;
        }

        public Integer getNumerOfTrips() {
            Integer numberOfTrips = 0;
            for (int i = 0; i < 51; i++) {
                for (int[] v : activityDistanceBins.values()) {
                    // Sum up all trips
                    numberOfTrips += v[i];
                }

            }
            return numberOfTrips;
        }

        public void writeTable(String filename) {
            BufferedWriter bw = IOUtils.getBufferedWriter(filename);
            try {
                bw.write("Mode;" + mode);
                bw.newLine();
                bw.write("distance");
                for (String s : activityDistanceBins.keySet()) {
                    if (s.equals("other"))
                        bw.write(";" + "4 - private Erledigung");
                    else if (s.equals("home"))
                        bw.write(";" + "1 - Wohnung");
                    else if (s.equals("work"))
                        bw.write(";" + "2 - Arbeit");
                    else if (s.equals("shopping"))
                        bw.write(";" + "3 - Einkauf");
                    else if (s.equals("education"))
                        bw.write(";" + "5 - Ausbildung");
                    else if (s.equals("leisure"))
                        bw.write(";" + "6 - Freizeit");

                }
                for (int i = 0; i < 51; i++) {
                    bw.newLine();
                    bw.write(Integer.toString(i));
                    for (int[] v : activityDistanceBins.values()) {
                        bw.write(";" + v[i]);
                    }

                }
                bw.flush();
                bw.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    class LivesBraunschweig implements PersonValidator {

        /*
         * (non-Javadoc)
         *
         * @see
         * analysis.traveldistances.PersonValidator#isValidPerson(org.matsim.api.core.
         * v01.population.Person)
         */
        @Override
        public boolean isValidPerson(Person person) {
            Id<Person> id = person.getId();
            // //Braunschweig
            if ((id.toString().startsWith("1")) && (id.toString().split("_")[0].length() == 3))
                return true;
            else
                return false;
        }

    }

    class LivesWolfsburg implements PersonValidator {

        /*
         * (non-Javadoc)
         *
         * @see
         * analysis.traveldistances.PersonValidator#isValidPerson(org.matsim.api.core.
         * v01.population.Person)
         */
        @Override
        public boolean isValidPerson(Person person) {
            Id<Person> id = person.getId();
            if ((id.toString().startsWith("3")) && (id.toString().split("_")[0].length() == 3))
                return true;
            else
                return false;

        }

    }

    class LivesCity implements PersonValidator {

        /*
         * (non-Javadoc)
         *
         * @see
         * analysis.traveldistances.PersonValidator#isValidPerson(org.matsim.api.core.
         * v01.population.Person)
         */
        @Override
        public boolean isValidPerson(Person person) {

            Boolean isvalid = false;

            for (PlanElement pe : person.getSelectedPlan().getPlanElements()) {
                if (pe instanceof Activity) {
                    // if (((Activity)pe).getType().equals("home")) {
                    if (((Activity) pe).getType().contains("home")) {

                        Activity activity = ((Activity) pe);
                        Coord coord = activity.getCoord();
                        // if (isWithinSpecificZone(coord,zoneMap,zoneList)){
                        if (isWithinZone(coord, zoneMap)) {
                            // System.out.println("Relevant agent found: "+person.getId());
                            isvalid = true;

                        }

                    }
                }
            }

            return isvalid;

        }

    }

    class Lives implements PersonValidator {

        /*
         * (non-Javadoc)
         *
         * @see
         * analysis.traveldistances.PersonValidator#isValidPerson(org.matsim.api.core.
         * v01.population.Person)
         */
        @Override
        public boolean isValidPerson(Person person) {

            Boolean isvalid = true;

            return isvalid;

        }

    }

    class AnyPerson implements PersonValidator {

        /*
         * (non-Javadoc)
         *
         * @see
         * analysis.traveldistances.PersonValidator#isValidPerson(org.matsim.api.core.
         * v01.population.Person)
         */
        @Override
        public boolean isValidPerson(Person person) {
            return true;
        }

    }

}
