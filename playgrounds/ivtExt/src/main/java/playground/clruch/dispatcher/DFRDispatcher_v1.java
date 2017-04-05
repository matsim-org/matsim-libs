/**
 =======================================================================================================================
  DISTRIBUTED FEEDBACK REBALANCING (DFR) DISPATCHER: Round(a+b) versionh
 =======================================================================================================================
 * Implemented by Marc Albert, Claudio Ruch (30.03.2017)
 */

package playground.clruch.dispatcher;

import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.alg.Array;
import ch.ethz.idsc.tensor.io.Pretty;
import ch.ethz.idsc.tensor.red.Mean;
import ch.ethz.idsc.tensor.red.Total;
import ch.ethz.idsc.tensor.sca.Round;
import com.google.inject.Inject;
import com.google.inject.name.Named;
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
import playground.sebhoerl.avtaxi.data.AVVehicle;
import playground.sebhoerl.avtaxi.dispatcher.AVDispatcher;
import playground.sebhoerl.avtaxi.framework.AVModule;
import playground.sebhoerl.avtaxi.generator.PopulationDensityGenerator;
import playground.sebhoerl.avtaxi.passenger.AVRequest;
import playground.sebhoerl.plcpc.ParallelLeastCostPathCalculator;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * {@link PopulationDensityGenerator}
 */
public class DFRDispatcher_v1 extends PartitionedDispatcher {
    public static final String KEY_REBALANCINGPERIOD = "rebalancingPeriod";
    public static final String KEY_VIRTUALNETWORKDIRECTORY = "virtualNetworkDirectory";
    public static final String KEY_DTEXTENSION = "dtExtension";
    public static final String KEY_WEIGHTSEXTENSION = "weightsExtension";
    public static final String KEY_FEEDBACKTERM = "feedbackTerm";
    public static final String KEY_REDISPATCHPERIOD = "redispatchPeriod";

    private final int rebalancingPeriod;
    private final int redispatchPeriod;
    final AbstractVirtualNodeDest virtualNodeDest;
    final AbstractRequestSelector requestSelector;
    final AbstractVehicleDestMatcher vehicleDestMatcher;
    final ArrivalInformation arrivalInformation;
    private final Map<VirtualLink, Double> vLinkWeights;
    private int rebCount = 0;
    private final int N_vStations;
    private final long popSize;
    private Tensor rebalancingOrderRest;

    private final FeedbackTerm feebackTerm;

    public DFRDispatcher_v1( //
                             AVDispatcherConfig config, //
                             AVGeneratorConfig generatorConfig, //
                             TravelTime travelTime, //
                             ParallelLeastCostPathCalculator router, //
                             EventsManager eventsManager, //
                             VirtualNetwork virtualNetwork, //
                             AbstractVirtualNodeDest abstractVirtualNodeDest, //
                             AbstractRequestSelector abstractRequestSelector, //
                             AbstractVehicleDestMatcher abstractVehicleDestMatcher, //
                             Map<VirtualLink, Double> linkWeightsIn, //
                             ArrivalInformation arrivalInformation) {
        super(config, travelTime, router, eventsManager, virtualNetwork);

        this.virtualNodeDest = abstractVirtualNodeDest;
        this.requestSelector = abstractRequestSelector;
        this.vehicleDestMatcher = abstractVehicleDestMatcher;
        vLinkWeights = linkWeightsIn;
        this.arrivalInformation = arrivalInformation;
        rebalancingPeriod = Integer.parseInt(config.getParams().get(KEY_REBALANCINGPERIOD));
        redispatchPeriod = Integer.parseInt(config.getParams().get(KEY_REDISPATCHPERIOD));
        N_vStations = virtualNetwork.getvNodesCount(); //Consider here nodecount -1 as no vLink to leftover region!!
        popSize = arrivalInformation.populationSize;
        rebalancingOrderRest   = Array.zeros(N_vStations, N_vStations);

        feebackTerm = FeedbackTerm.valueOf(
                config.getParams().get(KEY_FEEDBACKTERM));
    }

