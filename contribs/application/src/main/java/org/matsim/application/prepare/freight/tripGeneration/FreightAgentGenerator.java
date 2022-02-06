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

    public FreightAgentGenerator(Network network, Path shpPath, LanduseOptions landUse) throws IOException {
        this.locationCalculator = new DefaultLocationCalculator(network, shpPath, landUse);
        this.departureTimeCalculator = new DefaultDepartureTimeCalculator();
        this.numOfTripsCalculator = new DefaultNumberOfTripsCalculator();
        this.populationFactory = PopulationUtils.getFactory();
    }

    public List<Person> generateFreightAgents(TripRelation tripRelation, String tripRelationId) {
        List<Person> freightAgents = new ArrayList<>();
        String preRunMode = tripRelation.getModePreRun();
        String mainRunMode = tripRelation.getModeMainRun();
        String postRunMode = tripRelation.getModePostRun();

        if (!preRunMode.equals("2") && !mainRunMode.equals("2") && !postRunMode.equals("2")) {
            return freightAgents; // This trip relation is irrelevant as it does not contain any freight by road
        }

        int numOfTrips = numOfTripsCalculator.calculateNumberOfTrips(tripRelation.getTonsPerYear(), tripRelation.getGoodsType());
        for (int i = 0; i < numOfTrips; i++) {
            Person person = populationFactory.createPerson(Id.createPersonId("freight_" + tripRelationId + "_" + i));
            Plan plan = populationFactory.createPlan();

            double departureTime = departureTimeCalculator.getDepartureTime();
            // pre-run
            if (preRunMode.equals("2")) {
                Activity startAct = populationFactory.createActivityFromLinkId
                        ("freight", locationCalculator.getLocationOnNetwork(tripRelation.getOriginalCell()));
                startAct.setEndTime(departureTime);
                plan.addActivity(startAct);

                Leg leg = populationFactory.createLeg("freight");
                plan.addLeg(leg);

                Activity endAct = populationFactory.createActivityFromLinkId
                        ("freight", locationCalculator.getLocationOnNetwork(tripRelation.getOriginalCellMainRun()));
                if (mainRunMode.equals("2")) {
                    endAct.setEndTime(departureTime + 1);
                }
                plan.addActivity(endAct);
            }

            // main-run
            if (mainRunMode.equals("2")) {
                if (!preRunMode.equals("2")) {
                    Activity startAct = populationFactory.createActivityFromLinkId
                            ("freight", locationCalculator.getLocationOnNetwork(tripRelation.getOriginalCellMainRun()));
                    startAct.setEndTime(departureTime + 1);
                    plan.addActivity(startAct);
                }

                Leg leg = populationFactory.createLeg("freight");
                plan.addLeg(leg);

                Activity endAct = populationFactory.createActivityFromLinkId
                        ("freight", locationCalculator.getLocationOnNetwork(tripRelation.getDestinationCellMainRun()));
                if (postRunMode.equals("2")) {
                    endAct.setEndTime(departureTime + 2);
                }
                plan.addActivity(endAct);
            }

            // post-run
            if (postRunMode.equals("2")) {
                if (!mainRunMode.equals("2")) {
                    Activity startAct = populationFactory.createActivityFromLinkId
                            ("freight", locationCalculator.getLocationOnNetwork(tripRelation.getDestinationCellMainRun()));
                    startAct.setEndTime(departureTime + 2);
                    plan.addActivity(startAct);
                }

                Leg leg = populationFactory.createLeg("freight");
                plan.addLeg(leg);

                Activity endAct = populationFactory.createActivityFromLinkId
                        ("freight", locationCalculator.getLocationOnNetwork(tripRelation.getDestinationCell()));
                plan.addActivity(endAct);
            }
            person.addPlan(plan);

            // Write down attributes
            person.getAttributes().putAttribute("subpopulation", "freight");
            person.getAttributes().putAttribute("trip_relation_index", tripRelationId);
            person.getAttributes().putAttribute("pre-run_mode", tripRelation.getModePreRun());
            person.getAttributes().putAttribute("main-run_mode", tripRelation.getModeMainRun());
            person.getAttributes().putAttribute("post-run_mode", tripRelation.getModePostRun());
            person.getAttributes().putAttribute("initial_origin_cell", tripRelation.getOriginalCell());
            person.getAttributes().putAttribute("origin_cell_main_run", tripRelation.getDestinationCellMainRun());
            person.getAttributes().putAttribute("destination_cell_main_run", tripRelation.getDestinationCellMainRun());
            person.getAttributes().putAttribute("final_destination_cell", tripRelation.getDestinationCell());
            person.getAttributes().putAttribute("goods_type", tripRelation.getGoodsType());
            person.getAttributes().putAttribute("tons_per_year", tripRelation.getTonsPerYear());

            // Finally, add person to the list for output
            freightAgents.add(person);
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
