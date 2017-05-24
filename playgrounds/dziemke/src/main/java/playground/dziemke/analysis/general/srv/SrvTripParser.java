package playground.dziemke.analysis.general.srv;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.households.Household;
import playground.dziemke.analysis.general.Trip;
import playground.dziemke.cemdapMatsimCadyts.Zone;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.*;

/**
 * @author gthunig on 30.03.2017.
 */
public class SrvTripParser {

    private final static Logger log = Logger.getLogger(SrvTripParser.class);



    //class atributes
    private Map<String, Integer> columnNumbers;
    private Population population;
    private Map<Id<Person>, List<FromSrvTrip>> person2Trips = new HashMap<>();

    private String[] entries;

    SrvTripParser(Population population) {
        this.population = population;
    }

    public Population parse(String srvTripFilePath) {

        int lineCount = 0;

        try {
            BufferedReader bufferedReader = IOUtils.getBufferedReader(srvTripFilePath);

            // header
            String currentLine = bufferedReader.readLine();
            lineCount++;
            String[] heads = currentLine.split(";", -1);
            columnNumbers = new LinkedHashMap<>(heads.length);
            for (int i = 0; i < heads.length; i++) {
                columnNumbers.put(heads[i],i);
            }

            // read data
            while ((currentLine = bufferedReader.readLine()) != null) {
                String[] entries = currentLine.split(";", -1);

                parseAndAddLeg(entries);

                lineCount++;

                // test file (it.150 of run.104) has 111229 lines altogether
                // line count is correct since starting with "0" take in account the fact that
                // the first line is the header, which may not be counted

                if (lineCount % 100000 == 0) {
                    log.info(lineCount+ " lines read in so far.");
                    Gbl.printMemoryUsage();
                }
            }
        } catch (IOException e) {
            log.error(new Exception(e));
        }

        addTripsToPopulation();

        return population;
    }

    public List<FromSrvTrip> getTrips() {
        List<FromSrvTrip> result = new ArrayList<>();
        for (List<FromSrvTrip> trips : this.person2Trips.values()) {
            result.addAll(trips);
        }
        return result;
    }

    private void parseAndAddLeg(String[] entries) {
        this.entries = entries;

        FromSrvTrip trip = new FromSrvTrip();
        trip.setTripId(getTripId());
        trip.setLegMode(getLegMode());
        //endingActivity
        trip.setActivityTypeBeforeTrip(getActivityEndActType());
        trip.setDepartureZoneId(getDepartureZoneId());
        trip.setDepartureTime_s(getDepartureTimeSec());
        //leg
        trip.setDuration_s(getDurationSec());
        trip.setDistanceBeeline_m(getDistanceBeelineM());
        trip.setDistanceRouted_m(getDistanceRoutedFastestM());
        trip.setSpeed_m_s(getSpeedMS());
        trip.setWeight(getWeight());
        //startingActivity
        trip.setActivityTypeAfterTrip(getActivityStartActType());
        trip.setArrivalZoneId(getArrivalZoneId());
        trip.setArrivalTime_s(getArrivalTimeSec());

        Id<Household> householdId = Id.create(entries[columnNumbers.get(SrvTripUtils.HOUSEHOLD_ID)], Household.class);
        Id<Person> personId = Id.create(entries[columnNumbers.get(SrvTripUtils.PERSON_ID)], Person.class);
        Person person = getPerson(householdId, personId);

        addTripToPerson2Trips(person, trip);
    }

    private void addTripToPerson2Trips(Person person, FromSrvTrip trip) {
        List<FromSrvTrip> trips = getTrips(person);
        trips.add(trip);
    }

    private List<FromSrvTrip> getTrips(Person person) {
        return this.person2Trips.computeIfAbsent(person.getId(), k -> new ArrayList<>());
    }

    private Person getPerson(Id<Household> householdId, Id<Person> personId) {
        return population.getPersons().get(Srv2MatsimPopulationUtils.createUniquePersonId(householdId, personId));
    }

    private Id<Trip> getTripId() {
        return Id.create(entries[columnNumbers.get(SrvTripUtils.TRIP_ID)], Trip.class);
    }

    private Id<Zone> getArrivalZoneId() {
        String zoneString = entries[columnNumbers.get(SrvTripUtils.ARRIVAL_ZONE_ID)];
        return Id.create(zoneString, Zone.class);
    }

    private Id<Zone> getDepartureZoneId() {
        String zoneString = entries[columnNumbers.get(SrvTripUtils.DEPARTURE_ZONE_ID)];
        return Id.create(zoneString, Zone.class);
    }

    private String getActivityEndActType() {
        return Srv2MatsimPopulationUtils.transformActType(new Integer(entries[columnNumbers.get(SrvTripUtils.ACTIVITY_END_ACT_TYPE)]));
    }

    private String getActivityStartActType() {
        return Srv2MatsimPopulationUtils.transformActType(new Integer(entries[columnNumbers.get(SrvTripUtils.ACTIVITY_START_ACT_TYPE)]));
    }

