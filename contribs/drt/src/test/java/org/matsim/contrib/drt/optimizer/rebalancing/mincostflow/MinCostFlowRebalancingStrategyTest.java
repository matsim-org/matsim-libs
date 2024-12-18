package org.matsim.contrib.drt.optimizer.rebalancing.mincostflow;

import com.google.common.collect.ImmutableMap;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.prep.PreparedPolygon;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.common.zones.Zone;
import org.matsim.contrib.common.zones.ZoneImpl;
import org.matsim.contrib.common.zones.ZoneSystem;
import org.matsim.contrib.common.zones.ZoneSystemImpl;
import org.matsim.contrib.drt.analysis.zonal.MostCentralDrtZoneTargetLinkSelector;
import org.matsim.contrib.drt.optimizer.rebalancing.RebalancingParams;
import org.matsim.contrib.drt.optimizer.rebalancing.RebalancingStrategy;
import org.matsim.contrib.drt.optimizer.rebalancing.demandestimator.PreviousIterationDrtDemandEstimator;
import org.matsim.contrib.drt.optimizer.rebalancing.targetcalculator.DemandEstimatorAsTargetCalculator;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.dvrp.fleet.*;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.geometry.GeometryUtils;

import java.util.*;

public class MinCostFlowRebalancingStrategyTest {

    private static final int ESTIMATION_PERIOD = 1800;

    private final Network network = createNetwork();

    private final Link link1 = network.getLinks().get(Id.createLinkId("link_1"));
    private final Link link2 = network.getLinks().get(Id.createLinkId("link_2"));

    private final Zone zone1 = new ZoneImpl(
            Id.create("zone_1", Zone.class),
            new PreparedPolygon(GeometryUtils.createGeotoolsPolygon(
                    List.of(
                            new Coord(0, 0),
                            new Coord(0, 500),
                            new Coord(500, 500),
                            new Coord(500, 0),
                            new Coord(0, 0)
                    ))), "dummy");

    private final Zone zone2 = new ZoneImpl(
            Id.create("zone_2", Zone.class),
            new PreparedPolygon(GeometryUtils.createGeotoolsPolygon(
                    List.of(
                            new Coord(500, 0),
                            new Coord(500, 500),
                            new Coord(1000, 500),
                            new Coord(1000, 0),
                            new Coord(500, 0)
                    ))), "dummy");

    private final ZoneSystem zonalSystem = new ZoneSystemImpl(List.of(zone1, zone2), coord -> {
        if (coord == link1.getToNode().getCoord()) {
            return Optional.of(zone1);
        } else if (coord == link2.getToNode().getCoord()) {
            return Optional.of(zone2);
        } else {
            throw new RuntimeException();
        }
    }, network);


    @Test
    void testEmptyDemandAndTarget() {
        PreviousIterationDrtDemandEstimator estimator = createEstimator();
        DemandEstimatorAsTargetCalculator targetCalculator = new DemandEstimatorAsTargetCalculator(estimator, ESTIMATION_PERIOD);

        RebalancingParams rebalancingParams = new RebalancingParams();
        MinCostFlowRebalancingStrategyParams minCostFlowRebalancingStrategyParams = new MinCostFlowRebalancingStrategyParams();
        minCostFlowRebalancingStrategyParams.targetAlpha = 1.;
        minCostFlowRebalancingStrategyParams.targetBeta = 0.;
        rebalancingParams.addParameterSet(minCostFlowRebalancingStrategyParams);

        AggregatedMinCostRelocationCalculator relocationCalculator = new AggregatedMinCostRelocationCalculator(new MostCentralDrtZoneTargetLinkSelector(zonalSystem));
        MinCostFlowRebalancingStrategy strategy = new MinCostFlowRebalancingStrategy(targetCalculator,
                zonalSystem, createEmptyFleet(), relocationCalculator, rebalancingParams);

        Map<Zone, List<DvrpVehicle>> rebalanceableVehicles = new HashMap<>();
        List<RebalancingStrategy.Relocation> relocations = strategy.calculateMinCostRelocations(0, rebalanceableVehicles, Collections.emptyMap());
        Assertions.assertThat(relocations.isEmpty());
    }

