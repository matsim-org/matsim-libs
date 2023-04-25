package org.matsim.application.prepare.freight.tripGeneration;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.*;
import org.matsim.application.options.LanduseOptions;
import org.matsim.core.population.PopulationUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

public class FreightAgentGenerator {
    private final LocationCalculator locationCalculator;
    private final DepartureTimeCalculator departureTimeCalculator;
    private final NumOfTripsCalculator numOfTripsCalculator;
    private final PopulationFactory populationFactory;
    private final Network network;

    public FreightAgentGenerator(Network network, Path shpPath, LanduseOptions landUse, double averageLoad, int workingDays, double sample) throws IOException {
        this.locationCalculator = new DefaultLocationCalculator(network, shpPath, landUse);
        this.departureTimeCalculator = new DefaultDepartureTimeCalculator();
        this.numOfTripsCalculator = new DefaultNumberOfTripsCalculator(averageLoad, workingDays, sample);
        this.populationFactory = PopulationUtils.getFactory();
        this.network = network;
    }

    public List<Person> generateRoadFreightAgents(TripRelation tripRelation, String tripRelationId) {
        List<Person> freightAgents = new ArrayList<>();
        String preRunMode = tripRelation.getModePreRun();
        String mainRunMode = tripRelation.getModeMainRun();
        String postRunMode = tripRelation.getModePostRun();

        if (!preRunMode.equals("2") && !mainRunMode.equals("2") && !postRunMode.equals("2")) {
            return freightAgents; // This trip relation is irrelevant as it does not contain any freight by road
        }

        int numOfTrips = numOfTripsCalculator.calculateNumberOfTrips(tripRelation.getTonsPerYearMainRun(), tripRelation.getGoodsTypeMainRun());
        for (int i = 0; i < numOfTrips; i++) {
            // pre-run
            if (preRunMode.equals("2")) {
                Person person = populationFactory.createPerson(Id.createPersonId("freight_" + tripRelationId + "_" + i + "_pre"));
                Plan plan = populationFactory.createPlan();
                double departureTime = departureTimeCalculator.getDepartureTime();

                Id<Link> startLinkId = locationCalculator.getLocationOnNetwork(tripRelation.getOriginalOriginCell());
                Activity startAct = populationFactory.createActivityFromLinkId("freight_start", startLinkId);
                startAct.setCoord(network.getLinks().get(startLinkId).getToNode().getCoord());
                startAct.setEndTime(departureTime);
                plan.addActivity(startAct);

                Leg leg = populationFactory.createLeg("freight");
                plan.addLeg(leg);

                Id<Link> endLinkId = locationCalculator.getLocationOnNetwork(tripRelation.getOriginalCellMainRun());
                Activity endAct = populationFactory.createActivityFromLinkId("freight_end", endLinkId);
                endAct.setCoord(network.getLinks().get(endLinkId).getToNode().getCoord());
                plan.addActivity(endAct);

                person.addPlan(plan);
                person.getAttributes().putAttribute("trip_type", "pre-run");
				LongDistanceFreightUtils.writeCommonAttributesV1(person, tripRelation, tripRelationId);

                freightAgents.add(person);
            }

            // main-run
            if (mainRunMode.equals("2")) {
                Person person = populationFactory.createPerson(Id.createPersonId("freight_" + tripRelationId + "_" + i + "_main"));
                Plan plan = populationFactory.createPlan();
                double departureTime = departureTimeCalculator.getDepartureTime();

                Id<Link> startLinkId = locationCalculator.getLocationOnNetwork(tripRelation.getOriginalCellMainRun());
                Activity startAct = populationFactory.createActivityFromLinkId("freight_start", startLinkId);
                startAct.setEndTime(departureTime);
                startAct.setCoord(network.getLinks().get(startLinkId).getToNode().getCoord());
                plan.addActivity(startAct);

                Leg leg = populationFactory.createLeg("freight");
                plan.addLeg(leg);

                Id<Link> endLinkId = locationCalculator.getLocationOnNetwork(tripRelation.getDestinationCellMainRun());
                Activity endAct = populationFactory.createActivityFromLinkId("freight_end", endLinkId);
                endAct.setCoord(network.getLinks().get(endLinkId).getToNode().getCoord());
                plan.addActivity(endAct);

                person.addPlan(plan);
                person.getAttributes().putAttribute("trip_type", "main-run");
				LongDistanceFreightUtils.writeCommonAttributesV1(person, tripRelation, tripRelationId);

                freightAgents.add(person);
            }

            // post-run
            if (postRunMode.equals("2")) {
                Person person = populationFactory.createPerson(Id.createPersonId("freight_" + tripRelationId + "_" + i + "_post"));
                Plan plan = populationFactory.createPlan();
                double departureTime = departureTimeCalculator.getDepartureTime();

                Id<Link> startLinkId = locationCalculator.getLocationOnNetwork(tripRelation.getDestinationCellMainRun());
                Activity startAct = populationFactory.createActivityFromLinkId("freight_start", startLinkId);
                startAct.setCoord(network.getLinks().get(startLinkId).getToNode().getCoord());
                startAct.setEndTime(departureTime);
                plan.addActivity(startAct);

                Leg leg = populationFactory.createLeg("freight");
                plan.addLeg(leg);

                Id<Link> endLinkId = locationCalculator.getLocationOnNetwork(tripRelation.getFinalDestinationCell());
                Activity endAct = populationFactory.createActivityFromLinkId("freight_end", endLinkId);
                endAct.setCoord(network.getLinks().get(endLinkId).getToNode().getCoord());
                plan.addActivity(endAct);

                person.addPlan(plan);
                person.getAttributes().putAttribute("trip_type", "post-run");
				LongDistanceFreightUtils.writeCommonAttributesV1(person, tripRelation, tripRelationId);

                freightAgents.add(person);
            }
        }

        return freightAgents;
    }

    public interface LocationCalculator {
        Id<Link> getLocationOnNetwork(String verkehrszelle);
    }

    public interface DepartureTimeCalculator {
        double getDepartureTime();
    }

    public interface NumOfTripsCalculator {
        int calculateNumberOfTrips(double tonsPerYear, String goodsType);
    }

}
