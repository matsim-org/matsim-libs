/**
 * Dispatcher implementing the linear program from
 * Pavone, Marco, Stephen Smith, Emilio Frazzoli, and Daniela Rus. 2011.
 * “Load Balancing for Mobility-on-Demand Systems.” In Robotics: Science and Systems VII. doi:10.15607/rss.2011.vii.034.
 * <p>
 * <p>
 * Implemented by Claudio Ruch on 2017, 02, 25
 */


package playground.clruch.dispatcher;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import jdk.nashorn.internal.objects.Global;
import org.gnu.glpk.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.router.util.TravelTime;
import org.matsim.utils.objectattributes.attributeconverters.FloatConverter;
import playground.clruch.dispatcher.core.VehicleLinkPair;
import playground.clruch.dispatcher.utils.*;
import playground.clruch.netdata.VirtualLink;
import playground.clruch.netdata.VirtualNetwork;
import playground.clruch.netdata.VirtualNode;
import playground.clruch.utils.GlobalAssert;
import playground.sebhoerl.avtaxi.config.AVDispatcherConfig;
import playground.sebhoerl.avtaxi.config.AVGeneratorConfig;
import playground.sebhoerl.avtaxi.data.AVVehicle;
import playground.sebhoerl.avtaxi.dispatcher.AVDispatcher;
import playground.sebhoerl.avtaxi.framework.AVModule;
import playground.sebhoerl.avtaxi.framework.AVUtils;
import playground.sebhoerl.avtaxi.generator.AVGenerator;
import playground.sebhoerl.avtaxi.passenger.AVRequest;
import playground.sebhoerl.plcpc.ParallelLeastCostPathCalculator;

import java.util.*;
import java.util.stream.Collectors;