    @Test
    void testDemandWithoutSurplus() {
        PreviousIterationDrtDemandEstimator estimator = createEstimator();
        DemandEstimatorAsTargetCalculator targetCalculator = new DemandEstimatorAsTargetCalculator(estimator, ESTIMATION_PERIOD);

        RebalancingParams rebalancingParams = new RebalancingParams();
        MinCostFlowRebalancingStrategyParams minCostFlowRebalancingStrategyParams = new MinCostFlowRebalancingStrategyParams();
        minCostFlowRebalancingStrategyParams.targetAlpha = 1.;
        minCostFlowRebalancingStrategyParams.targetBeta = 0.;
        rebalancingParams.addParameterSet(minCostFlowRebalancingStrategyParams);

        AggregatedMinCostRelocationCalculator relocationCalculator = new AggregatedMinCostRelocationCalculator(new MostCentralDrtZoneTargetLinkSelector(zonalSystem));
        MinCostFlowRebalancingStrategy strategy = new MinCostFlowRebalancingStrategy(targetCalculator,
                zonalSystem, createEmptyFleet(), relocationCalculator, rebalancingParams);

        //time bin 0-1800
        estimator.handleEvent(departureEvent(100, link1, TransportMode.drt));
        estimator.handleEvent(departureEvent(200, link1, TransportMode.drt));
        estimator.handleEvent(departureEvent(500, link2, TransportMode.drt));
        estimator.handleEvent(departureEvent(1500, link1, TransportMode.drt));
        estimator.reset(1);

        Map<Zone, List<DvrpVehicle>> rebalanceableVehicles = new HashMap<>();
        List<RebalancingStrategy.Relocation> relocations = strategy.calculateMinCostRelocations(0, rebalanceableVehicles, Collections.emptyMap());
        Assertions.assertThat(relocations.isEmpty());
    }

    @Test
    void testDemandWithSurplus() {
        PreviousIterationDrtDemandEstimator estimator = createEstimator();
        DemandEstimatorAsTargetCalculator targetCalculator = new DemandEstimatorAsTargetCalculator(estimator, ESTIMATION_PERIOD);

        RebalancingParams rebalancingParams = new RebalancingParams();
        MinCostFlowRebalancingStrategyParams minCostFlowRebalancingStrategyParams = new MinCostFlowRebalancingStrategyParams();
        minCostFlowRebalancingStrategyParams.targetAlpha = 1.;
        minCostFlowRebalancingStrategyParams.targetBeta = 0.;
        rebalancingParams.addParameterSet(minCostFlowRebalancingStrategyParams);

        AggregatedMinCostRelocationCalculator relocationCalculator = new AggregatedMinCostRelocationCalculator(new MostCentralDrtZoneTargetLinkSelector(zonalSystem));
        MinCostFlowRebalancingStrategy strategy = new MinCostFlowRebalancingStrategy(targetCalculator,
                zonalSystem, createEmptyFleet(), relocationCalculator, rebalancingParams);

        // 3 expected trips in zone 1
        estimator.handleEvent(departureEvent(100, link1, TransportMode.drt));
        estimator.handleEvent(departureEvent(200, link1, TransportMode.drt));
        estimator.handleEvent(departureEvent(300, link1, TransportMode.drt));
        // 1 expected trip in zone 2
        estimator.handleEvent(departureEvent(100, link2, TransportMode.drt));
        estimator.reset(1);

        Map<Zone, List<DvrpVehicle>> rebalanceableVehicles = new HashMap<>();

        // 4 vehicles in zone 1 (surplus = 1)
        List<DvrpVehicle> rebalanceableVehiclesZone1 = new ArrayList<>();
        rebalanceableVehiclesZone1.add(getDvrpVehicle(Id.create("a1", DvrpVehicle.class), link1));
        rebalanceableVehiclesZone1.add(getDvrpVehicle(Id.create("a2", DvrpVehicle.class), link1));
        rebalanceableVehiclesZone1.add(getDvrpVehicle(Id.create("a3", DvrpVehicle.class), link1));
        rebalanceableVehiclesZone1.add(getDvrpVehicle(Id.create("a4", DvrpVehicle.class), link1));
        rebalanceableVehicles.put(zone1, rebalanceableVehiclesZone1);

        List<RebalancingStrategy.Relocation> relocations = strategy.calculateMinCostRelocations(0, rebalanceableVehicles, Collections.emptyMap());
        Assertions.assertThat(relocations.size()).isEqualTo(1);
        Assertions.assertThat(relocations.getFirst().link.getId()).isEqualTo(link2.getId());

        rebalanceableVehicles.clear();

        // 5 vehicles in zone 1 (surplus = 2)
        rebalanceableVehiclesZone1.add(getDvrpVehicle(Id.create("a1", DvrpVehicle.class), link1));
        rebalanceableVehiclesZone1.add(getDvrpVehicle(Id.create("a2", DvrpVehicle.class), link1));
        rebalanceableVehiclesZone1.add(getDvrpVehicle(Id.create("a3", DvrpVehicle.class), link1));
        rebalanceableVehiclesZone1.add(getDvrpVehicle(Id.create("a4", DvrpVehicle.class), link1));
        rebalanceableVehiclesZone1.add(getDvrpVehicle(Id.create("a5", DvrpVehicle.class), link1));
        rebalanceableVehicles.put(zone1, rebalanceableVehiclesZone1);

        //set alpha to 2 -> send two vehicles to zone 2
        minCostFlowRebalancingStrategyParams.targetAlpha = 2.;
        List<RebalancingStrategy.Relocation> relocations2 = strategy.calculateMinCostRelocations(0, rebalanceableVehicles, Collections.emptyMap());
        Assertions.assertThat(relocations2.size()).isEqualTo(2);
        Assertions.assertThat(relocations2.getFirst().link.getId()).isEqualTo(link2.getId());
        Assertions.assertThat(relocations2.getLast().link.getId()).isEqualTo(link2.getId());
    }

