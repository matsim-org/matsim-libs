package playground.mzilske.cdr;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.core.router.ActivityWrapperFacility;
import org.matsim.core.router.PlanRouter;
import org.matsim.core.router.TripRouter;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.population.algorithms.ParallelPersonAlgorithmRunner;
import org.matsim.population.algorithms.ParallelPersonAlgorithmRunner.PersonAlgorithmProvider;
import org.matsim.population.algorithms.PersonAlgorithm;
import playground.mzilske.cdr.ZoneTracker.LinkToZoneResolver;
import playground.mzilske.d4d.NetworkRoutingModule;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.*;


public class PopulationFromSightings {


    private static final int TRY_REDRAW_UNFEASIBLE_LOCATIONS = 0;

    public static void createPopulationWithTwoPlansEach(Scenario scenario, LinkToZoneResolver zones, final playground.mzilske.cdr.Sightings sightings) {
        for (Entry<Id, List<Sighting>> sightingsPerPerson : sightings.getSightingsPerPerson().entrySet()) {
            Id personId = sightingsPerPerson.getKey();
            List<Sighting> sightingsForThisPerson = sightingsPerPerson.getValue();
            Person person = scenario.getPopulation().getFactory().createPerson(personId);
            Plan plan1 = createPlanWithEndTimeAtLastSighting(scenario, zones,
                    sightingsForThisPerson);
            person.addPlan(plan1);
            Plan plan2 = createPlanWithEndTimeAtNextSightingElsewhere(scenario, zones,
                    sightingsForThisPerson);
            person.addPlan(plan2);
            scenario.getPopulation().addPerson(person);
        }
    }

    public static void createPopulationWithEndTimesAtLastSightings(Scenario scenario, LinkToZoneResolver zones, final playground.mzilske.cdr.Sightings sightings) {
        for (Entry<Id, List<Sighting>> sightingsPerPerson : sightings.getSightingsPerPerson().entrySet()) {
            Id personId = sightingsPerPerson.getKey();
            List<Sighting> sightingsForThisPerson = sightingsPerPerson.getValue();
            Person person = scenario.getPopulation().getFactory().createPerson(personId);
            Plan plan1 = createPlanWithEndTimeAtLastSighting(scenario, zones,
                    sightingsForThisPerson);
            person.addPlan(plan1);

            scenario.getPopulation().addPerson(person);
        }
    }

    public static void createPopulationWithRandomEndTimesInPermittedWindow(Scenario scenario, LinkToZoneResolver zones, final playground.mzilske.cdr.Sightings sightings) {
        for (Entry<Id, List<Sighting>> sightingsPerPerson : sightings.getSightingsPerPerson().entrySet()) {
            Id personId = sightingsPerPerson.getKey();
            List<Sighting> sightingsForThisPerson = sightingsPerPerson.getValue();
            Person person = scenario.getPopulation().getFactory().createPerson(personId);
            Plan plan1 = createPlanWithRandomEndTimesInPermittedWindow(scenario, zones,
                    sightingsForThisPerson);
            person.addPlan(plan1);
            scenario.getPopulation().addPerson(person);
        }
    }

    public static void createClonedPopulationWithRandomEndTimesInPermittedWindow(Scenario scenario, LinkToZoneResolver zones, final Map<Id, List<Sighting>> sightings, int clonefactor) {
        if (clonefactor < 2)
            return;
        for (Entry<Id, List<Sighting>> sightingsPerPerson : sightings.entrySet()) {
            Id personId = sightingsPerPerson.getKey();
            List<Sighting> sightingsForThisPerson = sightingsPerPerson.getValue();
            Person person = scenario.getPopulation().getFactory().createPerson(personId);
            Plan plan1 = createPlanWithRandomEndTimesInPermittedWindow(scenario, zones,
                    sightingsForThisPerson);
            person.addPlan(plan1);
            Plan plan2 = scenario.getPopulation().getFactory().createPlan();
            person.addPlan(plan2);
            person.setSelectedPlan(new RandomPlanSelector<Plan, Person>().selectPlan(person));
            scenario.getPopulation().addPerson(person);
            for (int i = 0; i < clonefactor - 1; i++) {
                Id<Person> cloneId = Id.create("I" + i + "_" + person.getId().toString(), Person.class);
                Person clone = scenario.getPopulation().getFactory().createPerson(cloneId);
                Plan clonePlan = createPlanWithRandomEndTimesInPermittedWindow(scenario, zones,
                        sightingsForThisPerson);
                clone.addPlan(clonePlan);
                Plan clonePlan2 = scenario.getPopulation().getFactory().createPlan();
                clone.addPlan(clonePlan2);
                clone.setSelectedPlan(new RandomPlanSelector<Plan, Person>().selectPlan(clone));
                scenario.getPopulation().addPerson(clone);
            }
        }
    }


