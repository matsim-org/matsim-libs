package playground.clruch.dispatcher.utils;

import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import ch.ethz.idsc.tensor.alg.Array;
import org.gnu.glpk.*;
import playground.clruch.netdata.VirtualLink;
import playground.clruch.netdata.VirtualNetwork;
import playground.clruch.netdata.VirtualNode;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;


/**
 * Created by Claudio on 3/17/2017.
 */
public class LPVehicleRebalancing {
    VirtualNetwork virtualNetwork;
    // TODO generalize this to "objective weights"
    Map<VirtualLink, Double> travelTimes;
    glp_prob lp;
    final int n;


    /**
     * @param virtualNetworkIn the virtual network (complete directed graph) on which the optimization is computed.
     * @param travelTimesIn    the travelTimes which are used as link weights.
     */
    public LPVehicleRebalancing(VirtualNetwork virtualNetworkIn,
                                Map<VirtualLink, Double> travelTimesIn) {
        virtualNetwork = virtualNetworkIn;
        travelTimes = travelTimesIn;
        initiateLP();
        n = virtualNetwork.getvNodesCount();
    }

    /**
     * initiate the linear program
     */
    public void initiateLP() {

        // map matching links to variable IDs in the LP
        Map<Integer, String> numij_vLinkMap = new LinkedHashMap<>();


        // assign a numbering to the virtual links, i.e. variables 1,2,...,numvNodes*numvNodes
        int varNum = 0;
        for (int n1 = 1; n1 < n + 1; ++n1) {
            for (int n2 = 1; n2 < n + 1; ++n2) {
                varNum = varNum + 1;
                VirtualNode fromNode = virtualNetwork.getVirtualNode(n2 - 1);
                VirtualNode toNode = virtualNetwork.getVirtualNode(n1 - 1);

                // find virtualLink associated to vNode pair fromNode -> toNode
                Optional<VirtualLink> optional = virtualNetwork.getVirtualLinks().stream()
                        .filter(v -> v.getFrom().equals(fromNode) && v.getTo().equals(toNode))
                        .findFirst();

                if (optional.isPresent()) {
                    numij_vLinkMap.put(varNum, optional.get().getId().toString());
                } else {
                    numij_vLinkMap.put(varNum, "noVLink");
                }
            }
        }


        try {



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
                if ((i % n == i / n) || (numij_vLinkMap.get(varNum).equals("noVLink"))) {
                    // For self-links and for non-existent links, make num_ii fixed variable (no self-rebalancing)
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


            SWIGTYPE_p_int ind;
            SWIGTYPE_p_double val;


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
        } catch (
                GlpkException ex)

        {
            ex.printStackTrace();
        }

    }

    /**
     * closing the LP in order to release allocated memory
     */
    public void closeLP() {
        // release storage allocated for LP
        GLPK.glp_delete_prob(lp);
        System.out.println("Book instance is getting destroyed");
    }

    /**
     * solving the LP with updated right-hand-sides
     *
     * @param vi_excessT  excess vehicles per virtual station
     * @param vi_desiredT desired vehicles per virtual station
     * @return
     */
    public Tensor solveUpdatedLP(Tensor vi_excessT, Tensor vi_desiredT) {

        // fill right-hand-side
        Tensor rhs = vi_desiredT.subtract(vi_excessT);
        for (int i = 0; i < vi_excessT.length(); ++i) {
            GLPK.glp_set_row_bnds(lp, i + 1, GLPKConstants.GLP_LO, ((RealScalar) (rhs.Get(i))).getRealDouble(), 0.0);
        }


        // Solve model
        GLPK.glp_write_lp(lp, null, "networklinearprogram_updated.lp");
        glp_smcp parm = new glp_smcp();
        GLPK.glp_init_smcp(parm);
        int ret = GLPK.glp_simplex(lp, parm);

        // Retrieve solution
        // TODO check if ret == 0 functions properly or not
        if (ret == 0) {
            System.out.println("successfully solved LP");
            //write_lp_solution(lp);
        } else {
            System.out.println("The problem could not be solved");
        }


        // fill result vector
        Tensor rebalanceOrder = Tensors.matrix((j, i) -> RealScalar.of((int) GLPK.glp_get_col_prim(lp, (j + 1) + (i) * n)), n, n);


        // if exists primal feasible solution, return it, otherwise return empty set.
        // TODO check if ret == 0 functions properly or not
        if (ret == 0) {
            return rebalanceOrder;
        } else {
            return Array.zeros(virtualNetwork.getvLinksCount());
        }


    }

    /**
     * writes the solution of the LP on the console
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

}
