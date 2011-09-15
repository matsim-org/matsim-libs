/* *********************************************************************** *
 * project: org.matsim.*
 * ShelterCounter.java
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
package playground.gregor.sims.shelters.assignment;

import java.util.Collection;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.evacuation.base.Building;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PersonImpl;

public class ShelterCounter {// implements LinkEnterEventHandler {

	private static final Logger log = Logger.getLogger(ShelterCounter.class);

	private final HashMap<Id, LinkInfo> linkInfos = new HashMap<Id, LinkInfo>();
	private final HashMap<Building, Id> reversMapping = new HashMap<Building, Id>();
	private int totalCapacity = 0;
	private final HashMap<Id, Building> mapping;

	private final int refCap;

	private int it = -1;

	public ShelterCounter(NetworkImpl network, HashMap<Id, Building> shelterLinkMapping) {
		this.mapping = shelterLinkMapping;
		for (Link link : network.getLinks().values()) {
			if ((link.getId().toString().contains("sl") && link.getId().toString().contains("b"))) {
				Building b = shelterLinkMapping.get(link.getId());
				getReversMapping().put(b, link.getId());
				this.linkInfos.put(link.getId(), new LinkInfo(b.getShelterSpace(), link.getId()));
				this.totalCapacity += b.getShelterSpace();
			} else if (link.getId().toString().equals("el1")) {
				Building b = shelterLinkMapping.get(link.getId());
				if (b == null) {
					throw new RuntimeException("This should not happen!");
				}
				getReversMapping().put(b, link.getId());
				this.linkInfos.put(link.getId(), new LinkInfo(b.getShelterSpace(), link.getId()));

			}
		}
		this.refCap = this.totalCapacity;
	}

	// @Override
	// public void handleEvent(LinkEnterEvent event) {
	// LinkInfo li = this.linkInfos.get(event.getLinkId());
	// if (li != null) {
	// if (!event.getLinkId().equals("el1")) {
	// this.inShelter ++;
	// }
	// li.count++;
	// if (li.count > li.space) {
	// throw new RuntimeException("Shelter space capacity exceeded:" + li.id +
	// " li.space:" + li.space + " event.getPersonId()" + event.getPersonId());
	// }
	// }
	//		
	// }

	public double getTotalCapacity() {
		return this.refCap;
	}

	public void changeCapacity(Id id, int amount) {
		// if (id.toString().equals("el1")) {
		// int ii = 0;
		// ii++;
		// }
		this.totalCapacity += amount;
		LinkInfo li = this.linkInfos.get(id);
		li.space += amount;
		if (li.space < 0) {
			throw new RuntimeException("negative capacities are not allowed");
		}
		Building b = this.mapping.get(id);
		b.setShelterSpace(b.getShelterSpace() + amount);
	}

	public Building getShelter(Id id) {
		return this.mapping.get(id);
	}

	public void testAdd(Id linkId) {
		LinkInfo li = this.linkInfos.get(linkId);
		if (li != null) {
			if (!linkId.toString().equals("el1")) {
				int ii = 0;
				ii++;
			}
			li.count++;
			if (this.it <= 0 && li.count > li.space) {
				li.space++;
			}

			if (li.count > li.space) {
				throw new RuntimeException("Shelter space capacity exceeded:" + li.id + " li.space:" + li.space);// +
																													// " event.getPersonId()"
																													// +
																													// event.getPersonId());
			}
		}
	}

	public void rm(Id linkId) {
		this.linkInfos.get(linkId).count--;
	}

	public Id tryToAddAgent(Building b) {
		Id linkId = getReversMapping().get(b);
		LinkInfo li = this.linkInfos.get(linkId);
		if (li.count < li.space) {
			li.count++;
			return linkId;
		}
		return null;
	}

	public void reset(int iteration, Collection<? extends Person> agents) {

		this.it++;
		// if (this.totalCapacity > this.refCap) {
		// throw new RuntimeException("totalCapacity:" + this.totalCapacity +
		// "  but should be:" + this.refCap);
		// }
		// this.refCap = this.totalCapacity;

		for (LinkInfo li : this.linkInfos.values()) {
			li.count = 0;
		}

		// this.inShelter = 0;
		for (Person pers : agents) {
			((PersonImpl) pers).removeUnselectedPlans();
			testAdd(((ActivityImpl) pers.getSelectedPlan().getPlanElements().get(2)).getLinkId());
		}
	}

	public void printStats() {
		log.info("======================================");
		log.info("total Shelters capacity: " + this.totalCapacity);
		int count = 0;
		for (LinkInfo li : this.linkInfos.values()) {
			printShelterStats(li);
			count += li.space;
		}
		// log.info("total Shelters capacity: " + (count-2055));
		log.info("======================================");
	}

	private void printShelterStats(LinkInfo li) {

		log.info("Shelter: " + li.id + "  cappacity:" + li.space + "  occupancy:" + li.count);
	}

	// public int getNumAgentsInShelter(){
	// return this.inShelter;
	// }

	public HashMap<Building, Id> getReversMapping() {
		return this.reversMapping;
	}

	private static class LinkInfo {
		int count = 0;
		int space;
		final Id id;

		public LinkInfo(int shelterSpace, Id id) {
			this.id = id;
			this.space = shelterSpace;

		}
	}

	public int getShelterFreeSpace(Id id) {
		LinkInfo li = this.linkInfos.get(id);
		return li.space - li.count;
	}

}
