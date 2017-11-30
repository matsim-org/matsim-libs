// code by andya
package playground.clruch.io.fleet;

import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.population.routes.GenericRouteFactory;
import org.matsim.core.population.routes.RouteFactory;

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
            for (IterationFolder iter : list) {
                storageSupplier = iter.storageSupplier;
                final int MAX_ITER = storageSupplier.size(); // storageSupplier.size()
                for (int index = 0; index < MAX_ITER / 10; index++) {
                    SimulationObject simulationObject = storageSupplier.getSimulationObject(index);

                    List<RequestContainer> rc = simulationObject.requests;

                    // Initialize all necessary properties
                    for (RequestContainer request : rc) {
                        try {
                            Id<Person> personID = Id.create(id, Person.class);
                            Person person = populationFactory.createPerson(personID);
                            Plan plan = populationFactory.createPlan();

                            OsmLink fromLink = db.getOsmLink(request.fromLinkIndex);
                            OsmLink toLink = db.getOsmLink(request.toLinkIndex);

                            Activity startActivity = populationFactory.createActivityFromLinkId("activitiy", fromLink.link.getId());
                            Activity endActivity = populationFactory.createActivityFromLinkId("activitiy", toLink.link.getId());
                            Leg leg = populationFactory.createLeg("av");
                            RouteFactory rf = new GenericRouteFactory();
                            Route route = rf.createRoute(fromLink.link.getId(), toLink.link.getId());
                            leg.setDepartureTime(request.submissionTime);
                            // leg.setTravelTime(200);
                            leg.setRoute(route);

                            // Add person to the population
                            if (id % 100 == 0)
                                System.out.println("INFO Adding person ID " + id + " to population");
                            plan.addActivity(startActivity);
                            plan.addLeg(leg);
                            person.addPlan(plan);
                            plan.addActivity(endActivity);
                            population.addPerson(person);
                            id++;
                        } catch (Exception e) {
                            System.err.println("WARN failed to add person ID " + id + " to current population container");
                        }
                    }
                }
            }
        }

        return population;
    }
}
