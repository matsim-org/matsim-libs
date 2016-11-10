package playground.pieter.singapore.demand;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.FacilitiesWriter;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;
import others.sergioo.util.dataBase.DataBaseAdmin;
import others.sergioo.util.dataBase.NoConnectionException;
import playground.pieter.singapore.utils.FacilitiesToSQL;

import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * Created by fouriep on 1/12/15.
 */
public class CapstonePlansWriter {
    private final MutableScenario scenario;
    private static int freightCount = 0;
    private int badplanCount;
    public int carlegs, ptlegs, passengerlegs;


    public CapstonePlansWriter(MutableScenario scenarioWithFacilities, DataBaseAdmin dba) {
        this.dataBaseAdmin = dba;
        this.scenario = scenarioWithFacilities;
        households = new HashMap<>();
        persons = new HashMap<>();

    }

    class PersonEntry {
        String hhid;
        String persid;
        int homePostcode;
        int mainPostcode;
        String mainActType;
        String dwell;
        String eth;
        String age;
        String sex;
        String income;
        String citizen;
        int license;
        boolean isDriver = false;

        LinkedList<ActivityInfo> activityBudgets;

        LinkedList<ActivityInfo> activitySequence;
        public int mainActCount = 0;

        @Override
        public String toString() {
            return String.format("lic: %d driver: %b acts: %d mainacts: %d", license, isDriver, activitySequence.size(), mainActCount);
        }

