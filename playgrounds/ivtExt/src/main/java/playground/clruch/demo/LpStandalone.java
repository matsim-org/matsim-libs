package playground.clruch.demo;

import org.gnu.glpk.GLPK;
import org.gnu.glpk.GLPKConstants;
import org.gnu.glpk.GlpkException;
import org.gnu.glpk.SWIGTYPE_p_double;
import org.gnu.glpk.SWIGTYPE_p_int;
import org.gnu.glpk.glp_prob;
import org.gnu.glpk.glp_smcp;

/**
 * THIS FILE IS A CONCISE DEMO OF FUNCTIONALITY
 * 
 * DO NOT MODIFY THIS FILE (unless you are the primary author),
 * BUT DO NOT RELY ON THIS FILE NOT BEING CHANGED
 * 
 * IF YOU WANT TO MAKE A SIMILAR CLASS OR REPLY ON THIS IMPLEMENTATION
 * THEN DUPLICATE THIS FILE AND MAKE THE CHANGES IN THE NEW FILE
 */
class LpStandalone {

    public static void main(String[] args) {
        glp_prob lp;
        glp_smcp parm;
        SWIGTYPE_p_int ind;
        SWIGTYPE_p_double val;
        int ret;

        try {
            // Create problem for n stations
            int n = 4;


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
            for (int i = 0; i < n * n; ++i) {
                String varname = ("num_" + ((i % n) + 1) + (i / n + 1));
                if (i % n == i / n) {
                    GLPK.glp_set_col_name(lp, i + 1, varname);
                    GLPK.glp_set_col_kind(lp, i + 1, GLPKConstants.GLP_CV);
                    GLPK.glp_set_col_bnds(lp, i + 1, GLPKConstants.GLP_FX, 0, 0); // Make num_ii fixed variable (no self-rebalancing)
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
            for (int eq = 1; eq <= n; ++eq) {
                // Allocate memory
                ind = GLPK.new_intArray(n * n);
                val = GLPK.new_doubleArray(n * n);
                String constr_name = ("c_" + (eq));
                GLPK.glp_set_row_name(lp, eq, constr_name);
                GLPK.glp_set_row_bnds(lp, eq, GLPKConstants.GLP_LO, 1.0, 0.0); // TODO change 15.0 to vi_excess-vi_d

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

                // Free memory
                GLPK.delete_intArray(ind);
                GLPK.delete_doubleArray(val);
            }
            GLPK.glp_set_row_bnds(lp, 1, GLPKConstants.GLP_LO, 46, 0.0); // TODO change 15.0 to vi_excess-vi_d
            GLPK.glp_set_row_bnds(lp, 2, GLPKConstants.GLP_LO, 15.0, 0.0); // TODO change 15.0 to vi_excess-vi_d
            GLPK.glp_set_row_bnds(lp, 3, GLPKConstants.GLP_LO, -11.0, 0.0); // TODO change 15.0 to vi_excess-vi_d
            GLPK.glp_set_row_bnds(lp, 4, GLPKConstants.GLP_LO, -50, 0.0); // TODO change 15.0 to vi_excess-vi_d


            // Define objective
            GLPK.glp_set_obj_name(lp, "z");
            GLPK.glp_set_obj_dir(lp, GLPKConstants.GLP_MIN);
            for (int var = 1; var <= n * n; ++var) {
                if ((((var - 1) % n) + 1) != ((var - 1) / n + 1)) {
                    GLPK.glp_set_obj_coef(lp, var, 0.00001); // TODO change 1.111 to T_ij
                }else{
                    GLPK.glp_set_obj_coef(lp, var, 0.0);
                }
            }



            // Write model to file
//            GLPK.glp_write_lp(lp, null, "testlinearprogram.lp");

            // Solve model
            parm = new glp_smcp();
            GLPK.glp_init_smcp(parm);
            ret = GLPK.glp_simplex(lp, parm);

            // Retrieve solution
            if (ret == 0) {
                write_lp_solution(lp);
            } else {
                System.out.println("The problem could not be solved");
            }

            // change values and solve again
            for (int eq = 1; eq <= n; ++eq) {
                GLPK.glp_set_row_bnds(lp, eq, GLPKConstants.GLP_LO, 18.0, 0.0); // TODO change 15.0 to vi_excess-vi_d
            }
//            GLPK.glp_write_lp(lp, null, "testlinearprogram_after.lp");



            // Free memory
            GLPK.glp_delete_prob(lp);

        } catch (GlpkException ex) {
            ex.printStackTrace();
            ret = 1;
        }
        System.exit(ret);
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
}