    @Test
    void testDemandWithSurplusZoneBasedTargetRates() {

        // set attributes
        zone1.getAttributes().putAttribute(MinCostFlowRebalancingStrategy.REBALANCING_ZONAL_TARGET_ALPHA, 0.);
        zone1.getAttributes().putAttribute(MinCostFlowRebalancingStrategy.REBALANCING_ZONAL_TARGET_BETA, 0.);
        zone2.getAttributes().putAttribute(MinCostFlowRebalancingStrategy.REBALANCING_ZONAL_TARGET_ALPHA, 1.);
        zone2.getAttributes().putAttribute(MinCostFlowRebalancingStrategy.REBALANCING_ZONAL_TARGET_BETA, 0.);


        PreviousIterationDrtDemandEstimator estimator = createEstimator();
        DemandEstimatorAsTargetCalculator targetCalculator = new DemandEstimatorAsTargetCalculator(estimator, ESTIMATION_PERIOD);

        RebalancingParams rebalancingParams = new RebalancingParams();
        MinCostFlowRebalancingStrategyParams minCostFlowRebalancingStrategyParams = new MinCostFlowRebalancingStrategyParams();
        minCostFlowRebalancingStrategyParams.targetCoefficientSource = MinCostFlowRebalancingStrategyParams.TargetCoefficientSource.FromZoneAttribute;
        rebalancingParams.addParameterSet(minCostFlowRebalancingStrategyParams);

        AggregatedMinCostRelocationCalculator relocationCalculator = new AggregatedMinCostRelocationCalculator(new MostCentralDrtZoneTargetLinkSelector(zonalSystem));
        MinCostFlowRebalancingStrategy strategy = new MinCostFlowRebalancingStrategy(targetCalculator,
                zonalSystem, createEmptyFleet(), relocationCalculator, rebalancingParams);

        // 3 expected trips in zone 1
        estimator.handleEvent(departureEvent(100, link1, TransportMode.drt));
        estimator.handleEvent(departureEvent(200, link1, TransportMode.drt));
        estimator.handleEvent(departureEvent(300, link1, TransportMode.drt));
        // 1 expected trip in zone 2
        estimator.handleEvent(departureEvent(100, link2, TransportMode.drt));
        estimator.reset(1);

        Map<Zone, List<DvrpVehicle>> rebalanceableVehicles = new HashMap<>();

        // 4 vehicles in zone 1 (surplus = 1)
        List<DvrpVehicle> rebalanceableVehiclesZone1 = new ArrayList<>();
        rebalanceableVehiclesZone1.add(getDvrpVehicle(Id.create("a1", DvrpVehicle.class), link1));
        rebalanceableVehiclesZone1.add(getDvrpVehicle(Id.create("a2", DvrpVehicle.class), link1));
        rebalanceableVehiclesZone1.add(getDvrpVehicle(Id.create("a3", DvrpVehicle.class), link1));
        rebalanceableVehiclesZone1.add(getDvrpVehicle(Id.create("a4", DvrpVehicle.class), link1));
        rebalanceableVehicles.put(zone1, rebalanceableVehiclesZone1);

        List<RebalancingStrategy.Relocation> relocations = strategy.calculateMinCostRelocations(0, rebalanceableVehicles, Collections.emptyMap());
        Assertions.assertThat(relocations.size()).isEqualTo(1);
        Assertions.assertThat(relocations.getFirst().link.getId()).isEqualTo(link2.getId());

        rebalanceableVehicles.clear();

        // 5 vehicles in zone 1 (surplus = 2)
        rebalanceableVehiclesZone1.add(getDvrpVehicle(Id.create("a1", DvrpVehicle.class), link1));
        rebalanceableVehiclesZone1.add(getDvrpVehicle(Id.create("a2", DvrpVehicle.class), link1));
        rebalanceableVehiclesZone1.add(getDvrpVehicle(Id.create("a3", DvrpVehicle.class), link1));
        rebalanceableVehiclesZone1.add(getDvrpVehicle(Id.create("a4", DvrpVehicle.class), link1));
        rebalanceableVehiclesZone1.add(getDvrpVehicle(Id.create("a5", DvrpVehicle.class), link1));
        rebalanceableVehicles.put(zone1, rebalanceableVehiclesZone1);

        //set alpha to 2 -> send two vehicles to zone 2
        zone2.getAttributes().putAttribute(MinCostFlowRebalancingStrategy.REBALANCING_ZONAL_TARGET_ALPHA, 2.);
        List<RebalancingStrategy.Relocation> relocations2 = strategy.calculateMinCostRelocations(0, rebalanceableVehicles, Collections.emptyMap());
        Assertions.assertThat(relocations2.size()).isEqualTo(2);
        Assertions.assertThat(relocations2.getFirst().link.getId()).isEqualTo(link2.getId());
        Assertions.assertThat(relocations2.getLast().link.getId()).isEqualTo(link2.getId());
    }

