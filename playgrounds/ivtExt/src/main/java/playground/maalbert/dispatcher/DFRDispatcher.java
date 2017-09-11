/**
 =======================================================================================================================
  DISTRIBUTED FEEDBACK REBALANCING (DFR) DISPATCHER: Round(a+b) versionh
 =======================================================================================================================
 * Implemented by Marc Albert, Claudio Ruch (30.03.2017)
 */

package playground.maalbert.dispatcher;

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

import ch.ethz.idsc.queuey.core.networks.VirtualLink;
import ch.ethz.idsc.queuey.core.networks.VirtualNetwork;
import ch.ethz.idsc.queuey.core.networks.VirtualNode;
import ch.ethz.idsc.queuey.util.GlobalAssert;
import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import ch.ethz.idsc.tensor.alg.Array;
import ch.ethz.idsc.tensor.io.Pretty;
import ch.ethz.idsc.tensor.red.Mean;
import ch.ethz.idsc.tensor.red.Total;
import ch.ethz.idsc.tensor.sca.Ceiling;
import ch.ethz.idsc.tensor.sca.Floor;
import playground.clruch.dispatcher.core.PartitionedDispatcher;
import playground.clruch.dispatcher.core.RoboTaxi;
import playground.clruch.dispatcher.utils.AbstractRequestSelector;
import playground.clruch.dispatcher.utils.AbstractVehicleDestMatcher;
import playground.clruch.dispatcher.utils.EuclideanDistanceFunction;
import playground.clruch.dispatcher.utils.HungarBiPartVehicleDestMatcher;
import playground.clruch.dispatcher.utils.OldestRequestSelector;
import playground.clruch.dispatcher.utils.virtualnodedestselector.AbstractVirtualNodeDest;
import playground.clruch.dispatcher.utils.virtualnodedestselector.KMeansVirtualNodeDest;
//import playground.clruch.netdata.vLinkDataReader; // this was deleted // TODO think if need something else for replacement. 
import playground.clruch.traveldata.TravelData;
import playground.sebhoerl.avtaxi.config.AVDispatcherConfig;
import playground.sebhoerl.avtaxi.config.AVGeneratorConfig;
import playground.sebhoerl.avtaxi.dispatcher.AVDispatcher;
import playground.sebhoerl.avtaxi.framework.AVModule;
import playground.sebhoerl.avtaxi.generator.PopulationDensityGenerator;
import playground.sebhoerl.avtaxi.passenger.AVRequest;
import playground.sebhoerl.plcpc.ParallelLeastCostPathCalculator;

/**
 * {@link PopulationDensityGenerator}
 */
public class DFRDispatcher extends PartitionedDispatcher {
    // ==================================================================================================================
    // Define and Read Simulation Parameters
    // ==================================================================================================================
    public static final String KEY_REBALANCINGPERIOD = "rebalancingPeriod";
    public static final String KEY_VIRTUALNETWORKDIRECTORY = "virtualNetworkDirectory";
    public static final String KEY_DTEXTENSION = "dtExtension";
    public static final String KEY_WEIGHTSEXTENSION = "weightsExtension";
    public static final String KEY_FEEDBACKTERM = "feedbackTerm";
    public static final String KEY_REDISPATCHPERIOD = "redispatchPeriod";
    // ==================================================================================================================
    // Class Variables
    // ==================================================================================================================
    private final int rebalancingPeriod;
    private final int redispatchPeriod;
    private int rebCount = 0;
    private final int N_vStations;
    private final long popSize;
    private final FeedbackTerm feebackTerm;
    private final Map<VirtualLink<Link>, Double> vLinkWeights;
    private Tensor rebalancingOrderRest;
    private Tensor inConsensus;
    private Tensor consensusVal;
    final Tensor neighCount;
    final AbstractVirtualNodeDest virtualNodeDest;
    final AbstractRequestSelector requestSelector;
    final AbstractVehicleDestMatcher vehicleDestMatcher;
    final TravelData arrivalInformation;

