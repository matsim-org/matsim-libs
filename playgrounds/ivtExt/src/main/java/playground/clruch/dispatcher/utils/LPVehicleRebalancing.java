// code by clruch
package playground.clruch.dispatcher.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.gnu.glpk.GLPK;
import org.gnu.glpk.GLPKConstants;
import org.gnu.glpk.GlpkException;
import org.gnu.glpk.SWIGTYPE_p_double;
import org.gnu.glpk.SWIGTYPE_p_int;
import org.gnu.glpk.glp_prob;
import org.gnu.glpk.glp_smcp;
import org.matsim.api.core.v01.network.Link;

import ch.ethz.idsc.queuey.core.networks.VirtualLink;
import ch.ethz.idsc.queuey.core.networks.VirtualNetwork;
import ch.ethz.idsc.queuey.core.networks.VirtualNode;
import ch.ethz.idsc.queuey.util.GlobalAssert;
import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.Scalars;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import ch.ethz.idsc.tensor.red.Total;

/**
 * Created by Claudio on 3/17/2017. Updated by Claudio on 5/7/2017.
 */
public class LPVehicleRebalancing {
    VirtualNetwork<Link> virtualNetwork;
    glp_prob lp;
    glp_smcp parm = new glp_smcp();
    final int n;
    final int m;

    /**
     * @param virtualNetworkIn
     *            the virtual network (complete directed graph) on which the optimization is computed.
     */
    public LPVehicleRebalancing(VirtualNetwork virtualNetworkIn) {
        virtualNetwork = virtualNetworkIn;
        n = virtualNetwork.getvNodesCount();
        m = virtualNetwork.getvLinksCount();
        System.out.println("creating rebalancing LP for system with " + n + " virtualNodes and " + m + " virtualLinks");
        // TODO this criterion is only valid for complete graph....
        // how to deal with it from ScenarioPrep
        // GlobalAssert.that(m == n * n - n);
        initiateLP();
    }

    /**
     * initiate the linear program
     */
    public void initiateLP() {

        // map with variableIDs in problem set up and linkIDs of virtualNetwork
        Map<Integer, Integer> varIDLinkID = new HashMap<>();

        try {
            lp = GLPK.glp_create_prob();
            System.out.println("Problem created");
            GLPK.glp_set_prob_name(lp, "Rebalancing Problem");

            // 1) VARIABLES and constraints on variables (non-negativity, fixed to zero)
            {
                GLPK.glp_add_cols(lp, n * n);
                int variableId = 0;
                for (int i = 0; i < n; ++i) {
                    for (int j = 0; j < n; ++j) {
                        // variable name and initialization
                        variableId = variableId + 1;
                        String varname = ("reb_" + i + j);
                        GLPK.glp_set_col_name(lp, variableId, varname);
                        GLPK.glp_set_col_kind(lp, variableId, GLPKConstants.GLP_CV);

                        // for every variable find virtualLink
                        VirtualNode<Link> vNodeFrom = virtualNetwork.getVirtualNode(i);
                        VirtualNode<Link> vNodeTo = virtualNetwork.getVirtualNode(j);
                        Optional<VirtualLink<Link>> optVLink = virtualNetwork.getVirtualLinks().stream()
                                .filter(v -> (v.getFrom().equals(vNodeFrom) && v.getTo().equals(vNodeTo))).findFirst();
                        if(optVLink.isPresent()){ // if virtualLink is present it is a lower bounded optimization variable
                            GLPK.glp_set_col_bnds(lp, variableId, GLPKConstants.GLP_LO, 0.0, 0.0); // Lower bound: second number irrelevant
                            varIDLinkID.put(variableId, optVLink.get().getIndex());
                        }else{ // fixed variable to 0.0
                            GLPK.glp_set_col_bnds(lp, variableId, GLPKConstants.GLP_FX, 0, 0);
                            varIDLinkID.put(variableId, -1);
                        }
                    }
                }
            }

            // 2) CONSTRAINT MATRIX A (right-hand side b set to -1)
            GLPK.glp_add_rows(lp, n);
            SWIGTYPE_p_int ind;
            SWIGTYPE_p_double val;

            // Set row details
            // for every virtualNode, set balance equations
            for (int eq = 1; eq <= n; ++eq) {
                // Allocate memory NOTE: the first value in this array is not used as variables are counte 1,2,3,...,n*n
                ind = GLPK.new_intArray(n * n + 1);
                val = GLPK.new_doubleArray(n * n + 1);
                String constr_name = ("c_" + (eq - 1));
                GLPK.glp_set_row_name(lp, eq, constr_name);
                GLPK.glp_set_row_bnds(lp, eq, GLPKConstants.GLP_LO, -1.0, 0.0);


                // set the entries of the coefficient matrix A
                {
                    int variableId = 0;
                    for (int i = 0; i < n; ++i) {
                        for (int j = 0; j < n; ++j) {
                            variableId = variableId + 1;
                            if (i == (eq - 1) && i != j) {
                                GLPK.intArray_setitem(ind, variableId, variableId);
                                GLPK.doubleArray_setitem(val, variableId, -1.0);
                            } else if (j == (eq - 1) && i != j) {
                                GLPK.intArray_setitem(ind, variableId, variableId);
                                GLPK.doubleArray_setitem(val, variableId, 1.0);
                            } else {
                                GLPK.intArray_setitem(ind, variableId, variableId);
                                GLPK.doubleArray_setitem(val, variableId, 0.0);
                            }
                        }
                    }
                }
                // turn over the entries to GLPK
                for (int variableId = 1; variableId <= n * n; ++variableId) {
                    GLPK.glp_set_mat_row(lp, eq, variableId, ind, val);
                }

                // Free memory
                GLPK.delete_intArray(ind);
                GLPK.delete_doubleArray(val);
            }

            // 3) OBJECTIVE vector
            GLPK.glp_set_obj_name(lp, "z");
            GLPK.glp_set_obj_dir(lp, GLPKConstants.GLP_MIN);

            for (int variableId = 1; variableId <= n * n; ++variableId) {
                int linkIndex = varIDLinkID.get(variableId);
                if (linkIndex > -1) {
                    VirtualLink vLink = virtualNetwork.getVirtualLink(linkIndex);
                    GLPK.glp_set_obj_coef(lp, variableId, vLink.getDistance());
                } else {
                    GLPK.glp_set_obj_coef(lp, variableId, 0.0);
                }
            }

            // write model to file
            // GLPK.glp_write_lp(lp, null, "rebalancing_linearprogram_initial.lp");
        } catch (GlpkException ex){
            ex.printStackTrace();
        }
    }

