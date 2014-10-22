/* *********************************************************************** *
 * project: org.matsim.*
 * CALink.java
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

import java.util.LinkedList;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.network.Link;

import playground.gregor.casim.simulation.physics.CAEvent.CAEventType;


public class CALinkStatic implements CANetworkEntity, CALink{

	private static final Logger log = Logger.getLogger(CALinkStatic.class);

	private final Link dsl;
	private final Link usl;

	private final CAAgent [] particles;
	private final double [] time;

	//Floetteroed Laemmel parameters
	private static final double alpha = 0;
	private static final double beta = 0.12;
	private static final double gamma = 1.38;

	private double vHat = 1.29; //cells per unit time
	private final double rhoHat = 6.661; //particles per meter
//	private double D = 0.72;
	private double D = 0.22;
	private final double z;

	private final int size;

	private final CANodeStatic ds;

	private final CANodeStatic us;

	private final CANetwork net;

	private final LinkedList<CAVehicle> dWaiting = new LinkedList<CAVehicle>();
	private final LinkedList<CAVehicle> uWaiting = new LinkedList<CAVehicle>();

	private final double zOrig;
	
	public CALinkStatic(Link dsl, Link usl, CANodeStatic ds, CANodeStatic us, CANetwork net) {
		double tmp = Math.pow(CANetwork.RHO, gamma);
		this.D = alpha+beta*tmp;
		
		double width = dsl.getCapacity();
		this.D /= width;
		this.vHat *= width;
//		this.rhoHat *= cap;
//		this.D /= 1/cap;
		//		if (dsl.getId().toString().equals("3")) {
		////			this.z = 1;
		//			this.vHat = 0.1;
		//		}
		this.dsl = dsl;
		this.usl = usl;
		this.size = (int) (width*dsl.getLength()*this.rhoHat+0.5);
		this.particles = new CAAgent[this.size];
		this.time = new double[this.size];
		this.ds = ds;
		this.us = us;
		this.net = net;
		this.z = this.D+1/(this.vHat*this.rhoHat);
		this.zOrig = this.z;

	}
	

	/* (non-Javadoc)
	 * @see playground.gregor.casim.simulation.physics.CALink#handleEvent(playground.gregor.casim.simulation.physics.CAEvent)
	 */
	@Override
	public void handleEvent(CAEvent e) {
//		this.z = this.zOrig + ((MatsimRandom.getRandom().nextDouble()-0.5)/10);
		CAAgent a = e.getCAAgent();
		double time = e.getEventExcexutionTime();
		if (e.getCAEventType() == CAEventType.SWAP){
			handelSwap(a,time);
		} else if (e.getCAEventType() == CAEventType.TTA){
			handleTTA(a,time);
		} else if (e.getCAEventType() == CAEventType.TTE) {
			handleTTE(a,time);
		}else {
			throw new RuntimeException("Unknown event type: " + e.getCAEventType());
		}
	}

	
	private void handleTTE(CAAgent a, double time) {
		
		int idx = a.getPos();
		int dir = a.getDir();
		if (this.particles[idx] == null) {
			this.particles[idx] = a;
			if (dir == -1) {
				this.uWaiting.poll();
				handleTTE(a,time,this.us);
			} else {
				this.dWaiting.poll();
				handleTTE(a,time,this.ds);
			}
		}
	}
	


	private void handleTTE(CAAgent a, double time, CANode node) {
		
		if (node.peekForAgent() == null){
			CAEvent e = new CAEvent(time + 1/(this.vHat*this.rhoHat), a, this, CAEventType.TTA);
			this.net.pushEvent(e);
		} else if (this.net.getCALink(node.peekForAgent().getNextLinkId()) == this){
			CAEvent e = new CAEvent(time + this.D+ 1/(this.vHat*this.rhoHat), a, this, CAEventType.SWAP);
			this.net.pushEvent(e);
		}
		
	}


	private void handleTTA(CAAgent a, double time) {
		int idx = a.getPos();
		int dir = a.getDir();
		int nextIdx = idx + dir;
		int prevIdx = idx - dir;


		if (nextIdx < 0) {
			if (this.us.peekForAgent() == null) {
				if (this.us.getTime() > time) {
					CAEvent e = new CAEvent(this.us.getTime(), a, this, CAEventType.TTA);
					this.net.pushEvent(e);
				} else {

					this.particles[idx] = null;
//					this.time[idx] = time+this.z+ 1/(this.vHat*this.rhoHat);
					this.time[idx] = time+this.z;
					fireUpstreamLeft(a,time);
					this.us.putAgent(a);
					Id nextCALinkId = a.getNextLinkId();
					CALink nextCALink = this.net.getCALink(nextCALinkId);
					int nextNextA;
					int nextRevDir;
					if (nextCALink.getUpstreamCANode() == this.us) {
						nextNextA = 0;
						nextRevDir = -1;
					} else if (nextCALink.getDownstreamCANode() == this.us) {
						nextNextA = nextCALink.getNumOfCells()-1;
						nextRevDir = 1;
					} else {
						log.warn("inconsitent network, agent:" + a + " becomes stuck!");
						return;
						//					throw new RuntimeException("inconsisten network!");
					}
					if (nextCALink.getParticles()[nextNextA] == null) {
						CAEvent e = new CAEvent(time + 1/(this.vHat*this.rhoHat), a, this.us, CAEventType.TTA);
						this.net.pushEvent(e);
					} else if (nextCALink.getParticles()[nextNextA].getDir() == nextRevDir) {
						//TODO dynamic D
						CAEvent e = new CAEvent(time + this.D + 1/(this.vHat*this.rhoHat), a, this.us, CAEventType.SWAP);
						this.net.pushEvent(e);
					}
					
					//check for waiting
					if (this.uWaiting.size() > 0){
//						CAEvent e = new CAEvent(time + this.z + 1/(this.vHat*this.rhoHat), this.particles[prevIdx], this, CAEventType.TTA);
						CAEvent e = new CAEvent(time + this.z, this.uWaiting.peek(), this, CAEventType.TTE);
						this.net.pushEvent(e);
					}else if (this.particles[prevIdx] != null && this.particles[prevIdx].getDir() == dir) {
//						CAEvent e = new CAEvent(time + this.z + 1/(this.vHat*this.rhoHat), this.particles[prevIdx], this, CAEventType.TTA);
						CAEvent e = new CAEvent(time + this.z, this.particles[prevIdx], this, CAEventType.TTA);
						this.net.pushEvent(e);
					}
				}
			}
		} else if (nextIdx >= this.size) {
			if (this.ds.peekForAgent() == null) {

				if (this.ds.getTime() > time) {
					CAEvent e = new CAEvent(this.ds.getTime(), a, this, CAEventType.TTA);
					this.net.pushEvent(e);
				} else {

//					this.time[idx] = time+this.z+ 1/(this.vHat*this.rhoHat);
					this.time[idx] = time+this.z;
					this.particles[idx] = null;
					fireDownstreamLeft(a,time);
					this.ds.putAgent(a);
					Id nextCALinkId = a.getNextLinkId();
					CALink nextCALink = this.net.getCALink(nextCALinkId);
					int nextNextA;
					int nextRevDir;
					if (nextCALink.getUpstreamCANode() == this.ds) {
						nextNextA = 0;
						nextRevDir = -1;
					} else if (nextCALink.getDownstreamCANode() == this.ds) {
						nextNextA = nextCALink.getNumOfCells()-1;
						nextRevDir = 1;
					} else {
						log.warn("inconsitent network, agent:" + a + " becomes stuck!");
						return;
						//					throw new RuntimeException("inconsisten network!");
					}
					if (nextCALink.getParticles()[nextNextA] == null) {
						CAEvent e = new CAEvent(time + 1/(this.vHat*this.rhoHat), a, this.ds, CAEventType.TTA);
						this.net.pushEvent(e);
					} else if (nextCALink.getParticles()[nextNextA].getDir() == nextRevDir) {
						//TODO dynamic D
						CAEvent e = new CAEvent(time + this.D + 1/(this.vHat*this.rhoHat), a, this.ds, CAEventType.SWAP);
						this.net.pushEvent(e);
					}
					if (this.dWaiting.size() > 0) {
//						CAEvent e = new CAEvent(time + this.z + 1/(this.vHat*this.rhoHat), this.particles[prevIdx], this, CAEventType.TTA);
						CAEvent e = new CAEvent(time + this.z, this.dWaiting.peek(), this, CAEventType.TTE);
						this.net.pushEvent(e);
					}else if (this.particles[prevIdx] != null && this.particles[prevIdx].getDir() == dir) {
//						CAEvent e = new CAEvent(time + this.z + 1/(this.vHat*this.rhoHat), this.particles[prevIdx], this, CAEventType.TTA);
						CAEvent e = new CAEvent(time + this.z, this.particles[prevIdx], this, CAEventType.TTA);
						this.net.pushEvent(e);
					}
				}
			}


		} else {
			if (this.particles[nextIdx] == null){
				if( this.time[nextIdx]  > time) {
					CAEvent e = new CAEvent(this.time[nextIdx], a, this, CAEventType.TTA);
					this.net.pushEvent(e);
				} else {
					//check time
//					this.time[idx] = time+this.z+ 1/(this.vHat*this.rhoHat);
					this.time[idx] = time+this.z;
					this.particles[idx] = null;
					this.particles[nextIdx] = a;
					a.proceed();
					int nextNextIdx = a.getPos() + dir;
					if (nextNextIdx < 0) {

						if (this.us.peekForAgent() != null && this.us.peekForAgent().getNextLinkId() == this.dsl.getId()) {
							//TODO dynamic D	
							CAEvent e = new CAEvent(time + this.D + 1/(this.vHat*this.rhoHat), a, this, CAEventType.SWAP);
							this.net.pushEvent(e);
						}else if (this.us.peekForAgent() == null) {
							CAEvent e = new CAEvent(time + 1/(this.vHat*this.rhoHat), a, this, CAEventType.TTA);
							this.net.pushEvent(e);
						}

					} else if (nextNextIdx >= this.size) {
						if (this.ds.peekForAgent() != null && this.ds.peekForAgent().getNextLinkId() == this.dsl.getId()) {
							//TODO dynamic D
							CAEvent e = new CAEvent(time + this.D + 1/(this.vHat*this.rhoHat), a, this, CAEventType.SWAP);
							this.net.pushEvent(e);
						}else if (this.ds.peekForAgent() == null) {
							CAEvent e = new CAEvent(time + 1/(this.vHat*this.rhoHat), a, this, CAEventType.TTA);
							this.net.pushEvent(e);
						}
					} else {
						if (this.particles[nextNextIdx] != null) {
							if (this.particles[nextNextIdx].getDir() != dir) {
								//TODO dynamic D
								CAEvent e = new CAEvent(time + this.D + 1/(this.vHat*this.rhoHat), a, this, CAEventType.SWAP);
								this.net.pushEvent(e);
							}
						} else {
							CAEvent e = new CAEvent(time + 1/(this.vHat*this.rhoHat), a, this, CAEventType.TTA);
							this.net.pushEvent(e);
						}
					}

					if (prevIdx < 0) {

						if (this.us.peekForAgent() != null && this.us.peekForAgent().getNextLinkId() == this.dsl.getId()){
//							CAEvent e = new CAEvent(time + this.z + 1/(this.vHat*this.rhoHat), this.us.peekForAgent(), this.us, CAEventType.TTA);
							CAEvent e = new CAEvent(time + this.z, this.us.peekForAgent(), this.us, CAEventType.TTA);
							this.net.pushEvent(e);
						}
					} else if (prevIdx >= this.size) {
						if (this.ds.peekForAgent() != null && this.ds.peekForAgent().getNextLinkId() == this.dsl.getId()){
//							CAEvent e = new CAEvent(time + this.z + 1/(this.vHat*this.rhoHat), this.ds.peekForAgent(), this.ds, CAEventType.TTA);
							CAEvent e = new CAEvent(time + this.z, this.ds.peekForAgent(), this.ds, CAEventType.TTA);
							this.net.pushEvent(e);					
						}
					} else {
						if (this.particles[prevIdx] != null && this.particles[prevIdx].getDir() == dir) {
//							CAEvent e = new CAEvent(time + this.z + 1/(this.vHat*this.rhoHat), this.particles[prevIdx], this, CAEventType.TTA);
							CAEvent e = new CAEvent(time + this.z, this.particles[prevIdx], this, CAEventType.TTA);
							this.net.pushEvent(e);
						}
					}
				}

			}


		}
	}

	private void handelSwap(CAAgent a, double time) {
		int idx = a.getPos();
		int dir = a.getDir();
		int nbIdx = idx + dir;
		if (nbIdx < 0) {
			fireUpstreamLeft(a, time);
			fireUpstreamEntered(this.us.peekForAgent(),time);
			swapWithNode(a,idx,dir,time,this.us);
		} else if (nbIdx >= this.size) {
			fireDownstreamLeft(a, time);
			fireDownstreamEntered(this.ds.peekForAgent(),time);
			swapWithNode(a,idx,dir,time,this.ds);
		} else {
			swapOnLink(a,idx,dir,time);
		}

	}

	@Override
	public void fireDownstreamEntered(CAAgent a, double time) {
		LinkEnterEvent e = new LinkEnterEvent(time, a.getId(), this.usl.getId(), a.getId());
		this.net.getEventsManager().processEvent(e);
//		System.out.println("down");

	}

	@Override
	public void fireUpstreamEntered(CAAgent a, double time) {
		LinkEnterEvent e = new LinkEnterEvent(time, a.getId(), this.dsl.getId(), a.getId());
		this.net.getEventsManager().processEvent(e);
//		System.out.println("up");
	}

	@Override
	public void fireDownstreamLeft(CAAgent a, double time) {
		LinkLeaveEvent e = new LinkLeaveEvent(time, a.getId(), this.dsl.getId(), a.getId());
		this.net.getEventsManager().processEvent(e);

	}

	@Override
	public void fireUpstreamLeft(CAAgent a, double time) {
		LinkLeaveEvent e = new LinkLeaveEvent(time, a.getId(), this.usl.getId(), a.getId());
		this.net.getEventsManager().processEvent(e);
	}

	private void swapWithNode(CAAgent a, int idx, int dir, double time,
			CANodeStatic n) {
		CAAgent swapA = n.pollAgent(time);
		swapA.moveOverNode(this,time);
		swapA.materialize(idx, -dir);
		this.particles[idx] = swapA;
		n.putAgent(a);
		Id nextLinkId = a.getNextLinkId();
		CALink nextCALink = this.net.getCALink(nextLinkId);
		int nextNextA;
		int nextRevDir;
		if (nextCALink.getUpstreamCANode() == n) {
			nextNextA = 0;
			nextRevDir = -1;
		} else if (nextCALink.getDownstreamCANode() == n) {
			nextNextA = nextCALink.getNumOfCells()-1;
			nextRevDir = 1;
		} else {
			log.warn("inconsitent network, agent:" + a + " becomes stuck!");
			return;
//			throw new RuntimeException("inconsisten network!");
		}
		if (nextCALink.getParticles()[nextNextA] != null) {
			if (nextCALink.getParticles()[nextNextA].getDir() == nextRevDir){
				//TODO dynamic D
				CAEvent e = new CAEvent(time + this.D + 1/(this.vHat*this.rhoHat), a, n, CAEventType.SWAP);
				this.net.pushEvent(e);		
			}
		} else {
			CAEvent e = new CAEvent(time + 1/(this.vHat*this.rhoHat), a, n, CAEventType.TTA);
			this.net.pushEvent(e);
		}


		int nextNextSwapA = idx -dir;
		if (nextNextSwapA < 0 ){
			if (this.us.peekForAgent() != null && this.us.peekForAgent().getNextLinkId() == this.dsl.getId()) {
				//TODO new us SWAP event
				throw new RuntimeException("not implemented yet, can only happen if link consits of two cells only");
			}
		} else if (nextNextSwapA >= this.size) {
			if (this.ds.peekForAgent() != null && this.ds.peekForAgent().getNextLinkId() == this.dsl.getId()) {
				//TODO new ds SWAP event
				throw new RuntimeException("not implemented yet, can only happen if link consits of two cells only");
			}
		} else {
			if (this.particles[nextNextSwapA] != null) {
				if (this.particles[nextNextSwapA].getDir() != swapA.getDir()) {
					//TODO dynamic D
					CAEvent e = new CAEvent(time + this.D + 1/(this.vHat*this.rhoHat), swapA, this, CAEventType.SWAP);
					this.net.pushEvent(e);
				}
			} else {
				CAEvent e = new CAEvent(time + 1/(this.vHat*this.rhoHat), swapA, this, CAEventType.TTA);
				this.net.pushEvent(e);
			}
		}

	}

	@Override
	public int getNumOfCells() {
		return this.size;
	}

	private void swapOnLink(CAAgent a, int idx, int dir, double time) {
		int nbIdx = idx+dir;
		CAAgent nb = this.particles[nbIdx];
		this.particles[nbIdx] = a;
		a.proceed();
		this.particles[idx] = nb;
		nb.proceed();
		int nextNextP = a.getPos() + dir;

		if (nextNextP < 0) {
			if (this.us.peekForAgent() != null) {
				if( this.us.peekForAgent().getNextLinkId() == this.dsl.getId()) {
					//TODO dynamic D
					CAEvent e = new CAEvent(time + this.D + 1/(this.vHat*this.rhoHat), a, this, CAEventType.SWAP);
					this.net.pushEvent(e);
				}
			} else {
				CAEvent e = new CAEvent(time  + 1/(this.vHat*this.rhoHat), a, this, CAEventType.TTA);
				this.net.pushEvent(e);
			}
		} else if (nextNextP >= this.size) {
			if (this.ds.peekForAgent() != null){ 
				if(this.ds.peekForAgent().getNextLinkId() == this.dsl.getId()){
					//TODO dynamic D
					CAEvent e = new CAEvent(time + this.D + 1/(this.vHat*this.rhoHat), a, this, CAEventType.SWAP);
					this.net.pushEvent(e);
				}
			} else {
				CAEvent e = new CAEvent(time  + 1/(this.vHat*this.rhoHat), a, this, CAEventType.TTA);
				this.net.pushEvent(e);
			}
		} else {
			if (this.particles[nextNextP] != null) {
				if (this.particles[nextNextP].getDir() != dir){
					//TODO dynamic D
					CAEvent e = new CAEvent(time + this.D + 1/(this.vHat*this.rhoHat), a, this, CAEventType.SWAP);
					this.net.pushEvent(e);
				}
			} else {
				CAEvent e = new CAEvent(time  + 1/(this.vHat*this.rhoHat), a, this, CAEventType.TTA);
				this.net.pushEvent(e);
			}
		}

		int nextNextNB = nb.getPos() + nb.getDir();
		if (nextNextNB < 0 ){
			if (this.us.peekForAgent() != null) {
				if(this.us.peekForAgent().getNextLinkId() == this.dsl.getId()) {
					//TODO dynamic D
					CAEvent e = new CAEvent(time + this.D + 1/(this.vHat*this.rhoHat), nb, this, CAEventType.SWAP);
					this.net.pushEvent(e);				
				}
			} else {
				CAEvent e = new CAEvent(time  + 1/(this.vHat*this.rhoHat), nb, this, CAEventType.TTA);
				this.net.pushEvent(e);
			}
		} else if (nextNextNB >= this.size) {
			if (this.ds.peekForAgent() != null) {
				if(this.ds.peekForAgent().getNextLinkId() == this.dsl.getId()) {
					//TODO dynamic D
					CAEvent e = new CAEvent(time + this.D + 1/(this.vHat*this.rhoHat), nb, this, CAEventType.SWAP);
					this.net.pushEvent(e);	
				} 
			} else {
				CAEvent e = new CAEvent(time  + 1/(this.vHat*this.rhoHat), nb, this, CAEventType.TTA);
				this.net.pushEvent(e);
			}
		} else {
			if (this.particles[nextNextNB] != null) {
				if (this.particles[nextNextNB].getDir() != nb.getDir()) {
					//TODO dynamic D
					CAEvent e = new CAEvent(time + this.D + 1/(this.vHat*this.rhoHat), nb, this, CAEventType.SWAP);
					this.net.pushEvent(e);
				}
			} else {
				CAEvent e = new CAEvent(time + 1/(this.vHat*this.rhoHat), nb, this, CAEventType.TTA);
				this.net.pushEvent(e);
			}
		}

	}

	/* (non-Javadoc)
	 * @see playground.gregor.casim.simulation.physics.CALink#getUpstreamCANode()
	 */
	@Override
	public CANode getUpstreamCANode() {
		return this.us;
	}
	/* (non-Javadoc)
	 * @see playground.gregor.casim.simulation.physics.CALink#getDownstreamCANode()
	 */
	@Override
	public CANode getDownstreamCANode() {
		return this.ds;
	}

	@Override
	public CAAgent[] getParticles() {
		return this.particles;
	}

	@Override
	public double[] getTimes() {
		return this.time;
	}

	/* (non-Javadoc)
	 * @see playground.gregor.casim.simulation.physics.CALink#getLink()
	 */
	@Override
	public Link getLink() {
		return this.dsl;
	}
	
	/* (non-Javadoc)
	 * @see playground.gregor.casim.simulation.physics.CALink#getUpstreamLink()
	 */
	@Override
	public Link getUpstreamLink() {
		return this.usl;
	}

	@Override
	public String toString() {
		return this.dsl.getId().toString();
	}

	
	//MATSim integration
	/* (non-Javadoc)
	 * @see playground.gregor.casim.simulation.physics.CALink#letAgentDepart(playground.gregor.casim.simulation.physics.CAVehicle)
	 */
	@Override
	public void letAgentDepart(CAVehicle veh) {
		Id id = veh.getInitialLinkId();
		if (id == this.dsl.getId()) {
			veh.materialize(getNumOfCells()-1, 1);
			this.dWaiting.add(veh);
		} else if(id == this.usl.getId()) {
			veh.materialize(0, -1);
			this.uWaiting.add(veh);
		}
		
		if (this.particles[veh.getPos()] == null) {
			CAEvent e = new CAEvent(this.time[veh.getPos()], veh, this, CAEventType.TTE);
			this.net.pushEvent(e);
		} 
	}


	@Override
	public double[] getDensities() {
		// TODO Auto-generated method stub
		return null;
	}
}