    private double getDepartureTimeSec() {
        return Double.parseDouble(entries[columnNumbers.get(SrvTripUtils.DEPARTURE_TIME_MIN)]) * 60;
    }

    private double getArrivalTimeSec() {
        return Double.parseDouble(entries[columnNumbers.get(SrvTripUtils.ARRIVAL_TIME_MIN)]) * 60;
    }

    private double getDurationSec() {
        return Double.parseDouble(entries[columnNumbers.get(SrvTripUtils.DURATION_MIN)]) * 60;
    }

    private double getDistanceBeelineM() {
        return Double.parseDouble(entries[columnNumbers.get(SrvTripUtils.DISTANCE_BEELINE_KM)]) * 1000;
    }

    private double getDistanceRoutedFastestM() {
        return Double.parseDouble(entries[columnNumbers.get(SrvTripUtils.DISTANCE_ROUTED_FASTEST_KM)]) * 1000;
    }

    private double getSpeedMS() {
        double speed_km_h= Double.parseDouble(entries[columnNumbers.get(SrvTripUtils.SPEED_KM_H)]);
        return speed_km_h / 3.6;
    }

    private double getWeight() {
        return Double.parseDouble(entries[columnNumbers.get(SrvTripUtils.WEIGHT)]);
    }

    private String getLegMode() {
        int hvm_4 = Integer.parseInt(entries[columnNumbers.get(SrvTripUtils.MODE)]);
        switch (hvm_4) {
            case 1:
                return TransportMode.walk;
            case 2:
                return TransportMode.bike;
            case 3:
                return TransportMode.car;
            case 4:
                return TransportMode.pt;
            default:
                return TransportMode.other;
        }
    }

    private void addTripsToPopulation() {
        for (Map.Entry<Id<Person>, ? extends Person> entry : population.getPersons().entrySet()) {
            List<FromSrvTrip> trips = this.person2Trips.get(entry.getKey());
            if (trips != null) {
                sortTrips(trips);
                for (FromSrvTrip trip : trips)
                    addTripToPerson(entry.getValue(), trip);
            }

        }
    }

    private void addTripToPerson(Person person, FromSrvTrip trip) {
        Plan plan = getPlan(person);

        Activity endingActivity;
        if (lastPlanElementIsActivity(plan)) {
            endingActivity = getLastActivity(plan);
        } else {
            endingActivity = PopulationUtils.createAndAddActivityFromCoord(plan, trip.getActivityTypeBeforeTrip(), getEmptyCoord());
            ActivityUtils.setZoneId(endingActivity, trip.getDepartureZoneId());
        }
        endingActivity.setEndTime(trip.getDepartureTime_s());

        Leg leg = population.getFactory().createLeg(trip.getLegMode());
        leg.setDepartureTime(trip.getDepartureTime_s());
        leg.setTravelTime(trip.getDuration_s());
        LegUtils.setDistanceBeelineM(leg, trip.getDistanceBeeline_m());
        LegUtils.setDistanceRoutedM(leg, trip.getDistanceRouted_m());
        LegUtils.setSpeedMS(leg, trip.getSpeed_m_s());
        LegUtils.setWeight(leg, trip.getWeight());
        plan.addLeg(leg);

        Activity arrivalActivity = PopulationUtils.createAndAddActivityFromCoord(plan, trip.getActivityTypeAfterTrip(), getEmptyCoord());
        ActivityUtils.setZoneId(arrivalActivity, trip.getArrivalZoneId());
    }

    private boolean lastPlanElementIsActivity(Plan plan) {
        PlanElement lastElement = getLastPlanElement(plan);
        return lastElement instanceof Activity;
    }

    private PlanElement getLastPlanElement(Plan plan) {
        List<PlanElement> planElements = plan.getPlanElements();
        if (planElements.size() == 0) {
            return null;
        }
        return planElements.get(planElements.size()-1);
    }

    private Activity getLastActivity(Plan plan) {
        return (Activity)getLastPlanElement(plan);
    }

    private Plan getPlan(Person person) {
        Plan plan = person.getSelectedPlan();
        if (plan == null) {
            if (person.getPlans().size() == 0) {
                plan = population.getFactory().createPlan();
                person.addPlan(plan);
                person.setSelectedPlan(plan);
            } else {
                plan = person.getPlans().get(0);
                person.setSelectedPlan(plan);
            }
        }
        return plan;
    }

    private static void sortTrips(List<FromSrvTrip> trips) {
        trips.sort((trip1, trip2) -> {
            // -1 - less than, 1 - greater than, 0 - equal
            return trip1.getDepartureTime_s() < trip2.getDepartureTime_s() ? -1 :
                    (trip1.getDepartureTime_s() > trip2.getDepartureTime_s()) ? 1 : 0;
        });
    }

    private static Coord getEmptyCoord() {
        return new Coord(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY);
    }
}
