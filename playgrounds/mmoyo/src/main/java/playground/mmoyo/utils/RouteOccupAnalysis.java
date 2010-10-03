/* *********************************************************************** *
 * project: org.matsim.*
 * RouteOccupAnalysis.java
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
package playground.mmoyo.utils;

import java.io.File;
import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;
import org.matsim.transitSchedule.api.TransitLine;
import org.matsim.transitSchedule.api.TransitRoute;
import org.matsim.transitSchedule.api.TransitRouteStop;
import org.matsim.transitSchedule.api.TransitSchedule;
import org.xml.sax.SAXException;
import de.micromata.opengis.kml.v_2_2_0.Kml;

	/**Creates a set of time line error graphs for a transit route*/
public class RouteOccupAnalysis {
	TransitSchedule schedule= null;
	File kmzFileObj = null;
		
	public RouteOccupAnalysis(final TransitSchedule schedule, String kmzFilePath){
		this.kmzFileObj= new File(kmzFilePath);
		this.schedule = schedule;
		System.out.println("was zum");
		///Kml.unmarshal(this.kmzFileObj).toString();
	}
		
	public void run() {
		for (TransitLine line: this.schedule.getTransitLines().values()){
			for (TransitRoute route: line.getRoutes().values()){
				for (TransitRouteStop stop : route.getStops()){
					//check if the kmzfile contains this stop graph. There should be a way to parse the content of main.kml
					}
				}
			}
		}
		
		
	public static void main(String[] args) throws SAXException, ParserConfigurationException, IOException  {
		final String SCHEDULEFILE = "../shared-svn/studies/countries/de/berlin-bvg09/pt/baseplan_10x_900s_bignetwork/transitSchedule.networkOevModellBln.xml.gz";
		final String NETWORKFILE  = "../shared-svn/studies/countries/de/berlin-bvg09/pt/baseplan_10x_900s_bignetwork/network.multimodal.xml.gz";
		final String KMZ_FILE = "../playgrounds/mmoyo/output/Cadyts/output24/ITERS/it.10/10.countscompare.kmz";
		final String OUTPUTDIR = "../playgrounds/mmoyo/output/filteredSchedule.xml";
		//TransitSchedule schedule = new DataLoader().readTransitSchedule(NETWORKFILE, SCHEDULEFILE);
		//new RouteOccupAnalysis(schedule, KMZ_FILE);
	}
}