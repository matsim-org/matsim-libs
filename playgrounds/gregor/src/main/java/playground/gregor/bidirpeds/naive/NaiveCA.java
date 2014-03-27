/* *********************************************************************** *
 * project: org.matsim.*
 * NaiveCA.java
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

package playground.gregor.bidirpeds.naive;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.PriorityQueue;

import org.matsim.core.gbl.MatsimRandom;



public class NaiveCA {

	private final double WARMUP = 250;

	private static final int SIZE = 100;
	
	
	private static final double alpha = 0;
	private static final double beta = 0.12;
	private static final double gamma = 1.38;

	private final static double vHat = 1.26; //cells per unit time
	private final static double rhoHat = 5.091; //particles per cell
	private final double D = 0.72;
	private final double z = this.D+1/(NaiveCA.vHat*NaiveCA.rhoHat);

	private final double length = SIZE * 1/NaiveCA.rhoHat;  
	
	private static final String TTA = "tta";
	private static final String SWAP = "swap";
	


	private final PriorityQueue<Event> events = new PriorityQueue<Event>();

	private final Particle [] particles = new Particle[SIZE];



	private void runConstD(Measurement m) {

		double lrT = 0;
		double rlT = 0;
		double tt = 0;
		
		boolean lrDep = false;
		boolean rlDep = false;
		while (this.events.size() > 0) {
			Event e = this.events.poll();
			double time = e.getEventTime();

			if (time >= this.WARMUP) {
				if (lrT >= this.WARMUP) {
					if (this.particles[1] != null && this.particles[1].id == -2) {
						lrDep = true;
					}
				}
				if (rlT >= this.WARMUP) {
					if (this.particles[1] != null && this.particles[1].id == -1) {
						rlDep = true;
					}
				}
								
				
				if (this.particles[0] != null && this.particles[0].id == -2 && m.vLR == 0) {
					if (lrT == 0) {
						lrT = time;
					} else if (lrDep){
						double travel = time - lrT;
						double speed = this.length/travel;
						m.vLR = speed;
						m.JLR = speed*m.rhoLR;
					}
				}
				if (this.particles[SIZE-1] != null && this.particles[SIZE-1].id == -1 && m.vRL == 0) {
					if (rlT == 0) {
						rlT = time;
					} else if (rlDep){
						double travel = time - rlT;
						double speed = this.length/travel;
						m.vRL = speed;
						m.JRL = speed*m.rhoRL;
					}
				}
				if (m.vRL != 0 && m.vLR != 0) {
					return;
				}
			}
//
//
//						if (time > tt) {
//							//DEBUG
//							for (int i =0 ; i < SIZE; i++) {
//								if (this.particles[i] != null) {
//									if (this.particles[i].id == -1) {
//										System.out.print("*");
//									} else if (this.particles[i].id == -2){
//										System.out.print("$");
//									}else if (this.particles[i].dir == 1) {
//										System.out.print("+");
//									} else {
//										System.out.print("-");
//									}
//								} else {
//									System.out.print(" ");
//								}
//							}
//							System.out.println(time);
//							try {
//								Thread.sleep(10);
//							} catch (InterruptedException e1) {
//								e1.printStackTrace();
//							}
//							tt = time;
//						}


			Particle p = e.p;

			if (e.getType().equals(SWAP)) { // +-
				int nbIdx = getIdx(p.pos+p.dir);
				int idx = p.pos;
				Particle nb = this.particles[nbIdx];
				this.particles[nbIdx] = p;
				p.pos = nbIdx;
				this.particles[idx] = nb;
				nb.pos = idx;

				int nextNextP = getIdx(p.pos+p.dir);

				if (this.particles[nextNextP] != null){ 
					if(this.particles[nextNextP].dir != p.dir) { // +--
						Event uP = new Event(p,time+this.D + 1/(NaiveCA.vHat*NaiveCA.rhoHat),SWAP);
						this.events.add(uP);		
					}
				} else { // +-_
					Event uP = new Event(p, time+1/(NaiveCA.vHat*NaiveCA.rhoHat), TTA);
					this.events.add(uP);
				}

				int nextNextNB = getIdx(nb.pos+nb.dir);
				if (this.particles[nextNextNB] != null) {
					if (this.particles[nextNextNB].dir != nb.dir){ // ++-
						Event uNB = new Event(nb,time+this.D + 1/(NaiveCA.vHat*NaiveCA.rhoHat),SWAP);
						this.events.add(uNB);
					}
				} else { // _+-
					Event uNB = new Event(nb, time+1/(NaiveCA.vHat*NaiveCA.rhoHat), TTA);
					this.events.add(uNB);					
				}
			} else if (e.getType().equals(TTA)) {
				int nextIdx = getIdx(p.pos+p.dir);
				if (this.particles[nextIdx] == null){ // +_
					int prevIdx = getIdx(p.pos-p.dir);
					this.particles[p.pos] = null;
					p.pos = nextIdx;
					this.particles[nextIdx] = p;

					int nextNextIdx = getIdx(p.pos+p.dir);
					if (this.particles[nextNextIdx] != null) {
						if (this.particles[nextNextIdx].dir != p.dir){ // +_-
							Event uP = new Event(p,time+this.D + 1/(NaiveCA.vHat*NaiveCA.rhoHat),SWAP);
							this.events.add(uP);		
						}
					} else {	// +__				
						Event uP = new Event(p, time+1/(NaiveCA.vHat*NaiveCA.rhoHat), TTA);
						this.events.add(uP);
					}
					if (this.particles[prevIdx] != null && this.particles[prevIdx].dir == p.dir) { // ++_
						Event uNB = new Event(this.particles[prevIdx],time + this.z+1/(NaiveCA.vHat*NaiveCA.rhoHat),TTA);
						this.events.add(uNB);
					}
				}
			}
		}

	}

	private void runVarD(Measurement m) {

		double lrT = 0;
		double rlT = 0;
		double tt = 0;
		
		boolean lrDep = false;
		boolean rlDep = false;
		while (this.events.size() > 0) {
			Event e = this.events.poll();
			double time = e.getEventTime();

			if (time >= this.WARMUP) {
				if (lrT >= this.WARMUP) {
					if (this.particles[1] != null && this.particles[1].id == -2) {
						lrDep = true;
					}
				}
				if (rlT >= this.WARMUP) {
					if (this.particles[1] != null && this.particles[1].id == -1) {
						rlDep = true;
					}
				}
								
				
				if (this.particles[0] != null && this.particles[0].id == -2 && m.vLR == 0) {
					if (lrT == 0) {
						lrT = time;
					} else if (lrDep){
						double travel = time - lrT;
						double speed = this.length/travel;
						m.vLR = speed;
						m.JLR = speed*m.rhoLR;
					}
				}
				if (this.particles[SIZE-1] != null && this.particles[SIZE-1].id == -1 && m.vRL == 0) {
					if (rlT == 0) {
						rlT = time;
					} else if (rlDep){
						double travel = time - rlT;
						double speed = this.length/travel;
						m.vRL = speed;
						m.JRL = speed*m.rhoRL;
					}
				}
				if (m.vRL != 0 && m.vLR != 0) {
					return;
				}
			}
//
//
//						if (time > tt) {
//							//DEBUG
//							for (int i =0 ; i < SIZE; i++) {
//								if (this.particles[i] != null) {
//									if (this.particles[i].id == -1) {
//										System.out.print("*");
//									} else if (this.particles[i].id == -2){
//										System.out.print("$");
//									}else if (this.particles[i].dir == 1) {
//										System.out.print("+");
//									} else {
//										System.out.print("-");
//									}
//								} else {
//									System.out.print(" ");
//								}
//							}
//							System.out.println(time);
//							try {
//								Thread.sleep(10);
//							} catch (InterruptedException e1) {
//								e1.printStackTrace();
//							}
//							tt = time;
//						}


			Particle p = e.p;

			if (e.getType().equals(SWAP)) { // +-
				int nbIdx = getIdx(p.pos+p.dir);
				int idx = p.pos;
				Particle nb = this.particles[nbIdx];
				this.particles[nbIdx] = p;
				p.pos = nbIdx;
				this.particles[idx] = nb;
				nb.pos = idx;

				int nextNextP = getIdx(p.pos+p.dir);

				if (this.particles[nextNextP] != null){ 
					if(this.particles[nextNextP].dir != p.dir) { // +--
						double d = getD(p);
						Event uP = new Event(p,time+d + 1/(NaiveCA.vHat*NaiveCA.rhoHat),SWAP);
						this.events.add(uP);		
					}
				} else { // +-_
					Event uP = new Event(p, time+1/(NaiveCA.vHat*NaiveCA.rhoHat), TTA);
					this.events.add(uP);
				}

				int nextNextNB = getIdx(nb.pos+nb.dir);
				if (this.particles[nextNextNB] != null) {
					if (this.particles[nextNextNB].dir != nb.dir){ // ++-
						double d = getD(p);
						Event uNB = new Event(nb,time+d + 1/(NaiveCA.vHat*NaiveCA.rhoHat),SWAP);
						this.events.add(uNB);
					}
				} else { // _+-
					Event uNB = new Event(nb, time+1/(NaiveCA.vHat*NaiveCA.rhoHat), TTA);
					this.events.add(uNB);					
				}
			} else if (e.getType().equals(TTA)) {
				int nextIdx = getIdx(p.pos+p.dir);
				if (this.particles[nextIdx] == null){ // +_
					int prevIdx = getIdx(p.pos-p.dir);
					this.particles[p.pos] = null;
					p.pos = nextIdx;
					this.particles[nextIdx] = p;

					int nextNextIdx = getIdx(p.pos+p.dir);
					if (this.particles[nextNextIdx] != null) {
						if (this.particles[nextNextIdx].dir != p.dir){ // +_-
							double d = getD(p);
							Event uP = new Event(p,time+d + 1/(NaiveCA.vHat*NaiveCA.rhoHat),SWAP);
							this.events.add(uP);		
						}
					} else {	// +__				
						Event uP = new Event(p, time+1/(NaiveCA.vHat*NaiveCA.rhoHat), TTA);
						this.events.add(uP);
					}
					if (this.particles[prevIdx] != null && this.particles[prevIdx].dir == p.dir) { // ++_
						Event uNB = new Event(this.particles[prevIdx],time + this.z+1/(NaiveCA.vHat*NaiveCA.rhoHat),TTA);
						this.events.add(uNB);
					}
				}
			}
		}

	}
	
	private double getD(Particle p) {
		int cntBeh = 2;
		int idxBeh = getIdx(p.pos-p.dir);
		while (this.particles[idxBeh] == null) {
			cntBeh++;
			idxBeh = getIdx(idxBeh-p.dir);
		}
		int cntInFront = 2;
		int idxInFront = getIdx(p.pos+2*p.dir);
		while (this.particles[idxInFront] == null) {
			cntInFront++;
			idxInFront = getIdx(idxInFront+p.dir);
		}
		double dens = rhoHat*4./(cntBeh+cntInFront);
		
		return alpha + beta * Math.pow(dens, gamma);
	}

	private int getIdx(int i) {
		if (i < 0) {
			return i+SIZE;
		}
		if (i >= SIZE) {
			return i-SIZE;
		}
		return i;
	}


	public static void main (String [] args) throws IOException  {

		BufferedWriter bf = new BufferedWriter(new FileWriter(new File("/Users/laemmel/devel/bipedca/fnd_naive_var")));

		for (double rho = 0.01; rho < rhoHat; rho += 0.1) {
			double incr = 0.01*0.01/rho;
			for (double split = 0.01; split < 0.99; split += incr) {
				
				for (int rep = 0; rep < 1; rep++) {
					boolean succ = false;
					while (!succ) {
					NaiveCA ca = new NaiveCA();
					//				double rho = 0.5;
					//				double split = 0.1;
//					rho = 0.5;
//					split = 0.1;
					double rhoLR = rho*(split);
					double rhoRL = rho*(1-split);
					Measurement m = new Measurement();
					m.rho = rho;
					m.rhoLR = rhoLR;
					m.rhoRL = rhoRL;
					
					
					succ = ca.loadInitialDemand(split,rho/rhoHat);
					if (!succ) {
						continue;
					}
					ca.runVarD(m);
					m.v = (m.vLR*m.rhoLR + m.vRL * m.rhoRL)/m.rho;
//					m.J = m.v*m.rho;
					m.J = m.JLR + m.JRL;

//									System.out.println(m);				
					bf.append(m.toString());
					};
				}
			}
		}
		bf.close();


	}





	private boolean loadInitialDemand(double split, double rho) {

		
		boolean lr = false;
		boolean rl = false;

		for (int i = 1; i < SIZE; i++) {
			if (MatsimRandom.getRandom().nextDouble() < rho) {
				Particle p = new Particle();
				p.pos = i;
				p.dir = MatsimRandom.getRandom().nextDouble() < split ? 1 : -1;
				
				if (p.dir == 1 && !lr) {
					p.id = -2;
					lr = true;
				} else if (p.dir == -1 && !rl){
					p.id = -1;
					rl = true;
				}
				
				this.particles[i] = p;

				if (p.dir == -1 && this.particles[i-1] != null && this.particles[i-1].dir == 1) {
					Event e = new Event(p,0,SWAP);
					this.events.add(e);
				} else {
					Event e = new Event(p,0,TTA);
					this.events.add(e);
				}
			}

		}
		
		return rl && lr;

	}

	private static final class Particle {
		//DEBUG
		int id;

		int pos;
		int dir;

		@Override
		public String toString() {
			return "pos:" + this.pos + "  dir:" + this.dir;
		}
	}

	private static final class Event implements Comparable<Event> {

		private final double time;
		private final Particle p;
		private final String type;

		public Event(Particle p, double time, String type) {
			this.p = p;
			this.time = time;
			this.type = type;
		}

		public String getType(){
			return this.type;
		}

		public double getEventTime() {
			return this.time;
		}

		@Override
		public int compareTo(Event o) {
			if (this.time < o.getEventTime()){
				return -1;
			} else if (this.time > o.getEventTime()) {
				return 1;
			}
			return 0;
		}

		@Override
		public String toString() {
			return this.type;
		}

	}

	private static final class Measurement {
		double rho;
		double v;
		double J;
		double rhoRL;
		double rhoLR;
		double vRL;
		double JRL;
		double vLR;
		double JLR;

		@Override
		public String toString() {

			return this.rho + " " + this.rhoLR + " " + this.rhoRL + " " + this.v + " " + this.J + " " + this.vLR + " " + this.JLR + " " + this.vRL + " " + this.JRL +"\n";
		}
	}

}
