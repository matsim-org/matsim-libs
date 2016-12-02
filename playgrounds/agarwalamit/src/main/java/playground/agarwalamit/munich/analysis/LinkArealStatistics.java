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

import com.vividsolutions.jts.geom.Geometry;
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

import playground.agarwalamit.utils.FileUtils;
import playground.agarwalamit.utils.GeometryUtils;
import playground.agarwalamit.utils.ListUtils;
import playground.agarwalamit.utils.LoadMyScenarios;

/**
 * @author amit
 */

public class LinkArealStatistics {

	private static final String DIR = FileUtils.RUNS_SVN + "/detEval/emissionCongestionInternalization/hEART/output/";
	private static final String shapeFileCity = "/Users/amit/Documents/repos/shared-svn/projects/detailedEval/Net/shapeFromVISUM/urbanSuburban/cityArea.shp";
	private static final String shapeFileMMA = "/Users/amit/Documents/repos/shared-svn/projects/detailedEval/Net/boundaryArea/munichMetroArea_correctedCRS_simplified.shp";

	private Network network ;

	public static void main(String[] args) {

		String cases [] = new String[] {"bau", "ei", "5ei", "10ei", "15ei", "20ei", "25ei"};
		for (String str : cases) {
			new LinkArealStatistics().run(str);
		}
	}

	private void run(final String runCase){
		String networkFile = DIR+"/"+runCase+"/output_network.xml.gz";
		String eventsFile = DIR+"/"+runCase+"/ITERS/it.1500/1500.events.xml.gz";
		network = LoadMyScenarios.loadScenarioFromNetwork(networkFile).getNetwork();

		Collection<SimpleFeature> features_city = new ArrayList<>();
		features_city.addAll( new ShapeFileReader().readFileAndInitialize(shapeFileCity) );
		Collection<Geometry> simplifiedGeoms_city = GeometryUtils.getSimplifiedGeometries(features_city);

		Collection<SimpleFeature> features_mma = new ArrayList<>();
		features_mma.addAll( new ShapeFileReader().readFileAndInitialize(shapeFileMMA) );
		Collection<Geometry> simplifiedGeoms_mma = GeometryUtils.getSimplifiedGeometries(features_mma);

		BufferedWriter writer = IOUtils.getBufferedWriter(DIR+"/analysis/linkArealStatistics_"+runCase+".txt");
		try {
			writer.write("area \t numberOfLinks \t totalTraveledDistanceInKm \n");

			writer.write("cityArea \t"+getNumberOfLinks(simplifiedGeoms_city)+"\t"+getTraveledDistance(eventsFile, simplifiedGeoms_city)/1000+"\n");
			writer.write("metroArea \t"+getNumberOfLinks(simplifiedGeoms_mma)+"\t"+getTraveledDistance(eventsFile, simplifiedGeoms_mma)/1000+"\n");
			writer.write("allInclusive \t "+getNumberOfLinks(null)+"\t"+getTraveledDistance(eventsFile, null)/1000+"\n");

			writer.close();
		} catch (Exception e) {
			throw new RuntimeException("Data is not written in the file. Reason - "+e);
		}
	}

	private Integer getNumberOfLinks (final Collection<Geometry> features){
		boolean isFiltering = (features!=null && ! features.isEmpty()) ;

		int numberOfLinks = 0;
		for (Link l : network.getLinks().values()){
			if(isFiltering) {
				if (GeometryUtils.isLinkInsideGeometries(features, l) ){
					numberOfLinks++;
				}
			} else {
				numberOfLinks++;
			}
		}
		return numberOfLinks;
	}
	
	private double getTraveledDistance (final String eventsFile, final Collection<Geometry> features){
		final List<Double> dists =  new ArrayList<>();
		boolean isFiltering = (features!=null && ! features.isEmpty()) ;

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
					if ( GeometryUtils.isLinkInsideGeometries(features, l) ) {
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