        public void allocateActivityTimes() {
            //the start time of the home activity in the evening is allocated first
            ActivityInfo last = activitySequence.getLast();
            double homeBudget = activityBudgets.getFirst().dur;
            last.start = activityBudgets.getFirst().start;
            last.dur = 86400 - activityBudgets.getFirst().start;
            homeBudget = homeBudget - last.dur;
            //the remainder is allocated to the morning
            ActivityInfo first = activitySequence.getFirst();
            first.start = 0;
            first.dur = homeBudget;
            double insideBudget = 0;
            if (mainActCount > 0) {
                double mainBudget = activityBudgets.get(1).dur;
                if (mainActCount > 1) {
                    insideBudget = 0.1 * mainBudget;
                }
                //allocate the main activity time budget
                Iterator<ActivityInfo> iterator = activitySequence.iterator();
                int remainingMainActs = mainActCount;
                int firstMainActMarker = 0;
                int i = 0;
                while (remainingMainActs > 0 && iterator.hasNext()) {
                    ActivityInfo activityInfo = iterator.next();
                    if (activityInfo.type.startsWith("w") || activityInfo.type.contains("school")) {
                        firstMainActMarker = firstMainActMarker == 0 ? i : firstMainActMarker;
                        double allocation = (mainBudget - insideBudget) / mainActCount;
                        activityInfo.dur = allocation;
                        mainBudget = mainBudget - allocation;
                        if (mainActCount - remainingMainActs == 0)
                            activityInfo.start = activityBudgets.get(1).start;
                        remainingMainActs--;
                    }
                    if (activityInfo.pos != null && activityInfo.pos.equals("inside"))
                        activityInfo.dur = insideBudget;
                    i++;
                }
//                go back from the first main activity and assign times
                i = firstMainActMarker - 1;
                double nextStart = activitySequence.get(firstMainActMarker).start;
                while (i >= 0) {
                    ActivityInfo activityInfo = activitySequence.get(i);
                    activityInfo.end = nextStart;
                    activityInfo.start = activityInfo.end - activityInfo.dur;
                    //TODO: what if there are more than one activities before the main act that push back to zero?
                    activityInfo.start = activityInfo.start < 0 ? 0 : activityInfo.start;
                    nextStart = activityInfo.start;
                    i--;
                }
//                go forward from the first main activity and assign times
                i = firstMainActMarker;
                activitySequence.get(i).end = activitySequence.get(i).start + activitySequence.get(i).dur;
                double prevEnd = activitySequence.get(i).end;
                i++;
                while (i < activitySequence.size()) {
                    ActivityInfo activityInfo = activitySequence.get(i);
                    activityInfo.start = prevEnd;
                    activityInfo.end = prevEnd + activityInfo.dur;
                    prevEnd = activityInfo.end;
                    i++;
                }
            } else {
                int i = 0;
                activitySequence.get(i).end = activitySequence.get(i).start + activitySequence.get(i).dur;
                double prevEnd = activitySequence.get(i).end;
                i++;
                while (i < activitySequence.size()) {
                    ActivityInfo activityInfo = activitySequence.get(i);
                    activityInfo.start = prevEnd;
                    activityInfo.end = prevEnd + activityInfo.dur;
                    prevEnd = activityInfo.end;
                    i++;
                }
            }

            if (mainActType != null && mainPostcode == 0) {
                //bad location
                badplanCount++;
                return;
            }
            // iterate through the activities and assign end times based on the last duration
            PopulationFactory populationFactory = scenario.getPopulation().getFactory();
            Person person = populationFactory.createPerson(Id.createPersonId(persid));
            Plan plan = populationFactory.createPlan();
            Leg prevLeg = null;
            ActivityFacility homefacility = scenario.getActivityFacilities().getFacilities().get(Id.create(homePostcode, ActivityFacility.class));
            ActivityFacility mainfacility = scenario.getActivityFacilities().getFacilities().get(Id.create(mainPostcode, ActivityFacility.class));
            int i = 0;
            Activity lastAct = null;
            while (i < activitySequence.size()) {
                if (i > 0) {
                    String legmode = TransportMode.pt;
                    if (isDriver) {
                        legmode = TransportMode.car;
                    } else {
                        if (lastAct.getType().equals("pudo")) {
                            if (!prevLeg.getMode().equals("passenger")) {
                                legmode = "passenger";
                            }
                        } else if (activitySequence.get(i).type.equals("pudo")) {
                            legmode = MatsimRandom.getRandom().nextDouble() > 0.5 ? "passenger" : TransportMode.pt;
                        }
                    }
                    Leg leg = populationFactory.createLeg(legmode);
                    plan.addLeg(leg);
                    switch (legmode) {
                        case TransportMode.car:
                            carlegs++;
                            break;
                        case TransportMode.pt:
                            ptlegs++;
                            break;
                        case "passenger":
                            passengerlegs++;
                            break;
                    }

                }

                ActivityInfo activityInfo = activitySequence.get(i);
                if (i == 0 || i == activitySequence.size() - 1) {
                    Coord homeCoord = homefacility.getCoord();
                    Activity home = (Activity) populationFactory.createActivityFromCoord("home", homeCoord);
                    plan.addActivity(home);
                    if (i == 0)
                        home.setEndTime(activityInfo.end);
                    home.setFacilityId(homefacility.getId());
                    lastAct = home;
                } else {
                    Coord thecoord;
                    if (activityInfo.type.startsWith("w") || activityInfo.type.endsWith("school")) {
                        thecoord = mainfacility.getCoord();
                        Activity mainAct = (Activity) populationFactory.createActivityFromCoord(activityInfo.type, thecoord);
                        plan.addActivity(mainAct);
                        mainAct.setEndTime(activityInfo.end);
                        mainAct.setFacilityId(mainfacility.getId());
                    } else {
                        ActivityFacility activityFacility = scenario.getActivityFacilities().getFacilities().get(Id.create(activityInfo.postcode, ActivityFacility.class));
                        thecoord = activityFacility.getCoord();
                        Activity act = (Activity) populationFactory.createActivityFromCoord(activityInfo.type, thecoord);
                        plan.addActivity(act);
                        act.setEndTime(activityInfo.end);
                        act.setFacilityId(activityFacility.getId());
                    }
                }
                i++;
            }
            person.addPlan(plan);
            scenario.getPopulation().addPerson(person);

        }
    }

    class ActivityInfo {
        String type;
        int postcode;
        double start;
        double dur;
        double end;
        String pos;

        @Override
        public String toString() {
            return String.format("%20s %10s start: %05.0f dur: %05.0f end: %05.0f", type, pos, start, dur, end);
        }
    }

    class Household {
        Map<String, PersonEntry> pax;
        int cars;
        int drivers = 0;

        void assignDrivers() {
            if (cars == 0)
                return;
            List<PersonEntry> potentialDrivers = new ArrayList<>();
            for (PersonEntry personEntry : pax.values()) {
                if (personEntry.license > 0)
                    potentialDrivers.add(personEntry);
            }
            if (potentialDrivers.size() == 0)
                return;
            int carsleft = cars;
            while (carsleft > 0) {
                PersonEntry driver = potentialDrivers.remove(0);
                driver.isDriver = true;
                carsleft--;
                drivers++;
                if (potentialDrivers.size() == 0)
                    return;
            }

        }

        @Override
        public String toString() {
            return String.format("cars: %d drivers: %d", cars, drivers);
        }
    }

    DataBaseAdmin dataBaseAdmin;
    Map<String, Household> households;
    Map<String, PersonEntry> persons;

