/**
 * Dispatcher implementing the linear program from
 * Pavone, Marco, Stephen Smith, Emilio Frazzoli, and Daniela Rus. 2011.
 * “Load Balancing for Mobility-on-Demand Systems.” In Robotics: Science and Systems VII. doi:10.15607/rss.2011.vii.034.
 * <p>
 * <p>
 * Implemented by Claudio Ruch on 2017, 02, 25
 */

package playground.clruch.dispatcher;

import ch.ethz.idsc.tensor.*;
import ch.ethz.idsc.tensor.alg.Array;
import ch.ethz.idsc.tensor.alg.Dimensions;
import ch.ethz.idsc.tensor.red.Total;
import ch.ethz.idsc.tensor.sca.Floor;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.gnu.glpk.glp_smcp;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.router.util.TravelTime;

import playground.clruch.dispatcher.core.PartitionedDispatcher;
import playground.clruch.dispatcher.core.VehicleLinkPair;
import playground.clruch.dispatcher.utils.*;
import playground.clruch.netdata.*;
import playground.clruch.utils.GlobalAssert;
import playground.sebhoerl.avtaxi.config.AVDispatcherConfig;
import playground.sebhoerl.avtaxi.config.AVGeneratorConfig;
import playground.sebhoerl.avtaxi.dispatcher.AVDispatcher;
import playground.sebhoerl.avtaxi.framework.AVModule;
import playground.sebhoerl.avtaxi.passenger.AVRequest;
import playground.sebhoerl.plcpc.ParallelLeastCostPathCalculator;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class LPFeedforwardDispatcher extends PartitionedDispatcher {
    // DEBUG Start
    public static final String KEY_REBALANCINGPERIOD = "rebalancingPeriod";
    public static final String KEY_VIRTUALNETWORKDIRECTORY = "virtualNetworkDirectory";
    public static final String KEY_DTEXTENSION = "dtExtension";
   // public static final String KEY_WEIGHTSEXTENSION = "weightsExtension";
    public static final String KEX_REDISPATCHPERIOD = "redispatchPeriod";
    //DEBUG End
    public final int rebalancingPeriod;
    public final int redispatchPeriod;
    final AbstractVirtualNodeDest virtualNodeDest;
    final AbstractRequestSelector requestSelector;
    final AbstractVehicleDestMatcher vehicleDestMatcher;
    final Map<VirtualLink, Double> travelTimes;
    final int numberOfAVs;
    private int total_rebalanceCount = 0;
    private final int N_vStations;
    Tensor printVals = Tensors.empty();
    ArrivalInformation arrivalInformation;
    Tensor rebalancingRate;
    Tensor rebalanceCount; //= Tensors.matrix((i, j) -> RealScalar.of(0.0), N_vStations, virtualNetwork.getvNodesCount());
    Tensor rebalanceCountInteger;// = Tensors.matrix((i, j) -> RealScalar.of(0.0), virtualNetwork.getvNodesCount(), virtualNetwork.getvNodesCount());

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
            Map<VirtualLink, Double> travelTimesIn, ArrivalInformation arrivalInformationIn) {
        super(config, travelTime, router, eventsManager, virtualNetwork);
        this.virtualNodeDest = abstractVirtualNodeDest;
        this.requestSelector = abstractRequestSelector;
        this.vehicleDestMatcher = abstractVehicleDestMatcher;
        travelTimes = travelTimesIn;
        numberOfAVs = (int) generatorConfig.getNumberOfVehicles();
        rebalancingPeriod = Integer.parseInt(config.getParams().get(KEY_REBALANCINGPERIOD));
        System.out.println(config.getParams().get(KEX_REDISPATCHPERIOD));
        redispatchPeriod = Integer.parseInt(config.getParams().get(KEX_REDISPATCHPERIOD));
        arrivalInformation = arrivalInformationIn;
        N_vStations = virtualNetwork.getvNodesCount()-1;
        rebalanceCount = Array.zeros(N_vStations,N_vStations);
        rebalanceCountInteger = Array.zeros(N_vStations,N_vStations);
    }

    @Override
    public void redispatch(double now) {
        // PART 0: match vehicles at a customer link
        new InOrderOfArrivalMatcher(this::setAcceptRequest) //
                .match(getStayVehicles(), getAVRequestsAtLinks());
        final long round_now = Math.round(now);

        // Recalculate new rebalancing rates periodically
        if (round_now % rebalancingPeriod == 0) {
            // setup linear program
            LPVehicleRebalancing lpVehicleRebalancing = new LPVehicleRebalancing(virtualNetwork, travelTimes);

            Map<VirtualNode, List<AVRequest>> requests = getVirtualNodeRequests();
            // II.i compute rebalancing vehicles and send to virtualNodes
            {
                // lambdas = [lambda_1, lambda_2, ... , lambda_n] where n number of virtual nodes and lambdas a row rector
                Tensor lambdas = Tensors.matrix((i, j) -> getLambdai(now, j), 1, N_vStations);

                // DEBUGGING
                for (int i = 0; i < Dimensions.of(lambdas).get(1); ++i) {
                    if (lambdas.get(0).Get(i).number().doubleValue() > 0) {
                        System.out.println("action beginning");
                    }
                }
                // END DEBUGGING

                // p_ij_bar row-stochastic matrix of dimension nxn with transition probabilities from i to j
                // p_ij = p_ij_bar with the diagonal elements set to zero
                Tensor p_ij = Tensors.matrix((i, j) -> getPij(now, i, j), N_vStations, N_vStations);

                // fill right-hand-side, i.e. rhs(i) = -lambda_i + sum_j lambda_j p_ji
                Tensor rhs = (lambdas.multiply(RealScalar.of(-1)).add(lambdas.dot(p_ij))).get(0);

                // solve the linear program with updated right-hand side
                rebalancingRate = lpVehicleRebalancing.solveUpdatedLP(rhs);

                // DEBUGGING
                for (int i = 0; i < Dimensions.of(rebalancingRate).get(0); ++i) {
                    for (int j = 0; j < Dimensions.of(rebalancingRate).get(1); ++j) {
                        if (rebalancingRate.get(i).Get(j).number().doubleValue() > 0) {
                            System.out.println("action beginning");
                        }
                    }
                }
                // END DEBUGGING

                // TODO this should never become active, can be removed later (nonnegative solution)
                for (int i = 0; i < N_vStations; ++i) {
                    for (int j = 0; j < N_vStations; ++j) {
                        double entry = rebalancingRate.Get(i, j).number().doubleValue();
                        if (entry < 0) {
                            System.out.println("negative element found.");
                            System.out.println("insert a breakpoint here.");
                            if (Math.abs(entry) < 10E-7) {
                                rebalancingRate.set(ZeroScalar.get(), i, j);
                            } else
                                GlobalAssert.that(false);
                        }
                    }
                }

                // close the LP to avoid data leaks
                // TODO can this be taken out to not delete the LP in every instance?
                lpVehicleRebalancing.closeLP();
            }
        }

        // Part II: outside rebalancing periods, permanently assign vehicles to requests if they have arrived at a customer i.e. stay on the same link
        // in the identical vNode match customers to requests, assign desitnations to vehicles using bipartite matching
        if (round_now % redispatchPeriod == 0) {
            // update rebalance count
            rebalanceCount = rebalanceCount.add(rebalancingRate.multiply(RealScalar.of(redispatchPeriod)));

            // DEBUGGING
            for (int i = 0; i < Dimensions.of(rebalanceCount).get(0); ++i) {
                for (int j = 0; j < Dimensions.of(rebalanceCount).get(1); ++j) {
                    double value = rebalanceCount.get(i).Get(j).number().doubleValue();
                    if (value > 1.0) {
                        System.out.println("action beginning");
                    }
                }
            }
            // END DEBUGGING

            // redispatch integer values and remove from rebalanceCount
            // TODO check for more elegant formulation
            for (int i = 0; i < Dimensions.of(rebalanceCount).get(0); ++i) {
                for (int j = 0; j < Dimensions.of(rebalanceCount).get(1); ++j) {
                    double toSend = rebalanceCount.get(i).Get(j).number().doubleValue();
                    if (toSend > 1.0) {
                        Tensor lineToUpdate = rebalanceCountInteger.get(i);
                        lineToUpdate.set(RealScalar.of(1), j);
                        rebalanceCountInteger.set(lineToUpdate, i);

                        Tensor lineToupdate2 = rebalanceCount.get(i);
                        lineToupdate2.set(lineToupdate2.Get(j).subtract(RealScalar.of(1)), j);
                        rebalanceCount.set(lineToupdate2, i);
                    }
                }
            }

            // ensure that not more vehicles are sent away than available
            Map<VirtualNode, List<VehicleLinkPair>> availableVehicles = getVirtualNodeDivertableNotRebalancingVehicles();
            Tensor feasibleRebalanceCount = returnFeasibleRebalance(rebalanceCountInteger.unmodifiable(), availableVehicles);
            total_rebalanceCount += (Integer) ((Scalar) Total.of(Tensor.of(feasibleRebalanceCount.flatten(-1)))).number();

            // generate routing instructions for rebalancing vehicles
            Map<VirtualNode, List<Link>> destinationLinks = createvNodeLinksMap();

            // fill rebalancing destinations
            for (int i = 0; i < virtualNetwork.getvLinksCount(); ++i) {
                VirtualLink virtualLink = this.virtualNetwork.getVirtualLink(i);
                VirtualNode toNode = virtualLink.getTo();
                VirtualNode fromNode = virtualLink.getFrom();
                int numreb = (Integer) (feasibleRebalanceCount.Get(fromNode.index, toNode.index)).number();
                List<Link> rebalanceTargets = virtualNodeDest.selectLinkSet(toNode, numreb);
                destinationLinks.get(fromNode).addAll(rebalanceTargets);
            }

            // consistency check: rebalancing destination links must not exceed available vehicles in virtual node
            Map<VirtualNode, List<VehicleLinkPair>> finalAvailableVehicles = availableVehicles;
            GlobalAssert.that(!virtualNetwork.getVirtualNodes().stream().filter(v -> finalAvailableVehicles.get(v).size() < destinationLinks.get(v).size()).findAny().isPresent());

            // send rebalancing vehicles using the setVehicleRebalance command
            for (VirtualNode virtualNode : destinationLinks.keySet()) {
                Map<VehicleLinkPair, Link> rebalanceMatching = vehicleDestMatcher.match(availableVehicles.get(virtualNode), destinationLinks.get(virtualNode));
                rebalanceMatching.keySet().forEach(v -> setVehicleRebalance(v, rebalanceMatching.get(v)));
            }

            // reset vector
            rebalanceCountInteger = Tensors.matrix((i, j) -> RealScalar.of(0.0), N_vStations,N_vStations);
        }

        // assign desitnations to vehicles using bipartite matching
        printVals = HungarianDispatcher.globalBipartiteMatching(this,
                () -> getVirtualNodeDivertableNotRebalancingVehicles().values().stream().flatMap(v -> v.stream()).collect(Collectors.toList()));
    }

    /**
     * @param rebalanceInput
     *            entry i,j contains the rebalance input from virtualNode i to virtualNode j
     * @param availableVehicles
     *            the available vehicles per virtualNode
     * @return
     */
    private Tensor returnFeasibleRebalance(Tensor rebalanceInput, Map<VirtualNode, List<VehicleLinkPair>> availableVehicles) {
        Tensor feasibleRebalance = rebalanceInput.copy();

        for (int i = 0; i < N_vStations; ++i) {
            // count number of outgoing vehicles per vNode
            double outgoingNmrvNode = 0.0;
            Tensor outgoingVehicles = rebalanceInput.get(i);
            for (int j = 0; j < N_vStations; ++j) {
                outgoingNmrvNode = outgoingNmrvNode + (Integer) (outgoingVehicles.Get(j)).number();
            }
            int outgoingVeh = (int) outgoingNmrvNode;
            int finalI = i;
            int availableVehvNode = availableVehicles.get(availableVehicles.keySet().stream().filter(v -> v.index == finalI).findAny().get()).size();
            // if number of outoing vehicles too small, reduce proportionally
            if (availableVehvNode < outgoingVeh) {
                long shrinkingFactor = ((long) availableVehvNode / ((long) outgoingVeh));
                Tensor newRow = Floor.of(rebalanceInput.get(i).multiply(RealScalar.of(shrinkingFactor)));
                feasibleRebalance.set(newRow, i);
            }
        }
        return feasibleRebalance;
    }

    /**
     * @param time
     *            current time
     * @param index
     *            index of node to be checked
     * @return lambda value of this node for given time
     */
    Scalar getLambdai(double time, int index) {
        return arrivalInformation.getLambdaforTime((int) time, index);
    }

    /**
     * @param time
     *            current time
     * @param row
     *            from node index
     * @param col
     *            to node index
     * @return p_ij = p_fromTo for that time
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
        public static Map<VirtualLink, Double> travelTimes;

        @Override
        public AVDispatcher createDispatcher(AVDispatcherConfig config, AVGeneratorConfig generatorConfig) {

            AbstractVirtualNodeDest abstractVirtualNodeDest = new KMeansVirtualNodeDest();
            AbstractRequestSelector abstractRequestSelector = new OldestRequestSelector();
            AbstractVehicleDestMatcher abstractVehicleDestMatcher = new HungarBiPartVehicleDestMatcher();

            //DEBUG START
            // ---
            GlobalAssert.that(config.getParams().containsKey(KEY_VIRTUALNETWORKDIRECTORY));
            GlobalAssert.that(config.getParams().containsKey(KEY_DTEXTENSION));
            // ---
            final File virtualnetworkDir = new File(config.getParams().get(KEY_VIRTUALNETWORKDIRECTORY));
            GlobalAssert.that(virtualnetworkDir.isDirectory());
            // ---
            {
                final File virtualnetworkFile = new File(virtualnetworkDir, "virtualNetwork.xml");
                GlobalAssert.that(virtualnetworkFile.isFile());
                virtualNetwork = VirtualNetworkLoader.fromXML(network, virtualnetworkFile);
                travelTimes = vLinkDataReader.fillvLinkData(virtualnetworkFile, virtualNetwork, "Ttime");
            }
            // ---
            {
             //   final String string = "consensusWeights_" + config.getParams().get(KEY_WEIGHTSEXTENSION) + ".xml";
             //   final File linkWeightsXML = new File(virtualnetworkDir, string);
             //   GlobalAssert.that(linkWeightsXML.isFile());
                // linkWeights = vLinkDataReader.fillvLinkData(linkWeightsXML, virtualNetwork, "weight");
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
                    arrivalInformationIn = new ArrivalInformation(virtualNetwork, lambdaXML, pijFile, alphaijFile,//
                            populationSize, //
                            rebalancingPeriod //
                    );
                } catch (Exception e) {
                    e.printStackTrace();
                    GlobalAssert.that(false);
                }
            }

            return new LPFeedforwardDispatcher(config, generatorConfig, travelTime, router, eventsManager, virtualNetwork, abstractVirtualNodeDest, abstractRequestSelector,
                    abstractVehicleDestMatcher, travelTimes, arrivalInformationIn);
        }
    }
}