public class LPFeedbackLIPDispatcher extends PartitionedDispatcher {
    public final int REBALANCING_PERIOD;
    final AbstractVirtualNodeDest virtualNodeDest;
    final AbstractRequestSelector requestSelector;
    final AbstractVehicleDestMatcher vehicleDestMatcher;
    final Map<VirtualLink, Double> travelTimes;
    Map<VirtualLink, Double> rebalanceFloating;
    final AbstractVehicleRequestMatcher vehicleRequestMatcher;
    private int total_matchedRequests = 0;
    // setup linear program
    glp_prob lp;
    glp_smcp parm;
    SWIGTYPE_p_int ind;
    SWIGTYPE_p_double val;
    int ret;
    Map<Integer, String> numij_vLinkMap = new LinkedHashMap<>(); // LinkedHashMap chosen as the ordering should remain the same for iterating several times over the keyset.
    Map<Integer, VirtualNode> num_vNodeMap = new LinkedHashMap<>();
    final int numberOfAVs;


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
        rebalanceFloating = new HashMap<>();
        for (VirtualLink virtualLink : virtualNetwork.getVirtualLinks()) {
            rebalanceFloating.put(virtualLink, 0.0);
        }
        travelTimes = travelTimesIn;
        vehicleRequestMatcher = new InOrderOfArrivalMatcher(this::setAcceptRequest);
        this.initiateLP();
        numberOfAVs = (int) generatorConfig.getNumberOfVehicles();
        REBALANCING_PERIOD = Integer.parseInt(config.getParams().get("rebalancingPeriod"));
    }


    @Override
    public void redispatch(double now) {
        // A: outside rebalancing periods, permanently assign vehicles to requests if they have arrived at a customer
        //    i.e. stay on the same link
        total_matchedRequests += vehicleRequestMatcher.match(getStayVehicles(), getAVRequestsAtLinks());
        //Map<VirtualNode, List<VehicleLinkPair>> availableVehicles = getVirtualNodeAvailableNotRebalancingVehicles();


        // B: redispatch all vehicles
        final long round_now = Math.round(now);
        if (round_now % REBALANCING_PERIOD == 0) {
            Map<VirtualNode, List<AVRequest>> requests = getVirtualNodeRequests();
            {
                Map<VirtualNode, List<VehicleLinkPair>> availableVehicles = getVirtualNodeAvailableNotRebalancingVehicles();

                System.out.println("LPFeedbackLIPDispatcher @" + round_now + " mr = " + total_matchedRequests);

                // 0) count vi_excess vehicles and vi_desired vehicles
                // TODO (write as stream())
                int num_requests = 0;
                for (List<AVRequest> avRequests : requests.values()) {
                    num_requests = num_requests + avRequests.size();
                }

                int vi_desired_num = (int) ((numberOfAVs - num_requests) / (double) virtualNetwork.getVirtualNodes().size());
                GlobalAssert.that(vi_desired_num*virtualNetwork.getVirtualNodes().size() <= numberOfAVs);
                Map<VirtualNode, Integer> vi_desired = new HashMap<>();
                for (VirtualNode virtualNode : virtualNetwork.getVirtualNodes()) {
                    vi_desired.put(virtualNode, vi_desired_num);
                }


                // Calculate the excess vehicles per virtual Node i, where v_i excess = vi_own - c_i = v_i + sum_j (v_ji) - c_i
                // TODO check if sum_j (v_ji) also contains the customer vehicles travelling to v_i and add if so.
                Map<VirtualNode, Integer> vi_excess = new HashMap<>();
                Map<VirtualNode, Set<AVVehicle>> v_ij_reb = getVirtualNodeRebalancingToVehicles();
                Map<VirtualNode, Set<AVVehicle>> v_ij_cust = getVirtualNodeArrivingWCustomerVehicles();
                for (VirtualNode virtualNode : availableVehicles.keySet()) {
                    if (v_ij_cust.get(virtualNode).size() > 0) {
                        System.out.println("Customer is travelling");
                    }
                    vi_excess.put(virtualNode, availableVehicles.get(virtualNode).size()
                            + v_ij_reb.get(virtualNode).size()
                            + v_ij_cust.get(virtualNode).size()
                            - requests.get(virtualNode).size());
                }

                // 1) solve the linear program with updated right-hand side
                Map<VirtualLink, Integer> rebalanceCount = solveUpdatedLP(vi_excess, vi_desired);

                // DEBUGGING STARTED
                long number_reb_commands = rebalanceCount.values().stream().mapToInt(Number::intValue).sum();
                if(number_reb_commands>0){
                    System.out.println("Sending rebalancing vehicles");
                }
                // DEBUGGING ENDED

                // 2) rebalance the vehicles according to the LP
                // create a Map that contains all the outgoing vLinks for a vNode
                Map<VirtualNode, List<VirtualLink>> vLinkShareFromvNode = virtualNetwork.getVirtualLinks().stream().collect(Collectors.groupingBy(VirtualLink::getFrom));

                // ensure that not more vehicles are sent away than available
                for (VirtualNode virtualNode : virtualNetwork.getVirtualNodes()) {

                    // count outgoing vehicles from this node:
                    int totRebVehicles = 0;
                    for (VirtualLink vLink : vLinkShareFromvNode.get(virtualNode)) {
                        totRebVehicles = totRebVehicles + rebalanceCount.get(vLink);
                    }

                    // TODO CHECK, REWRITE AND EXTERNALIZE THIS METHOD
                    // not enough available vehicles
                    if (availableVehicles.get(virtualNode).size() < totRebVehicles) {
                        // calculate by how much to shrink
                        double shrinkingFactor = ((double) availableVehicles.get(virtualNode).size()) / ((double) totRebVehicles);
                        // remove rebalancing vehicles
                        for (VirtualLink virtualLink : vLinkShareFromvNode.get(virtualNode)) {
                            if (rebalanceCount.get(virtualLink) > 0) {
                                double newRebCountTot = rebalanceCount.get(virtualLink) * shrinkingFactor;
                                int newIntRebCount = (int) Math.floor(newRebCountTot);
                                int newLeftOver = rebalanceCount.get(virtualLink) - newIntRebCount;

                                rebalanceCount.put(virtualLink, newIntRebCount);
                                rebalanceFloating.put(virtualLink, rebalanceFloating.get(virtualLink) + newLeftOver);
                                VirtualLink oppositeLink = virtualNetwork.getVirtualLinks().stream().filter(v -> (v.getFrom().equals(virtualLink.getTo()) && v.getTo().equals(virtualLink.getFrom()))).findFirst().get();
                                rebalanceCount.put(oppositeLink, 0);
                                rebalanceFloating.put(oppositeLink, rebalanceFloating.get(oppositeLink) - newLeftOver);
                            }
                        }
                    }
                }

                // 2 generate routing instructions for vehicles
                // 2.1 gather the destination links
                Map<VirtualNode, List<Link>> destinationLinks = new HashMap<>();
                for (VirtualNode virtualNode : virtualNetwork.getVirtualNodes())
                    destinationLinks.put(virtualNode, new ArrayList<>());

                // 2.2 fill rebalancing destinations
                // TODO size negative?
                for (Map.Entry<VirtualLink, Integer> entry : rebalanceCount.entrySet()) {
                    final VirtualLink virtualLink = entry.getKey();
                    final int size = entry.getValue();
                    final VirtualNode fromNode = virtualLink.getFrom();
                    // Link origin = fromNode.getLinks().iterator().next(); //
                    VirtualNode toNode = virtualLink.getTo();

                    List<Link> rebalanceTargets = virtualNodeDest.selectLinkSet(toNode, size);

                    destinationLinks.get(fromNode).addAll(rebalanceTargets);
                }

                // 2.3 consistency check: rebalancing destination links must not exceed available vehicles in virtual node
                for (VirtualNode virtualNode : virtualNetwork.getVirtualNodes()) {
                    int sizeV = availableVehicles.get(virtualNode).size();
                    int sizeL = destinationLinks.get(virtualNode).size();
                    if (sizeL > sizeV)
                        throw new RuntimeException("rebalancing inconsistent " + sizeL + " > " + sizeV);
                }

                // 2.4 send rebalancing vehicles using the setVehicleRebalance command
                for (VirtualNode virtualNode : destinationLinks.keySet()) {
                    Map<VehicleLinkPair, Link> rebalanceMatching = vehicleDestMatcher.match(availableVehicles.get(virtualNode), destinationLinks.get(virtualNode));
                    for (VehicleLinkPair vehicleLinkPair : rebalanceMatching.keySet()) {
                        // DEBUGGING STARTED
                        /*
                        for(Set<AVVehicle> avset : rebvehiclesdebug.values()){
                            if(avset.contains(vehicleLinkPair.avVehicle)){
                                System.out.println("trying to add rebalancing vehicle");
                            }


                        }
                        */
                        // DEBUGGING ENDED
                        setVehicleRebalance(vehicleLinkPair, rebalanceMatching.get(vehicleLinkPair));
                    }
                }

                // DEBUGGING STARTED
                availableVehicles = getVirtualNodeAvailableNotRebalancingVehicles();
                System.out.println("Stuff");
                // DEBUGGING ENDED
            }

            {
                Map<VirtualNode, List<Link>> destinationLinks = new HashMap<>();
                for (VirtualNode virtualNode : virtualNetwork.getVirtualNodes())
                    destinationLinks.put(virtualNode, new ArrayList<>());

                Map<VirtualNode, List<VehicleLinkPair>> availableVehicles = getVirtualNodeAvailableNotRebalancingVehicles();
                // fill request destinations
                for (VirtualNode virtualNode : virtualNetwork.getVirtualNodes()) {
                    // number of vehicles that can be matched to requests
                    int size = Math.min( //
                            availableVehicles.get(virtualNode).size() - destinationLinks.get(virtualNode).size(), //
                            requests.get(virtualNode).size());

                    Collection<AVRequest> collection = requestSelector.selectRequests( //
                            availableVehicles.get(virtualNode), //
                            requests.get(virtualNode), //
                            size);

                    // TODO

                    destinationLinks.get(virtualNode).addAll( // stores from links
                            collection.stream().map(AVRequest::getFromLink).collect(Collectors.toList()));
                }


                // 2.4 assign destinations to the available vehicles
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

                // 2.5 define action for leftover vehicles
                // no action taken here, possible to send leftover vehicles to new destination in
                // virtual node where they currently stay.

            }
        }
    }


    private Map<VirtualLink, Integer> solveUpdatedLP(Map<VirtualNode, Integer> vi_excess, Map<VirtualNode, Integer> vi_desired) {
        int n = this.virtualNetwork.getVirtualNodes().size();
        Map<VirtualLink, Integer> rebalanceOrder = new HashMap<>();

        // update LP // TODO make this more elegant
        int eq = 0;
        for (VirtualNode virtualNode : vi_excess.keySet()) {
            for (int key : num_vNodeMap.keySet()) {
                if (num_vNodeMap.get(key).equals(virtualNode)) {
                    eq = key;
                    GLPK.glp_set_row_bnds(lp, eq, GLPKConstants.GLP_LO, (double) (vi_desired.get(virtualNode) - vi_excess.get(virtualNode)), 0.0);
                }
            }
        }

        // Solve model
        GLPK.glp_write_lp(lp, null, "networklinearprogram_updated.lp");
        parm = new glp_smcp();
        GLPK.glp_init_smcp(parm);
        ret = GLPK.glp_simplex(lp, parm);

        // Retrieve solution
        if (ret == 0) {
            write_lp_solution(lp);
        } else {
            System.out.println("The problem could not be solved");
        }

        // fill result vector
        for (int var = 1; var <= n * n; ++var) {
            String vLinkID = numij_vLinkMap.get(var);
            if (!vLinkID.equals("noVLink")) {
                VirtualLink vLink = virtualNetwork.getVirtualLinks().stream().filter(v -> v.getId().toString().equals(vLinkID)).findFirst().get();
                rebalanceOrder.put(vLink, (int) GLPK.glp_get_col_prim(lp, var));
            }
        }

        // if exists primal feasible solution, return it, otherwise return empty set.
        if (ret == 0) {
            return rebalanceOrder;
        } else {
            rebalanceOrder.keySet().stream().forEach(v -> rebalanceOrder.put(v, 0));
            return rebalanceOrder;
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


    // TODO find out if this needs to be eplicitely called or if it is taken care of by the JAVA garbage collector
    private void closeLP() {
        // Free memory
        GLPK.glp_delete_prob(lp);
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
            // TODO relate to general directory given in argument of main function


            // TODO:

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




