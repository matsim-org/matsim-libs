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

import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import org.matsim.core.utils.collections.Tuple;

import playground.gregor.sim2d_v4.cgal.CGAL;
import playground.gregor.sim2d_v4.simulation.physics.PhysicalSim2DSection;
import playground.gregor.sim2d_v4.simulation.physics.PhysicalSim2DSection.Segment;
import playground.gregor.sim2d_v4.simulation.physics.Sim2DAgent;

import com.vividsolutions.jts.geom.Envelope;

public class Neighbors {

	
	private double sqRange = 10;
	private double range = Math.sqrt(this.sqRange);
	private int nrNeighbors = 3;
	
	
	public List<Tuple<Double,Sim2DAgent>> computeNeighbors(Sim2DAgent agent) {
		LinkedList<Tuple<Double,Sim2DAgent>> ret = new LinkedList<Tuple<Double,Sim2DAgent>>();
		int size = 0;
		
		double[] aPos = agent.getPos();
		PhysicalSim2DSection psec = agent.getPSec();
		
		double currentMaxSqRange = this.sqRange;
		
		
		double twoDTreeRange = this.range;
		Envelope e = new Envelope(agent.getPos()[0]-twoDTreeRange,agent.getPos()[0]+twoDTreeRange,agent.getPos()[1]-twoDTreeRange,agent.getPos()[1]+twoDTreeRange);
		List<Sim2DAgent> agents = psec.getAgents(e);
		//agents from same section visible by definition
		for (Sim2DAgent b : agents) {
			if (b.equals(agent)) {
				continue;
			}
			double[] bPos = b.getPos();
			double xDiff = aPos[0] - bPos[0];
			double yDiff = aPos[1] - bPos[1];
			double sqRange = xDiff*xDiff + yDiff*yDiff;
			if (sqRange > currentMaxSqRange) {
				continue;
			}
			ListIterator<Tuple<Double,Sim2DAgent>> it = ret.listIterator();
			boolean notPlaced = true;
			while(notPlaced) {
				if (!it.hasNext()) {
					it.add(new Tuple<Double,Sim2DAgent>(sqRange,b));
					size++;
					notPlaced = false;
				} else {
					Tuple<Double, Sim2DAgent> next = it.next();
					if (sqRange < next.getFirst()) {
						it.previous();
						it.add(new Tuple<Double,Sim2DAgent>(sqRange,b));
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
		Segment[] openings = psec.getOpenings();
		for (int i = 0; i < openings.length; i++) {
			Segment opening = openings[i];
			PhysicalSim2DSection qSec = psec.getNeighbor(opening);
			if (qSec == null) {
				continue;
			}
			
			twoDTreeRange = this.range;
			e = new Envelope(agent.getPos()[0]-twoDTreeRange,agent.getPos()[0]+twoDTreeRange,agent.getPos()[1]-twoDTreeRange,agent.getPos()[1]+twoDTreeRange);
			agents = qSec.getAgents(e);
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
				ListIterator<Tuple<Double,Sim2DAgent>> it = ret.listIterator();
				boolean notPlaced = true;
				while(notPlaced) {
					if (!it.hasNext()) {
						it.add(new Tuple<Double,Sim2DAgent>(sqRange,b));
						size++;
						notPlaced = false;
					} else {
						Tuple<Double, Sim2DAgent> next = it.next();
						if (sqRange < next.getFirst()) {
							it.previous();
							it.add(new Tuple<Double,Sim2DAgent>(sqRange,b));
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
}