    // ==================================================================================================================
    // Class Constructor
    // ==================================================================================================================
    public DFRDispatcher( //
            AVDispatcherConfig config, //
            AVGeneratorConfig generatorConfig, //
            TravelTime travelTime, //
            ParallelLeastCostPathCalculator router, //
            EventsManager eventsManager, //
            VirtualNetwork<Link> virtualNetwork, //
            AbstractVirtualNodeDest abstractVirtualNodeDest, //
            AbstractRequestSelector abstractRequestSelector, //
            AbstractVehicleDestMatcher abstractVehicleDestMatcher, //
            Map<VirtualLink<Link>, Double> linkWeightsIn, //
            TravelData arrivalInformation) {
        super(config, travelTime, router, eventsManager, virtualNetwork);

        this.virtualNodeDest = abstractVirtualNodeDest;
        this.requestSelector = abstractRequestSelector;
        this.vehicleDestMatcher = abstractVehicleDestMatcher;
        this.arrivalInformation = arrivalInformation;
        vLinkWeights = linkWeightsIn;
        rebalancingPeriod = Integer.parseInt(config.getParams().get(KEY_REBALANCINGPERIOD));
        redispatchPeriod = Integer.parseInt(config.getParams().get(KEY_REDISPATCHPERIOD));
        N_vStations = virtualNetwork.getvNodesCount(); // Consider here nodecount -1 as no vLink to leftover region!!
        popSize = arrivalInformation.populationSize;
        rebalancingOrderRest = Array.zeros(N_vStations, N_vStations);
        inConsensus = Array.zeros(N_vStations);
        consensusVal = Array.zeros(N_vStations);
        feebackTerm = FeedbackTerm.valueOf(config.getParams().get(KEY_FEEDBACKTERM));
        // TODO: Get rid of Loop
        neighCount = Array.zeros(N_vStations);
        for (int i = 0; i < N_vStations; i++) {
            neighCount.set(RealScalar.of(virtualNetwork.getVirtualNode(i).getNeighCount()), i);
        }

    }

