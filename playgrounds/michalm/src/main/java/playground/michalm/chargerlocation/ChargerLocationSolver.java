/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.michalm.chargerlocation;

//import gurobi.*;
import playground.michalm.chargerlocation.ChargerLocationProblem.ChargerLocationSolution;


public class ChargerLocationSolver
{
    private final ChargerLocationProblem problem;

//    private GRBModel model;
//    private GRBVar[] xVar;//charging station selection, j
//    private GRBVar[][] fVar; //assignments, (i, j)


    public ChargerLocationSolver(ChargerLocationProblem problem)
    {
        this.problem = problem;
    }


    public ChargerLocationSolution solve(ChargerLocationSolution initialSolution)
    {
        return null;
    }
//        try {
//            model = new GRBModel(new GRBEnv());
//
//            // this is the internal model (a copy of that passed to the constructor)
//            GRBEnv env = model.getEnv();
//
//            //env.set(GRB.DoubleParam.TimeLimit, mode.timeLimit);// 2 hours
//            env.set(GRB.DoubleParam.MIPGap, 0.01);//0.01=1%
//
//            //env.set(GRB.IntParam.MIPFocus, 1);//the focus towards finding feasible solutions
//            //or alternatively: focus towards finding feasible solutions after 1 hour
//            //env.set(GRB.DoubleParam.ImproveStartTime, 3600);
//
//            //env.set(GRB.IntParam.Threads, 1);//number of threads
//
//            //env.set(GRB.IntParam.OutputFlag, mode.output ? 1 : 0);//output
//
//            addXVariables();
//            addFVariables();
//            model.update();
//
//            setObjective();
//
//            addEnergyDemandConstraint();
//            addEnergySupplyConstraint();
//            addChargerCountConstraint();
//            model.update();
//
//            //model.write("D:/model.lp");
//
//            if (initialSolution != null) {
//                applyInitialSolution(initialSolution);
//            }
//
//            model.optimize();
//
//            //model.write("D:/gurobi_solution.sol");
//
//            ChargerLocationSolution solution = extractSolution();
//
//            model.dispose();
//            env.dispose();
//
//            return solution;
//        }
//        catch (GRBException e) {
//            throw new RuntimeException(e);
//        }
//    }
//
//
//    private void addXVariables()
//        throws GRBException
//    {
//        xVar = new GRBVar[problem.J];
//        for (int j = 0; j < problem.J; j++) {
//            xVar[j] = model.addVar(0, problem.maxChargersInZone, 0, GRB.INTEGER, "x_" + j);
//        }
//    }
//
//
//    private void addFVariables()
//        throws GRBException
//    {
//        fVar = new GRBVar[problem.I][problem.J];
//        for (int i = 0; i < problem.I; i++) {
//            double potential_i = problem.zoneData.entries.get(i).potential;
//
//            for (int j = 0; j < problem.J; j++) {
//                double maxFlow_ij = problem.distances[i][j] > problem.maxDistance ? 0 : //
//                        potential_i * problem.zoneData.potentialToEnergy;
//                fVar[i][j] = model.addVar(0, maxFlow_ij, 0, GRB.CONTINUOUS, "f_" + i + "," + j);
//            }
//        }
//    }
//
//
//    private void setObjective()
//        throws GRBException
//    {
//        GRBLinExpr obj = new GRBLinExpr();
//
//        for (int i = 0; i < problem.I; i++) {
//            for (int j = 0; j < problem.J; j++) {
//                obj.addTerm(problem.distances[i][j], fVar[i][j]);
//            }
//        }
//
//        model.setObjective(obj);
//        model.set(GRB.IntAttr.ModelSense, 1);
//    }
//
//
//    private void addEnergyDemandConstraint()
//        throws GRBException
//    {
//        for (int i = 0; i < problem.I; i++) {
//            GRBLinExpr expr = new GRBLinExpr();
//
//            for (int j = 0; j < problem.J; j++) {
//                expr.addTerm(1, fVar[i][j]);
//            }
//
//            double demand_i = problem.zoneData.entries.get(i).potential
//                    * problem.zoneData.potentialToEnergy;
//
//            model.addConstr(expr, GRB.EQUAL, demand_i, "demand_" + i);
//        }
//    }
//
//
//    private void addEnergySupplyConstraint()
//        throws GRBException
//    {
//        for (int j = 0; j < problem.J; j++) {
//            GRBLinExpr expr = new GRBLinExpr();
//
//            for (int i = 0; i < problem.I; i++) {
//                expr.addTerm(1, fVar[i][j]);
//            }
//
//            double supply_j = problem.chargerData.stations.get(j).getPower()
//                    * problem.chargerData.powerToEnergy;
//            expr.addTerm(-supply_j, xVar[j]);
//
//            model.addConstr(expr, GRB.LESS_EQUAL, 0, "supply_" + j);
//        }
//    }
//
//
//    private void addChargerCountConstraint()
//        throws GRBException
//    {
//        GRBLinExpr expr = new GRBLinExpr();
//
//        for (int j = 0; j < problem.J; j++) {
//            expr.addTerm(1, xVar[j]);
//        }
//
//        model.addConstr(expr, GRB.LESS_EQUAL, problem.maxChargers, "charger_count");
//    }
//
//
//    private void applyInitialSolution(ChargerLocationSolution initialSolution)
//        throws GRBException
//    {
//        for (int j = 0; j < problem.J; j++) {
//            xVar[j].set(GRB.DoubleAttr.Start, initialSolution.x[j]);
//        }
//
//        for (int i = 0; i < problem.I; i++) {
//            for (int j = 0; j < problem.J; j++) {
//                fVar[i][j].set(GRB.DoubleAttr.Start, initialSolution.f[i][j]);
//            }
//        }
//    }
//
//
//    private ChargerLocationSolution extractSolution()
//        throws GRBException
//    {
//        int[] x = new int[problem.J];
//        for (int j = 0; j < problem.J; j++) {
//            x[j] = (int)Math.round(xVar[j].get(GRB.DoubleAttr.X));
//        }
//
//        double[][] f = new double[problem.I][problem.J];
//        for (int i = 0; i < problem.I; i++) {
//            for (int j = 0; j < problem.J; j++) {
//                f[i][j] = fVar[i][j].get(GRB.DoubleAttr.X);
//            }
//        }
//
//        return new ChargerLocationSolution(x, f);
//    }
}