    @Override
    public void redispatch(double now) {
        // A: outside rebalancing periods, permanently assign vehicles to requests if they have arrived at a customer
        // i.e. stay on the same link
        new InOrderOfArrivalMatcher(this::setAcceptRequest) //
                .match(getStayVehicles(), getAVRequestsAtLinks());
        // Map<VirtualNode, List<VehicleLinkPair>> available_Vehicles = getVirtualNodeAvailableNotRebalancingVehicles();

        // B: redispatch all vehicles
        //==============================================================================================================
        // Get Open Requests
        //==============================================================================================================
        Map<VirtualNode, List<AVRequest>> requests = getVirtualNodeRequests();
        //==============================================================================================================
        // DFR Algorithm
        //==============================================================================================================
        final long round_now = Math.round(now);
        if (round_now % rebalancingPeriod == 0) {

            {   //======================================================================================================
                // Initialize
                //======================================================================================================
                // Get System State
                Map<VirtualNode, List<VehicleLinkPair>> available_Vehicles = getVirtualNodeDivertableNotRebalancingVehicles();
                Map<VirtualNode, Set<AVVehicle>> v_ij_reb = getVirtualNodeRebalancingToVehicles();
                //Init Matrices
                Tensor rebalancingTovStation = Array.zeros(N_vStations);
                Tensor openRequests = Array.zeros(N_vStations);
                Tensor availableVehicles = Array.zeros(N_vStations);
                Tensor feedback_Rebalancing_DFR = Array.zeros(N_vStations, N_vStations);
                Tensor feedfwrd_Rebalancing_LPR = Array.zeros(N_vStations, N_vStations);
                //Update Matrices
                available_Vehicles.entrySet().stream().forEach(e -> availableVehicles.set(RealScalar.of(e.getValue().size()), e.getKey().index));
                v_ij_reb.entrySet().stream().forEach(e -> rebalancingTovStation.set(RealScalar.of(e.getValue().size()), e.getKey().index));
                requests.entrySet().stream().forEach(e -> openRequests.set(RealScalar.of(e.getValue().size()), e.getKey().index));
                //======================================================================================================
                // Compute Excess Vehicles -> System Imbalance
                //======================================================================================================
                Tensor systemImbalance = openRequests.subtract(availableVehicles).subtract(rebalancingTovStation);
                //DEBUG START
                System.out.println("--------------------------------------------------------------------");
                System.out.println("Available Vehicles: " + availableVehicles.toString());
                System.out.println("Open Requests     : " + openRequests.toString());
                System.out.println("Rebalancing to vS : " + rebalancingTovStation.toString());
                System.out.println("System Imbalance : " + systemImbalance.toString());
                //DEBUG END
                //======================================================================================================
                // DFR Iterations: Compute Rebalancing
                //======================================================================================================
                {
                    //==================================================================================================
                    // Get Feedforward rebalancing Rates
                    //==================================================================================================
                    Tensor alpha_ij = arrivalInformation.getAlphaijforTime((int) now);
                    Tensor alphaij = alpha_ij.multiply(RealScalar.of(popSize));

                    //FeedForward Rebalancing
                    for (int i = 0; i < N_vStations - 1; i++) {
                        for (int j = 0; j < N_vStations - 1; j++) {
                            feedfwrd_Rebalancing_LPR.set(alphaij.Get(i, j), i, j);
                        }
                    }

                    //==================================================================================================
                    // Compute Feedback Rebalancing
                    //==================================================================================================
                    for (Map.Entry<VirtualLink, Double> entry : vLinkWeights.entrySet()) {
                        //==============================================================================================
                        // SETUP
                        //==============================================================================================
                        //Get vLink info: weight and nodes
                        VirtualLink vLink = entry.getKey();
                        double linkWeight = entry.getValue();
                        int indexFrom = vLink.getFrom().getIndex();
                        int indexTo = vLink.getTo().getIndex();
                        //Get Imbalance
                        int imbalanceFrom = systemImbalance.Get(indexFrom).number().intValue(); //potentially leave as scalar
                        int imbalanceTo = systemImbalance.Get(indexTo).number().intValue();
                        // compute the rebalancing vehicles
                        double rebalance_From_To = 0.0;
                        //==============================================================================================
                        // Feedback Rebalancing Term
                        //==============================================================================================
                        switch (feebackTerm) {
                            case LDX: {
                                // Get Lambda and test if not in consensus Set
                                Tensor lambda = arrivalInformation.getNextNonZeroLambdaforTime((int) now);
                                GlobalAssert.that(Total.of(lambda).Get().number().doubleValue() != 0); // no lambda is ever zero (if zero take next non-zero)
                                Tensor consensusVal = consensusLDX(systemImbalance, lambda);
                                boolean notConsSetFrom = (Math.abs((double) imbalanceFrom - consensusVal.Get(indexFrom).number().doubleValue()) > (double) vLink.getFrom().getNeighCount());
                                boolean notConsSetTo = (Math.abs((double) imbalanceTo - consensusVal.Get(indexTo).number().doubleValue()) > (double) vLink.getTo().getNeighCount());
                                if (notConsSetFrom || notConsSetTo) {
                                    double lambdaFrom = lambda.Get(indexFrom).number().doubleValue();
                                    double lambdaTo = lambda.Get(indexTo).number().doubleValue();
                                    // do  feedback rebalancing if not in consensus Set
                                    rebalance_From_To = //
                                            rebalancingPeriod * (double) popSize * linkWeight * ( //
                                                    (double) imbalanceTo / lambdaTo - //
                                                            (double) imbalanceFrom / lambdaFrom) +//
                                                    rebalancingOrderRest.Get(indexFrom, indexTo).number().doubleValue() -//
                                                    rebalancingOrderRest.Get(indexTo, indexFrom).number().doubleValue();
                                } else {
                                    //if in consensus set only do FF rebalancing
                                    rebalance_From_To = rebalancingPeriod * (feedfwrd_Rebalancing_LPR.Get(indexFrom, indexTo).number().doubleValue() - //
                                            feedfwrd_Rebalancing_LPR.Get(indexTo, indexFrom).number().doubleValue()) + //
                                            rebalancingOrderRest.Get(indexFrom, indexTo).number().doubleValue() -//
                                            rebalancingOrderRest.Get(indexTo, indexFrom).number().doubleValue();
                                }
                                break;
                            }
                            case LX: {
                                // Test if not in consensus Set
                                Scalar consensusVal = consensusLX(systemImbalance);
                                boolean notConsSetFrom = (Math.abs(imbalanceFrom - consensusVal.number().intValue()) > vLink.getFrom().getNeighCount());
                                boolean notConsSetTo = (Math.abs(imbalanceTo - consensusVal.number().intValue()) > vLink.getTo().getNeighCount());
                                if (notConsSetFrom || notConsSetTo) {
                                    // do feedback rebalancing if not in consensus Set
                                    rebalance_From_To = //
                                            rebalancingPeriod * linkWeight * ( //
                                                    (double) imbalanceTo - //
                                                            (double) imbalanceFrom) +//
                                                    rebalancingOrderRest.Get(indexFrom, indexTo).number().doubleValue() - //
                                                    rebalancingOrderRest.Get(indexTo, indexFrom).number().doubleValue();
                                } else {
                                    //if on consensus set only do FF rebalancing
                                    rebalance_From_To = rebalancingPeriod * (feedfwrd_Rebalancing_LPR.Get(indexFrom, indexTo).number().doubleValue() - //
                                            feedfwrd_Rebalancing_LPR.Get(indexTo, indexFrom).number().doubleValue()) + //
                                            rebalancingOrderRest.Get(indexFrom, indexTo).number().doubleValue() -//
                                            rebalancingOrderRest.Get(indexTo, indexFrom).number().doubleValue();
                                }
                                break;
                            }
                            case FFonly: {
                                //DEBUG START
                                double tst = (feedfwrd_Rebalancing_LPR.Get(indexFrom, indexTo).number().doubleValue() - //
                                        feedfwrd_Rebalancing_LPR.Get(indexTo, indexFrom).number().doubleValue());
                                if (tst != 0) {
                                    System.out.println("Debug BP");
                                }
                                //DEBUG END
                                rebalance_From_To = rebalancingPeriod * (feedfwrd_Rebalancing_LPR.Get(indexFrom, indexTo).number().doubleValue() - //
                                        feedfwrd_Rebalancing_LPR.Get(indexTo, indexFrom).number().doubleValue()) + //
                                        rebalancingOrderRest.Get(indexFrom, indexTo).number().doubleValue() -//
                                        rebalancingOrderRest.Get(indexTo, indexFrom).number().doubleValue();
                            }
                        }
                        //Update feedback_Rebalance tensor
                        if (rebalance_From_To > 0) {
                            feedback_Rebalancing_DFR.set(RealScalar.of(rebalance_From_To), indexFrom, indexTo);
                        } else {
                            feedback_Rebalancing_DFR.set(RealScalar.of(-rebalance_From_To), indexTo, indexFrom);
                        }
                    }
                    //DEBUG START
//                    System.out.println("DFRRebalancing Tensor:\n" + Pretty.of(feedback_Rebalancing_DFR));
//                    System.out.println("FF Rebalancing Tensor:\n" + Pretty.of(feedfwrd_Rebalancing_LPR.multiply(RealScalar.of(rebalancingPeriod))));
                    //DEBUG END
                }
                //======================================================================================================
                // Quantize Total Rebalancing
                //======================================================================================================
                Tensor rebalancingOrder = Round.of(feedback_Rebalancing_DFR);
                rebalancingOrderRest = feedback_Rebalancing_DFR.subtract(rebalancingOrder);

                //DEBUG START
//                System.out.println("Rebalancing Tensor:\n" + Pretty.of(rebalancingOrder));
//                System.out.println("Rebalancing Rest Tensor:\n" + Pretty.of(rebalancingOrderRest));
                //DEBUG END

                //======================================================================================================
                // Total Rebalancing and Feasibility Test //TODO feasibility (test)
                //======================================================================================================

                Tensor feasibleRebalanceOrder = rebalancingOrder;
                    /*
                     * feasibleRebalanceOrder = returnFeasibleRebalance(rebalanceCount, available_Vehicles);
                     */
                {
                    int rebCars = Total.of(Total.of(rebalancingOrder)).Get().number().intValue();
                    rebCount += rebCars;
                    //DEBUG START
                    System.out.println("Total Number of Cars to Rebalance now (fake! only count those actually sent!):" + Integer.toString(rebCars));
                    System.out.println("Total Number of Total Rebalanced Cars (fake! only count those actually sent!) :" + Integer.toString(rebCount));
                    System.out.println("--------------------------------------------------------------------");
                    //DEBUG STOPP
                }
                //======================================================================================================
                // Execute Rebalancing Order
                //======================================================================================================
                Map<VirtualNode, List<Link>> destinationLinks = createvNodeLinksMap();

                //Fill destinationLinks Map
                for (int rebalanceFromidx = 0; rebalanceFromidx < N_vStations - 1; rebalanceFromidx++) {
                    for (int rebalanceToidx = 0; rebalanceToidx < N_vStations - 1; rebalanceToidx++) {

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
                if (virtualNetwork.getVirtualNodes().stream().filter(v -> available_Vehicles.get(v).size() < destinationLinks.get(v).size()).findAny().isPresent()) {
                    System.out.print("too many verhilces sent;");
                }

                // send rebalancing vehicles using the setVehicleRebalance command
                // TODO Count Rebalanced cars correctly!
                // What happens if to many vehicles sent? does it send the ones it has and over or none if?or what happens here?
                for (VirtualNode virtualNode : destinationLinks.keySet()) {
                    Map<VehicleLinkPair, Link> rebalanceMatching = vehicleDestMatcher.match(available_Vehicles.get(virtualNode), destinationLinks.get(virtualNode));
                    rebalanceMatching.keySet().forEach(v -> setVehicleRebalance(v, rebalanceMatching.get(v)));
                }
            }
        }
        //==========================================================================================================
        // Match Remaining Cars to Customers
        //==========================================================================================================
        if (round_now % redispatchPeriod == 0){
        // II.ii if vehilces remain in vNode, send to customers
            {
                // collect destinations per vNode
                Map<VirtualNode, List<Link>> destinationLinks = createvNodeLinksMap();

                for (VirtualNode vNode : virtualNetwork.getVirtualNodes()) {
                    destinationLinks.get(vNode).addAll( // stores from links
                            requests.get(vNode).stream().map(AVRequest::getFromLink).collect(Collectors.toList()));
                }

                // collect available vehicles per vNode
                Map<VirtualNode, List<VehicleLinkPair>> available_Vehicles = getVirtualNodeDivertableNotRebalancingVehicles();

                // assign destinations to the available vehicles
                {
                    GlobalAssert.that(available_Vehicles.keySet().containsAll(virtualNetwork.getVirtualNodes()));
                    GlobalAssert.that(destinationLinks.keySet().containsAll(virtualNetwork.getVirtualNodes()));

                    // DO NOT PUT PARALLEL anywhere in this loop !
                    for (VirtualNode virtualNode : virtualNetwork.getVirtualNodes())
                        vehicleDestMatcher //
                                .match(available_Vehicles.get(virtualNode), destinationLinks.get(virtualNode)) //
                                .entrySet().stream().forEach(this::setVehicleDiversion);
                }
            }

        }
    }

    private Scalar consensusLX(Tensor systemImbalance){
        return Mean.of(systemImbalance.extract(0,N_vStations-1)).Get(); // -1 due to leftOver Node
    }

    private Tensor consensusLDX(Tensor systemImbalance, Tensor lambda){
        Scalar factor = Total.of(systemImbalance).Get().divide(Total.of(lambda).Get());
        return lambda.multiply(factor);
    }
//======================================================================================================================
//    EOF
//======================================================================================================================
    /*
    @Deprecated
    private Map<VirtualLink, Integer> returnFeasibleRebalance(Map<VirtualLink, Integer> rebalanceInput, Map<VirtualNode, List<VehicleLinkPair>> available_Vehicles) {
        Map<VirtualLink, Integer> feasibleRebalance = new HashMap<>();
        feasibleRebalance = rebalanceInput;

        // for every vNode check if enough vehicles are available to rebalance
        for (VirtualNode virtualNode : virtualNetwork.getVirtualNodes()) {

            // count outgoing rebalancing vehicles from the vNode
            int totRebVecFromvNode = 0;
            for (VirtualLink vLink : rebalanceInput.keySet()) {
                if (vLink.getFrom().equals(virtualNode) || rebalanceInput.get(vLink) >= 0) {
                    totRebVecFromvNode = totRebVecFromvNode + rebalanceInput.get(vLink);
                }
                if (vLink.getTo().equals(virtualNode) || rebalanceInput.get(vLink) < 0) {
                    totRebVecFromvNode = totRebVecFromvNode - rebalanceInput.get(vLink);
                }
            }

            // TODO think if instead of shrinking factor just for some links vehicles should be sent instead (less wait time)
            // adapt number of vehicles to be sent
            if (available_Vehicles.get(virtualNode).size() < totRebVecFromvNode) {
                // calculate by how much to shrink
                double shrinkingFactor = ((double) available_Vehicles.get(virtualNode).size()) / ((double) totRebVecFromvNode);
                // remove rebalancing vehicles
                for (VirtualLink vLink : rebalanceInput.keySet()) {
                    if (vLink.getFrom().equals(virtualNode) || rebalanceInput.get(vLink) >= 0) {
                        int newIntRebCount = (int) Math.floor(rebalanceInput.get(vLink) * shrinkingFactor);
                        int newLeftOver = rebalanceInput.get(vLink) - newIntRebCount;
                        feasibleRebalance.put(vLink, newIntRebCount);
                        double oldRebFloating = rebalanceFloating.get(vLink);
                        rebalanceFloating.put(vLink, oldRebFloating + (double) newLeftOver);
                    }
                    if (vLink.getTo().equals(virtualNode) || rebalanceInput.get(vLink) < 0) {
                        int newIntRebCount = (int) Math.floor(rebalanceInput.get(vLink) * shrinkingFactor);
                        int newLeftOver = rebalanceInput.get(vLink) - newIntRebCount;
                        feasibleRebalance.put(vLink, newIntRebCount);
                        double oldRebFloating = rebalanceFloating.get(vLink);
                        rebalanceFloating.put(vLink, oldRebFloating + (double) newLeftOver);
                    }
                }
            }
        }
        return feasibleRebalance;
    }
*/
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

        public static VirtualNetwork virtualNetwork;
        public static Map<VirtualLink, Double> linkWeights;

        @Override
        public AVDispatcher createDispatcher(AVDispatcherConfig config, AVGeneratorConfig generatorConfig) {

            AbstractVirtualNodeDest abstractVirtualNodeDest = new KMeansVirtualNodeDest();
            AbstractRequestSelector abstractRequestSelector = new OldestRequestSelector();
            AbstractVehicleDestMatcher abstractVehicleDestMatcher = new HungarBiPartVehicleDestMatcher();
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
            }
            // ---
            {
                final String string = "consensusWeights_" + config.getParams().get(KEY_WEIGHTSEXTENSION) + ".xml";
                final File linkWeightsXML = new File(virtualnetworkDir, string);
                GlobalAssert.that(linkWeightsXML.isFile());
                linkWeights = vLinkDataReader.fillvLinkData(linkWeightsXML, virtualNetwork, "weight");
            }

            ArrivalInformation arrivalInformation = null;
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
                    arrivalInformation = new ArrivalInformation(virtualNetwork, lambdaXML, pijFile, alphaijFile,//
                            populationSize, //
                            rebalancingPeriod //
                    );
                } catch (Exception e) {
                    e.printStackTrace();
                    GlobalAssert.that(false);
                }
            }

            return new DFRDispatcher_v1(config, generatorConfig, travelTime, router, eventsManager, virtualNetwork, //
                    abstractVirtualNodeDest, //
                    abstractRequestSelector, //
                    abstractVehicleDestMatcher, //
                    linkWeights, //
                    arrivalInformation);
        }
    }
}