    // ==================================================================================================================
    // DISTRIBUTED FEEDBACK REBALANCING AND REDISPATCHING ALGORITHM
    // ==================================================================================================================
    @Override
    public void redispatch(double now) {
        // A: outside rebalancing periods, permanently assign vehicles to requests if they have arrived at a customer
        // i.e. stay on the same link

        GlobalAssert.that(false); // THIS DISPATCHER IS NOT WORKING, NOT ADAPTED TO NEW DISPATCHER STRUCTURE, RELEVANT CODE
        // WAS COMMENTED.
        // new InOrderOfArrivalMatcher(this::setAcceptRequest) //
        // .match(getStayVehicles(), getAVRequestsAtLinks());

        // --------------------------------------------------------------------------------------------------------------
        // Get Open Requests
        // --------------------------------------------------------------------------------------------------------------
        Map<VirtualNode<Link>, List<AVRequest>> requests = getVirtualNodeRequests();
        // ==============================================================================================================
        // DFR Algorithm
        // ==============================================================================================================
        final long round_now = Math.round(now);
        if (round_now % rebalancingPeriod == 0) {

            { // ------------------------------------------------------------------------------------------------------
              // Initialize
              // ------------------------------------------------------------------------------------------------------
              // Get System State
                Map<VirtualNode<Link>, List<RoboTaxi>> available_Vehicles = getVirtualNodeDivertableNotRebalancingRoboTaxis();
                Map<VirtualNode<Link>, List<RoboTaxi>> v_ij_reb = getVirtualNodeRebalancingToRoboTaxis();
                // Declare System State Matrices
                Tensor rebalancingTovStation = Array.zeros(N_vStations);
                Tensor openRequests = Array.zeros(N_vStations);
                Tensor availableVehicles = Array.zeros(N_vStations);
                Tensor feedback_Rebalancing_DFR = Array.zeros(N_vStations, N_vStations);
                Tensor feedfwrd_Rebalancing_LPR = Array.zeros(N_vStations, N_vStations);
                // Initialize System State Matrices
                available_Vehicles.entrySet().stream().forEach(e -> availableVehicles.set(RealScalar.of(e.getValue().size()), e.getKey().getIndex()));
                v_ij_reb.entrySet().stream().forEach(e -> rebalancingTovStation.set(RealScalar.of(e.getValue().size()), e.getKey().getIndex()));
                requests.entrySet().stream().forEach(e -> openRequests.set(RealScalar.of(e.getValue().size()), e.getKey().getIndex()));
                // ------------------------------------------------------------------------------------------------------
                // System Imbalance
                // ------------------------------------------------------------------------------------------------------
                Tensor systemImbalance = openRequests.subtract(availableVehicles).subtract(rebalancingTovStation);
                // ------------------------------------------------------------------------------------------------------
                // System Wait Times
                // ------------------------------------------------------------------------------------------------------
                Tensor waitTimes = Tensors.empty();
                for (int i = 0; i < N_vStations; i++) {
                    VirtualNode vStation = virtualNetwork.getVirtualNode(i);
                    Tensor waitTimes_i = Tensor.of(requests.get(vStation).stream().map(r -> RealScalar.of(now - r.getSubmissionTime())));
                    if (waitTimes_i.length() < 1) {
                        waitTimes.append(RealScalar.of(0));
                    } else {
                        waitTimes.append(Mean.of(waitTimes_i));
                    }
                }
                waitTimes = waitTimes.multiply(RealScalar.of(1 / 60.0)); // Wait Times in Minutes
                // DEBUG START
                System.out.println("--------------------------------------------------------------------");
                System.out.println("Available Vehicles: " + availableVehicles.toString());
                System.out.println("Open Requests     : " + openRequests.toString());
                System.out.println("Rebalancing to vS : " + rebalancingTovStation.toString());
                System.out.println("System Imbalance  : " + systemImbalance.toString());
                System.out.println("Mean Wait Times   : " + waitTimes.toString());
                // DEBUG END
                // ======================================================================================================
                // DFR Iterations: Compute Rebalancing
                // ======================================================================================================
                {
                    // --------------------------------------------------------------------------------------------------
                    // Get Feedforward rebalancing Rates
                    //--------------------------------------------------------------------------------------------------
                    Tensor alphaij = arrivalInformation.getAlphaijPSFforTime((int) now).multiply(RealScalar.of(popSize));
                    //FeedForward Rebalancing
                    for (int i = 0; i < N_vStations; i++) {
                        for (int j = 0; j < N_vStations; j++) {
                            feedfwrd_Rebalancing_LPR.set(alphaij.Get(i, j), i, j);
                        }
                    }
                    // --------------------------------------------------------------------------------------------------
                    // Check if in Consensus Set
                    // --------------------------------------------------------------------------------------------------
                    Tensor lambda = Tensors.empty();
                    switch (feebackTerm) {
                    case LDX: {
                        lambda = arrivalInformation.getNextNonZeroLambdaforTime((int) now);
                        GlobalAssert.that(Total.of(lambda).Get().number().doubleValue() != 0); // no lambda is ever zero (if zero take next non-zero)
                        consensusVal = consensusLDX(systemImbalance, lambda);
                        consensusVal.append(RealScalar.of(0)); // LeftoverStation
                        break;
                    }
                    case LX: {
                        consensusVal = consensusLX(systemImbalance);
                        break;
                    }
                    case LW: {
                        consensusVal = consensusLX(waitTimes);
                        break;
                    }
                    }
                    Tensor lowerConsensusValue = systemImbalance.subtract(Floor.of(consensusVal));
                    Tensor upperConsensusValue = systemImbalance.subtract(Ceiling.of(consensusVal));
                    for (int i = 0; i < N_vStations; i++) {
                        boolean aboveConsSet_LB = lowerConsensusValue.Get(i).number().doubleValue() >= -neighCount.Get(i).number().doubleValue();
                        boolean belowConsSet_UB = upperConsensusValue.Get(i).number().doubleValue() <= neighCount.Get(i).number().doubleValue();
                        if (aboveConsSet_LB && belowConsSet_UB) {
                            inConsensus.set(RealScalar.of(1), i);
                        } else {
                            inConsensus.set(RealScalar.of(0), i);
                        }
                    }
                    // ==================================================================================================
                    // Compute Feedback Rebalancing
                    // ==================================================================================================
                    for (Map.Entry<VirtualLink<Link>, Double> entry : vLinkWeights.entrySet()) {
                        // ----------------------------------------------------------------------------------------------
                        // Get Link Imbalance
                        // ----------------------------------------------------------------------------------------------
                        // Get vLink info: weight and nodes
                        VirtualLink vLink = entry.getKey();
                        double linkWeight = entry.getValue();
                        int indexFrom = vLink.getFrom().getIndex();
                        int indexTo = vLink.getTo().getIndex();
                        // Get Imbalance
                        double imbalanceFrom = systemImbalance.Get(indexFrom).number().doubleValue(); // potentially leave as scalar
                        double imbalanceTo = systemImbalance.Get(indexTo).number().doubleValue();
                        // compute the rebalancing vehicles
                        double rebalance_From_To = 0.0;
                        // Get Wait Times
                        double waitTimesFrom = waitTimes.Get(indexFrom).number().doubleValue();
                        double waitTimesTo = waitTimes.Get(indexTo).number().doubleValue();
                        // ----------------------------------------------------------------------------------------------
                        // Feedback Rebalancing Term (do onyl if not in consensus Set, else FF solution)
                        // ----------------------------------------------------------------------------------------------

                        double diff_alphaij = feedfwrd_Rebalancing_LPR.Get(indexFrom, indexTo).number().doubleValue() - //
                                feedfwrd_Rebalancing_LPR.Get(indexTo, indexFrom).number().doubleValue();

                        double diff_rest = rebalancingOrderRest.Get(indexFrom, indexTo).number().doubleValue() - //
                                rebalancingOrderRest.Get(indexTo, indexFrom).number().doubleValue();

                        // if (Total.of(inConsensus).Get().number().intValue() != N_vStations){
                        if (true) {
                            switch (feebackTerm) {
                            case LDX: {
                                double lambdaFrom = lambda.Get(indexFrom).number().doubleValue();
                                double lambdaTo = lambda.Get(indexTo).number().doubleValue();
                                rebalance_From_To = rebalancingPeriod
                                        * (diff_alphaij + //
                                                (double) popSize * linkWeight * //
                                                        (imbalanceTo / lambdaTo - imbalanceFrom / lambdaFrom))
                                        + //
                                        diff_rest;
                                break;
                            }
                            case LW: {
                                rebalance_From_To = rebalancingPeriod
                                        * (diff_alphaij + //
                                                linkWeight * (waitTimesTo - waitTimesFrom))
                                        + //
                                        diff_rest;
                                break;

                            }
                            case LX: {
                                rebalance_From_To = rebalancingPeriod
                                        * (diff_alphaij + //
                                                linkWeight * (imbalanceTo - imbalanceFrom))
                                        + //
                                        diff_rest;
                                break;
                            }
                            case FFonly: {
                                rebalance_From_To = rebalancingPeriod * diff_alphaij + diff_rest;
                                break;
                            }
                            }
                        } else {
                            rebalance_From_To = rebalancingPeriod * diff_alphaij + diff_rest;
                        }
                        // Update feedback_Rebalance tensor
                        if (rebalance_From_To > 0) {
                            feedback_Rebalancing_DFR.set(RealScalar.of(rebalance_From_To), indexFrom, indexTo);
                        } else {
                            feedback_Rebalancing_DFR.set(RealScalar.of(-rebalance_From_To), indexTo, indexFrom);
                        }
                    }
                    // //DEBUG START
                    // System.out.println("DFRRebalancing Tensor:\n" + Pretty.of(feedback_Rebalancing_DFR));
                    // System.out.println("FF Rebalancing Tensor:\n" +
                    // Pretty.of(feedfwrd_Rebalancing_LPR.multiply(RealScalar.of(rebalancingPeriod))));
                    // //DEBUG END
                }
                // ======================================================================================================
                // Quantize Total Rebalancing //TODO Test against floor!!
                // ======================================================================================================
                // Tensor rebalancingOrder = Round.of(feedback_Rebalancing_DFR);
                Tensor rebalancingOrder = Floor.of(feedback_Rebalancing_DFR);
                rebalancingOrderRest = feedback_Rebalancing_DFR.subtract(rebalancingOrder);

                // //DEBUG START
                // System.out.println("Rebalancing Tensor:\n" + Pretty.of(rebalancingOrder));
                // System.out.println("Rebalancing Rest Tensor:\n" + Pretty.of(rebalancingOrderRest));
                System.out.println("Consensus Set:      " + Pretty.of(inConsensus));
                System.out.println("Neigh Count  :      " + Pretty.of(neighCount));
                if (Total.of(inConsensus).Get().number().intValue() == N_vStations) {
                    System.out.println("In Consensus Set: true");
                } else {
                    System.out.println("In Consensus Set: false");
                }
                // DEBUG END

                // ======================================================================================================
                // Total Rebalancing and Feasibility Test //TODO feasibility (test) & Count only actually rebalancing avs
                // ======================================================================================================

                Tensor feasibleRebalanceOrder = rebalancingOrder;
                /*
                 * feasibleRebalanceOrder = returnFeasibleRebalance(rebalanceCount, available_Vehicles);
                 */
                {
                    int rebCars = Total.of(Total.of(rebalancingOrder)).Get().number().intValue();
                    rebCount += rebCars;
                    // DEBUG START
                    System.out.println("Total Number of Cars to Rebalance now (fake! only count those actually sent!):" + Integer.toString(rebCars));
                    System.out
                            .println("Total Number of Total Rebalanced Cars (fake! only count those actually sent!) :" + Integer.toString(rebCount));
                    System.out.println("--------------------------------------------------------------------");
                    // DEBUG END
                }
                // ======================================================================================================
                // Execute Rebalancing Order
                // ======================================================================================================
                Map<VirtualNode<Link>, List<Link>> destinationLinks = virtualNetwork.createVNodeTypeMap();

                // Fill destinationLinks Map
                for (int rebalanceFromidx = 0; rebalanceFromidx < N_vStations; rebalanceFromidx++) {
                    for (int rebalanceToidx = 0; rebalanceToidx < N_vStations; rebalanceToidx++) {

                        int rebalanceCars = feasibleRebalanceOrder.Get(rebalanceFromidx, rebalanceToidx).number().intValue();
                        if (rebalanceCars != 0) {
                            VirtualNode rebalanceFromvNode = virtualNetwork.getVirtualNode(rebalanceFromidx);
                            VirtualNode rebalanceTovNode = virtualNetwork.getVirtualNode(rebalanceToidx);
                            List<Link> rebalanceTargets = virtualNodeDest.selectLinkSet(rebalanceTovNode, rebalanceCars);
                            destinationLinks.get(rebalanceFromvNode).addAll(rebalanceTargets);
                        }
                    }
                }

                // consistency check: rebalancing destination links must not exceed available vehicles in virtual node
                if (virtualNetwork.getVirtualNodes().stream().filter(v -> available_Vehicles.get(v).size() < destinationLinks.get(v).size()).findAny()
                        .isPresent()) {
                    System.out.print("too many verhilces sent;");
                }

                // send rebalancing vehicles using the setVehicleRebalance command
                // TODO Count Rebalanced cars correctly!
                // What happens if to many vehicles sent? does it send the ones it has and over or none if?or what happens here?
                for (VirtualNode virtualNode : destinationLinks.keySet()) {
                    Map<RoboTaxi, Link> rebalanceMatching = vehicleDestMatcher.matchLink(available_Vehicles.get(virtualNode),
                            destinationLinks.get(virtualNode));
                    rebalanceMatching.keySet().forEach(v -> setRoboTaxiRebalance(v, rebalanceMatching.get(v)));
                }
            }
        }
        // ==========================================================================================================
        // Match Remaining Cars to Customers
        // ==========================================================================================================
        if (round_now % redispatchPeriod == 0) {
            // II.ii if vehilces remain in vNode, send to customers
            {
                // collect destinations per vNode
                Map<VirtualNode<Link>, List<Link>> destinationLinks = virtualNetwork.createVNodeTypeMap();

                for (VirtualNode<Link> vNode : virtualNetwork.getVirtualNodes()) {
                    destinationLinks.get(vNode).addAll( // stores from links
                            requests.get(vNode).stream().map(AVRequest::getFromLink).collect(Collectors.toList()));
                }

                // collect available vehicles per vNode
                Map<VirtualNode<Link>, List<RoboTaxi>> available_Vehicles = getVirtualNodeDivertableNotRebalancingRoboTaxis();

                // assign destinations to the available vehicles
                {
                    GlobalAssert.that(available_Vehicles.keySet().containsAll(virtualNetwork.getVirtualNodes()));
                    GlobalAssert.that(destinationLinks.keySet().containsAll(virtualNetwork.getVirtualNodes()));

                    // DO NOT PUT PARALLEL anywhere in this loop !
                    GlobalAssert.that(false);
                    // THIS NEEDS TO BE FIXED, i.e. adapted to new dispatcher structure. Not working
                    // for (VirtualNode virtualNode : virtualNetwork.getVirtualNodes())
                    // vehicleDestMatcher //
                    // .matchLink(available_Vehicles.get(virtualNode), destinationLinks.get(virtualNode)) //
                    // .entrySet().stream().forEach(this::setVehiclePickup);
                }
            }

        }
    }

