/* *********************************************************************** *
 * project: org.matsim.*
 * CASimpleAgent.java
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

package playground.gregor.casim.simulation.physics;

import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

public class CASimpleDynamicAgent extends CAAgent {

	private CANetworkEntity currentEntity;

	private final List<Link> links;
	private int next;
	private final Id id;
	private CALink link;

	private double lastEnterTime = -1/(CANetworkDynamic.V_HAT*CANetworkDynamic.RHO_HAT);
	private double myDirectionRho = 0;
	private double myV;

	private final double delta = 0.9; //decay
	private double currentDecay = 1;
	private double lastOncommingAgent = 0;
	private double oncomingAgentsRate = 0;
	private double oncommingV = 0;
	private double theirDirectionRho = 0;

	private final double lastUpdate = 0;
	private final double updateBeforeLastUpdate = 0;

	private double cumWaitTime;


	private static double n = 0;
	private static double msaRho = 0;

	public CASimpleDynamicAgent(List<Link> links, int i, Id id, CALink caLink) {
		super(id);
		this.links = links;
		this.next = i;
		this.id = id;
		this.link = caLink;
		this.currentEntity = caLink;
	}

	@Override
	Id getNextLinkId() {

		return this.links.get(this.next).getId();
	}

	@Override
	public void moveToNode(CANode n) {
		this.currentEntity = n;
	}

	@Override
	void moveOverNode(CALink link, double time) {
		//		System.out.println("DEBUG");
		//		if (this.id.toString().equals("46") && link.getLink().getId().toString().equals("7") && time > 300){
		//			System.out.println("DEBUG");
		//		}
		this.link = link;
		this.currentEntity = link;
		this.next++;
		//		System.out.println(link.getLink().getId() + " " + this.links.get(this.next).getId());
		if (this.next == this.links.size()) {
			this.next = this.links.size()-1;
		}
		//		System.out.println(this.next);
	}



	@Override
	public CALink getCurrentLink() {
		return this.link;
	}


	public void updateMyDynamicQuantitiesOnAdvance(double timeGap, double time, double cellLength,double channelWidth) {
		//		if (this.getId().toString().equals("1242") && this.getCurrentLink().getLink().getId().toString().equals("2")) {
		//			System.out.println("got you!!!");
		//		}
		//		if (this.getCurrentLink().getLink().getId().toString().equals("2b")) {
		////			System.out.println( + " " + this.myDirectionRho);
		//		}

		double tt = time - this.lastEnterTime;
		if (tt < 0.01) { //TODO use here tFree instead
			this.lastEnterTime = time;
			return;
		}
		//		if (tt <= 0) {
		//			System.out.println("got you!!!");
		//		}

//		double w1 = this.lastUpdate-this.updateBeforeLastUpdate;
//		double w2 = time-this.lastUpdate;
//		double sum = (w1+w2);
//		w1 /= sum;
//		w2 /= sum;
//		this.updateBeforeLastUpdate = this.lastUpdate;
//		this.lastUpdate = time;

		this.myV =cellLength/tt;//TODO maybe we need some smoothing here ...
		//		this.myV = cellLength/tt;//TODO maybe we need some smoothing here ...
//		this.myV = cellLength/tt;//TODO maybe we need some smoothing here ...
		//		if (this.myV < 1) {
		//			System.err.println(this.myV);
		//		}
		//		System.out.println(this.myV);
		double predecessorDist = timeGap*this.myV;
//		double widthRatio = CANetworkDynamic.PED_WIDTH/channelWidth;
//		double widthRatio = 1/(channelWidth*CANetworkDynamic.PED_WIDTH);
		double tmp = 1/(predecessorDist*channelWidth);
		//		System.out.println(widthRatio*tmp);
		//		this.myDirectionRho =0.5*this.myDirectionRho + 0.5*Math.min(widthRatio*tmp,CANetworkDynamic.RHO_HAT);
		this.myDirectionRho =Math.min(tmp,CANetworkDynamic.RHO_HAT);
		//		if (this.myDirectionRho > CANetworkDynamic.RHO_HAT) {
		//			System.err.println("dd");
		//		}
//		if (this.getCurrentLink().getLink().getId().toString().equals("2a")) {
//			msaRho = n/(n+1) * msaRho + 1/(1+n) * this.myDirectionRho;
//			System.out.println(predecessorDist + " " + this.myV + " " + this.myDirectionRho +" " + msaRho + " " + cellLength * this.getCurrentLink().getNumOfCells());
//			n++;
//		}
//		if (this.myDirectionRho < 0) {
//			System.out.println("got you!!!");
//		}
		this.lastEnterTime = time;
		this.currentDecay *= (this.delta); //TODO delta should be proportional to update time interval
		
//		if (this.getCurrentLink().getLink().getId().toString().equals("2") && (this.myDirectionRho+ this.theirDirectionRho) > 2.5 && this.myV > 0.8){
//			System.out.println(this.myDirectionRho + " " + this.theirDirectionRho + " " + (this.myDirectionRho+ this.theirDirectionRho));
//		}
	}

	public void updateMyDynamicQuantitiesOnSwap(double oncommingV, double time, double channelWidth,double rho) {
		this.oncomingAgentsRate = 1 /(time - this.lastOncommingAgent );//TODO maybe we need some smoothing here ...
		this.lastOncommingAgent = time;
		this.oncommingV = oncommingV;//TODO maybe we need some smoothing here ...
		double widthRatio = CANetworkDynamic.PED_WIDTH/channelWidth;
		this.theirDirectionRho = Math.min(CANetworkDynamic.RHO_HAT, widthRatio*this.oncomingAgentsRate/(this.myV+oncommingV));
		
		this.currentDecay = 1;
	}

	public double getLastEnterTime() {
		return this.lastEnterTime;
	}

	@Override
	public double getD() {
		double tmp = Math.pow(this.myDirectionRho*CANetworkDynamic.PED_WIDTH+this.theirDirectionRho*this.currentDecay*CANetworkDynamic.PED_WIDTH, CANetworkDynamic.GAMMA);
		return CANetworkDynamic.ALPHA + CANetworkDynamic.BETA*tmp;
	}
	@Override
	public double getZ() {
		double d = getD();
		double z = d + 1/(CANetworkDynamic.RHO_HAT*CANetworkDynamic.PED_WIDTH*CANetworkDynamic.V_HAT);
		//		if (z < 0) {
		//			System.out.println("err!!");
		//		}
//		if (this.getCurrentLink().getLink().getId().toString().equals("2b")) {
//			System.out.println(z + " " + this.myV + " " + this.myDirectionRho +" " + msaRho);
//		}
		return z;
//				return 0.6;
	}

	public double getV() {
		return this.myV;
	}

	@Override
	public CANetworkEntity getCurrentCANetworkEntity() {
		return this.currentEntity;
	}

	public double getMyDirectionRho() {
		return this.myDirectionRho;
	}

}
