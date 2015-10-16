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


import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.framework.DriverAgent;
import org.matsim.vehicles.Vehicle;
import playground.gregor.ctsim.simulation.CTEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;

/**
 * Created by laemmel on 12/10/15.
 */
public class CTNodeCell extends CTCell {

	private final Map<Id<Link>, LinkedList<CTPed>> peds = new HashMap<>();


	public CTNodeCell(double x, double y, CTNetwork net, CTNetworkEntity parent, double width) {
		super(x, y, net, parent, width);
		Node node = ((CTNode) parent).getNode();
		for (Link l : node.getOutLinks().values()) {
			peds.put(l.getId(), new LinkedList<CTPed>());
		}
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
		for (LinkedList<CTPed> l : this.peds.values()) {
			if (l.size() == 0) {
				continue;
			}
			double rate = chooseNextCellAndReturnJumpRate(l.peek());

			double rnd = -Math.log(1 - MatsimRandom.getRandom().nextDouble());
			double jumpTime = now + rnd / rate;
			if (jumpTime < minJumpTime) {
				minJumpTime = jumpTime;
				nextJumper = l.peek();
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
		CTNetworkEntity nbp = face.nb.getParent();
		if (nbp instanceof CTLink) {
			Link usLink = ((CTLink) nbp).getUsLink();
			Link dsLink = ((CTLink) nbp).getDsLink();
			if (usLink != null && ped.getNextLinkId() == usLink.getId()) {
				return 2 * this.width / CTLink.WIDTH;
			}
			else {
				if (ped.getNextLinkId() == dsLink.getId()) {
					return 2 * this.width / CTLink.WIDTH;
				}
			}
		}
		return 0;
	}

	@Override
	public void jumpOffPed(CTPed ctPed, double time) {
		LinkedList<CTPed> l = this.peds.get(ctPed.getNextLinkId());
		if (l.peek() != ctPed) {
			throw new RuntimeException("Pedestrian: " + ctPed + " is not first one in the expected queue!");
		}
		l.poll();
		ctPed.notifyMoveOverNode();
		DriverAgent driver = ctPed.getDriver();
		LinkEnterEvent e = new LinkEnterEvent(Math.floor(time), driver.getId(), driver.getCurrentLinkId(), Id.create(driver.getId(), Vehicle.class));

		this.net.getEventsManager().processEvent(e);

		this.n--;
		this.setRho(this.n / getAlpha());
	}

	@Override
	public boolean jumpOnPed(CTPed ctPed, double time) {
		DriverAgent driver = ctPed.getDriver();
		if (peds.containsKey(driver.getCurrentLinkId())) {
//			log.warn("wrong direction my friend!");
			return false;
		}

		LinkLeaveEvent e = new LinkLeaveEvent(Math.floor(time), driver.getId(), driver.getCurrentLinkId(), Id.create(driver.getId(), Vehicle.class));
		this.net.getEventsManager().processEvent(e);
		if (ctPed.getNextLinkId() == null) {
			CTNetsimEngine en = this.net.getEngine();
			en.letPedArrive(ctPed);
		}
		else {
//			Id<Link> nextL = ctPed.getNextLinkId();
//			LinkedList<CTPed> xx = this.peds.get(nextL);
//			if (xx == null) {
//				System.out.println("Gotcha!");
//			}
//			xx.add(ctPed);
			this.peds.get(ctPed.getNextLinkId()).add(ctPed);
			this.n++;
			this.setRho(this.n / getAlpha());
		}

		return true;
	}

	@Override
	HashSet<CTPed> getPeds() {
		return null;
	}

	public void init() {
		double maxCap = 0;
		for (Link l : ((CTNode) this.getParent()).getNode().getInLinks().values()) {
			if (l.getCapacity() > maxCap) {
				maxCap = l.getCapacity();
			}
		}
		for (Link l : ((CTNode) this.getParent()).getNode().getOutLinks().values()) {
			if (l.getCapacity() > maxCap) {
				maxCap = l.getCapacity();
			}
		}
		double area = (maxCap / 1.33) * (maxCap / 1.33);

		setArea(area);
	}
}