    private Tensor consensusLX(Tensor systemImbalance) {
        return Tensors.vector(j -> Mean.of(systemImbalance).Get(), N_vStations);
    }

    private Tensor consensusLDX(Tensor systemImbalance, Tensor lambda) {
        Scalar factor = Total.of(systemImbalance).Get().divide(Total.of(lambda).Get());
        return lambda.multiply(factor);
    }

    private Tensor avg_WaitTimePerVirtualNode(Map<VirtualNode<Link>, List<AVRequest>> requests, long timenow) {
        // TODO remove for-loop for more elgancy
        Tensor meanWaitTimepervNode = Tensors.empty();
        for (List<AVRequest> avRequests : requests.values()) {
            Tensor submission = Tensor.of(avRequests.stream().map(rc -> RealScalar.of(timenow - (long) rc.getSubmissionTime())));
            Scalar waitTimeMean = RealScalar.of(0);
            if (submission.length() != 0) {
                waitTimeMean = Mean.of(submission).Get();
            }
            meanWaitTimepervNode.append(waitTimeMean);
        }
        return meanWaitTimepervNode;
    }

    @Override
    public String getInfoLine() {
        return String.format("%s RE=%5d", //
                super.getInfoLine(), //
                rebCount //
        );
    }

    /**
     * FIXME in {@link PopulationDensityGenerator}
     */

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

