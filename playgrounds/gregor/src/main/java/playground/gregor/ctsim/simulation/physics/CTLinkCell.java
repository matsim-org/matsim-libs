package playground.gregor.ctsim.simulation.physics;
/* *********************************************************************** *
 * project: org.matsim.*
 *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

import org.matsim.core.gbl.MatsimRandom;
import playground.gregor.ctsim.simulation.CTEvent;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by laemmel on 12/10/15.
 */
public class CTLinkCell extends CTCell {


	private final LinkedList<CTPed> dsList = new LinkedList<>();
	private final LinkedList<CTPed> usList = new LinkedList<>();

	private final Map<Double, Double> cosLookup = new TreeMap<>();

	public CTLinkCell(double x, double y, CTNetwork net, CTNetworkEntity parent, double width) {
		super(x, y, net, parent, width);
	}

	@Override
	public void updateIntendedCellJumpTimeAndChooseNextJumper(double now) {
		if (this.currentEvent != null) {
			this.currentEvent.invalidate();
		}
		if (this.n == 0) {
			this.nextCellJumpTime = Double.NaN;
			return;
		}
		double minJumpTime = Double.POSITIVE_INFINITY;
		CTPed nextJumper = null;
		if (dsList.size() > 0) {
			double rate = chooseNextCellAndReturnJumpRate(dsList.peek());

			double rnd = -Math.log(1 - MatsimRandom.getRandom().nextDouble());
			double jumpTime = now + rnd / rate;
			if (jumpTime < minJumpTime) {
				minJumpTime = jumpTime;
				nextJumper = dsList.peek();
			}
		}
		if (usList.size() > 0) {
			double rate = chooseNextCellAndReturnJumpRate(usList.peek());

			double rnd = -Math.log(1 - MatsimRandom.getRandom().nextDouble());
			double jumpTime = now + rnd / rate;
			if (jumpTime < minJumpTime) {
				minJumpTime = jumpTime;
				nextJumper = usList.peek();
			}
		}

		if (nextJumper == null) {
			return;
		}
		this.next = nextJumper;

		this.nextCellJumpTime = minJumpTime;
		CTEvent e = new CTEvent(this, nextCellJumpTime);
		this.currentEvent = e;
		this.net.addEvent(e);
	}

	@Override
	double getFHHi(CTPed ped, CTCellFace face) {
//		if (ped.getDesiredDir()*face.h_i < 0 && face.nb.getParent() instanceof CTNode) {
//			return 0;
//		}
		double diff = ped.getDesiredDir() - face.h_i;
		Double d = cosLookup.get(diff);
		if (d == null) {
			d = 1 + Math.cos(diff);
			cosLookup.put(diff, d);
		}
		return d;
	}

	public void jumpOffPed(CTPed ctPed, double time) {
		if (dsList.peek() == ctPed) {
			dsList.poll();
		}
		else {
			if (usList.peek() == ctPed) {
				usList.poll();
			}
			else {
				throw new RuntimeException("Pedestrian:" + ctPed + " is neither first one in dsList nor in usList!");
			}
		}
		this.n--;
		this.setRho(this.n / getAlpha());
	}

	public boolean jumpOnPed(CTPed ctPed, double time) {
		if (ctPed.getDesiredDir() == Math.PI / 2.) {
			this.dsList.add(ctPed);
		}
		else {
			if (ctPed.getDesiredDir() == -Math.PI / 2.) {
				this.usList.add(ctPed);
			}
			else {
				throw new RuntimeException("Unsupported desired movement direction: " + ctPed.getDesiredDir() + "!");
			}
		}
		this.n++;
		this.setRho(this.n / getAlpha());


		return true;
	}

	@Override
	HashSet<CTPed> getPeds() {
		HashSet<CTPed> ret = new HashSet<>(this.dsList);
		ret.addAll(this.usList);
		return ret;
	}

}
