/* *********************************************************************** *
 * project: org.matsim.*
 * BiDirEventsBasedCA.java
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

package playground.gregor.bidirpeds.eventsca;

import java.util.LinkedList;
import java.util.PriorityQueue;

import org.matsim.core.gbl.MatsimRandom;

public class BiDirEventsBasedCA {
	
	private final double D = 0.1;
	private final double z = 0.1;
	private final double v = 0.1;
	
	//TODO own LinkedList implementation to access list nodes (for accessing previous/last element)
	//for now we using the an ArrayList
	
	private final int size = 100;
	
	private final PriorityQueue<Update> events = new PriorityQueue<Update>();
	
	
	
	public static void main(String [] args) {
		BiDirEventsBasedCA ca = new BiDirEventsBasedCA();
		ca.loadInitialDemand();
		ca.run();
	}

	private void run() {
		double time = 0;
		while (this.events.size() > 0 ) {
			Update e = this.events.poll();
			
			double tt = e.getEventTime();
			if (tt < time) {
				continue;
			}
			time = tt;
			
			Particle p = e.getParticle();
			
			Particle next;
			if (p.dir == 1) {
				next = p.rightNeighbor;
			} else { 
				next = p.leftNeighbor;
			}
			
			int dist = getDist(p,next);
			
			
			// +-
			if (next.dir != p.dir) {
			
				if (dist == 1){ // ...+-...
					swap(p,next,time);
				}else { // ...+...-...
					mv(p,dist/2,time);
				}
				
				continue;
			} 
			
			if (dist == 1) { // ...++...
//				Update u = new Update(p,time+this.z);
//				this.events.add(u);
//next needs to inform p				
				continue;
			}
			
			// ....+....+....-
			int mv1 = dist-1;
			
			Particle nextOncomming = next;
			do {
				if (p.dir == -1) {
					nextOncomming = nextOncomming.leftNeighbor;
				} else {
					nextOncomming = nextOncomming.rightNeighbor;
				}
			}while (nextOncomming.dir == p.dir);
			int mv2 = getDist(p,nextOncomming)/2;
			mv(p,Math.min(mv1,mv2),time);
			
		}
		
	}
	
	
	

	private int getDist(Particle p, Particle next) {
		if (p.dir == 1){
			if (next.pos < p.pos) {
				return (next.pos + this.size) - p.pos; 
			}
			return next.pos - p.pos;
		}
//		if ()
		
		return 0;
	}

	private void mv(Particle p, int dist, double time) {
		Particle prev;
		if (p.dir == 1) {
			prev = p.leftNeighbor;
		} else {
			prev = p.rightNeighbor;
		}
		dist = getDist(prev,p);
		if (dist == 1) {
			Update uN = new Update(prev, time+this.z);
			this.events.add(uN);
		}
		p.pos = getIdx(p.pos+dist*p.dir);
		Update uS = new Update(p,time + dist/this.v);
		this.events.add(uS);
		
	}

	private void swap(Particle p, Particle next, double time) {
		// TODO Auto-generated method stub
		
	}

	private int getIdx(int i) {
		if (i > this.size) {
			return i % this.size;
		} if (i < 0) {
			return getIdx(i+this.size);
		}
		
		throw new RuntimeException("impossible!");
	}

	private void loadInitialDemand() {
		LinkedList<Particle> particles = new LinkedList<Particle>();
		Particle prev = null;
		for (int i = 0; i < this.size; i++) {
			if (MatsimRandom.getRandom().nextDouble() < 0.1) {
				Particle p = new Particle();
				p.leftNeighbor = prev;
				p.pos = i;
				p.dir = MatsimRandom.getRandom().nextDouble() < 0.5 ? 1 : -1;
				particles.add(p);
				Update u = new Update(p,0);
				this.events.add(u);
				if (prev != null) {
					prev.rightNeighbor = p;
				}
				prev = p;
			}
			
		}
		prev.rightNeighbor = particles.getFirst();
		particles.getFirst().leftNeighbor = prev;
		
	}

	private static final class Update implements CAEvent, Comparable<Update> {

		private final Particle p;
		private final double time;

		public Update(Particle p, double time) {
			this.p = p;
			this.time = time;
		}
		
		@Override
		public double getEventTime() {
			// TODO Auto-generated method stub
			return 0;
		}
		
		
		public Particle getParticle() {
			return this.p;
		}

		@Override
		public int compareTo(Update arg0) {
			if (this.time < arg0.getEventTime()) {
				return -1;
			} else if (this.time > arg0.getEventTime()) {
				return 1;
			}
			return 0;
		}
	}
	
	private static final class Particle {
		int pos;
		int dir;
		int listPos;
		Particle leftNeighbor;
		Particle rightNeighbor;
	}
}