    /**
     * @param args 0 - facilities table
     *             1 - output file
     *             2 - sample fraction (0,1]
     *             3 - database properties file
     * @throws SQLException
     * @throws NoConnectionException
     * @throws ClassNotFoundException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws IOException
     */
    public static void main(String[] args) throws SQLException, NoConnectionException, ClassNotFoundException,
            InstantiationException, IllegalAccessException, IOException {
        DataBaseAdmin dba = new DataBaseAdmin(new File("/Users/fouriep/IdeaProjects/matsim/playgrounds/pieter/connections/matsim2postgres.properties"));
        MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
        FacilitiesToSQL f2sql = new FacilitiesToSQL(dba, scenario);
        f2sql.loadFacilitiesFromSQL("ro_p_facilities.new_facilities");
        new CapstonePlansWriter(scenario, dba).run("capsUnroutedPlans", 0.25);

    }


    public void run(String plansFile, double fraction) throws SQLException, NoConnectionException {
        long start = System.currentTimeMillis();
        System.out.println(start);
        generateCitizenPlansFromSQL(fraction);

        generateFreightplans(fraction);

        writeObjectAttributes(plansFile);
        writePlans(plansFile);
        writeFacilities(plansFile);
        getAllActivityTypes();

        long end = System.currentTimeMillis();
        System.out.println(end);

        int dur = (int) (end - start) / 1000;
        System.out.printf("Time: %d sec", dur);
        System.err.println("Bad plans: " + badplanCount);
        System.out.println("car legs:" + carlegs);
        System.out.println("pt legs:" + ptlegs);
        System.out.println("psgr legs:" + passengerlegs);
    }

    private void getAllActivityTypes() {
        Map<String,Integer> actTypes = new HashMap<>();

        Map<Id<Person>, ? extends Person> persons = scenario.getPopulation().getPersons();
        for (Person person : persons.values()) {
            Plan plan = person.getPlans().get(0);
            List<PlanElement> planElements = plan.getPlanElements();
            for (PlanElement planElement : planElements) {
                if(planElement instanceof Activity){
                    Activity activity = (Activity) planElement;
                    try{
                        actTypes.put(activity.getType(),actTypes.get(activity.getType())+1);
                    }catch (NullPointerException e){
                        actTypes.put(activity.getType(),1);
                    }
                }
            }

        }
        System.out.println(actTypes.toString());
    }

    private void writeFacilities(String fileName) {
        FacilitiesWriter facilitiesWriter = new FacilitiesWriter(scenario.getActivityFacilities());
        facilitiesWriter.write(fileName+"_facilities.xml.gz");
    }

    private void generateFreightplans(double fraction) throws SQLException, NoConnectionException {
        ResultSet resultSet = dataBaseAdmin.executeQuery("SELECT * FROM " +
                "ro_freight_and_foreign_trips.freighttrips where rand2 <= " + fraction);
        PopulationFactory populationFactory = scenario.getPopulation().getFactory();
        while (resultSet.next()) {
            if (resultSet.getDouble("rand2") <= fraction) {
                int fromCode = resultSet.getInt("from");
                int toCode = resultSet.getInt("to");
                double start = resultSet.getDouble("start");
                ActivityFacility origin = scenario.getActivityFacilities().getFacilities().get(Id.create(fromCode, ActivityFacility.class));
                ActivityFacility destination = scenario.getActivityFacilities().getFacilities().get(Id.create(toCode, ActivityFacility.class));
                Person person = populationFactory.createPerson(Id.createPersonId("freight_" + freightCount++));
                scenario.getPopulation().addPerson(person);
                Plan plan = populationFactory.createPlan();
                person.addPlan(plan);
                Coord orig = origin.getCoord();
                Activity origAct = populationFactory.createActivityFromCoord("freight", orig);
                origAct.setFacilityId(origin.getId());
                origAct.setEndTime(start);
                plan.addActivity(origAct);
                plan.addLeg(populationFactory.createLeg("freight"));
                Coord dest = destination.getCoord();
                Activity destAct = populationFactory.createActivityFromCoord("freight", dest);
                destAct.setFacilityId(destination.getId());
                plan.addActivity(destAct);
            }
        }
    }

