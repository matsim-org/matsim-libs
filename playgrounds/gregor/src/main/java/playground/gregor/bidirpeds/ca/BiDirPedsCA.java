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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;





public class BiDirPedsCA {


	private final  long last = 0;

	private final Random r = new Random();

	private final double maxSpeed = 1.5;
	private final double L = .19;
	private final double w = .21;
	private final double z = this.w/this.L; 


	private final double minCellTravelTime = this.L/this.maxSpeed;
	private final double conflictDelay = 0.9;//0.5+this.minCellTravelTime;

	private static final int CELLS = 30; //(int) (1000/.19);

	private final Cell [] cells = new Cell[CELLS+2];
	private final Cell [] tmpCells = new Cell[CELLS+2];

	private final BiDirPedsCAVis vis;

	private BufferedWriter bf;

	private String fileName;


	private static final double WARMUP_TIME = 10;
	private static final double MAX_TIME = WARMUP_TIME+10;

	private static final boolean VISUALIZE = true;

	private static final boolean SAVE_DAT = false;

	public BiDirPedsCA() {

		this.vis = new BiDirPedsCAVis(CELLS);

	}

	public void run() {

		int idx1 = 0;
		for (double rho1 = 0.075; rho1 <= 1; rho1 += 0.025) {
			idx1++;
			int idx2 = 0;
			for (double rho2 = rho1; rho2 <= 1; rho2 += 0.025) {
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
				for (int i = 1; i < CELLS; i++) {
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
				
				if (SAVE_DAT) {
					this.fileName = "/Users/laemmel/tmp/ca/biDir/spaceTime_"+rrho1+"_"+rrho2;
					try {
						this.bf = new BufferedWriter(new FileWriter(new File(this.fileName)));
					} catch (IOException e) {
						e.printStackTrace();
					}
					try {
						new GnuplotScript(this.fileName, this.cells, numPeds,CELLS);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}

				double t = 0;
				while (t < MAX_TIME) {

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

					Ped cand = this.cells[1].ped;
					if (cand != null && (this.tmpCells[1].ped == null || this.tmpCells[1].ped != cand)) {
						cand.it++;
					}

					for (int i = 1; i <= CELLS; i++) {
						this.cells[i].lastEnter = this.tmpCells[i].lastEnter;
						this.cells[i].lastExit = this.tmpCells[i].lastExit;
						this.cells[i].ped = this.tmpCells[i].ped;
						//						this.tmpCells[i] = new Cell();
						this.tmpCells[i].lastEnter = 0;
						this.tmpCells[i].lastExit = 0;
						this.tmpCells[i].ped = null;
						//						buf.append(this.cells[i]+"|");
					}
					if (VISUALIZE) {
						this.vis.setCells(this.cells,t);
					}

					if (SAVE_DAT) {
						int [] spaceDat = new int[numPeds];
						for (int i = 1; i <= CELLS; i++) {
							Ped ped = this.cells[i].ped;
							if (ped != null ) {
								if (i == CELLS) {
									spaceDat[ped.nr] = -1;
								} else {
									spaceDat[ped.nr] = i;
								}
							}
						}
						StringBuffer bf = new StringBuffer();
						
							bf.append(t);
						
						//					bf.append(' ');
						for (int i : spaceDat) {
							bf.append(' ');
							bf.append(i+"");
						}
						bf.append("\n");
						String str = bf.toString();
						try {
							this.bf.append(str);
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
					//					System.out.println(str);
				}
				int lrPeds = 0;
				int rlPeds = 0;
				double RLFlow = 0;
				double LRFlow = 0;
				double RLSpeed = 0;
				double LRSpeed = 0;
				double overallFlow = 0;
				double overallSpeed = 0;
				double timeInt = MAX_TIME - WARMUP_TIME;
				for (int i = 1; i <= CELLS; i++) {
					Ped ped = this.cells[i].ped;
					if (ped == null) {
						continue;
					}
					int tr = ped.traveled;
					double v = tr/timeInt * this.L;
					if (ped.dir == 1) {
						lrPeds++;
						LRFlow += v*rrho1;
						LRSpeed += v;
						overallFlow += v*rrho1;
						overallSpeed += v;
					} else {
						rlPeds++;
						RLFlow += v*rrho2;
						RLSpeed += v;
						overallFlow += v*rrho2;
						overallSpeed += v;						
					}

				}
				RLSpeed /= rlPeds;
				RLFlow /= rlPeds;
				LRSpeed /= lrPeds;
				LRFlow /= lrPeds;
				overallFlow += RLFlow+LRFlow;
				overallSpeed /= (rlPeds+lrPeds);

				System.out.println(rrho + "," + rrho1 + " " + rrho2 +  " " + overallSpeed + " " + overallFlow + " " + LRSpeed + " " + LRFlow + " " + RLSpeed + " " + RLFlow + " " + idx1 + " " + idx2);
				if (SAVE_DAT) {
					try {
						this.bf.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				if (VISUALIZE){// || SAVE_DAT) {
//					System.exit(0);
				}
				
			}
		}
	}


	private double updateCell(int i,double time) {


		Cell curr = this.cells[i];
		if (curr.ped == null) {
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
			//			if (next.lastExit > 0) {
			//				System.out.println();
			//			}
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

		if (time >= WARMUP_TIME) {
			ped.traveled ++;
		}
		nextTmp.lastEnter = time;
		nextTmp.ped = ped;
		Cell currTmp = this.tmpCells[i];
		if (next.ped == null){
			currTmp.lastExit = time;
		} else {
			currTmp.lastExit = 0;
		}
		return time + this.minCellTravelTime;
	}


	public static void main (String [] args) {
		new BiDirPedsCA().run();
	}

	/*package*/ static class Cell {
		double lastExit = 0;
		double lastEnter = 0;
		Ped ped = null;

		@Override
		public String toString() {
			return this.ped == null ? " " : this.ped.toString();
		}
	}

	static class Ped {
		public int traveled = 0;
		final String id;
		final int nr;
		public int it = 0;
		public Ped(int i, int j) {
			this.nr = i;
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
