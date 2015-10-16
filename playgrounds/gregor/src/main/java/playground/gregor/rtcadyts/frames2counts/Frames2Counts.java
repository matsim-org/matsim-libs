/* *********************************************************************** *
 * project: org.matsim.*
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
package playground.gregor.rtcadyts.frames2counts;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.network.NetworkImpl;

import playground.gregor.rtcadyts.io.SensorDataFrame;
import playground.gregor.rtcadyts.io.SensorDataVehicle;

public class Frames2Counts {

	private static final double TIME_SLICE_SZ = 2*60;
	
	private List<SensorDataFrame> frames;
	private Scenario sc;
	private NetworkImpl net;
	private double minTime = Double.POSITIVE_INFINITY;

	private final Map<Id<Link>,LinkInfo> lis = new HashMap<>();

	public Frames2Counts(Scenario sc, List<SensorDataFrame> frames) {
		this.sc = sc;
		this.frames = frames;
		this.net = (NetworkImpl) sc.getNetwork();
	}

	public void run() {
		
		for (SensorDataFrame fr : frames) {
//			if (fr.getTime() < minTime) {
//				minTime = fr.getTime();
//			}
			for (SensorDataVehicle veh : fr.getVehicles()){
				
				handleVehicle(veh);
			}
		}
//		Collections.sort(frames, new Comparator<SensorDataFrame>() {
//			@Override
//			public int compare(SensorDataFrame o1, SensorDataFrame o2) {
//				return o1.getTime() - o2.getTime() < 0 ? -1 : 1;
//			}
//		});
		for (LinkInfo li : this.lis.values()) {
			handleLinkInfo(li);
		}

	}

	private void handleLinkInfo(LinkInfo li) {
		double length = li.getLink().getLength();
		double lanes = li.getLink().getNumberOfLanes();
		List<SensorDataVehicle> vehs = li.getVeh();
		Collections.sort(vehs, new Comparator<SensorDataVehicle>() {
			@Override
			public int compare(SensorDataVehicle o1, SensorDataVehicle o2) {
				return o1.getTime() < o2.getTime() ? -1 : 1;
			}
		});
		int episods = 0;
		double rho = 0;
		double v  =0;
		double time = vehs.get(0).getTime();
		
		int eCnt = 0;
		double eV = 0;
		for (SensorDataVehicle veh : vehs) {
			if (veh.getTime() > time) {
				episods++;
				v+=eV/eCnt;
				rho+=eCnt/(lanes*length);
				eCnt = 0;
				eV = 0;
			}
			eV += veh.getSpeed();
			eCnt++;
		}
		episods++;
		v+=eV/eCnt;
		rho+=eCnt/(lanes*length);
		
		v /= episods;
		rho /= episods;
		double q = rho*v;
		li.setFlow(q*lanes);
		
	}

	private void handleVehicle(SensorDataVehicle veh) {
		Link tentativeLink = net.getNearestLinkExactly(new Coord(veh.getX(), veh.getY()));
		LinkInfo li = this.lis.get(tentativeLink.getId());
		if (li == null) {
			li = new LinkInfo(tentativeLink);
			this.lis.put(tentativeLink.getId(),li);
		}

		li.addVeh(veh);
	}

	public Collection<LinkInfo> getLinkInfos() {
		return this.lis.values();
	}

	private int getTimeSlice(double time) {
		return (int) ((time-this.minTime)/TIME_SLICE_SZ);
	}
}
