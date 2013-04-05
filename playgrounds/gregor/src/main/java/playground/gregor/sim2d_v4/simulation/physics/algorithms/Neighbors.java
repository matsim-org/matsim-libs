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

import org.matsim.core.utils.collections.Tuple;

import playground.gregor.sim2d_v4.cgal.CGAL;
import playground.gregor.sim2d_v4.scenario.Sim2DConfig;
import playground.gregor.sim2d_v4.simulation.physics.PhysicalSim2DSection;
import playground.gregor.sim2d_v4.simulation.physics.PhysicalSim2DSection.Segment;
import playground.gregor.sim2d_v4.simulation.physics.SimpleAgent;

import com.vividsolutions.jts.geom.Envelope;

public class Neighbors {


	private double sqRange = 10;
	private double range = Math.sqrt(this.sqRange);
	private int nrNeighbors = 3;

	List<SimpleAgent> agentsSec = new ArrayList<SimpleAgent>();
	Map<Segment,List<SimpleAgent>> agentsNeigh = new HashMap<Segment,List<SimpleAgent>>();
	private final SimpleAgent agent;

	private List<Tuple<Double,SimpleAgent>> cachedNeighbors;
	private final double dT;
	private double updateIntervall;
	private double timeAfterLastUpdate = Double.POSITIVE_INFINITY;
	
	public Neighbors(SimpleAgent agent, Sim2DConfig conf) {
		this.agent = agent;
		this.dT = conf.getTimeStepSize();
		this.updateIntervall = this.dT;
	}
	private void updateNeighbors() {
		this.agentsSec.clear();
		this.agentsNeigh.clear();
		double[] aPos = this.agent.getPos();
		PhysicalSim2DSection psec = this.agent.getPSec();



		double twoDTreeRange = this.range;
		Envelope e = new Envelope(aPos[0]-twoDTreeRange,aPos[0]+twoDTreeRange,aPos[1]-twoDTreeRange,aPos[1]+twoDTreeRange);
		List<SimpleAgent> agents = psec.getAgents(e);
		this.agentsSec.addAll(agents);


		//agents from neighboring sections
		Segment[] openings = psec.getOpenings();
		for (int i = 0; i < openings.length; i++) {
			Segment opening = openings[i];
			PhysicalSim2DSection qSec = psec.getNeighbor(opening);
			if (qSec == null) {
				continue;
			}

			twoDTreeRange = this.range;
			e = new Envelope(this.agent.getPos()[0]-twoDTreeRange,this.agent.getPos()[0]+twoDTreeRange,this.agent.getPos()[1]-twoDTreeRange,this.agent.getPos()[1]+twoDTreeRange);
			List<SimpleAgent> tmp = qSec.getAgents(e);
			this.agentsNeigh.put(opening, tmp);
			//agents from neighboring sections need to check visibility
		}
	}

	public List<Tuple<Double,SimpleAgent>> getNeighbors() {//TODO consider adding time as attribute [gl April '13]
		this.timeAfterLastUpdate += this.dT;
		if (this.timeAfterLastUpdate >= this.updateIntervall) {
			computeNeighbors();
			this.timeAfterLastUpdate = 0;
		} 
		
		
		return this.cachedNeighbors;
	}
	
	private List<Tuple<Double,SimpleAgent>> computeNeighbors() {
		
		updateNeighbors();
		
		LinkedList<Tuple<Double,SimpleAgent>> ret = new LinkedList<Tuple<Double,SimpleAgent>>();
		int size = 0;

		double[] aPos = this.agent.getPos();
		double currentMaxSqRange = this.sqRange;

		//agents from same section visible by definition
		for (SimpleAgent b : this.agentsSec) {
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
			ListIterator<Tuple<Double,SimpleAgent>> it = ret.listIterator();
			boolean notPlaced = true;
			while(notPlaced) {
				if (!it.hasNext()) {
					it.add(new Tuple<Double,SimpleAgent>(sqRange,b));
					size++;
					notPlaced = false;
				} else {
					Tuple<Double, SimpleAgent> next = it.next();
					if (sqRange < next.getFirst()) {
						it.previous();
						it.add(new Tuple<Double,SimpleAgent>(sqRange,b));
						size++;
						notPlaced = false;
					}
				}
			}
			if (size > this.nrNeighbors) {
				ret.removeLast();
				size = this.nrNeighbors;
				currentMaxSqRange = ret.getLast().getFirst();

			}
		}

		//agents from neighboring sections
		for (Entry<Segment, List<SimpleAgent>> e : this.agentsNeigh.entrySet()) {
			List<SimpleAgent> agents = e.getValue();
			Segment opening = e.getKey();
			//agents from neighboring sections need to check visibility
			for (SimpleAgent b : agents) {
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
				ListIterator<Tuple<Double,SimpleAgent>> it = ret.listIterator();
				boolean notPlaced = true;
				while(notPlaced) {
					if (!it.hasNext()) {
						it.add(new Tuple<Double,SimpleAgent>(sqRange,b));
						size++;
						notPlaced = false;
					} else {
						Tuple<Double, SimpleAgent> next = it.next();
						if (sqRange < next.getFirst()) {
							it.previous();
							it.add(new Tuple<Double,SimpleAgent>(sqRange,b));
							size++;
							notPlaced = false;
						}
					}
				}
				if (size > this.nrNeighbors) {
					ret.removeLast();
					size = this.nrNeighbors;
					currentMaxSqRange = ret.getLast().getFirst();

				}
			}
			//..
		}
		this.cachedNeighbors = ret;
		return ret;

	}

	private boolean visible(double[] aPos, double[] bPos, Segment opening) {
		double l0 = CGAL.isLeftOfLine( opening.x0, opening.y0,aPos[0], aPos[1],bPos[0], bPos[1]);
		double l1 = CGAL.isLeftOfLine( opening.x1, opening.y1,aPos[0], aPos[1],bPos[0], bPos[1]);
		return l0*l1 < 0;
	}

	public void setRangeAndMaxNrOfNeighbors(double range, int nrNeighbors) {
		this.sqRange = range*range;
		this.range = range;
		this.nrNeighbors = nrNeighbors;
	}
	
	public void setUpdateInterval(double intervall) {
		this.updateIntervall = intervall;
	}
}
