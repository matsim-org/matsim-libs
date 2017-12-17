// code by andya
package playground.clruch.io.fleet;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;

import ch.ethz.idsc.queuey.util.GlobalAssert;
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
        HashSet<RequestContainer> allRequests = findAllRequests(storageUtils);
        final int TRAVEL_TIME = 3600; // TODO magic const.
        fillPopulation(allRequests, population, db, TRAVEL_TIME);

        System.out.println(allRequests.size());
        System.out.println(population.getPersons().size());

        return population;
    }

    private static HashSet<RequestContainer> findAllRequests(StorageUtils storageUtils) throws Exception {
        System.out.println("INFO Remove redundant requests from SimulationObjects");
        HashMap<Integer, RequestContainer> uniqueRequests = new HashMap<>();
        HashSet<RequestContainer> allRequests = new HashSet<>();
        List<IterationFolder> list = storageUtils.getAvailableIterations();
        for (IterationFolder iter : list) {
            StorageSupplier storageSupplier = iter.storageSupplier;
            for (int i = 0; i < storageSupplier.size(); ++i) {
                SimulationObject simobj = storageSupplier.getSimulationObject(i);
                List<RequestContainer> rcs = simobj.requests;
                rcs.stream().forEach(rc -> uniqueRequests.put(rc.requestIndex, rc));
            }
        }
        uniqueRequests.values().forEach(r -> allRequests.add(r));
        System.out.println("INFO Passing " + allRequests.size() + " unique Requests");
        System.out.println("INFO From " + uniqueRequests.size() + " total amount of Requests");
        return allRequests;
    }

    private static void fillPopulation(HashSet<RequestContainer> allRequests, Population population, MatsimStaticDatabase db, int TRAVEL_TIME) {
        PopulationFactory populationFactory = population.getFactory();
        for (RequestContainer rc : allRequests) {
            Id<Person> personID = Id.create(rc.requestIndex, Person.class);

            Person person = populationFactory.createPerson(personID);
            Plan plan = populationFactory.createPlan();

            // Get links
            OsmLink fromLink = db.getOsmLink(rc.fromLinkIndex);
            OsmLink toLink = db.getOsmLink(rc.toLinkIndex);

            // Create activities
            Activity startActivity = populationFactory.createActivityFromLinkId("activity", fromLink.link.getId());
            startActivity.setEndTime(rc.submissionTime);
            Activity endActivity = populationFactory.createActivityFromLinkId("activity", toLink.link.getId());
            endActivity.setStartTime(rc.submissionTime + TRAVEL_TIME);

            // Create legs and routes
            Leg leg = populationFactory.createLeg("av");
            // leg.setTravelTime(200);
            // leg.setDepartureTime(rc.submissionTime);
            // leg.setTravelTime(TRAVEL_TIME);
            plan.addActivity(startActivity);
            plan.addLeg(leg);
            plan.addActivity(endActivity);
            person.addPlan(plan);
            population.addPerson(person);
        }
        GlobalAssert.that(population.getPersons().size() == allRequests.size());
    }
}