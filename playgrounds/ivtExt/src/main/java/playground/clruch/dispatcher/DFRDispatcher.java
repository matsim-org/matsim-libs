/**
 =======================================================================================================================
  DISTRIBUTED FEEDBACK REBALANCING (DFR) DISPATCHER
 =======================================================================================================================
 * Implemented by Claudio Ruch, Marc Albert (30.03.2017)
 */

package playground.clruch.dispatcher;

import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.alg.Array;
import ch.ethz.idsc.tensor.alg.Dimensions;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * {@link PopulationDensityGenerator}
 */
public class DFRDispatcher extends PartitionedDispatcher {
    public static final String KEY_REBALANCINGPERIOD = "rebalancingPeriod";
    public static final String KEY_VIRTUALNETWORKDIRECTORY = "virtualNetworkDirectory";
    public static final String KEY_DTEXTENSION = "dtExtension";
    public static final String KEY_WEIGHTSEXTENSION = "weightsExtension";
    public static final String KEY_FEEDBACKTERM = "feedbackTerm";

    private final int rebalancingPeriod;
    final AbstractVirtualNodeDest virtualNodeDest;
    final AbstractRequestSelector requestSelector;
    final AbstractVehicleDestMatcher vehicleDestMatcher;
    final ArrivalInformation arrivalInformation;
    private final Map<VirtualLink, Double> rebalanceFloating;
    private final Map<VirtualLink, Double> vLinkWeights;
    private int rebCount = 0;
    private final int N_vStations;
    private final long popSize;

    private final FeedbackTerm feebackTerm;