    private void writeObjectAttributes(String popname) throws SQLException, NoConnectionException {
        ObjectAttributes personAttributes = new ObjectAttributes();

        ResultSet resultSet = dataBaseAdmin.executeQuery("SELECT * FROM ro_plansgeneration.plans_input_objattr");
        while (resultSet.next()) {
            if (persons.containsKey(resultSet.getString("persid"))) {
                personAttributes.putAttribute(resultSet.getString(1), resultSet.getString(2), resultSet.getString(3));
            }
        }
        for (int i = 0; i < freightCount; i++) {
            personAttributes.putAttribute("freight_" + i, "income_subpop", "freight");

        }
        resultSet.close();
//        add age
        for (PersonEntry personEntry : persons.values()) {
            personAttributes.putAttribute(personEntry.persid,"age",personEntry.age);
        }

        new ObjectAttributesXmlWriter(personAttributes).writeFile(popname + "_objattr.xml.gz");
    }


    private void generateCitizenPlansFromSQL(double fraction) throws SQLException, NoConnectionException {
        ResultSet resultSet = dataBaseAdmin.executeQuery("SELECT p.*  FROM ro_plansgeneration.plans_input p "+
                "ORDER BY p.hhid, persid, \"no\" ;");
        String hhid = null;
        String persid = null;
        Household household = null;
        PersonEntry person = null;


        while (resultSet.next()) {
            String hhid1 = resultSet.getString("hhid");
            if (!hhid1.equals(hhid)) {
                if (household != null) {
                    household.assignDrivers();
                    for (PersonEntry personEntry : household.pax.values()) {
                        personEntry.allocateActivityTimes();
                    }
                }
                hhid = hhid1;
                household = new Household();
                household.cars = resultSet.getInt("car");
                households.put(hhid, household);
                household.pax = new HashMap<>();
            }
            String persid1 = resultSet.getString("persid");
            if (!persid1.equals(persid)) {
                persid = persid1;
                person = new PersonEntry();
                person.hhid = resultSet.getString("hhid");
                person.persid = resultSet.getString("persid");
                person.homePostcode = resultSet.getInt("postcode");
                person.dwell = resultSet.getString("dwell");
                person.eth = resultSet.getString("eth");
                person.age = resultSet.getString("age");
                person.sex = resultSet.getString("sex");
                person.income = resultSet.getString("income");
                person.citizen = resultSet.getString("citizen");
                person.license = resultSet.getInt("license");
                person.activityBudgets = new LinkedList<>();
                person.activitySequence = new LinkedList<>();
                persons.put(persid, person);
                household.pax.put(persid, person);
            }
            if (resultSet.getInt("no") == -2) {
                ActivityInfo activityInfo = new ActivityInfo();
                activityInfo.type = "home";
                activityInfo.start = resultSet.getDouble("start");
                activityInfo.dur = resultSet.getDouble("dur");
                activityInfo.postcode = resultSet.getInt("postcode");
                person.activityBudgets.add(activityInfo);
            } else if (resultSet.getInt("no") == -1) {
                ActivityInfo activityInfo = new ActivityInfo();
                activityInfo.type = resultSet.getString("act_type");
                if(activityInfo.type.equals(  "main")){
                    //bad person entry, remove
                    System.out.println(person);
                    household.pax.remove(persid);
                    persons.remove(persid);
                }
                activityInfo.start = resultSet.getDouble("start");
                activityInfo.dur = resultSet.getDouble("dur");
                activityInfo.postcode = resultSet.getInt("main.postcode");
                person.mainPostcode = resultSet.getInt("main.postcode");
                person.mainActType = resultSet.getString("act_type");
                person.activityBudgets.add(activityInfo);
            } else {
                ActivityInfo activityInfo = new ActivityInfo();
                activityInfo.type = resultSet.getString("type");
                activityInfo.pos = resultSet.getString("pos");
                if (activityInfo.type.equals("h")) {
                    activityInfo.type = "home";
                    activityInfo.postcode = person.homePostcode;
                    try {
                        if (resultSet.getString("act_type").equals("home.visit"))
                            activityInfo.dur = resultSet.getDouble("dur");
                    } catch (NullPointerException ne) {
                    }
                } else if (activityInfo.type.equals("w") || activityInfo.type.equals("s")) {
                    person.mainActCount++;
                    activityInfo.type = person.mainActType;
                    activityInfo.postcode = person.mainPostcode;
                } else {
                    activityInfo.type = resultSet.getString("act_type");
                    activityInfo.dur = resultSet.getDouble("dur");
                    activityInfo.postcode = resultSet.getInt("postcode");
                    person.activityBudgets.add(activityInfo);
                }
                person.activitySequence.add(activityInfo);
            }
        }
    }

    private void writePlans(String plansFile) {
        new PopulationWriter(scenario.getPopulation(), scenario.getNetwork()).write(plansFile+".xml.gz");
    }

}
