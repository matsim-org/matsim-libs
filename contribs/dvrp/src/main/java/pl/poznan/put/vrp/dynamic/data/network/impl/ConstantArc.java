/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package pl.poznan.put.vrp.dynamic.data.network.impl;

import pl.poznan.put.vrp.dynamic.data.network.*;


public class ConstantArc
    extends AbstractArc
{
    private int arcTime;
    private double arcCost;


    public ConstantArc(Vertex fromVertex, Vertex toVertex, int arcTime, double arcCost)
    {
        super(fromVertex, toVertex);
        this.arcTime = arcTime;
        this.arcCost = arcCost;
    }


    @Override
    public int getTimeOnDeparture(int departureTime)
    {
        return arcTime;
    }


    @Override
    public int getTimeOnArrival(int arrivalTime)
    {
        return arcTime;
    }


    @Override
    public double getCostOnDeparture(int departureTime)
    {
        return arcCost;
    }


    public static class ConstantArcFactory
        implements ArcFactory
    {
        private final int[][] times;
        private final double[][] costs;


        /**
         * @param times - n x n matrix with (time-independent) arc times
         * @param costs - n x n matrix with (time-independent) arc costs
         */
        public ConstantArcFactory(int[][] times, double[][] costs)
        {
            this.times = times;
            this.costs = costs;
        }


        @Override
        public Arc createArc(Vertex fromVertex, Vertex toVertex)
        {
            int i = fromVertex.getId();
            int j = toVertex.getId();

            return new ConstantArc(fromVertex, toVertex, times[i][j], costs[i][j]);
        }
    }
}