    public static Plan createPlanWithEndTimeAtLastSighting(Scenario scenario,
                                                           LinkToZoneResolver zones, List<Sighting> sightingsForThisPerson) {
        Plan plan = scenario.getPopulation().getFactory().createPlan();
        boolean first = true;
        Map<Activity, String> cellsOfSightings;
        cellsOfSightings = new HashMap<>();
        for (Sighting sighting : sightingsForThisPerson) {
            String zoneId = sighting.getCellTowerId();
            Activity activity = createActivityInZone(scenario, zones,
                    zoneId);
            cellsOfSightings.put(activity, zoneId);
            activity.setEndTime(sighting.getTime());
            if (first) {
                plan.addActivity(activity);
                first = false;
            } else {
                Activity lastActivity = (Activity) plan.getPlanElements().get(plan.getPlanElements().size()-1);
                if ( !(zoneId.equals(cellsOfSightings.get(lastActivity))) ) {
                    Leg leg = scenario.getPopulation().getFactory().createLeg("car");
                    plan.addLeg(leg);
                    plan.addActivity(activity);
                } else {
                    lastActivity.setEndTime(sighting.getTime());
                }
            }
        }
        return plan;
    }


    public static Plan createPlanWithEndTimeAtNextSightingElsewhere(Scenario scenario,
                                                                    LinkToZoneResolver zones, List<Sighting> sightingsForThisPerson) {
        Plan plan = scenario.getPopulation().getFactory().createPlan();
        boolean first = true;
        Map<Activity, String> cellsOfSightings;
        cellsOfSightings = new HashMap<Activity, String>();
        for (Sighting sighting : sightingsForThisPerson) {
            String zoneId = sighting.getCellTowerId();
            Activity activity = createActivityInZone(scenario, zones,
                    zoneId);
            cellsOfSightings.put(activity, zoneId);
            activity.setEndTime(sighting.getTime());
            if (first) {
                plan.addActivity(activity);
                first = false;
            } else {
                Activity lastActivity = (Activity) plan.getPlanElements().get(plan.getPlanElements().size()-1);
                if ( !(zoneId.equals(cellsOfSightings.get(lastActivity))) ) {
                    Leg leg = scenario.getPopulation().getFactory().createLeg("car");
                    plan.addLeg(leg);
                    plan.addActivity(activity);
                    TripRouter tripRouter = new TripRouter();
                    tripRouter.setRoutingModule("car", new NetworkRoutingModule(scenario.getPopulation().getFactory(), scenario.getNetwork(), new FreeSpeedTravelTime()));
                    List<? extends PlanElement> route = tripRouter.calcRoute("car", new ActivityWrapperFacility(lastActivity), new ActivityWrapperFacility(activity), sighting.getTime(), null);
                    double travelTime = ((Leg) route.get(0)).getTravelTime();
                    lastActivity.setEndTime(sighting.getTime() - travelTime);
                } else {
                    lastActivity.setEndTime(sighting.getTime());
                }
            }
        }
        return plan;
    }

    public static Plan createPlanWithRandomEndTimesInPermittedWindow(Scenario scenario,
                                                                    LinkToZoneResolver zones, List<Sighting> sightingsForThisPerson) {
        Plan plan = scenario.getPopulation().getFactory().createPlan();
        boolean first = true;
        Map<Activity, String> cellsOfSightings;
        cellsOfSightings = new HashMap<>();
        for (Sighting sighting : sightingsForThisPerson) {
            String zoneId = sighting.getCellTowerId();
            Activity activity = createActivityInZone(scenario, zones,
                    zoneId);
            cellsOfSightings.put(activity, zoneId);
            activity.setEndTime(sighting.getTime());
            if (first) {
                plan.addActivity(activity);
                first = false;
            } else {
                Activity lastActivity = (Activity) plan.getPlanElements().get(plan.getPlanElements().size()-1);
                if ( !(zoneId.equals(cellsOfSightings.get(lastActivity))) ) {
                    Leg leg = scenario.getPopulation().getFactory().createLeg("car");
                    plan.addLeg(leg);
                    plan.addActivity(activity);
                    TripRouter tripRouter = new TripRouter();
                    FreeSpeedTravelTime linkTravelTime = new FreeSpeedTravelTime();
                    tripRouter.setRoutingModule("car", new NetworkRoutingModule(scenario.getPopulation().getFactory(), scenario.getNetwork(), linkTravelTime));
                    List<? extends PlanElement> route = tripRouter.calcRoute("car", new ActivityWrapperFacility(lastActivity), new ActivityWrapperFacility(activity), sighting.getTime(), null);
                    Leg routerLeg = (Leg) route.get(0);
                    double travelTime = routerLeg.getTravelTime();
                    double correctedTravelTime = travelTime + linkTravelTime.getLinkTravelTime(scenario.getNetwork().getLinks().get(activity.getLinkId()), routerLeg.getDepartureTime() + travelTime, null, null);
                    double latestStartTime = sighting.getTime() - correctedTravelTime;
                    double earliestStartTime = lastActivity.getEndTime();
                    double startTime = earliestStartTime + (Math.random() * (latestStartTime - earliestStartTime));
                    lastActivity.setEndTime(startTime);
                } else {
                    lastActivity.setEndTime(sighting.getTime());
                }
            }
        }
        return plan;
    }


