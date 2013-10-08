/* *********************************************************************** *
 * project: org.matsim.*
 * BiDirPedsCA.java
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

package playground.gregor.bidirpeds.ca;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;





public class BiDirPedsCA {


	private  long last = 0;

	private final Random r = new Random();

	private final double maxSpeed = 1.34;
	private final double L = .19;
	private final double z = .5; 


	private final double minCellTravelTime = this.L/this.maxSpeed;
	private final double conflictDelay = 0.5;//0.5+this.minCellTravelTime;

	private static final int CELLS = 200;

	private final Cell [] cells = new Cell[CELLS+2];
	private final Cell [] tmpCells = new Cell[CELLS+2];

	private static final double MAX_TIME = 1000;

	private static final double WARMUP_TIME = 100;

	private static final boolean VISUALIZE = false;

	public BiDirPedsCA() {

	}

	public void run() {

		int idx1 = 0;
		for (double rho1 = 0; rho1 <= 1; rho1 += 0.005) {
			idx1++;
			int idx2 = 0;
			for (double rho2 = 0; rho2 <= 1; rho2 += 0.005) {
				idx2++;
				double rho = rho1+rho2;
				if (rho > 1) {
					break;
				}
				double rrho = rho/this.L;
				double rrho1 = (rho1)/this.L;
				double rrho2 = (rho2)/this.L;




				//initialization
				for (int i = 0; i < CELLS+2; i++) {
					this.cells[i] = new Cell();
					this.tmpCells[i] = new Cell();
				}






				//demand
				ArrayList<Integer> idxs = new ArrayList<Integer>();
				for (int i = 0; i < CELLS+2; i++) {
					idxs.add(i);
				}
				Collections.shuffle(idxs);

				int numPeds = 0;


				double threshold = (CELLS*rho) * rho1/(rho1+rho2);
				for (int i = 1 ; i <= CELLS*rho; i ++) {
					//			if (this.r.nextDouble()<0.45) {
					int dir = i <= threshold ? 1 : -1;
					this.cells[idxs.get(i)].ped = new Ped(numPeds,dir);
					numPeds++;
					//			}
				}
//				System.out.println(threshold + " " + numPeds);

				//				for (int i = CELLS-1 ; i < CELLS+1; i++) {
				//					//			this.cells[i].ped = new Ped(i,-1);
				//				}


				double t = 0;


				Map<Ped,Double> pedMap = new HashMap<Ped,Double>();


				int m = 0;
				int mLR = 0;
				int mRL = 0;
				double msaTT = 0;
				double msaTTLR = 0;
				double msaTTRL = 0;
				while (t < MAX_TIME) {



//					if (VISUALIZE) {
//						StringBuffer strBuff = new StringBuffer();
//						strBuff.append(t);
//						strBuff.append(' ');
//						ArrayList<Integer> peds = new ArrayList<Integer>();
//						for (int i = 0; i < numPeds; i++) {
//							peds.add(0);
//						}
//
//						for (int i = 1; i <= CELLS; i++ ) {
//							Ped ped = this.cells[i].ped;
//							if (ped != null) {
//								peds.set(Integer.parseInt(ped.id), i);
//							}
//						}
//						for (int i : peds) {
//							strBuff.append(i+" ");
//						}
//						strBuff.append("\n");
//						try {
//							bw.append(strBuff);
//						} catch (IOException e1) {
//							e1.printStackTrace();
//						}
//					}

					double nextT = this.minCellTravelTime+t;//latest next update

					//update periodic boundary conditions
					this.cells[0].lastEnter = this.cells[CELLS].lastEnter;
					this.cells[0].lastExit = this.cells[CELLS].lastExit;
					this.cells[0].ped = this.cells[CELLS].ped;
					this.cells[CELLS+1].lastEnter = this.cells[1].lastEnter;
					this.cells[CELLS+1].lastExit = this.cells[1].lastExit;
					this.cells[CELLS+1].ped = this.cells[1].ped;


					//update dynamics
					for (int i = 1; i <= CELLS; i++) {
						double earliestUpdate = updateCell(i,t);
						if (nextT > earliestUpdate) {
							nextT = earliestUpdate;
						}
					}
					double incr = (nextT - t)*1000;
					t = nextT;

					StringBuffer buf = new StringBuffer();
					buf.append('\r');


					Ped cand = this.cells[1].ped;
					if (t > WARMUP_TIME && cand != null && (this.tmpCells[1].ped == null||this.tmpCells[1].ped != cand)) {
						Double lastRound = pedMap.get(cand);
						if (lastRound != null) {
							double tt = t-lastRound;
							double tmp0 = m/(m+1.) * msaTT;
							double tmp1 = 1./(m+1.)*tt;
							msaTT = tmp0 + tmp1; 
							m++;
							if (cand.dir == 1) {
								double tmp0LR = mLR/(mLR+1.) * msaTTLR;
								double tmp1LR = 1./(mLR+1.)*tt;
								msaTTLR = tmp0LR + tmp1LR; 
								mLR++;
							} else {
								double tmp0RL = mRL/(mRL+1.) * msaTTRL;
								double tmp1RL = 1./(mRL+1.)*tt;
								msaTTRL = tmp0RL + tmp1RL; 
								mRL++;
							}							
							//					System.out.println(msaTT);
						}
						pedMap.put(cand, t);
					}

					for (int i = 1; i <= CELLS; i++) {
						this.cells[i].lastEnter = this.tmpCells[i].lastEnter;
						this.cells[i].lastExit = this.tmpCells[i].lastExit;
						this.cells[i].ped = this.tmpCells[i].ped;
						this.tmpCells[i] = new Cell();
						buf.append(this.cells[i]+"|");
					}


					if (VISUALIZE) {
						System.out.print(buf.toString());
						//					the next block synchronizes the simulation with the real time
						long time = System.currentTimeMillis();
						if (time-this.last < incr) {
							try {
								long diff = (time-this.last);
								long sleep = (long) (incr-diff);
								Thread.sleep(sleep/10);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}				
						}
						this.last = System.currentTimeMillis();
					}

				}
				//				try {
				//					bw.close();
				//				} catch (IOException e) {
				//					e.printStackTrace();
				//				}
				double v = this.L/(msaTT/CELLS);

				double vRL = this.L/(msaTTRL/CELLS);
				double vLR = this.L/(msaTTLR/CELLS);
				if (msaTTRL == 0) {
					vRL = 0;
				}
				if (msaTTLR == 0) {
					vLR = 0;
				}
				
				double RLFlow = rrho2*vRL;
				double LRFlow = rrho1*vLR;
				double totalFlow = RLFlow + LRFlow;
				System.out.println(rrho + "," + rrho1 + " " + rrho2 +  " " + v + " " + totalFlow + " " + vLR + " " + rrho1*vLR + " " + vRL + " " + rrho2*vRL + " " + idx1 + " " + idx2);

			}
		}
	}


	private double updateCell(int i,double time) {


		Cell curr = this.cells[i];
		if (curr.ped == null) {
			//			this.tmpCells[i].ped = curr.ped;
			//			this.tmpCells[i].lastEnter = curr.lastEnter;
			this.tmpCells[i].lastExit = curr.lastExit;
			return Double.POSITIVE_INFINITY;
		}
		double entered = curr.lastEnter;

		//ped has to wait
		if (entered+this.minCellTravelTime > time) {
			this.tmpCells[i].ped = curr.ped;
			this.tmpCells[i].lastEnter = curr.lastEnter;
			this.tmpCells[i].lastExit = curr.lastExit;
			return entered+this.minCellTravelTime; 
		}

		Ped ped = curr.ped;

		int idx = i+ped.dir;
		if (idx == 0) {
			idx = CELLS;
		} else if (idx == CELLS+1) {
			idx = 1;
		}


		Cell next = this.cells[idx];

		//conflict: oncoming pedestrians
		if (next.ped != null && ((next.ped.dir == curr.ped.dir) || (next.lastEnter + this.minCellTravelTime + this.conflictDelay > time) || (curr.lastEnter + this.minCellTravelTime + this.conflictDelay > time) || (next.lastExit + this.z > time) ||(curr.lastExit + this.z > time) )) {//hook for swap
			this.tmpCells[i].ped = curr.ped;
			this.tmpCells[i].lastEnter = curr.lastEnter;
			this.tmpCells[i].lastExit = curr.lastExit;
			return Double.POSITIVE_INFINITY; 
		}

		Cell nextTmp = this.tmpCells[idx];
		//min time gap not reached
		if (next.lastExit + this.z > time) {
			this.tmpCells[i].ped = curr.ped;
			this.tmpCells[i].lastEnter = curr.lastEnter;
			this.tmpCells[i].lastExit = curr.lastExit;
			nextTmp.lastExit=next.lastExit;
			return next.lastExit + this.z; 
		}

		//conflict: two (oncoming) pedestrians want to enter the same cell
		if (nextTmp.ped != null) {
			if (this.r.nextBoolean()) {
				int idx2 = idx+ped.dir;
				if (idx2 == 0) {
					idx2 = CELLS;
				} else if (idx2 == CELLS+1) {
					idx2 = 1;
				}
				this.tmpCells[idx2].ped = nextTmp.ped;
				this.tmpCells[idx2].lastEnter = 0;
			} else {
				this.tmpCells[i].ped = curr.ped;
				this.tmpCells[i].lastEnter = curr.lastEnter;
				this.tmpCells[i].lastExit = curr.lastExit;
				return time + this.z;
			}
		}
		nextTmp.lastEnter = time;
		nextTmp.ped = ped;
		Cell currTmp = this.tmpCells[i];
		currTmp.lastExit = time;
		return time + this.minCellTravelTime;
	}


	public static void main (String [] args) {
		new BiDirPedsCA().run();
	}

	private static class Cell {
		double lastExit = 0;
		double lastEnter = 0;
		Ped ped = null;

		@Override
		public String toString() {
			return this.ped == null ? " " : this.ped.toString();
		}
	}

	private static class Ped {
		private final String id;

		public Ped(int i, int j) {
			if (i <10) {
				this.id = "0"+i;
			} else {
				this.id = i+"";
			}
			this.dir = j;
		}

		int dir = 1;
		@Override
		public String toString() {
			return this.dir == 1 ? "." : "+"; 
		}
	}
}
