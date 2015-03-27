/* *********************************************************************** *
 * project: org.matsim.*
 * LaneSpeedObserver.java
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

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;

import playground.gregor.casim.simulation.physics.AbstractCANetwork;
import playground.gregor.casim.simulation.physics.CAMoveableEntity;

public class LaneSpeedObserver implements LinkEnterEventHandler, LinkLeaveEventHandler{
		

	private final Map<Id,AgentInfo> agents = new HashMap<>();
	
	private final AbstractCANetwork net;

	private final double[] lanesTT;

	private final double[] lanesCnt;
	private final double[] lanesAgents;
	private final double[] lanesFlow;
	
	private int cnt = 0;

	private final double ll;

	private final double ringLaneArea;
	
	public LaneSpeedObserver(AbstractCANetwork net, int nrLanes, double linkLength) {
		this.net = net;
		this.lanesTT = new double[nrLanes];
		this.lanesCnt = new double[nrLanes];
		this.lanesAgents = new double[nrLanes];
		this.lanesFlow = new double[nrLanes];
		for (int i = 0 ; i < nrLanes; i++) {
			this.lanesTT[i] = 30;
		}
		this.ll = linkLength;
		this.ringLaneArea = 4*this.ll*AbstractCANetwork.PED_WIDTH;
	}

	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		AgentInfo ai = this.agents.get(event.getVehicleId());
		if (ai == null) {
			return;
		}
		double travelTime = event.getTime() - ai.enterTime;
		CAMoveableEntity m = this.net.getCAMovableEntity(ai.id);
		int lane = m.getLane();
		double cnt = this.lanesCnt[lane];
		double avgTT = this.lanesTT[lane] * cnt/(1+cnt) + travelTime * 1/(1+cnt);
//		double avgTT = this.lanesTT[lane] * 0.99 + travelTime *0.01;
		this.lanesCnt[lane]++;
		this.lanesTT[lane] = avgTT;
		
		double rho = this.lanesAgents[lane]/this.ringLaneArea;
		double spd = this.ll/this.lanesTT[lane];
		this.lanesFlow[lane] = rho*spd;
		
		this.lanesAgents[lane]--;
		
		
		if (this.cnt++ % 100 == 0) {
			for (int i = 0; i < this.lanesCnt.length; i++) {
				cnt = this.lanesCnt[i];
				avgTT = this.lanesTT[i] * cnt/(1+cnt);//+this.ll/AbstractCANetwork.V_HAT * 1/(1+cnt);
//				avgTT = this.lanesTT[i] * 0.99;// + 20 * 0.01;
				this.lanesCnt[i]++;
				this.lanesTT[i] = avgTT;
//				System.out.print(((int)(100*this.lanesFlow[lane]))/100.0 + " ");
//				rho = this.lanesAgents[i]/this.ringLaneArea;
//				spd = this.ll/this.lanesTT[i];
//				this.lanesFlow[i] = rho*spd;
			}
//			System.out.println();
			
		}
	}

	public double getLaneAvgTT(int lane) {
		return -this.lanesFlow[lane];
	}
	
	
	@Override
	public void handleEvent(LinkEnterEvent event) {
		AgentInfo ai = this.agents.get(event.getVehicleId());
		if (ai == null) {
			ai = new AgentInfo();
			this.agents.put(event.getVehicleId(), ai);
		}
		ai.enterTime = event.getTime();
		ai.id = event.getVehicleId();
		CAMoveableEntity m = this.net.getCAMovableEntity(ai.id);
		int lane = m.getLane();
		this.lanesAgents[lane]++;
	}
	
	private static final class AgentInfo {
		double enterTime;
		Id id;
	}

}