    public static Activity createActivityInZone(Scenario scenario, LinkToZoneResolver zones, String zoneId) {
        return scenario.getPopulation().getFactory().createActivityFromLinkId("sighting", zones.chooseLinkInZone(zoneId));
    }



    public static void preparePopulation(final Scenario scenario, final LinkToZoneResolver linkToZoneResolver2, final playground.mzilske.cdr.Sightings allSightings) {
        ParallelPersonAlgorithmRunner.run(scenario.getPopulation(), 8, new org.matsim.population.algorithms.XY2Links(scenario));
        ParallelPersonAlgorithmRunner.run(scenario.getPopulation(), 8, new PersonAlgorithmProvider() {

            @Override
            public PersonAlgorithm getPersonAlgorithm() {
                TripRouter tripRouter = new TripRouter();
                tripRouter.setRoutingModule("car", new NetworkRoutingModule(scenario.getPopulation().getFactory(), scenario.getNetwork(), new FreeSpeedTravelTime()));
                return new PlanRouter(tripRouter);
            }

        });

        Population unfeasiblePeople = PopulationUtils.createPopulation(scenario.getConfig(), scenario.getNetwork());

        for (int i=0; i < TRY_REDRAW_UNFEASIBLE_LOCATIONS; i++) {
            unfeasiblePeople = PopulationUtils.createPopulation(scenario.getConfig(), scenario.getNetwork());
            for (Person person : scenario.getPopulation().getPersons().values()) {
                Plan plan = person.getSelectedPlan();
                if (!isFeasible(plan)) {
                    unfeasiblePeople.addPerson(person);
                }
            }
            System.out.println("Unfeasible plans: " + unfeasiblePeople.getPersons().size() + " of " +scenario.getPopulation().getPersons().size());

            ParallelPersonAlgorithmRunner.run(unfeasiblePeople, 8, new PersonAlgorithm() {

                @Override
                public void run(Person person) {
                    Iterator<Sighting> sightingsForThisAgent = allSightings.getSightingsPerPerson().get(person.getId()).iterator();
                    for (PlanElement planElement : person.getSelectedPlan().getPlanElements()) {
                        if (planElement instanceof Activity) {
                            Sighting sighting = sightingsForThisAgent.next();
                            ActivityImpl activity = (ActivityImpl) planElement;
                            activity.setLinkId(linkToZoneResolver2.chooseLinkInZone(sighting.getCellTowerId()));
                        }
                    }
                }

            });

            ParallelPersonAlgorithmRunner.run(unfeasiblePeople, 8, new org.matsim.population.algorithms.XY2Links(scenario));


            ParallelPersonAlgorithmRunner.run(unfeasiblePeople, 8, new PersonAlgorithmProvider() {

                @Override
                public PersonAlgorithm getPersonAlgorithm() {
                    TripRouter tripRouter = new TripRouter();
                    tripRouter.setRoutingModule("car", new NetworkRoutingModule(scenario.getPopulation().getFactory(), scenario.getNetwork(), new FreeSpeedTravelTime()));
                    return new PlanRouter(tripRouter);
                }

            });

        }

        for (Person person : unfeasiblePeople.getPersons().values()) {
            scenario.getPopulation().getPersons().remove(person.getId());
        }
    }


    private static boolean isFeasible(Plan plan) {
        double currentTime = 0.0;
        for (PlanElement planElement : plan.getPlanElements()) {
            if (planElement instanceof Leg) {
                LegImpl leg = (LegImpl) planElement;
                currentTime = leg.getArrivalTime();
            } else if (planElement instanceof Activity) {
                ActivityImpl activity = (ActivityImpl) planElement;
                double sightingTime = activity.getEndTime();
                if (sightingTime < currentTime) {
                    return false;
                }
            }
        }
        return true;
    }

    public static void createPopulationWithRandomRealization(final Scenario scenario, final playground.mzilske.cdr.Sightings sightings, final LinkToZoneResolver zones) {
        ExecutorService executorService = Executors.newFixedThreadPool(8);
        List<Callable<Person>> jobs = new ArrayList<>();
        for (final Entry<Id, List<Sighting>> sightingsPerPerson : sightings.getSightingsPerPerson().entrySet()) {
            jobs.add(new Callable<Person>() {

                @Override
                public Person call() throws Exception {
                    Id<Person> personId = sightingsPerPerson.getKey();
                    Person person = scenario.getPopulation().getFactory().createPerson(personId);
                    Plan plan = PopulationFromSightings.createPlanWithRandomEndTimesInPermittedWindow(scenario, zones, sightings.getSightingsPerPerson().get(personId));
                    for (PlanElement pe : plan.getPlanElements()) {
                        if (pe instanceof Leg) {
                            ((Leg) pe).setMode("car");
                        }
                    }
                    person.addPlan(plan);
                    return person;
                }
            });
        }
        try {
            for (Future<Person> person : executorService.invokeAll(jobs)) {
                scenario.getPopulation().addPerson(person.get());
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
        executorService.shutdown();
    }
}