        public static VirtualNetwork<Link> virtualNetwork;
        public static Map<VirtualLink<Link>, Double> linkWeights;

        @Override
        public AVDispatcher createDispatcher(AVDispatcherConfig config, AVGeneratorConfig generatorConfig) {

            AbstractVirtualNodeDest abstractVirtualNodeDest = new KMeansVirtualNodeDest();
            AbstractRequestSelector abstractRequestSelector = new OldestRequestSelector();
            AbstractVehicleDestMatcher abstractVehicleDestMatcher = new HungarBiPartVehicleDestMatcher(new EuclideanDistanceFunction());
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
                // TODO  XML networks are deprecated
                GlobalAssert.that(false);
                virtualNetwork = null;
//                virtualNetwork = VirtualNetworkIO.fromXML(network, virtualnetworkFile);
            }
            // ---
            {
                final String string = "consensusWeights_" + config.getParams().get(KEY_WEIGHTSEXTENSION) + ".xml";
                final File linkWeightsXML = new File(virtualnetworkDir, string);
                GlobalAssert.that(linkWeightsXML.isFile());
                //linkWeights = vLinkDataReader.fillvLinkData(linkWeightsXML, virtualNetwork, "weight");
                linkWeights = null;
                GlobalAssert.that(linkWeights!=null);
                //TODO datareader above was deleted, read your data directly from virtualNetwork. 
            }