    /**
     * closing the LP in order to release allocated memory
     */
    public void closeLP() {
        // release storage allocated for LP
        parm.delete();
        GLPK.glp_delete_prob(lp);
        System.out.println("LP instance is getting destroyed");
    }

    /**
     * solving the LP with updated right-hand-sides 
     * @param rhs for problem, i.e. b-vector, e.g. rhs = vi_desiredT - vi_excessT;
     * @parm GLPrhs, sign for right hand-side: GLP_LO -> Ax<=b, GLP_FX -> Ax==b
     * @return
     */
    public Tensor solveUpdatedLP(Tensor rhs, int GLPrhs) {

        // use rhs to set constraints
        GlobalAssert.that(rhs.length() == n);
        for (int i = 0; i < n; ++i) {
            GLPK.glp_set_row_bnds(lp, i + 1, GLPrhs, rhs.Get(i).number().doubleValue(), 0.0);
        }

        // Solve model
        // write model to file
        //GLPK.glp_write_lp(lp, null, "rebalancing_linearprogram_updated.lp");
        GLPK.glp_init_smcp(parm);
        int ret = GLPK.glp_simplex(lp, parm); // ret==0 indicates of the algorithm ran correctly
        GlobalAssert.that(ret==0);
        int stat = GLPK.glp_get_status(lp);
        
        boolean rhsLessEqualsZero = Scalars.lessEquals((Scalar) Total.of(rhs),RealScalar.ZERO );
        boolean lpFeasibleSolution = stat != GLPK.GLP_NOFEAS;
        
        if(!rhsLessEqualsZero || !lpFeasibleSolution){
            System.out.println("sum of right-hand-side less or equal than zero: " + rhsLessEqualsZero);
            System.out.println("LP has feasible solution: " + lpFeasibleSolution);
            GlobalAssert.that(false);
        }        
        //GlobalAssert.that(stat != GLPK.GLP_NOFEAS);


        // fill result vector, rebalance from i to j is equal to variable  (i*n)+ j +1
        Tensor rebalanceOrder = Tensors.matrix((i, j) -> RealScalar.of( GLPK.glp_get_col_prim(lp, (i*n)+ j +1)), n, n);
        return rebalanceOrder;

    }

    /**
     * writes the solution of the LP on the console
     *
     * @param lp
     *            problem
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
