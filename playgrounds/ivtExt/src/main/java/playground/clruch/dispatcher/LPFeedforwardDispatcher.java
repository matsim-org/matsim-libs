/**
 * Dispatcher implementing the linear program from
 * Pavone, Marco, Stephen Smith, Emilio Frazzoli, and Daniela Rus. 2011.
 * “Load Balancing for Mobility-on-Demand Systems.” In Robotics: Science and Systems VII. doi:10.15607/rss.2011.vii.034.
 * <p>
 * <p>
 * Implemented by Claudio Ruch on 2017, 02, 25
 */

package playground.clruch.dispatcher;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.router.util.TravelTime;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import ch.ethz.idsc.tensor.alg.Array;
import ch.ethz.idsc.tensor.alg.Dimensions;
import ch.ethz.idsc.tensor.red.Total;
import playground.clruch.dispatcher.core.PartitionedDispatcher;
import playground.clruch.dispatcher.core.VehicleLinkPair;
import playground.clruch.dispatcher.utils.AbstractRequestSelector;
import playground.clruch.dispatcher.utils.AbstractVehicleDestMatcher;
import playground.clruch.dispatcher.utils.AbstractVirtualNodeDest;
import playground.clruch.dispatcher.utils.ArrivalInformation;
import playground.clruch.dispatcher.utils.FeasibleRebalanceCreator;
import playground.clruch.dispatcher.utils.HungarBiPartVehicleDestMatcher;
import playground.clruch.dispatcher.utils.InOrderOfArrivalMatcher;
import playground.clruch.dispatcher.utils.KMeansVirtualNodeDest;
import playground.clruch.dispatcher.utils.LPVehicleRebalancing;
import playground.clruch.dispatcher.utils.OldestRequestSelector;
import playground.clruch.netdata.VirtualLink;
import playground.clruch.netdata.VirtualNetwork;
import playground.clruch.netdata.VirtualNetworkIO;
import playground.clruch.netdata.VirtualNode;
import playground.clruch.utils.GlobalAssert;
import playground.sebhoerl.avtaxi.config.AVDispatcherConfig;
import playground.sebhoerl.avtaxi.config.AVGeneratorConfig;
import playground.sebhoerl.avtaxi.dispatcher.AVDispatcher;
import playground.sebhoerl.avtaxi.framework.AVModule;
import playground.sebhoerl.avtaxi.passenger.AVRequest;
import playground.sebhoerl.plcpc.ParallelLeastCostPathCalculator;

public class LPFeedforwardDispatcher extends PartitionedDispatcher {
    // DEBUG Start
    public static final String KEY_REBALANCINGPERIOD = "rebalancingPeriod";
    public static final String KEY_VIRTUALNETWORKDIRECTORY = "virtualNetworkDirectory";
    public static final String KEY_DTEXTENSION = "dtExtension";
    // public static final String KEY_WEIGHTSEXTENSION = "weightsExtension";
    public static final String KEX_REDISPATCHPERIOD = "redispatchPeriod";
    // DEBUG End
    public final int rebalancingPeriod;
    public final int redispatchPeriod;
    final AbstractVirtualNodeDest virtualNodeDest;
    final AbstractRequestSelector requestSelector;
    final AbstractVehicleDestMatcher vehicleDestMatcher;
    final int numberOfAVs;
    private int total_rebalanceCount = 0;
    private final int nVNodes;
    private final int nVLinks;
    Tensor printVals = Tensors.empty();
    ArrivalInformation arrivalInformation;
    Tensor rebalancingRate;
    Tensor rebalanceCount;
    Tensor rebalanceCountInteger;
    LPVehicleRebalancing lpVehicleRebalancing;

    public LPFeedforwardDispatcher( //
            AVDispatcherConfig config, //
            AVGeneratorConfig generatorConfig, //
            TravelTime travelTime, //
            ParallelLeastCostPathCalculator router, //
            EventsManager eventsManager, //
            VirtualNetwork virtualNetwork, //
            AbstractVirtualNodeDest abstractVirtualNodeDest, //
            AbstractRequestSelector abstractRequestSelector, //
            AbstractVehicleDestMatcher abstractVehicleDestMatcher, //
            ArrivalInformation arrivalInformationIn) {
        super(config, travelTime, router, eventsManager, virtualNetwork);
        this.virtualNodeDest = abstractVirtualNodeDest;
        this.requestSelector = abstractRequestSelector;
        this.vehicleDestMatcher = abstractVehicleDestMatcher;
        numberOfAVs = (int) generatorConfig.getNumberOfVehicles();
        rebalancingPeriod = Integer.parseInt(config.getParams().get(KEY_REBALANCINGPERIOD));
        System.out.println(config.getParams().get(KEX_REDISPATCHPERIOD));
        redispatchPeriod = Integer.parseInt(config.getParams().get(KEX_REDISPATCHPERIOD));
        arrivalInformation = arrivalInformationIn;
        nVNodes = virtualNetwork.getvNodesCount();
        nVLinks = virtualNetwork.getvLinksCount();
        rebalanceCount = Array.zeros(nVNodes, nVNodes);
        rebalanceCountInteger = Array.zeros(nVNodes, nVNodes);
        lpVehicleRebalancing = new LPVehicleRebalancing(virtualNetwork);
    }

