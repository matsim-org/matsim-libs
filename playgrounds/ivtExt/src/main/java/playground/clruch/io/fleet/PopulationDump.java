// code by andya
package playground.clruch.io.fleet;

import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;

import playground.clruch.net.DummyStorageSupplier;
import playground.clruch.net.IterationFolder;
import playground.clruch.net.MatsimStaticDatabase;
import playground.clruch.net.OsmLink;
import playground.clruch.net.RequestContainer;
import playground.clruch.net.SimulationObject;
import playground.clruch.net.StorageSupplier;
import playground.clruch.net.StorageUtils;

/** @author Andreas Aumiller */
enum PopulationDump {
    ;

    public static Population of(Population population, Network network, MatsimStaticDatabase db, StorageUtils storageUtils) throws Exception {
        // Parse RequestContainer into population
        List<IterationFolder> list = storageUtils.getAvailableIterations();
        if (list.isEmpty() != true) {
            StorageSupplier storageSupplier = new DummyStorageSupplier();
            System.out.println("INFO initializing factories and properties");
            PopulationFactory populationFactory = population.getFactory();
            int id = 0;
            final int TRAVEL_TIME = 3600;
            for (IterationFolder iter : list) {
                storageSupplier = iter.storageSupplier;
                final int MAX_ITER = storageSupplier.size(); // storageSupplier.size()
                for (int index = 0; index < MAX_ITER; index++) {
                    if (index % 200 == 0) {
                        SimulationObject simulationObject = storageSupplier.getSimulationObject(index);

                        List<RequestContainer> rc = simulationObject.requests;

                        // Initialize all necessary properties
                        for (RequestContainer request : rc) {
                            try {
                                Id<Person> personID = Id.create(request.requestIndex, Person.class);
                                Map<Id<Person>, ? extends Person> persons = population.getPersons();
                                if (persons.containsKey(personID) == false) {
                                    Person person = populationFactory.createPerson(personID);
                                    Plan plan = populationFactory.createPlan();

                                    // Get links and coordinates
                                    OsmLink fromLink = db.getOsmLink(request.fromLinkIndex);
                                    Coord fromCoord = fromLink.getAt(0.5);
                                    OsmLink toLink = db.getOsmLink(request.toLinkIndex);
                                    Coord toCoord = toLink.getAt(0.5);

                                    // Create activities
                                    Activity startActivity = populationFactory.createActivityFromLinkId("activity", fromLink.link.getId());
                                    startActivity.setEndTime(request.submissionTime);
                                    startActivity.setCoord(fromCoord);
                                    Activity endActivity = populationFactory.createActivityFromLinkId("activity", toLink.link.getId());
                                    endActivity.setStartTime(request.submissionTime + TRAVEL_TIME);
                                    endActivity.setCoord(toCoord);

                                    // Create legs and routes
                                    Leg leg = populationFactory.createLeg("av");
                                    // RouteFactory routeFactory = new AVRouteFactory();
                                    // Route route = routeFactory.createRoute(fromLink.link.getId(), toLink.link.getId());
                                    // // NetworkRoute nwRoute = RouteUtils.createLinkNetworkRouteImpl(fromLink.link.getId(), toLink.link.getId());
                                    // // route.setDistance(RouteUtils.calcDistance(nwRoute, 0.5, 0.5, network));
                                    // route.setTravelTime(TRAVEL_TIME);
                                    // route.setRouteDescription("av");
                                    // leg.setRoute(route);
                                    leg.setTravelTime(200);
                                    leg.setDepartureTime(request.submissionTime);
                                    leg.setTravelTime(TRAVEL_TIME);

                                    // Add person to the population
                                    if (id % 100 == 0)
                                        System.out.println("INFO Adding request ID " + request.requestIndex + " to population");
                                    plan.addActivity(startActivity);
                                    plan.addLeg(leg);
                                    plan.addActivity(endActivity);
                                    person.addPlan(plan);
                                    population.addPerson(person);
                                    id++;
                                }
                            } catch (Exception e) {
                                System.err.println("WARN failed to add request ID " + request.requestIndex + " to current population container");
                            }
                        }
                    }
                }
            }
        }

        return population;
    }
}
