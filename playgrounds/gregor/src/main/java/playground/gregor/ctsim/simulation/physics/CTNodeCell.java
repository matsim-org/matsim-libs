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

import java.util.*;

/**
 * Created by laemmel on 12/10/15.
 */
public class CTNodeCell extends CTCell {

	private final Map<Id<Link>, DirInfo> dirInfos = new HashMap<>();


	public CTNodeCell(double x, double y, CTNetwork net, CTNetworkEntity parent, double width) {
		super(x, y, net, parent, width, 0);

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
		for (DirInfo info : this.dirInfos.values()) {
			LinkedList<CTPed> l = info.peds;
			if (l.size() == 0) {
				continue;
			}
			double j = chooseNextCellAndReturnJ(l.peek());
			double rnd = -Math.log(1 - MatsimRandom.getRandom().nextDouble());
			double nxtJmpTm = rnd / (getDirectionalProportion(l.peek()) * j);

			if (nxtJmpTm < minJumpTime) {
				minJumpTime = nxtJmpTm;
				nextJumper = l.peek();
			}

		}
		if (nextJumper == null) {
			return;
		}
		this.next = nextJumper;

		this.nextCellJumpTime = now + minJumpTime;
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
		DirInfo info = this.dirInfos.get(ctPed.getNextLinkId());
		LinkedList<CTPed> l = info.peds;

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
		updateDirProps();

		List<CTCellFace> f = this.getFaces();
		Collections.shuffle(f);
	}

	@Override
	public boolean jumpOnPed(CTPed ctPed, double time) {
		DriverAgent driver = ctPed.getDriver();
		if (dirInfos.containsKey(driver.getCurrentLinkId())) {
			return false;
		}

		LinkLeaveEvent e = new LinkLeaveEvent(Math.floor(time), driver.getId(), driver.getCurrentLinkId(), Id.create(driver.getId(), Vehicle.class));
		this.net.getEventsManager().processEvent(e);
		if (ctPed.getNextLinkId() == null) {
			CTNetsimEngine en = this.net.getEngine();
			en.letPedArrive(ctPed);
		}
		else {
			DirInfo info = this.dirInfos.get(ctPed.getNextLinkId());
			info.peds.add(ctPed);
			ctPed.setDir(info.dir);
			this.n++;
			this.setRho(this.n / getAlpha());
			updateDirProps();
		}

		return true;
	}

	@Override
	HashSet<CTPed> getPeds() {
		return null;
	}

	@Override
	double getDirectionalProportion(CTPed ped) {

		DirInfo info = this.dirInfos.get(ped.getNextLinkId());
		if (info == null) {
			return 1.;
		}
		if (this.n == 0) {
			return 1;
		}
		return info.dirProp;
	}

	private void updateDirProps() {

		for (DirInfo nfo : this.dirInfos.values()) {
			nfo.dirProp = (double) nfo.peds.size() / (double) this.n;
			if (Double.isNaN(nfo.dirProp)) {
				nfo.dirProp = 0;
			}
		}
	}

	public void init() {
		double maxCap = 0;
		double mxLength = 0;
		for (Link l : ((CTNode) this.getParent()).getNode().getInLinks().values()) {
			if (l.getCapacity() > maxCap) {
				maxCap = l.getCapacity();
				mxLength = l.getLength();
			}
		}
		for (Link l : ((CTNode) this.getParent()).getNode().getOutLinks().values()) {
			if (l.getCapacity() > maxCap) {
				maxCap = l.getCapacity();
				mxLength = l.getLength();
			}
		}

		double width = (maxCap / 1.33);
		if (width < CTLink.WIDTH) {
			width = CTLink.WIDTH;
		}
		double area = width * width;
		if (((CTNode) this.getParent()).getNode().getOutLinks().size() == 1
				&& ((CTNode) this.getParent()).getNode().getInLinks().size() == 1
				&& ((CTNode) this.getParent()).getNode().getOutLinks().values().iterator().next().getToNode()
				== ((CTNode) this.getParent()).getNode().getInLinks().values().iterator().next().getFromNode()) {
			area = width * mxLength;
		}


		setArea(area);
		Node node = ((CTNode) parent).getNode();
		for (Link l : node.getOutLinks().values()) {

			CTLink ctLink = net.getLinks().get(l.getId());
			double dir;
			if (ctLink.getDsLink() == l) {
				dir = Math.PI / 2;
			}
			else {
				if (ctLink.getUsLink() == l) {
					dir = -Math.PI / 2;
				}
				else {
					throw new RuntimeException("Network seems to be faulty wired");
				}
			}
			this.dirInfos.put(l.getId(), new DirInfo(dir));
		}
	}

	private static final class DirInfo {
		private final double dir;
		LinkedList<CTPed> peds = new LinkedList<CTPed>();
		double dirProp = 0;

		public DirInfo(double dir) {
			this.dir = dir;
		}
	}
}