    @Override
    public void redispatch(double now) {
        // PART 0: match vehicles at a customer link
        new InOrderOfArrivalMatcher(this::setAcceptRequest) //
                .match(getStayVehicles(), getAVRequestsAtLinks());
        final long round_now = Math.round(now);

        // Recalculate new rebalancing rates periodically
        if (round_now % rebalancingPeriod == 0) {

            Map<VirtualNode, List<AVRequest>> requests = getVirtualNodeRequests();
            // II.i compute rebalancing vehicles and send to virtualNodes
            {
                // lambdas = [lbd_1, ... , lbd] row vector, where n nmbr. of vNodes
                System.out.println("number of virtual Nodes: " + nVNodes);
                Tensor lambdas = Tensors.matrix((i, j) -> getLambdai(now, j), 1, nVNodes);

                // p_ij_bar row-stochastic matrix (nxn) with transition probabilities from i to j
                // p_ij = p_ij_bar with the diagonal elements set to zero
                Tensor p_ij = Tensors.matrix((i, j) -> getPij(now, i, j), nVNodes, nVNodes);

                // fill right-hand-side, i.e. rhs(i) = -lambda_i + sum_j * lambda_j p_ji
                Tensor rhs = (lambdas.multiply(RealScalar.of(-1)).add(lambdas.dot(p_ij))).get(0);

                // solve the linear program with updated right-hand side
                rebalancingRate = lpVehicleRebalancing.solveUpdatedLP(rhs);

                // ensure positivity of solution (small negative values possible due to solver
                // accuracy)
                rebalancingRate.flatten(-1).forEach(v -> GlobalAssert.that(v.Get().number().doubleValue() > -10E-7));
            }
        }

        // permanently rebalance vehicles according to the rates output by the LP
        if (round_now % redispatchPeriod == 0) {
            // update rebalance count using current rate
            rebalanceCount = rebalanceCount.add(rebalancingRate.multiply(RealScalar.of(redispatchPeriod)));

            // redispatch values > 0 and remove from rebalanceCount
            for (int i = 0; i < Dimensions.of(rebalanceCount).get(0); ++i) {
                for (int j = 0; j < Dimensions.of(rebalanceCount).get(1); ++j) {
                    double toSend = rebalanceCount.Get(i, j).number().doubleValue();
                    if (toSend > 1.0) {
                        rebalanceCountInteger.set(RealScalar.of(1), i, j);
                        rebalanceCount.set(rebalanceCount.Get(i, j).subtract(RealScalar.of(1)), i, j);
                    }
                }
            }

            // ensure that not more vehicles are sent away than available
            Map<VirtualNode, List<VehicleLinkPair>> availableVehicles = getVirtualNodeDivertableNotRebalancingVehicles();
            Tensor feasibleRebalanceCount = FeasibleRebalanceCreator.returnFeasibleRebalance(rebalanceCountInteger.unmodifiable(),
                    availableVehicles);
            total_rebalanceCount += (Integer) ((Scalar) Total.of(Tensor.of(feasibleRebalanceCount.flatten(-1)))).number();

            // generate routing instructions for rebalancing vehicles
            Map<VirtualNode, List<Link>> destinationLinks = createvNodeLinksMap();

            // fill rebalancing destinations
            for (int i = 0; i < nVLinks; ++i) {
                VirtualLink virtualLink = this.virtualNetwork.getVirtualLink(i);
                VirtualNode toNode = virtualLink.getTo();
                VirtualNode fromNode = virtualLink.getFrom();
                int numreb = (Integer) (feasibleRebalanceCount.Get(fromNode.index, toNode.index)).number();
                List<Link> rebalanceTargets = virtualNodeDest.selectLinkSet(toNode, numreb);
                destinationLinks.get(fromNode).addAll(rebalanceTargets);
            }

            // consistency check: rebalancing destination links must not exceed
            // available vehicles in virtual node
            Map<VirtualNode, List<VehicleLinkPair>> finalAvailableVehicles = availableVehicles;
            GlobalAssert.that(!virtualNetwork.getVirtualNodes().stream()
                    .filter(v -> finalAvailableVehicles.get(v).size() < destinationLinks.get(v).size()).findAny().isPresent());

            // send rebalancing vehicles using the setVehicleRebalance command
            for (VirtualNode virtualNode : destinationLinks.keySet()) {
                Map<VehicleLinkPair, Link> rebalanceMatching = vehicleDestMatcher.match(availableVehicles.get(virtualNode),
                        destinationLinks.get(virtualNode));
                rebalanceMatching.keySet().forEach(v -> setVehicleRebalance(v, rebalanceMatching.get(v)));
            }

            // reset vector
            rebalanceCountInteger = Tensors.matrix((i, j) -> RealScalar.of(0.0), nVNodes, nVNodes);
        }

        // assign destinations to vehicles using bipartite matching
        printVals = HungarianUtils.globalBipartiteMatching(this, () -> getVirtualNodeDivertableNotRebalancingVehicles().values()
                .stream().flatMap(v -> v.stream()).collect(Collectors.toList()));
    }

