/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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
package playground.agarwalamit.munich.analysis;

import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.io.IOUtils;
import org.opengis.feature.simple.SimpleFeature;

import playground.agarwalamit.utils.GeometryUtils;
import playground.agarwalamit.utils.ListUtils;
import playground.agarwalamit.utils.LoadMyScenarios;

/**
 * @author amit
 */

public class LinkArealStatistics {

	private static final String dir = "../../../../repos/runs-svn/detEval/emissionCongestionInternalization/hEART/output/";
	private static final String networkFile = dir+"/bau/output_network.xml.gz";
	private static final String eventsFile = dir+"/bau/ITERS/it.1500/1500.events.xml.gz";
	private Network network ;

	public static void main(String[] args) {
		new LinkArealStatistics().run();
	}

	private void run(){
		network = LoadMyScenarios.loadScenarioFromNetwork(networkFile).getNetwork();
		String shapeFile_city = "/Users/amit/Documents/repos/shared-svn/projects/detailedEval/Net/shapeFromVISUM/urbanSuburban/cityArea.shp";
		String shapeFile_mma = "/Users/amit/Documents/repos/shared-svn/projects/detailedEval/Net/boundaryArea/munichMetroArea_correctedCRS_simplified.shp";
		BufferedWriter writer = IOUtils.getBufferedWriter(dir+"/analysis/linkArealStatistics.txt");
		try {
			writer.write("area \t numberOfLinks \t totalTraveledDistanceInKm \n");

			writer.write("cityArea \t"+getNumberOfLinks(shapeFile_city)+"\t"+getTraveledDistance(eventsFile, shapeFile_city)/1000+"\n");
			writer.write("metroArea \t"+getNumberOfLinks(shapeFile_mma)+"\t"+getTraveledDistance(eventsFile, shapeFile_mma)/1000+"\n");
			writer.write("allInclusive \t "+getNumberOfLinks(null)+"\t"+getTraveledDistance(eventsFile, null)/1000+"\n");

			writer.close();
		} catch (Exception e) {
			throw new RuntimeException("Data is not written in the file. Reason - "+e);
		}
	}

	private Integer getNumberOfLinks (String polygonShapeFile){
		boolean isFiltering = !(polygonShapeFile == null) ;
		Collection<SimpleFeature> features = new ArrayList<>();
		if ( isFiltering ) features.addAll( new ShapeFileReader().readFileAndInitialize(polygonShapeFile) );
		
		int numberOfLinks = 0;
		for (Link l : network.getLinks().values()){
			if(isFiltering) {
				if (GeometryUtils.isLinkInsideCity(features, l) ){
					numberOfLinks++;
				}
			} else {
				numberOfLinks++;
			}
		}
		return numberOfLinks;
	}
	
	private double getTraveledDistance (String eventsFile, String polygonShapeFile){
		final List<Double> dists =  new ArrayList<>();
		
		final boolean isFiltering = !(polygonShapeFile == null) ;
		final Collection<SimpleFeature> features = new ArrayList<>();
		if ( isFiltering ) features.addAll( new ShapeFileReader().readFileAndInitialize(polygonShapeFile) );
		
		EventsManager events = EventsUtils.createEventsManager();
		MatsimEventsReader reader = new MatsimEventsReader(events);
		events.addHandler(new LinkLeaveEventHandler() {
			@Override
			public void reset(int iteration) {
				dists.clear();
			}
			@Override
			public void handleEvent(LinkLeaveEvent event) {
				Link l = network.getLinks().get(event.getLinkId());
				if (isFiltering){
					if ( GeometryUtils.isLinkInsideCity(features, l) ) {
						dists.add(l.getLength());
					}
				} else {
					dists.add(l.getLength());
				}
			}
		});
		reader.readFile(eventsFile);
		return ListUtils.doubleSum(dists);
	}
}