    public DFRDispatcher( //
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
        rebalanceFloating = new HashMap<>();
        for (VirtualLink virtualLink : virtualNetwork.getVirtualLinks())
            rebalanceFloating.put(virtualLink, 0.0);
        vLinkWeights = linkWeightsIn;
        this.arrivalInformation = arrivalInformation;
        rebalancingPeriod = Integer.parseInt(config.getParams().get(KEY_REBALANCINGPERIOD));
        N_vStations = virtualNetwork.getvNodesCount(); //Consider here nodecount -1 as no vLink to leftover region!!
        popSize = arrivalInformation.populationSize;
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
        final long round_now = Math.round(now);
        if (round_now % rebalancingPeriod == 0) {
            //==========================================================================================================
            // Get Open Requests
            //==========================================================================================================
                Map<VirtualNode, List<AVRequest>> requests = getVirtualNodeRequests();
            //==========================================================================================================
            // DFR Algorithm
            //==========================================================================================================
            {   //======================================================================================================
                // Initialize
                //======================================================================================================
                    // Get System State
                    Map<VirtualNode, List<VehicleLinkPair>> available_Vehicles = getVirtualNodeDivertableNotRebalancingVehicles();
                    Map<VirtualNode, Set<AVVehicle>> v_ij_reb = getVirtualNodeRebalancingToVehicles();
                    //Init Matrices
                    Tensor rebalancingTovStation    = Array.zeros(N_vStations);
                    Tensor openRequests             = Array.zeros(N_vStations);
                    Tensor availableVehicles        = Array.zeros(N_vStations);
                    Tensor feedback_Rebalancing_DFR = Array.zeros(N_vStations, N_vStations);
                    Tensor feedback_Rebalancing     = Array.zeros(N_vStations, N_vStations);
                    Tensor feedfwrd_Rebalancing_LPR = Array.zeros(N_vStations, N_vStations);
                    //Update Matrices
                    available_Vehicles.entrySet().stream().forEach(e -> availableVehicles.set(RealScalar.of(e.getValue().size()), e.getKey().index));
                    v_ij_reb.entrySet().stream().forEach(e -> rebalancingTovStation.set(RealScalar.of(e.getValue().size()),e.getKey().index));
                    requests.entrySet().stream().forEach(e->openRequests.set(RealScalar.of(e.getValue().size()),e.getKey().index));
                //======================================================================================================
                // Compute Excess Vehicles -> System Imbalance
                //======================================================================================================
                    Tensor systemImbalance = openRequests.subtract(availableVehicles).subtract(rebalancingTovStation);
                //DEBUG START
                    System.out.println("Available Vehicles: "+ availableVehicles.toString());
                    System.out.println("Open Requests     : "+ openRequests.toString());
                    System.out.println("Rebalancing to vS : "+ rebalancingTovStation.toString());
                    System.out.println("System Imbalance : "+ systemImbalance.toString());
                //DEBUG END
                //======================================================================================================
                // DFR Iterations: Compute Rebalancing
                //======================================================================================================
                 Map<VirtualLink, Integer> rebalanceCount = new HashMap<>(); // TODO
                {
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
                        // Update Rebalancing
                        //==============================================================================================
                        switch (feebackTerm) {
                            case LDX: {
                                // Get Lambda and test if not in consensus Set
                                Tensor lambda = arrivalInformation.getLambdaforTime(now);
                                GlobalAssert.that(Total.of(lambda).Get().number().doubleValue() != 0); // no lambda is ever zero (if zero take next non-zero)
                                Tensor consensusVal = consensusLDX(systemImbalance,lambda);
                                boolean notConsSetFrom = (Math.abs(imbalanceFrom - consensusVal.Get(indexFrom).number().intValue()) > vLink.getFrom().getNeighCount());
                                boolean notConsSetTo = (Math.abs(imbalanceTo - consensusVal.Get(indexTo).number().intValue()) > vLink.getTo().getNeighCount());
                                if (notConsSetFrom || notConsSetTo) {
                                    double lambdaFrom = lambda.Get(indexFrom).number().doubleValue();
                                    double lambdaTo = lambda.Get(indexTo).number().doubleValue();
                                // do rebalancing if not in consensus Set
                                    rebalance_From_To = //
                                            rebalancingPeriod * (double) popSize * linkWeight * ( //
                                                    (double) imbalanceTo / lambdaTo - //
                                                            (double) imbalanceFrom / lambdaFrom) + //
                                                    rebalanceFloating.get(vLink);

                                }
                                break;
                            }
                            case LX: {
                                // Test if not in consensus Set
                                Scalar consensusVal = consensusLX(systemImbalance) ;
                                boolean notConsSetFrom = (Math.abs(imbalanceFrom - consensusVal.number().intValue()) > vLink.getFrom().getNeighCount());
                                boolean notConsSetTo = (Math.abs(imbalanceTo - consensusVal.number().intValue()) > vLink.getTo().getNeighCount());
                                if (notConsSetFrom || notConsSetTo) {
                                // do rebalancing if not in consensus Set
                                    rebalance_From_To = //
                                            rebalancingPeriod * linkWeight * ( //
                                                    (double) imbalanceTo - //
                                                            (double) imbalanceFrom) + //
                                                    rebalanceFloating.get(vLink);
                                }
                                break;
                            }
                        }
                        //==============================================================================================
                        // Quantize Rebalancing
                        //==============================================================================================

                        int rebalanceFromTo = (int) Math.round(rebalance_From_To);

                        double rebalanceRest = rebalance_From_To - (double) rebalanceFromTo;
                        rebalanceCount.put(vLink, rebalanceFromTo); // TODO ABOVE READ FROM TENSOR
                        rebalanceFloating.put(vLink, rebalanceRest);
                        //Update feedback_Rebalance tensor
                        if (rebalanceFromTo > 0) {
                            feedback_Rebalancing_DFR.set(RealScalar.of(rebalanceFromTo), indexFrom, indexTo);
                        } else {
                            feedback_Rebalancing_DFR.set(RealScalar.of(-rebalanceFromTo), indexTo, indexFrom);
                        }
                    }
                }


                System.out.println("Rebalancing Tensor:\n" + Pretty.of(feedback_Rebalancing_DFR));

                //======================================================================================================
                // Test Feasibility of Rebalancing Assignment // TODO?
                //======================================================================================================
                Map<VirtualLink, Integer> feasibleRebalanceCount = rebalanceCount; // new HashMap<>();

                    /*
                     * feasibleRebalanceCount = returnFeasibleRebalance(rebalanceCount, available_Vehicles);
                     */
                {
                    int posReb = feasibleRebalanceCount.values().stream().filter(v -> v > 0).mapToInt(v -> v).sum();
                    int negReb = feasibleRebalanceCount.values().stream().filter(v -> v < 0).mapToInt(v -> v).sum();
                    rebCount += posReb - negReb;
                }
                // generate routing instructions for rebalancing vehicles

                //======================================================================================================
                // Execute Rebalancing Order //TODO Tensor notation for fill rebalancing destin
                //======================================================================================================
                // TODO: ensure that a rebalanced vehicle is then under the control of the to-virtualNode and can be dispatched there: Claudio
                Map<VirtualNode, List<Link>> destinationLinks = createvNodeLinksMap();

                // fill rebalancing destinations
                for (Map.Entry<VirtualLink, Integer> entry : feasibleRebalanceCount.entrySet()) {
                    if (feasibleRebalanceCount.get(entry.getKey()) >= 0) {
                        List<Link> rebalanceTargets = virtualNodeDest.selectLinkSet(entry.getKey().getTo(), entry.getValue());
                        destinationLinks.get(entry.getKey().getFrom()).addAll(rebalanceTargets);
                    } else {
                        List<Link> rebalanceTargets = virtualNodeDest.selectLinkSet(entry.getKey().getFrom(), -entry.getValue());
                        destinationLinks.get(entry.getKey().getTo()).addAll(rebalanceTargets);
                    }
                }

                // consistency check: rebalancing destination links must not exceed available vehicles in virtual node
                if (virtualNetwork.getVirtualNodes().stream().filter(v -> available_Vehicles.get(v).size() < destinationLinks.get(v).size()).findAny().isPresent()) {
                    System.out.print("too many verhilces sent;");
                }

                //TODO REWRITE in TENSOR
                // send rebalancing vehicles using the setVehicleRebalance command
                for (VirtualNode virtualNode : destinationLinks.keySet()) {
                    Map<VehicleLinkPair, Link> rebalanceMatching = vehicleDestMatcher.match(available_Vehicles.get(virtualNode), destinationLinks.get(virtualNode));
                    rebalanceMatching.keySet().forEach(v -> setVehicleRebalance(v, rebalanceMatching.get(v)));
                }
            }
            //==========================================================================================================
            // Match Remaining Cars to Customers
            //==========================================================================================================

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

                try {
                    long populationSize = population.getPersons().size();
                    int rebalancingPeriod = Integer.parseInt(config.getParams().get("rebalancingPeriod"));
                    arrivalInformation = new ArrivalInformation(virtualNetwork, lambdaXML, pijFile, //
                            populationSize, //
                            rebalancingPeriod //
                    );
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
