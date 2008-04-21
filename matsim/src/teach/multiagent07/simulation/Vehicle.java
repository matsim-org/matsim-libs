/* *********************************************************************** *
 * project: org.matsim.*
 * Vehicle.java
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

package teach.multiagent07.simulation;

import java.util.Collection;
import java.util.Iterator;

import org.matsim.basic.v01.BasicLinkImpl;
import org.matsim.basic.v01.Id;
import org.matsim.basic.v01.IdImpl;
import org.matsim.interfaces.networks.basicNet.BasicLink;

import teach.multiagent07.net.CALink;

public class Vehicle {

	public static int Idcounter = 0;
	protected Id id  = new IdImpl(Idcounter++);

	public Id getId() {return this.id;};


	// new Methods for moving planed vehicles

	public double getDepartureTime() {
		return 0;
	}

	public BasicLink getDepartureLink() {
		return null;
	}

	public BasicLink getDestinationLink() {
		return null;
	}

	public void setCurrentLink(BasicLinkImpl link)  {
	}

	public void leaveActivity() {

	}

	public void reachActivity(){

	}

	public CALink getNextLink(Collection<? extends BasicLink> outlinks) {
		//return getNextLink23(outlinks);
		return getNextLinkNormal(outlinks);
	}

	private IdImpl hauptstrasseId = new IdImpl("23");

	public CALink getNextLink23(Collection<? extends BasicLink> outlinks) {
		CALink erg = null;

		for (BasicLink link : outlinks) {
			if (link.getId().equals(this.hauptstrasseId)) {
				erg = (CALink) link;
			}
		}

		if ((erg != null) && (Math.random() < 0.8)) {
			return erg;
		}
		return getNextLinkNormal(outlinks);
	}


	public CALink getNextLinkNormal(Collection<? extends BasicLink> outlinks) {
		CALink erg = null;

		int rnd = (int)(Math.random() * outlinks.size());

		Iterator iter = outlinks.iterator();
		for (int i = 0; i <= rnd; i++) erg = (CALink)iter.next();

		return erg;
	}
}
