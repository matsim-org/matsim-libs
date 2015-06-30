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

import playground.michalm.chargerlocation.ChargerLocationProblem.ChargerLocationSolution;


public class ChargerLocationFinder
{
    private final ChargerLocationProblem problem;

    private int[] x;
    private double[][] f;

    private double[] supply;


    public ChargerLocationFinder(ChargerLocationProblem problem)
    {
        this.problem = problem;
    }


    public ChargerLocationSolution findInitialSolution()
    {
        openChargers();
        calcSupply();
        calcFlows();
        return new ChargerLocationSolution(x, f);
    }


    private void openChargers()
    {
        int chargersPerZone = problem.maxChargers / problem.J;
        int remainingChargers = problem.maxChargers % problem.J;

        x = new int[problem.J];
        for (int j = 0; j < remainingChargers; j++) {
            x[j] = chargersPerZone + 1;
        }
        for (int j = remainingChargers; j < problem.J; j++) {
            x[j] = chargersPerZone;
        }
    }


    private void calcSupply()
    {
        supply = new double[problem.J];
        for (int j = 0; j < problem.J; j++) {
            supply[j] = problem.chargerData.stations.get(j).getPower() * x[j]
                    * problem.chargerData.powerToEnergy;
        }
    }


    private void calcFlows()
    {
        f = new double[problem.I][problem.J];

        //(modified) North-West corner
        for (int i = 0; i < problem.I; i++) {
            calcFlowsForZone(i, supply);
        }
    }


    private void calcFlowsForZone(int i, double[] supply)
    {
        double demand_i = problem.zoneData.entries.get(i).potential
                * problem.zoneData.potentialToEnergy;

        for (int j = 0; j < problem.J; j++) {
            if (problem.distances[i][j] <= problem.maxDistance) {
                if (demand_i <= supply[j]) {
                    supply[j] -= demand_i;
                    f[i][j] += demand_i;
                    return;
                }
                else {
                    demand_i -= supply[j];
                    supply[j] = 0;
                }
            }
        }

        throw new IllegalStateException("Unmet demand for zone " + i);
    }
}
