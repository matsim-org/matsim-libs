/* *********************************************************************** *
 * project: org.matsim.*
 * Neighbors.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.gregor.sim2d_v4.simulation.physics.algorithms;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;

import playground.gregor.sim2d_v4.cgal.CGAL;
import playground.gregor.sim2d_v4.cgal.LineSegment;
import playground.gregor.sim2d_v4.scenario.Section;
import playground.gregor.sim2d_v4.scenario.Sim2DConfig;
import playground.gregor.sim2d_v4.simulation.physics.PhysicalSim2DSection;
import playground.gregor.sim2d_v4.simulation.physics.Sim2DAgent;

import com.vividsolutions.jts.geom.Envelope;

public class KDTreeNeighbors implements Neighbors {


	private double sqRange = 10;
	private double range = Math.sqrt(this.sqRange);
	private int nrNeighbors = 3;

	List<Sim2DAgent> agentsSec = new ArrayList<Sim2DAgent>();
	Map<LineSegment,List<Sim2DAgent>> agentsNeigh = new HashMap<LineSegment,List<Sim2DAgent>>();
	private final Sim2DAgent agent;

	private List<Sim2DAgent> cachedNeighbors;
	//	private final double dT;
	//	private double updateIntervall;
	//	private double timeAfterLastUpdate = Double.POSITIVE_INFINITY;

	public KDTreeNeighbors(Sim2DAgent agent, Sim2DConfig conf) {
		this.agent = agent;
		//		this.dT = conf.getTimeStepSize();
		//		this.updateIntervall = this.dT;
	}
	private void updateNeighbors() {
		this.agentsSec.clear();
		this.agentsNeigh.clear();
		double[] aPos = this.agent.getPos();
		PhysicalSim2DSection psec = this.agent.getPSec();


		double twoDTreeRange = this.range;
		Envelope e = new Envelope(aPos[0]-twoDTreeRange,aPos[0]+twoDTreeRange,aPos[1]-twoDTreeRange,aPos[1]+twoDTreeRange);
		List<Sim2DAgent> agents = psec.getAgents(e);
		this.agentsSec.addAll(agents);


		//agents from neighboring sections
		List<LineSegment> openings = psec.getOpeningSegments();
		for (LineSegment opening : openings) {
			
			Section qSec = psec.getNeighbor(opening);
			if (qSec == null) {
				continue;
			}

			twoDTreeRange = this.range;
			e = new Envelope(this.agent.getPos()[0]-twoDTreeRange,this.agent.getPos()[0]+twoDTreeRange,this.agent.getPos()[1]-twoDTreeRange,this.agent.getPos()[1]+twoDTreeRange);
			PhysicalSim2DSection qPSec = this.agent.getPSec().getPhysicalEnvironment().getPhysicalSim2DSection(qSec);
			List<Sim2DAgent> tmp = qPSec.getAgents(e);
			this.agentsNeigh.put(opening, tmp);
			//agents from neighboring sections need to check visibility
		}
	}

	/* (non-Javadoc)
	 * @see playground.gregor.sim2d_v4.simulation.physics.algorithms.Neighbors#getNeighbors(double)
	 */
	@Override
	public List<Sim2DAgent> getNeighbors() {

		//		//DEBUG!!!!
		//		if (this.agent.getId().equals(new IdImpl("b129"))) {
		//			System.out.println(this.agent);
		//		}
		//		
		//		this.timeAfterLastUpdate += this.dT;
		//		if (this.timeAfterLastUpdate >= this.updateIntervall) {
		computeNeighbors();
		//			this.timeAfterLastUpdate = 0;
		//		} 

		//DEBUG!!!!
		//		if (this.agent.getId().equals(new IdImpl("b111"))) {
		//			this.agent.getPSec().getPhysicalEnvironment().getEventsManager().processEvent(new NeighborsEvent(time, this.agent.getId(), this.cachedNeighbors, this.agent));
		//		}

		return this.cachedNeighbors;
	}

	private List<Sim2DAgent> computeNeighbors() {

		updateNeighbors();

		LinkedList<Sim2DAgent> ret = new LinkedList<Sim2DAgent>();
		LinkedList<Double> sqrRange = new LinkedList<Double>();
		int size = 0;

		double[] aPos = this.agent.getPos();
		double currentMaxSqRange = this.sqRange;

		//agents from same section visible by definition
		for (Sim2DAgent b : this.agentsSec) {
			if (b.equals(this.agent)) {
				continue;
			}
			double[] bPos = b.getPos();
			double xDiff = aPos[0] - bPos[0];
			double yDiff = aPos[1] - bPos[1];
			double sqRange = xDiff*xDiff + yDiff*yDiff;
			if (sqRange > currentMaxSqRange) {
				continue;
			}
			ListIterator<Sim2DAgent> it = ret.listIterator();
			ListIterator<Double> sqrRangeIt = sqrRange.listIterator();

			boolean notPlaced = true;
			while(notPlaced) {
				if (!it.hasNext()) {
					it.add(b);
					sqrRangeIt.add(sqRange);
					size++;
					notPlaced = false;
				} else {
					Sim2DAgent next = it.next();
					double nextSqrRange = sqrRangeIt.next();
					if (sqRange < nextSqrRange) {
						it.previous();
						sqrRangeIt.previous();
						it.add(b);
						sqrRangeIt.add(sqRange);
						size++;
						notPlaced = false;
					}
				}
			}
			if (size > this.nrNeighbors) {
				ret.removeLast();

				sqrRange.removeLast();
				size = this.nrNeighbors;
				currentMaxSqRange = sqrRange.getLast();

			}
		}

		//agents from neighboring sections
		for (Entry<LineSegment, List<Sim2DAgent>> e : this.agentsNeigh.entrySet()) {
			List<Sim2DAgent> agents = e.getValue();
			LineSegment opening = e.getKey();
			//agents from neighboring sections need to check visibility
			for (Sim2DAgent b : agents) {
				double[] bPos = b.getPos();
				if (!visible(aPos,bPos,opening)) {
					continue;
				}
				double xDiff = aPos[0] - bPos[0];
				double yDiff = aPos[1] - bPos[1];
				double sqRange = xDiff*xDiff + yDiff*yDiff;
				if (sqRange > currentMaxSqRange) {
					continue;
				}
				ListIterator<Sim2DAgent> it = ret.listIterator();
				ListIterator<Double> sqrRangeIt = sqrRange.listIterator();
				boolean notPlaced = true;
				while(notPlaced) {
					if (!it.hasNext()) {
						it.add(b);
						sqrRangeIt.add(sqRange);
						size++;
						notPlaced = false;
					} else {
						Sim2DAgent next = it.next();
						double nextSqrRange = sqrRangeIt.next();
						if (sqRange < nextSqrRange) {
							it.previous();
							sqrRangeIt.previous();
							it.add(b);
							sqrRangeIt.add(sqRange);
							size++;
							notPlaced = false;
						}
					}
				}
				if (size > this.nrNeighbors) {
					ret.removeLast();
					sqrRange.removeLast();
					size = this.nrNeighbors;
					currentMaxSqRange = sqrRange.getLast();

				}
			}
			//..
		}
		this.cachedNeighbors = ret;

//		if (this.agent.getId().toString().endsWith("57")){
//			this.agent.getPSec().getPhysicalEnvironment().getEventsManager().processEvent(new NeighborsEvent(0, this.agent.getId(), ret, this.agent));
//		}

		return ret;

	}

	private boolean visible(double[] aPos, double[] bPos, LineSegment opening) {
		double l0 = CGAL.isLeftOfLine( opening.x0, opening.y0,aPos[0], aPos[1],bPos[0], bPos[1]);
		double l1 = CGAL.isLeftOfLine( opening.x1, opening.y1,aPos[0], aPos[1],bPos[0], bPos[1]);
		return l0*l1 < 0;
	}

	public void setRangeAndMaxNrOfNeighbors(double range, int nrNeighbors) {
		this.sqRange = range*range;
		this.range = range;
		this.nrNeighbors = nrNeighbors;
	}

	//	public void setUpdateInterval(double intervall) {
	//		this.updateIntervall = intervall;
	//	}
}
