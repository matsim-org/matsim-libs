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

import java.util.*;

/**
 * Created by laemmel on 12/10/15.
 */
public class CTLinkCell extends CTCell {


	private final LinkedList<CTPed> dsList = new LinkedList<>();
	private final LinkedList<CTPed> usList = new LinkedList<>();

	private final Map<Double, Double> cosLookup = new TreeMap<>();
	private double usProp;
	private double dsProp;

	public CTLinkCell(double x, double y, CTNetwork net, CTNetworkEntity parent, double width, double area) {
		super(x, y, net, parent, width, area);
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
			double j = chooseNextCellAndReturnJ(dsList.peek());
			if (!Double.isNaN(j)) {
//				double j = getJ(dsList.peek().getTentativeNextCell());
				double rnd = -Math.log(1 - MatsimRandom.getRandom().nextDouble());
				double meanJumpTime = 1. / (this.getDirectionalProportion(dsList.peek()) * j);
				double nxtJumpTime = meanJumpTime * rnd;

				if (nxtJumpTime < minJumpTime) {
					minJumpTime = nxtJumpTime;
					nextJumper = dsList.peek();
				}
			}
		}


		if (usList.size() > 0) {
			double j = chooseNextCellAndReturnJ(usList.peek());
			if (!Double.isNaN(j)) {
//				double j = getJ(usList.peek().getTentativeNextCell());
				double rnd = -Math.log(1 - MatsimRandom.getRandom().nextDouble());
				double meanJumpTime = 1. / (this.getDirectionalProportion(usList.peek()) * j);
				double nxtJumpTime = meanJumpTime * rnd;
				if (nxtJumpTime < minJumpTime) {
					minJumpTime = nxtJumpTime;
					nextJumper = usList.peek();
				}
			}
		}

		if (nextJumper == null) {
			return;
		}
		this.next = nextJumper;

//		double j = getJ(nextJumper.getTentativeNextCell());///(Math.sqrt(3)/2);
//		double rnd = -Math.log(1 - MatsimRandom.getRandom().nextDouble());
//		double meanJumpTime = 1. / j;
////		log.info(meanJumpTime);
//		double nextJumpTime = now + rnd * meanJumpTime;

		this.nextCellJumpTime = now + minJumpTime;
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

		updateProps();

		List<CTCellFace> f = this.getFaces();
		Collections.shuffle(f);
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
		updateProps();


		return true;
	}

	@Override
	HashSet<CTPed> getPeds() {
		HashSet<CTPed> ret = new HashSet<>(this.dsList);
		ret.addAll(this.usList);
		return ret;
	}

	@Override
	double getDirectionalProportion(CTPed ped) {
		if (this.n == 0) {
			return 1;
		}
		if (ped.getDesiredDir() == Math.PI / 2.) {
			return this.dsProp;
		}
		else {
			if (ped.getDesiredDir() == -Math.PI / 2.) {
				return this.usProp;
			}
			else {
				throw new RuntimeException("unsupported direction");
			}
		}
	}

	private void updateProps() {
		this.usProp = (double) this.usList.size() / (double) this.n;
		if (Double.isNaN(this.usProp)) {
			this.usProp = 0;
		}
		this.dsProp = (double) this.dsList.size() / (double) this.n;
		if (Double.isNaN(this.dsProp)) {
			this.dsProp = 0;
		}
	}

}
