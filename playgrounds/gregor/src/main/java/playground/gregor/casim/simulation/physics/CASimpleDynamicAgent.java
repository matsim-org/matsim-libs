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

import java.util.ArrayList;
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
	private double myV = CANetworkDynamic.V_HAT;

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


	public void updateMyDynamicQuantitiesOnAdvance(double  gap, double time, double cellLength,double channelWidth) {
		
//		if (this.getId().toString().equals("246") && this.getCurrentLink().getLink().getId().toString().equals("2")) {
//			System.out.println("got you!!!");
//		}

		double tt = time - this.lastEnterTime;
		if (tt < 0.0001) { //TODO use here tFree instead
			this.lastEnterTime = time;
			return;
		}


		this.myV = cellLength/tt;//TODO maybe we need some smoothing here ...

		double flow = 1/(channelWidth*gap);
		this.myDirectionRho = flow/this.myV;
		
		
//		if (pred != null && pred.getCurrentCANetworkEntity() == this.getCurrentCANetworkEntity()) {
////			System.out.println(this.getId() + " " + pred.getId());
//			
//			int cells = (Math.abs(pred.getPos()-getPos()));
//			if (cells <= 2) {
//				cells -= .25;
//			}
////			else if (cells <=3) {
////				cells -= 1;
////			}
//			
//			double dist = cells*cellLength;
//		
//			double persSpace = dist*channelWidth;
//			this.myDirectionRho = 1/persSpace;
////			if (this.myDirectionRho > 4 ) {
////				System.out.println("myRho" + this.myDirectionRho);
////			}
//			
//		}
		
//		double predecessorDist = 1*this.myV;

//		double tmp = 1/(predecessorDist*channelWidth);
		
		
//		this.myDirectionRho = tmp;

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
//		this.theirDirectionRho = Math.min(CANetworkDynamic.RHO_HAT, widthRatio*this.oncomingAgentsRate/(this.myV+oncommingV));
		
		this.currentDecay = 1;
	}

	public double getLastEnterTime() {
		return this.lastEnterTime;
	}

	@Override
	public double getD() {
		double tmp = Math.pow(this.getMyDirectionRho()*CANetworkDynamic.PED_WIDTH+this.getTheirDirectionRho()*this.currentDecay*CANetworkDynamic.PED_WIDTH, CANetworkDynamic.GAMMA);
		return CANetworkDynamic.ALPHA + CANetworkDynamic.BETA*tmp;
	}
	@Override
	public double getZ() {
		double d = getD();
		double z = d + 1/(CANetworkDynamic.RHO_HAT*CANetworkDynamic.V_HAT);
//		double z = d + 1/(CANetworkDynamic.RHO_HAT*CANetworkDynamic.V_HAT);

//		double gEq = 1/(this.myV*CANetworkDynamic.RHO_HAT);
//		if (z < gEq ) {
//			
////			System.out.println(z + " " + gEq);
//			return Math.min(gEq,CANetworkDynamic.MAX_Z);
//		}
		
		return 0.61;
//		return Math.min(0.5, z);
	}

	public double getV() {
		return this.myV;
	}

	@Override
	public CANetworkEntity getCurrentCANetworkEntity() {
		return this.currentEntity;
	}

	public double getMyDirectionRho() {
		if (this.currentEntity instanceof CALinkDynamic && false) {
			int d = this.getDir();
			int p = this.getPos();
			
			double w = ((CALinkDynamic) this.currentEntity).getWidth();
			
			List<Double> spacings = new ArrayList<Double>();
			int idx = p;
			int last = idx;
			//forward
			while (spacings.size() < 1) {
				idx += d;
				if (idx < 0 || idx >= ((CALinkDynamic) this.currentEntity).getNumOfCells()){
					break;
				}
				CAAgent part = ((CALinkDynamic) this.currentEntity).getParticles()[idx];
				if (part != null) {
					if (part.getDir() == d) {
						int spaces = Math.abs(idx-last);
						double dist = spaces * ((CALinkDynamic) this.currentEntity).getCellLength();
						spacings.add(dist);
						last = idx;
					}
				}
			}
			int sz = spacings.size();
			//backward
			idx = p;
			last = idx;
			while (spacings.size() < sz) {
				idx -= d;
				if (idx < 0 || idx >= ((CALinkDynamic) this.currentEntity).getNumOfCells()){
					break;
				}
				CAAgent part = ((CALinkDynamic) this.currentEntity).getParticles()[idx];
				if (part != null) {
					if (part.getDir() == d) {
						int spaces = Math.abs(idx-last);
						double dist = spaces * ((CALinkDynamic) this.currentEntity).getCellLength();
						spacings.add(dist);
						last = idx;
					}
				}
			}
			
			if (spacings.size() == 0) {
				return 0;
			}
			

			double rho = 0;
//			if (((CALinkDynamic) this.currentEntity).getLink().getId().toString().equals("2") && spacings.size() > 2){
//				System.out.println("got you!!!");
//			}
			double spSum = 0;
//			for (Double sp : spacings) {
//				rho += 1/(w*sp);
//			}
			for (Double sp : spacings) {
				spSum += (w*sp);
			}			
			
			rho = spacings.size()/(spSum);
//			rho /= spacings.size();
			this.myDirectionRho = 0.9*this.myDirectionRho + .1*rho;
//			System.out.println(this.myDirectionRho);
//			this.myDirectionRho;
			
		} 
		return this.myDirectionRho;
	}
	
	public double getTheirDirectionRho() {
		if (this.currentEntity instanceof CALinkDynamic && false) {
			int d = this.getDir();
			int p = this.getPos();
			
			double w = ((CALinkDynamic) this.currentEntity).getWidth();
			
			List<Double> spacings = new ArrayList<Double>();
			int idx = p;
			int last = idx;
			//forward
			while (spacings.size() < 2) {
				idx += d;
				if (idx < 0 || idx >= ((CALinkDynamic) this.currentEntity).getNumOfCells()){
					break;
				}
				CAAgent part = ((CALinkDynamic) this.currentEntity).getParticles()[idx];
				if (part != null) {
					if (part.getDir() == -d) {
						int spaces = Math.abs(idx-last);
						double dist = spaces * ((CALinkDynamic) this.currentEntity).getCellLength();
						spacings.add(dist);
						last = idx;
					}
				}
			}
			int sz = spacings.size();
			//backward
			idx = p;
			last = idx;
			while (spacings.size() < sz+1) {
				idx -= d;
				if (idx < 0 || idx >= ((CALinkDynamic) this.currentEntity).getNumOfCells()){
					break;
				}
				CAAgent part = ((CALinkDynamic) this.currentEntity).getParticles()[idx];
				if (part != null) {
					if (part.getDir() == -d) {
						int spaces = Math.abs(idx-last);
						double dist = spaces * ((CALinkDynamic) this.currentEntity).getCellLength();
						spacings.add(dist);
						last = idx;
					}
				}
			}
			
			if (spacings.size() == 0) {
				return 0;
			}
			

			double rho = 0;
//			if (((CALinkDynamic) this.currentEntity).getLink().getId().toString().equals("2") && spacings.size() > 2){
//				System.out.println("got you!!!");
//			}
			for (Double sp : spacings) {
				rho += 1/(w*sp);
			}
			
			rho /= spacings.size();
			this.theirDirectionRho = 0.9*this.theirDirectionRho + 0.1*rho;
//			System.out.println(this.myDirectionRho);
//			this.myDirectionRho;
			
		} 
		return this.theirDirectionRho;
	}
	
	@Override
	public double getCumWaitTime() {
		double ret = this.cumWaitTime;
		this.cumWaitTime = 0;
//		if (ret > 0) {
//			System.out.println(ret);
//		}
//		return ret;
		return 0;
	}

	@Override
	public void setCumWaitTime(double tFree) {
		this.cumWaitTime = tFree;
		this.myV = 0;
	}

}