            TravelData arrivalInformation = null;
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
                    GlobalAssert.that(false);
                    arrivalInformation = null;
                    // TODO load from serialized data not XML, XML load function deleted. 
//                    arrivalInformation = new TravelData(virtualNetwork, lambdaXML, pijFile, alphaijFile, //
//                            populationSize, //
//                            rebalancingPeriod //
//                    );
                } catch (Exception e) {
                    e.printStackTrace();
                    GlobalAssert.that(false);
                }
            }

            return new DFRDispatcher(config, generatorConfig, travelTime, router, eventsManager, virtualNetwork, //
                    abstractVirtualNodeDest, //
                    abstractRequestSelector, //
                    abstractVehicleDestMatcher, //
                    linkWeights, //
                    arrivalInformation);
        }
    }
}
// ======================================================================================================================
// EOF
// ======================================================================================================================
/*
 * OLD RETURN FEASIBLE REQUEST FCT
 * 
 * @Deprecated private Map<VirtualLink, Integer> returnFeasibleRebalance(Map<VirtualLink, Integer> rebalanceInput, Map<VirtualNode,
 * List<VehicleLinkPair>> available_Vehicles) { Map<VirtualLink, Integer> feasibleRebalance = new HashMap<>(); feasibleRebalance = rebalanceInput;
 * 
 * // for every vNode check if enough vehicles are available to rebalance for (VirtualNode virtualNode : virtualNetwork.getVirtualNodes()) {
 * 
 * // count outgoing rebalancing vehicles from the vNode int totRebVecFromvNode = 0; for (VirtualLink vLink : rebalanceInput.keySet()) { if
 * (vLink.getFrom().equals(virtualNode) || rebalanceInput.get(vLink) >= 0) { totRebVecFromvNode = totRebVecFromvNode + rebalanceInput.get(vLink); } if
 * (vLink.getTo().equals(virtualNode) || rebalanceInput.get(vLink) < 0) { totRebVecFromvNode = totRebVecFromvNode - rebalanceInput.get(vLink); } }
 * 
 * // TODO think if instead of shrinking factor just for some links vehicles should be sent instead (less wait time) // adapt number of vehicles to be
 * sent if (available_Vehicles.get(virtualNode).size() < totRebVecFromvNode) { // calculate by how much to shrink double shrinkingFactor = ((double)
 * available_Vehicles.get(virtualNode).size()) / ((double) totRebVecFromvNode); // remove rebalancing vehicles for (VirtualLink vLink :
 * rebalanceInput.keySet()) { if (vLink.getFrom().equals(virtualNode) || rebalanceInput.get(vLink) >= 0) { int newIntRebCount = (int)
 * Math.floor(rebalanceInput.get(vLink) * shrinkingFactor); int newLeftOver = rebalanceInput.get(vLink) - newIntRebCount; feasibleRebalance.put(vLink,
 * newIntRebCount); double oldRebFloating = rebalanceFloating.get(vLink); rebalanceFloating.put(vLink, oldRebFloating + (double) newLeftOver); } if
 * (vLink.getTo().equals(virtualNode) || rebalanceInput.get(vLink) < 0) { int newIntRebCount = (int) Math.floor(rebalanceInput.get(vLink) *
 * shrinkingFactor); int newLeftOver = rebalanceInput.get(vLink) - newIntRebCount; feasibleRebalance.put(vLink, newIntRebCount); double oldRebFloating
 * = rebalanceFloating.get(vLink); rebalanceFloating.put(vLink, oldRebFloating + (double) newLeftOver); } } } } return feasibleRebalance; }
 */
