/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.kai.urbansim;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.experimental.network.NetworkWriter;
import org.matsim.core.config.Config;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.ConfigUtils;

/**
 * Translates emme networks into matsim networks.
 * <p/>
 * Uses the "emme network export" result as input, NOT the GIS export.
 * <p/>
 * A serious problem is that there is not enough info in the headers of the emme files:
 * (column counting starts at 0!!!)
 * - link "length" (column # 3) is in arbitrary units, connected to rest of system only through freespeed value
 * - Column # 8-10 are user-defined BUT contain freespeed, capacity, ... in arbitrary order and units
 *   (connected to rest of system through user-defined volume-delay functions)
 * <p/>
 * Keyword(s): emme/2
 * 
 * @author nagel
 * 
 */
public class NetworkEmme2Matsim {
	private static final Logger log = Logger.getLogger(NetworkEmme2Matsim.class);

	private static final int PSRC = 0 ;
	private static final int EUGENE = 1 ;

	private static final int NW_NAME = PSRC ;

	public static void readNetwork( Scenario sc ) {
		Network network = sc.getNetwork() ;
		((NetworkImpl) network).setCapacityPeriod(3600.) ;
		((NetworkImpl) network).setEffectiveLaneWidth(3.75) ;
//		network.setEffectiveCellSize(7.5) ;

		// read emme3 network
		try {
//			BufferedReader reader = IOUtils.getBufferedReader("/home/nagel/tmp/tab/net1.out" ) ;
			BufferedReader reader = IOUtils.getBufferedReader("/Users/nagel/eclipse/shared-svn/studies/countries/us/psrc/network/emme-export/net1.out" ) ;

			boolean weAreReadingNodes = true ;
			long linkCnt = 0 ;

			String line ;
			while ( (line = reader.readLine()) != null ) {
				String[] parts = line.split("[ \t\n]+");

				if ( parts[0].equals("c") ) {
					// is a comment; ignore
				} else if ( parts[0].equals("t") ) {
					if ( parts[1].equals("links") ) {
						weAreReadingNodes = false ;
					}
				} else if ( parts[0].equals("a") ) { // || parts[0].equals("a*") ) { // a* seem to be centroid connectors
					if ( weAreReadingNodes ) {
						String idStr = parts[1] ;
						String xxStr = parts[2] ;
						String yyStr = parts[3] ;
						Node node = network.getFactory().createNode( sc.createId(idStr), 
								sc.createCoord(Double.parseDouble(xxStr),Double.parseDouble(yyStr)) ) ;
						network.addNode( node ) ;
					} else {
						Node fromNode = network.getNodes().get(sc.createId(parts[1]) ) ;
						Node toNode = network.getNodes().get(sc.createId(parts[2]));
						if ( fromNode==null || toNode==null ) {
//							log.info("fromNode or toNode ==null; probably connector link; skipping it ...") ;
							continue ;
						}
						if ( parts[4].equals("r") || parts[4].equals("b") ) {
							log.info("rail only or bus only link; skipping it ...") ;
							continue;
						}
						double length = 1600 * Double.parseDouble( parts[3] ) ; // probably miles
//						String type = parts[5] ;

						double permlanes = Double.parseDouble( parts[6] ) ;
						if ( permlanes <= 0 ) { permlanes = 0.5 ; }

						double capacity, freespeed ;
						if ( NW_NAME==PSRC ) {
							capacity = permlanes * Double.parseDouble( parts[8] ) ;
							if ( capacity <= 500 ) { capacity = 500. ; }

							freespeed = Double.parseDouble( parts[9] ) ; // mph
							if ( freespeed < 10. ) { freespeed = 10. ; }
							freespeed *= 1600./3600. ;
						} else if ( NW_NAME==EUGENE ) {
							log.warn("For EUGENE, I have not clarified if capacity really needs to be multiplied by number of lanes.");
							capacity = permlanes * Double.parseDouble( parts[10] ) ;
							if ( capacity <= 500 ) { capacity = 500. ; }

							freespeed = Double.parseDouble( parts[9] ) ; // mph
							if ( freespeed < 10. ) { freespeed = 10. ; }
							freespeed *= 1600./3600. ;
						} else {
							log.error( "NW_NAME not known; aborting" ) ;
							System.exit(-1);
						}

						Id id = sc.createId( Long.toString(linkCnt) ) ; linkCnt++ ;

						Link link = network.getFactory().createLink(id, fromNode, toNode ) ;
						link.setLength(length) ;
						link.setFreespeed(freespeed) ;
						link.setCapacity(capacity) ;
						link.setNumberOfLanes(permlanes) ;
						
						network.addLink(link); 
					}
				} else {
					// something else; do nothing
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}



	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Config config = ConfigUtils.createConfig() ;
		// (no api-only way to get a scenario without config)
		
		Scenario sc = ScenarioUtils.createScenario(config) ;
		// (no api-only way to get a network, Id, Coord without scenario)
		
		Network network = sc.getNetwork();

		log.info("reading network ...");
		readNetwork(sc) ;
		log.info("... finished reading network.\n");

		log.info("cleaning network ...");
		NetworkCleaner nwCleaner = new NetworkCleaner() ;
		nwCleaner.run( network ) ;
		log.info("... finished cleaning network.\n") ;

		log.info("writing network ...") ;
		new NetworkWriter(network).write("/home/nagel/tmp/net.xml.gz");
		log.info("... finished writing network.\n") ;
	}

}
