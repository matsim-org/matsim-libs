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

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.analysis.kai.KNAnalysisEventsHandler;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;

/**
 * @author nagel
 *
 */
public class KNLinksCompare {

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		Config config = ConfigUtils.createConfig() ;

		config.network().setInputFile( args[0] );

		Scenario scenario = ScenarioUtils.loadScenario(config) ;

		ObjectAttributes net1Attribs = new ObjectAttributes() ;
		new ObjectAttributesXmlReader( net1Attribs ).readFile( args[1] );

		ObjectAttributes net2Attribs = new ObjectAttributes() ;
		new ObjectAttributesXmlReader( net2Attribs ).readFile( args[2] );

		BufferedWriter writerLo = IOUtils.getBufferedWriter( "linksLo.txt" ) ;
		BufferedWriter writerHi = IOUtils.getBufferedWriter( "linksHi.txt" ) ;

		Network newNetwork = ScenarioUtils.createScenario( ConfigUtils.createConfig() ).getNetwork() ;
		for ( Node node : scenario.getNetwork().getNodes().values() ) {
			newNetwork.addNode( node );
		}

		List<Id> linkIds4removal = new ArrayList<Id>() ;
		for( Iterator<? extends Link> it = scenario.getNetwork().getLinks().values().iterator() ; it.hasNext(); ) {
			Link link = it.next() ;
			String ttimeSum1 = (String) net1Attribs.getAttribute( link.getId().toString(), KNAnalysisEventsHandler.TTIME_SUM ) ;
			String ttimeSum2 = (String) net2Attribs.getAttribute( link.getId().toString(), KNAnalysisEventsHandler.TTIME_SUM ) ;
			double ttimeDiffPerM = 0. ;
			if ( ttimeSum1 != null && ttimeSum2 != null ) {
				Double cnt1 = Double.valueOf( (String) net1Attribs.getAttribute( link.getId().toString(), KNAnalysisEventsHandler.CNT ) ) ;
				Double cnt2 = Double.valueOf( (String) net2Attribs.getAttribute( link.getId().toString(), KNAnalysisEventsHandler.CNT ) ) ;
				double ttime1 = Double.valueOf(ttimeSum1) / cnt1 ;
				double ttime2 = Double.valueOf(ttimeSum2) / cnt2 ;
//				ttimeDiffPerM = cnt2 * (ttime2 - ttime1) / link.getLength() ; // negative is an improvement
				ttimeDiffPerM = (ttime2 - ttime1) / link.getLength() ; // negative is an improvement
			} 
			
//			Link newLink = newNetwork.getFactory().createLink( link.getId(), link.getFromNode(), link.getToNode() ) ;
//			newLink.setLength( link.getLength() ) ;
//			newLink.setCapacity( link.getCapacity() ) ;
//			newLink.setNumberOfLanes( link.getNumberOfLanes() );

			link.setFreespeed(ttimeDiffPerM);

//			newNetwork.addLink(newLink) ;

			
			if ( link.getCapacity() >= 2000. ) {
				try {
					BufferedWriter writer = writerLo ;
					if ( link.getCapacity() >= 4000 ) {
						writer = writerHi ;
					} 
					writer.write( link.getFromNode().getCoord().getX() + "\t" + link.getFromNode().getCoord().getY() + "\t" + Double.toString(ttimeDiffPerM) + "\n" );
					writer.write( link.getToNode().getCoord().getX() + "\t" + link.getToNode().getCoord().getY() + "\t" + Double.toString(ttimeDiffPerM) + "\n\n\n" );
				} catch (IOException e) {
					e.printStackTrace();
				}
			} else if ( link.getCapacity() < 1000. ){
				linkIds4removal.add(link.getId()) ;
			}
		}

		for ( Id id : linkIds4removal ) {
			scenario.getNetwork().removeLink( id ) ;
		}

		try {
			writerHi.close() ;
			writerLo.close() ;
		} catch (IOException e) {
			e.printStackTrace();
		}

		new NetworkWriter( scenario.getNetwork() ).write("newNetwork.xml.gz");

	}

}
