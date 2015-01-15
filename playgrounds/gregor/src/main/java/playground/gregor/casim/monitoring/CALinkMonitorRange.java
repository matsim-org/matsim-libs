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

package playground.gregor.casim.monitoring;

import java.io.BufferedWriter;
import java.io.IOException;

import playground.gregor.casim.simulation.physics.CALink;
import playground.gregor.casim.simulation.physics.CAMoveableEntity;

public class CALinkMonitorRange extends CALinkMonitorExact {

	private double dens1;
	private double dens2;
	private CAMoveableEntity a1;
	private CAMoveableEntity a2;
	private int sp1;
	private int sp2;
	private int target;
	private CAMoveableEntity[] parts;
	private double ttA1;
	private int trA1;
	private double ttA2;
	private int trA2;
	private CALink l;

	public CALinkMonitorRange(CALink l, double range, CAMoveableEntity[] parts,
			double laneWidth, double avgDens1, double avgDens2,
			CAMoveableEntity a1, CAMoveableEntity a2) {
		super(l, range, parts, laneWidth);
		this.dens1 = avgDens1;
		this.dens2 = avgDens2;
		this.a1 = a1;
		this.a2 = a2;
		this.sp1 = a1.getPos();
		this.sp2 = a2.getPos();
		this.target = parts.length / 2;
		this.parts = parts;
		this.l = l;

	}

	@Override
	public void trigger(double time) {

		if (a1 != null && parts[target - 5] == a1) {
			a1 = null;
			this.ttA1 = time;
			this.trA1 = target - 5 - sp1;
		}
		if (a2 != null && parts[target + 5] == a2) {
			a2 = null;
			this.ttA2 = time;
			this.trA2 = sp2 - (target + 5);
		}

	}

	@Override
	public void report(BufferedWriter bw) throws IOException {

		double cellLength = l.getLink().getLength() / this.parts.length;
		double spd1 = (trA1 * cellLength) / ttA1;
		double spd2 = (trA2 * cellLength) / ttA2;

		bw.append("NaN " + dens1 + " " + spd1 + " " + dens2 + " " + spd2 + "\n");
	}
}
