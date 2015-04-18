/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.kai.usecases.ownmobsim;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.RunnableMobsim;

/**
 * @author nagel
 *
 */
class MyMobsim implements RunnableMobsim {
	
	private Scenario sc;
	private EventsManager ev;
	
	private static final Comparator<DriverVehicleUnit> CMP = new Comparator<DriverVehicleUnit>() {
		@Override
		public int compare(DriverVehicleUnit p0, DriverVehicleUnit p1 ) {
			if ( p0.getCurrentActivityEndTime() < p1.getCurrentActivityEndTime() ) {
				return -1 ;
			} else if ( p0.getCurrentActivityEndTime() > p1.getCurrentActivityEndTime() ) {
				return 1 ;
			} else {
				return 0 ;
			}
		}
	} ;
	private PriorityQueue<DriverVehicleUnit> personsAtActivities = new PriorityQueue<DriverVehicleUnit>( 11, CMP ) ;
	private Map<Id,MobsimNode> mobsimNodes = new TreeMap<Id,MobsimNode>();
	private Map<Id,MobsimLink> mobsimLinks = new TreeMap<Id,MobsimLink>();
	
	MyMobsim( Scenario sc, EventsManager ev ) {
		Network net = this.sc.getNetwork() ;
		for ( Node node : net.getNodes().values() ) {
			MobsimNode mn = new MobsimNode( node ) ;
			mobsimNodes.put( node.getId(), mn) ;
		}
		for ( Link link : net.getLinks().values() ) {
			MobsimLink ml = new MobsimLink( link ) ;
			mobsimLinks.put( link.getId(), ml ) ;
		}
		
		
		Population pop = this.sc.getPopulation() ;
		for ( Person person : pop.getPersons().values() ) {
			DriverVehicleUnit mp = new DriverVehicleUnit( person ) ;
			personsAtActivities.add(mp) ;
		}
	}

	@Override
	public void run() {
		for ( long now = 0 ; now < 24*2600; now++ ) {
			
			// ``activities engine'':
			while ( personsAtActivities.peek().getCurrentActivityEndTime() <= now ) {
				DriverVehicleUnit mp = personsAtActivities.remove() ;
				MobsimLink mLink =  mobsimLinks.get( mp.getCurrentLinkId() ) ;
				mLink.addToParking( mp ) ;
			}
			
			// ``traffic flow engine'':
			for ( MobsimLink link : mobsimLinks.values() ) {
				link.doSimStep() ;
			}
			for ( MobsimNode node : mobsimNodes.values() ) {
				node.doSimStep() ;
			}
			
		}
	}

}
