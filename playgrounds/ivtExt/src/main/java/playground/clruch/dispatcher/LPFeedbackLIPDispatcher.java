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
import ch.ethz.idsc.tensor.alg.Floor;
import ch.ethz.idsc.tensor.alg.LinearSolve;
import ch.ethz.idsc.tensor.alg.Transpose;
import ch.ethz.idsc.tensor.io.Pretty;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.gnu.glpk.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
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
import playground.sebhoerl.avtaxi.passenger.AVRequest;
import playground.sebhoerl.plcpc.ParallelLeastCostPathCalculator;
import sun.plugin2.message.GetAppletMessage;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class LPFeedbackLIPDispatcher extends PartitionedDispatcher {
    public final int rebalancingPeriod;
    final AbstractVirtualNodeDest virtualNodeDest;
    final AbstractRequestSelector requestSelector;
    final AbstractVehicleDestMatcher vehicleDestMatcher;
    final Map<VirtualLink, Double> travelTimes;
    final AbstractVehicleRequestMatcher vehicleRequestMatcher;
    private int total_matchedRequests = 0;
    final int numvNodes;
    final int numvLinks;
    // setup linear program
    glp_prob lp;
    glp_smcp parm;
    SWIGTYPE_p_int ind;
    SWIGTYPE_p_double val;
    int ret;
    Map<Integer, String> numij_vLinkMap = new LinkedHashMap<>(); // LinkedHashMap chosen as the ordering should remain the same for iterating several times over the keyset.
    Map<Integer, VirtualNode> num_vNodeMap = new LinkedHashMap<>();
    final int numberOfAVs;
    Map<VirtualNode, List<VirtualLink>> vLinkSameFromVNode = virtualNetwork.getVirtualLinks()
            .stream().collect(Collectors.groupingBy(VirtualLink::getFrom));


    public LPFeedbackLIPDispatcher( //
                                    AVDispatcherConfig config, //
                                    AVGeneratorConfig generatorConfig, //
                                    TravelTime travelTime, //
                                    ParallelLeastCostPathCalculator router, //
                                    EventsManager eventsManager, //
                                    VirtualNetwork virtualNetwork, //
                                    AbstractVirtualNodeDest abstractVirtualNodeDest, //
                                    AbstractRequestSelector abstractRequestSelector, //
                                    AbstractVehicleDestMatcher abstractVehicleDestMatcher, //
                                    Map<VirtualLink, Double> travelTimesIn
    ) {
        super(config, travelTime, router, eventsManager, virtualNetwork);
        this.virtualNodeDest = abstractVirtualNodeDest;
        this.requestSelector = abstractRequestSelector;
        this.vehicleDestMatcher = abstractVehicleDestMatcher;
        travelTimes = travelTimesIn;
        vehicleRequestMatcher = new InOrderOfArrivalMatcher(this::setAcceptRequest);
        this.initiateLP();
        numberOfAVs = (int) generatorConfig.getNumberOfVehicles();
        rebalancingPeriod = Integer.parseInt(config.getParams().get("rebalancingPeriod"));
        numvNodes = virtualNetwork.getVirtualNodes().size();
        numvLinks = virtualNetwork.getVirtualLinks().size();
    }


    @Override
    public void redispatch(double now) {
        // Part I: outside rebalancing periods, permanently assign vehicles to requests if they have arrived at a customer i.e. stay on the same link
        total_matchedRequests += vehicleRequestMatcher.match(getStayVehicles(), getAVRequestsAtLinks());
        //Map<VirtualNode, List<VehicleLinkPair>> availableVehicles = getVirtualNodeAvailableNotRebalancingVehicles();


        // PART II: redispatch all vehicles
        final long round_now = Math.round(now);
        if (round_now % rebalancingPeriod == 0) {
            System.out.println(getClass().getSimpleName() + " @" + round_now + " mr = " + total_matchedRequests);
            Map<VirtualNode, List<AVRequest>> requests = getVirtualNodeRequests();
            // II.i compute rebalancing vehicles and send to virtualNodes
            {
                Map<VirtualNode, List<VehicleLinkPair>> availableVehicles = getVirtualNodeAvailableNotRebalancingVehicles();

                // calculate desired vehicles per vNode
                int num_requests = requests.values().stream().mapToInt(List::size).sum();
                int vi_desired_num = (int) ((numberOfAVs - num_requests) / (double) virtualNetwork.getVirtualNodes().size());
                GlobalAssert.that(vi_desired_num * virtualNetwork.getVirtualNodes().size() <= numberOfAVs);
                Tensor vi_desiredT = Tensors.vector(i -> RationalScalar.of(vi_desired_num, 1), numvNodes);


                // calculate excess vehicles per virtual Node i, where v_i excess = vi_own - c_i = v_i + sum_j (v_ji) - c_i
                Map<VirtualNode, Set<AVVehicle>> v_ij_reb = getVirtualNodeRebalancingToVehicles();
                Map<VirtualNode, Set<AVVehicle>> v_ij_cust = getVirtualNodeArrivingWCustomerVehicles();
                Tensor vi_excessT = Array.zeros(numvNodes);
                for (VirtualNode virtualNode : availableVehicles.keySet()) {
                    int viExcessVal = availableVehicles.get(virtualNode).size()
                            + v_ij_reb.get(virtualNode).size()
                            + v_ij_cust.get(virtualNode).size()
                            - requests.get(virtualNode).size();
                    vi_excessT.set(RationalScalar.of(viExcessVal, 1), virtualNode.index);
                }


                // solve the linear program with updated right-hand side
                Tensor rebalanceCount = solveUpdatedLP(vi_excessT, vi_desiredT);
                // TODO this should never become active, can be removed later (nonnegative solution)
                for (int i = 0; i < numvNodes; ++i) {
                    for (int j = 0; j < numvNodes; ++j) {
                        Tensor rebRow = rebalanceCount.get(i);
                        RealScalar value = (RealScalar) rebRow.Get(j);
                        double entry = value.getAbsDouble();
                        GlobalAssert.that(entry >= 0);
                    }
                }


                // ensure that not more vehicles are sent away than available
                Tensor feasibleRebalanceCount = returnFeasibleRebalance(rebalanceCount, availableVehicles);

                // generate routing instructions for rebalancing vehicles
                Map<VirtualNode, List<Link>> destinationLinks = new HashMap<>();
                for (VirtualNode virtualNode : virtualNetwork.getVirtualNodes())
                    destinationLinks.put(virtualNode, new ArrayList<>());

                // fill rebalancing destinations
                for (int i = 0; i < numvLinks; ++i) {
                    VirtualLink virtualLink = this.virtualNetwork.getVirtualLink(i);
                    VirtualNode toNode = virtualLink.getTo();
                    VirtualNode fromNode = virtualLink.getFrom();
                    int numreb = (int) ((RealScalar) feasibleRebalanceCount.get(fromNode.index).Get(toNode.index)).getAbsDouble();
                    List<Link> rebalanceTargets = virtualNodeDest.selectLinkSet(toNode, numreb);
                    destinationLinks.get(fromNode).addAll(rebalanceTargets);
                }


                // consistency check: rebalancing destination links must not exceed available vehicles in virtual node
                Map<VirtualNode, List<VehicleLinkPair>> finalAvailableVehicles = availableVehicles;
                GlobalAssert.that(!virtualNetwork.getVirtualNodes().stream()
                        .filter(v -> finalAvailableVehicles.get(v).size() < destinationLinks.get(v).size())
                        .findAny().isPresent());


                // send rebalancing vehicles using the setVehicleRebalance command
                for (VirtualNode virtualNode : destinationLinks.keySet()) {
                    Map<VehicleLinkPair, Link> rebalanceMatching = vehicleDestMatcher.match(availableVehicles.get(virtualNode), destinationLinks.get(virtualNode));
                    rebalanceMatching.keySet().forEach(v -> setVehicleRebalance(v, rebalanceMatching.get(v)));
                }

            }

            // II.ii if vehilces remain in vNode, send to customers
            {
                // collect destinations per vNode
                Map<VirtualNode, List<Link>> destinationLinks = new HashMap<>();
                virtualNetwork.getVirtualNodes().stream().forEach(v -> destinationLinks.put(v, new ArrayList<>()));
                for (VirtualNode vNode : virtualNetwork.getVirtualNodes()) {
                    destinationLinks.get(vNode).addAll( // stores from links
                            requests.get(vNode).stream().map(AVRequest::getFromLink).collect(Collectors.toList()));
                }

                // collect available vehicles per vNode
                Map<VirtualNode, List<VehicleLinkPair>> availableVehicles = getVirtualNodeAvailableNotRebalancingVehicles();


                // assign destinations to the available vehicles
                {
                    GlobalAssert.that(availableVehicles.keySet().containsAll(virtualNetwork.getVirtualNodes()));
                    GlobalAssert.that(destinationLinks.keySet().containsAll(virtualNetwork.getVirtualNodes()));

                    long tic = System.nanoTime(); // DO NOT PUT PARALLEL anywhere in this loop !
                    for (VirtualNode virtualNode : virtualNetwork.getVirtualNodes())
                        vehicleDestMatcher //
                                .match(availableVehicles.get(virtualNode), destinationLinks.get(virtualNode)) //
                                .entrySet().stream().forEach(this::setVehicleDiversion);
                    long dur = System.nanoTime() - tic;
                }
            }
        }
    }


    /**
     * @param rebalanceInput    entry i,j contains the rebalance input from virtualNode i to virtualNode j
     * @param availableVehicles the available vehicles per virtualNode
     * @return
     */
    private Tensor returnFeasibleRebalance(Tensor rebalanceInput, Map<VirtualNode, List<VehicleLinkPair>> availableVehicles) {
        Tensor feasibleRebalance = rebalanceInput.copy();

        for (int i = 0; i < numvNodes; ++i) {
            // count number of outgoing vehicles per vNode
            double outgoingNmrvNode = 0.0;
            Tensor outgoingVehicles = rebalanceInput.get(i);
            for (int j = 0; j < numvNodes; ++j) {
                outgoingNmrvNode = outgoingNmrvNode + ((RealScalar) outgoingVehicles.Get(j)).getAbsDouble();
            }
            int outgoingVeh = (int) outgoingNmrvNode;
            int finalI = i;
            int availableVehvNode = availableVehicles.get(availableVehicles.keySet().stream().filter(v -> v.index == finalI).findAny().get()).size();
            // if number of outoing vehicles too small, reduce proportionally
            if (availableVehvNode < outgoingVeh) {
                long shrinkingFactor = ((long) availableVehvNode / ((long) outgoingVeh));
                Tensor newRow = Floor.of(rebalanceInput.get(i).multiply(RealScalar.of(shrinkingFactor))).copy();
                for (int j = 0; j < numvNodes; ++j) {
                    feasibleRebalance.get(i).set(newRow.Get(j), j);
                }
            }
        }
        return feasibleRebalance;
    }


    private Tensor solveUpdatedLP(Tensor vi_excessT, Tensor vi_desiredT) {

        // fill right-hand-side
        for (int i = 0; i < vi_excessT.length(); ++i) {
            double v1desired = vi_desiredT.Get(i).getAbsDouble();
            double v1excess = vi_excessT.Get(i).getAbsDouble();
            double rhs = v1desired - v1excess;
            GLPK.glp_set_row_bnds(lp, i + 1, GLPKConstants.GLP_LO, rhs, 0.0);
        }


        // Solve model
        GLPK.glp_write_lp(lp, null, "networklinearprogram_updated.lp");
        parm = new glp_smcp();
        GLPK.glp_init_smcp(parm);
        ret = GLPK.glp_simplex(lp, parm);

        // Retrieve solution
        // TODO check if ret == 0 functions properly or not
        if (ret == 0) {
            System.out.println("successfully solved LP");
            //write_lp_solution(lp);
        } else {
            System.out.println("The problem could not be solved");
        }


        // fill result vector
        Tensor rebalanceOrder = Tensors.matrix((j, i) -> RealScalar.of((int) GLPK.glp_get_col_prim(lp, (j + 1) + (i) * numvNodes)), numvNodes, numvNodes);


        // if exists primal feasible solution, return it, otherwise return empty set.
        // TODO check if ret == 0 functions properly or not
        if (ret == 0) {
            return rebalanceOrder;
        } else {
            return Array.zeros(numvLinks);
        }


    }


    private void initiateLP() {
        // assign a numbering to the virtual nodes
        // TODO Solve this using the indexes in the VirtualLink instead of map
        int nodeNum = 0;
        for (VirtualNode virtualNode : virtualNetwork.getVirtualNodes()) {
            nodeNum = nodeNum + 1;
            num_vNodeMap.put(nodeNum, virtualNode);
        }

        // assign a numbering to the virtual links
        int varNum = 0;
        for (int n1 : num_vNodeMap.keySet()) {
            for (int n2 : num_vNodeMap.keySet()) {
                varNum = varNum + 1;
                VirtualNode fromNode = num_vNodeMap.get(n2);
                VirtualNode toNode = num_vNodeMap.get(n1);
                boolean foundVLink = false;
                for (VirtualLink virtualLink : virtualNetwork.getVirtualLinks()) {
                    if (virtualLink.getFrom().equals(fromNode) && virtualLink.getTo().equals(toNode)) {
                        numij_vLinkMap.put(varNum, virtualLink.getId().toString());
                        foundVLink = true;
                        break;
                    }
                }
                if (!foundVLink) {
                    numij_vLinkMap.put(varNum, "noVLink");
                }
            }
        }


        Map<VirtualLink, Integer> rebalanceOrder = new HashMap<>();
        try {
            // Create problem for n stations
            int n = this.virtualNetwork.getVirtualNodes().size();


            lp = GLPK.glp_create_prob();
            System.out.println("Problem created");
            GLPK.glp_set_prob_name(lp, "Rebalancing Problem");

            // Define columns (i.e. variables) including non-negativity constraints on variables
            // GLP_CV continuous variable;
            // GLP_IV integer variable;
            // GLP_BV binary variable
            // GLP_FR −1 < x < +1 Free (unbounded) variable
            // GLP_LO lb · x < +1 Variable with lower bound
            // GLP_UP −1 < x · ub Variable with upper bound
            // GLP_DB lb · x · ub Double-bounded variable
            // GLP_FX lb = x = ub Fixed variable
            GLPK.glp_add_cols(lp, n * n);
            varNum = 0;
            for (int i = 0; i < n * n; ++i) {
                String varname = ("num_" + ((i % n) + 1) + (i / n + 1));
                varNum = varNum + 1;
                if ((i % n == i / n) || (numij_vLinkMap.get(varNum).equals("noVLink"))) {   // For self-links and for non-existent links, make num_ii fixed variable (no self-rebalancing)
                    GLPK.glp_set_col_name(lp, i + 1, varname);
                    GLPK.glp_set_col_kind(lp, i + 1, GLPKConstants.GLP_CV);
                    GLPK.glp_set_col_bnds(lp, i + 1, GLPKConstants.GLP_FX, 0, 0);
                } else {
                    GLPK.glp_set_col_name(lp, i + 1, varname);
                    GLPK.glp_set_col_kind(lp, i + 1, GLPKConstants.GLP_CV);
                    GLPK.glp_set_col_bnds(lp, i + 1, GLPKConstants.GLP_LO, 0.0, 0.0); // Lower bound: second number irrelevant
                }
            }

            // Create constraint matrix
            // Create rows
            GLPK.glp_add_rows(lp, n);

            // Set row details
            int eq = 0;
            for (VirtualNode virtualNode : virtualNetwork.getVirtualNodes()) {
                eq = eq + 1;
                // Allocate memory
                ind = GLPK.new_intArray(n * n);
                val = GLPK.new_doubleArray(n * n);
                String constr_name = ("c_" + (eq));
                GLPK.glp_set_row_name(lp, eq, constr_name);
                GLPK.glp_set_row_bnds(lp, eq, GLPKConstants.GLP_LO, -1.0, 0.0);

                for (int var = 1; var <= n * n; ++var) {
                    GLPK.intArray_setitem(ind, var, var);
                    if (((eq - 1) * n + 1) <= var && var <= (eq * n) && (((var - 1) % n) + 1) != ((var - 1) / n + 1)) {
                        GLPK.doubleArray_setitem(val, var, 1.0);
                    }

                    if (((var % n == eq) || (var % n + n == eq)) && (((var - 1) % n) + 1) != ((var - 1) / n + 1)) {
                        GLPK.doubleArray_setitem(val, var, -1.0);
                    }


                    GLPK.glp_set_mat_row(lp, eq, var, ind, val);
                }

                // F
                // ree memory
                GLPK.delete_intArray(ind);
                GLPK.delete_doubleArray(val);
            }


            // Define objective
            GLPK.glp_set_obj_name(lp, "z");
            GLPK.glp_set_obj_dir(lp, GLPKConstants.GLP_MIN);
            for (int var = 1; var <= n * n; ++var) {
                String vLinkID = numij_vLinkMap.get(var);
                if (!vLinkID.equals("noVLink")) {
                    VirtualLink vLink = virtualNetwork.getVirtualLinks().stream().filter(v -> v.getId().toString().equals(vLinkID)).findFirst().get();
                    GLPK.glp_set_obj_coef(lp, var, travelTimes.get(vLink));
                } else {
                    GLPK.glp_set_obj_coef(lp, var, 0.0);
                }

            }

            // Write model to file
            GLPK.glp_write_lp(lp, null, "networklinearprogram_initial.lp");
        } catch (GlpkException ex) {
            ex.printStackTrace();
            ret = 1;
        }
    }


    // TODO assure no memory leaks happen if MATSim done for multiple iterations
    @Override
    public void finalize() {
        // release storage allocated for LP
        GLPK.glp_delete_prob(lp);
        System.out.println("Book instance is getting destroyed");
    }


    /**
     * write simplex solution
     *
     * @param lp problem
     */
    static void write_lp_solution(glp_prob lp) {
        int i;
        int n;
        String name;
        double val;

        name = GLPK.glp_get_obj_name(lp);
        val = GLPK.glp_get_obj_val(lp);
        System.out.print(name);
        System.out.print(" = ");
        System.out.println(val);
        n = GLPK.glp_get_num_cols(lp);
        for (i = 1; i <= n; i++) {
            name = GLPK.glp_get_col_name(lp, i);
            val = GLPK.glp_get_col_prim(lp, i);
            System.out.print(name);
            System.out.print(" = ");
            System.out.println(val);
        }
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

        public static VirtualNetwork virtualNetwork;
        public static Map<VirtualLink, Double> travelTimes;

        @Override
        public AVDispatcher createDispatcher(AVDispatcherConfig config, AVGeneratorConfig generatorConfig) {

            AbstractVirtualNodeDest abstractVirtualNodeDest = new KMeansVirtualNodeDest();
            AbstractRequestSelector abstractRequestSelector = new OldestRequestSelector();
            AbstractVehicleDestMatcher abstractVehicleDestMatcher = new HungarBiPartVehicleDestMatcher();

            File virtualnetworkXML = new File(config.getParams().get("virtualNetworkFile"));
            System.out.println("" + virtualnetworkXML.getAbsoluteFile());
            virtualNetwork = VirtualNetworkLoader.fromXML(network, virtualnetworkXML);
            travelTimes = vLinkDataReader.fillvLinkData(virtualnetworkXML, virtualNetwork, "Ttime");

            return new LPFeedbackLIPDispatcher(
                    config,
                    generatorConfig,
                    travelTime,
                    router,
                    eventsManager,
                    virtualNetwork,
                    abstractVirtualNodeDest,
                    abstractRequestSelector,
                    abstractVehicleDestMatcher,
                    travelTimes
            );
        }
    }


}