    /**
     * @param time,
     *            time step
     * @param index,
     *            vNode index
     * @return lambda at this node and index
     */
    Scalar getLambdai(double time, int index) {
        return arrivalInformation.getLambdaforTime((int) time, index);
    }

    /**
     * @param time,
     *            time step
     * @param row,
     *            fromNode
     * @param col,
     *            toNode
     * @return
     */
    Scalar getPij(double time, int row, int col) {
        if (row == col) { // diagonal elements have to be zero
            return RealScalar.of(0.0);
        } else { // off-diagonal elements according to data input
            return arrivalInformation.getpijforTime((int) time, row, col);
        }
    }

    @Override
    public String getInfoLine() {
        return String.format("%s RV=%s H=%s", //
                super.getInfoLine(), //
                total_rebalanceCount, //
                printVals.toString() //
        );
    }

    public static class Factory implements AVDispatcherFactory {
        @Inject
        @Named(AVModule.AV_MODE)
        private ParallelLeastCostPathCalculator router;

        @Inject
        @Named(AVModule.AV_MODE)
        private TravelTime travelTime;

        @Inject
        private EventsManager eventsManager;

        @Inject
        private Network network;

        @Inject
        private Population population;

        public static VirtualNetwork virtualNetwork;

        @Override
        public AVDispatcher createDispatcher(AVDispatcherConfig config, AVGeneratorConfig generatorConfig) {

            AbstractVirtualNodeDest abstractVirtualNodeDest = new KMeansVirtualNodeDest();
            AbstractRequestSelector abstractRequestSelector = new OldestRequestSelector();
            AbstractVehicleDestMatcher abstractVehicleDestMatcher = new HungarBiPartVehicleDestMatcher();

            final File virtualnetworkDir = new File(config.getParams().get(KEY_VIRTUALNETWORKDIRECTORY));
            GlobalAssert.that(virtualnetworkDir.isDirectory());
            {
                final File virtualnetworkFile = new File(virtualnetworkDir, "virtualNetwork.xml");
                GlobalAssert.that(virtualnetworkFile.isFile());
                virtualNetwork = VirtualNetworkIO.fromXML(network, virtualnetworkFile);
            }

            ArrivalInformation arrivalInformationIn = null;
            {
                final String ext = config.getParams().get(KEY_DTEXTENSION);
                final File lambdaXML = new File(virtualnetworkDir, "poissonParameters_" + ext + ".xml");
                GlobalAssert.that(lambdaXML.isFile());
                final File pijFile = new File(virtualnetworkDir, "transitionProbabilities_" + ext + ".xml");
                GlobalAssert.that(pijFile.isFile());
                final File alphaijFile = new File(virtualnetworkDir, "rebalancingRates_" + ext + ".xml");
                GlobalAssert.that(alphaijFile.isFile());

                try {
                    long populationSize = population.getPersons().size();
                    int rebalancingPeriod = Integer.parseInt(config.getParams().get("rebalancingPeriod"));
                    arrivalInformationIn = new ArrivalInformation(virtualNetwork, lambdaXML, pijFile, alphaijFile, //
                            populationSize, //
                            rebalancingPeriod //
                    );
                } catch (Exception e) {
                    e.printStackTrace();
                    GlobalAssert.that(false);
                }
            }

            return new LPFeedforwardDispatcher(config, generatorConfig, travelTime, router, eventsManager, virtualNetwork,
                    abstractVirtualNodeDest, abstractRequestSelector, abstractVehicleDestMatcher, arrivalInformationIn);
        }
    }
}
