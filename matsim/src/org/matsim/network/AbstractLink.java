/* *********************************************************************** *
 * project: org.matsim.*
 * AbstractLink.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.network;

import org.apache.log4j.Logger;
import org.matsim.basic.v01.BasicLinkImpl;
import org.matsim.basic.v01.Id;
import org.matsim.interfaces.networks.basicNet.BasicNode;
import org.matsim.utils.geometry.CoordI;
import org.matsim.utils.misc.ResizableArray;

public abstract class AbstractLink extends BasicLinkImpl implements Link {

	
	private final static Logger log = Logger.getLogger(AbstractLink.class);
	
	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	protected String type = null;
	protected String origid = null;
	private final ResizableArray<Object> roles = new ResizableArray<Object>(5);

	protected double euklideanDist;
	
	private double flowCapacity;

	public AbstractLink(NetworkLayer network, Id id, BasicNode from,
			BasicNode to) {
		super(network, id, from, to);
	}

	
	
	
	//////////////////////////////////////////////////////////////////////
	// calc methods
	//////////////////////////////////////////////////////////////////////


	public void calcFlowCapacity() {
		int capacityPeriod = ((NetworkLayer)this.getLayer()).getCapacityPeriod();
		
		this.flowCapacity = this.capacity / capacityPeriod;
//		log.debug("flow cap: " + this.flowCapacity);
	}



	/* (non-Javadoc)
	 * @see org.matsim.network.Link#calcDistance(org.matsim.utils.geometry.CoordI)
	 */
	@Override
	public final double calcDistance(final CoordI coord) {
		CoordI fc = this.from.getCoord();
		CoordI tc =  this.to.getCoord();
		double tx = tc.getX();    double ty = tc.getY();
		double fx = fc.getX();    double fy = fc.getY();
		double zx = coord.getX(); double zy = coord.getY();
		double ax = tx-fx;        double ay = ty-fy;
		double bx = zx-fx;        double by = zy-fy;
		double la2 = ax*ax + ay*ay;
		double lb2 = bx*bx + by*by;
		if (la2 == 0.0) { return Math.sqrt(lb2); } // from == to
		double xla = ax*bx+ay*by; // scalar product
		if (xla <= 0.0) { return Math.sqrt(lb2); }
		else if (xla >= la2) {
			double cx = zx-tx;
			double cy = zy-ty;
			return Math.sqrt(cx*cx+cy*cy); }
		else { // lb2-xla*xla/la2 = lb*lb-x*x
			double tmp = xla*xla;
			tmp = tmp/la2;
			tmp = lb2 - tmp;
			// tmp can be slightly negativ, likely due to rounding errors (coord lies on the link!). Therefore, use at least 0.0
			tmp = Math.max(0.0, tmp);
			return Math.sqrt(tmp);
		}
	}

	//////////////////////////////////////////////////////////////////////
	// get methods
	//////////////////////////////////////////////////////////////////////



	// DS TODO try to remove these and update references
	// (for the time being, they are here because otherwise the returned type is wrong. kai)
	/* (non-Javadoc)
	 * @see org.matsim.network.Link#getFromNode()
	 */
	@Override
	public final Node getFromNode() {
		return (Node)this.from;
	}

	/* (non-Javadoc)
	 * @see org.matsim.network.Link#getToNode()
	 */
	@Override
	public final Node getToNode() {
		return (Node)this.to;
	}

	/* (non-Javadoc)
	 * @see org.matsim.network.Link#getOrigId()
	 */
	public final String getOrigId() {
		return this.origid;
	}


	/* (non-Javadoc)
	 * @see org.matsim.network.Link#getType()
	 */
	public final String getType() {
		return this.type;
	}

	/* (non-Javadoc)
	 * @see org.matsim.network.Link#getRole(int)
	 */
	public final Object getRole(final int idx) {
		if (idx < this.roles.size() ) {
			return this.roles.get(idx);
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.matsim.network.Link#getEuklideanDistance()
	 */
	public final double getEuklideanDistance() {
		return this.euklideanDist;
	}

	/* (non-Javadoc)
	 * @see org.matsim.network.Link#getFlowCapacity()
	 */
	public final double getFlowCapacity() {
		return this.flowCapacity;
	}

	//////////////////////////////////////////////////////////////////////
	// set methods
	//////////////////////////////////////////////////////////////////////

	public final void setOrigId(final String id) {
		this.origid = id;
	}


	/* (non-Javadoc)
	 * @see org.matsim.network.Link#setRole(int, java.lang.Object)
	 */
	public final void setRole(final int idx, final Object role) {
		if (idx > this.roles.size()) {
			this.roles.resize(idx+1);
		}
		this.roles.set(idx, role);
	}

	public void setMaxRoleIndex(final int index) {
		this.roles.resize(index+1);
	}

	protected void setEuklideanDist(double euklideanDist) {
		this.euklideanDist = euklideanDist;
	}

	public void setType(String type) {
		this.type = type;
	}

}