    private DvrpVehicleImpl getDvrpVehicle(Id<DvrpVehicle> id, Link link) {
        return new DvrpVehicleImpl(
                ImmutableDvrpVehicleSpecification.newBuilder()
                        .id(id)
                        .capacity(0)
                        .serviceBeginTime(0)
                        .serviceEndTime(0)
                        .startLinkId(link.getId())
                        .build(), link);
    }

    private static Fleet createEmptyFleet() {
        return () -> ImmutableMap.<Id<DvrpVehicle>, DvrpVehicle>builder().build();
    }


    private PreviousIterationDrtDemandEstimator createEstimator() {
        RebalancingParams rebalancingParams = new RebalancingParams();
        rebalancingParams.interval = ESTIMATION_PERIOD;

        DrtConfigGroup drtConfigGroup = new DrtConfigGroup();
        drtConfigGroup.addParameterSet(rebalancingParams);

        return new PreviousIterationDrtDemandEstimator(zonalSystem, drtConfigGroup, ESTIMATION_PERIOD);
    }

    private PersonDepartureEvent departureEvent(double time, Link link, String mode) {
        return new PersonDepartureEvent(time, null, link.getId(), mode, mode);
    }

    static Network createNetwork() {
        Network network = NetworkUtils.createNetwork();
        Node a = network.getFactory().createNode(Id.createNodeId("a"), new Coord(0,0));
        Node b = network.getFactory().createNode(Id.createNodeId("b"), new Coord(500,0));
        network.addNode(a);
        network.addNode(b);

        Link ab = network.getFactory().createLink(Id.createLinkId("link_1"), a, b);
        Link ba = network.getFactory().createLink(Id.createLinkId("link_2"), b, a);
        network.addLink(ab);
        network.addLink(ba);
        return network;
    }
}
