/* *********************************************************************** *
 * project: org.matsim.*
 * WayDataElement
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.jbischoff.waySplitter;

import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
/**
 * @author jbischoff
 *
 */
public class WayDataElement {

	Id pid;
	Id wayID;

	String a2;
	String a2_n;
	String a3;
	String a3_n;
	String a41;
	String a42;
	String a43;
	String a44;
	String a45;
	String a46;
	String a47;
	String a48;
	String a49;
	String a410;
	String a411;
	String a412;
	String a498;
	String a498t;
	String a5;
	String a5t;
	String a6a;
	String a6an;
	String a6b;
	String a6bn;
	String a6c;
	String a6cn;
	String a6d;
	String a6dn;
	String a6e;
	String a6en;
	String a6f;
	String a6fn;
	String a6g;
	String a6gn;
	String a6h;
	String a6hn;
	String a6i;
	String a6in;
	String a6j;
	String a6jn;
	String a6k;
	String a6kn;
	String a6l;
	String a6ln;
	String a6m;
	String a6mn;
	String a6n;
	String a6nn;
	String a7;
	String a7tn;
	
	public void setWayID(int i) {
		this.wayID = new IdImpl(i);
	}
	public void setPID(String pID) {
		this.pid = new IdImpl(pID);
	}
}
