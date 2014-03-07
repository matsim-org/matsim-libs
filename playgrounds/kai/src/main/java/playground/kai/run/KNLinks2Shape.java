/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package playground.kai.run;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.utils.gis.matsim2esri.network.CapacityBasedWidthCalculator;
import org.matsim.utils.gis.matsim2esri.network.FeatureGeneratorBuilderImpl;
import org.matsim.utils.gis.matsim2esri.network.Links2ESRIShape;
import org.matsim.utils.gis.matsim2esri.network.PolygonFeatureGenerator;

/**
 * @author nagel
 *
 */
public class KNLinks2Shape {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		String defaultCRS = "DHDN_GK4";
		boolean commonWealth = true ;
		
		String outputFileP = "t.shp" ;
		
		Config config = ConfigUtils.createConfig() ;
		config.network().setInputFile( args[0] );
		Scenario scenario = ScenarioUtils.loadScenario( config ) ;
		
		final Map<Id,Double> ttimeSums = new HashMap<Id,Double>() ;
		final Map<Id,Double> cnt = new HashMap<Id,Double>() ; 

		EventsManager events = new EventsManagerImpl() ;
		events.addHandler( new BasicEventHandler(){
			@Override
			public void reset(int iteration) {
			}

			Map<Id,Double> enterTimes = new HashMap<Id,Double>() ;
			@Override
			public void handleEvent(Event event) {
				if ( event instanceof LinkEnterEvent ) {
					LinkEnterEvent ev = (LinkEnterEvent) event ;
					enterTimes.put( ev.getVehicleId(), ev.getTime() ) ;
				} else if ( event instanceof LinkLeaveEvent ) {
					LinkLeaveEvent ev = (LinkLeaveEvent) event ;
					Double enterTime = enterTimes.get( ev.getVehicleId() ) ;
					if ( enterTime != null ) {
						Double ttimeSum = ttimeSums.get( ev.getLinkId() ) ;
						if ( ttimeSum==null ) {
							ttimeSums.put( ev.getLinkId(), ev.getTime()-enterTime ) ;
							cnt.put( ev.getLinkId(), 1. ) ;
						} else {
							ttimeSums.put( ev.getLinkId(), ttimeSum + ev.getTime()-enterTime ) ; // will not result in same as av vel !!
							cnt.put( ev.getLinkId(), cnt.get( ev.getLinkId() ) + 1. ) ;
						}
					}
				}
			}
		} );
		
		
		new MatsimEventsReader(events).readFile( args[1] );

		
		Network newNetwork = NetworkUtils.createNetwork() ;
		
		for ( Link link : scenario.getNetwork().getLinks().values() ) {
			Double ttimeSum = ttimeSums.get( link.getId() ) ;
			if ( ttimeSum != null && link.getCapacity() >= 4000. ) {
				Link newLink = newNetwork.getFactory().createLink(link.getId(), link.getFromNode(), link.getToNode()) ;
			
				double speed = link.getLength() / ( ttimeSum/cnt.get( link.getId() ) ) ;
				newLink.setCapacity(speed); 
				
				newNetwork.addNode( link.getFromNode() );
				newNetwork.addNode( link.getToNode() );
				newNetwork.addLink(newLink);
			}
		}
		

		FeatureGeneratorBuilderImpl builder = new FeatureGeneratorBuilderImpl( newNetwork , defaultCRS);

		builder.setWidthCoefficient((commonWealth ? -1 : 1) * 0.3);
		builder.setFeatureGeneratorPrototype(PolygonFeatureGenerator.class);
		builder.setWidthCalculatorPrototype(CapacityBasedWidthCalculator.class);

		new Links2ESRIShape( newNetwork,outputFileP, builder).write();
	
	}

}
