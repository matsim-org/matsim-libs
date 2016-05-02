/*
 * *********************************************************************** *
 * project: org.matsim.*                                                   *
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
 * *********************************************************************** *
 */

package playground.boescpa.av.staticDemand;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordUtils;

/**
 * WHAT IS IT FOR?
 * WHAT DOES IT?
 *
 * @author boescpa
 */
public class AutonomousVehicle {

    private Coord myPosition;
    private int arrivalTime;
    private int lastArrivalTime;

    protected AutonomousVehicle(Coord initialCoords) {
        myPosition = initialCoords;
        arrivalTime = -1;
        lastArrivalTime = 0;
    }

    public Coord getMyPosition() {
        return CoordUtils.createCoord(myPosition.getX(), myPosition.getY());
    }

    /**
     * Moves AV to the new position and returns the travel time.
     * @param newPosition
     * @return travelTime
     */
    public double moveTo(Coord newPosition) {
        if (Double.isNaN(newPosition.getX()) || Double.isNaN(newPosition.getY())) {
            throw new IllegalArgumentException("NaN-Coordinates as new position!");
        }
        double distance = CoordUtils.calcEuclideanDistance(myPosition, newPosition) * Constants.BEELINE_FACTOR_STREET;
        myPosition = newPosition;
        return distance / Constants.AV_SPEED;
    }

    /**
     * If the vehicle is in use, get time when it's free again.
     *
     * @return -1 if not in use, the arrival time else.
     */
    public int getArrivalTime() {
        return arrivalTime;
    }

    public void setArrivalTime(int arrivalTime) {
        if (arrivalTime > 0) {
            this.arrivalTime = arrivalTime;
            this.lastArrivalTime = arrivalTime;
        } else {
            throw new IllegalArgumentException("illegal arrival time");
        }
    }

    public void resetArrivalTime() {
        this.arrivalTime = -1;
    }

    public int getLastArrivalTime() {
        return lastArrivalTime;
    }

    // ------ Stats ------

    private int numberOfServices = 0;
    private double totalAccessTime = 0;
    private double totalAccessDistance = 0;
    private double totalServiceTime = 0;
    private double totalServiceDistance = 0;
    private double totalWaitingTimeForAgents = 0;
    //private double maxWaitingTimeForAgent = 0;

    public void incNumberOfServices() {
        numberOfServices++;
    }

    public void incAccessTime(double accessTime) {
        if (accessTime >= 0) {
            totalAccessTime += accessTime;
        } else {
            throw new IllegalArgumentException("Negative access time!");
        }
    }

    public void incAccessDistance(double accessDistance) {
        if (accessDistance >= 0) {
            totalAccessDistance += accessDistance;
        } else {
            throw new IllegalArgumentException("Negative access distance!");
        }
    }

    public void incServiceTime(double serviceTime) {
        if (serviceTime >= 0) {
            totalServiceTime += serviceTime;
        } else {
            throw new IllegalArgumentException("Negative service time!");
        }
    }

    public void incServiceDistance(double serviceDistance) {
        if (serviceDistance >= 0) {
            totalServiceDistance += serviceDistance;
        } else {
            throw new IllegalArgumentException("Negative service distance!");
        }
    }

    public void incWaitingTime(double waitingTimeForAgent) {
        if (waitingTimeForAgent >= 0) {
            totalWaitingTimeForAgents += waitingTimeForAgent;
			/*if (waitingTimeForAgent > maxWaitingTimeForAgent) {
				maxWaitingTimeForAgent = waitingTimeForAgent;
			}*/
        } else {
            throw new IllegalArgumentException("Negative waiting time for agent!");
        }
    }

    public static String getStatsDescr() {
        return "numberOfServices"
                + Stats.delimiter + "totalAccessDistance"
                + Stats.delimiter + "totalServiceDistance"
                + Stats.delimiter + "totalAccessTime"
                + Stats.delimiter + "totalWaitingTimeForAgents"
                + Stats.delimiter + "totalServiceTime"
                + Stats.delimiter + "idleTime";
        //+ Stats.delimiter + "maxWaitingTimeForAgent";

    }

    public String getStats() {
        return numberOfServices
                + Stats.delimiter + totalAccessDistance
                + Stats.delimiter + totalServiceDistance
                + Stats.delimiter + totalAccessTime
                + Stats.delimiter + totalWaitingTimeForAgents
                + Stats.delimiter + totalServiceTime
                + Stats.delimiter + (Constants.TOTAL_SIMULATION_TIME - totalAccessTime - totalServiceTime - totalWaitingTimeForAgents);
        //+ Stats.delimiter + maxWaitingTimeForAgent;
    }
}
