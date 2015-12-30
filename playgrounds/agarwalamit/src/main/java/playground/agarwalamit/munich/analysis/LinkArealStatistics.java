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
import java.util.Collection;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.io.IOUtils;
import org.opengis.feature.simple.SimpleFeature;

import playground.agarwalamit.utils.GeometryUtils;
import playground.agarwalamit.utils.LoadMyScenarios;

/**
 * @author amit
 */

public class LinkArealStatistics {

	private static final String dir = "../../../../repos/runs-svn/detEval/emissionCongestionInternalization/hEART/output/";
	private static final String networkFile = dir+"/bau/output_network.xml.gz";
	private Network network ;

	public static void main(String[] args) {
		new LinkArealStatistics().run();
	}

	private void run(){
		network = LoadMyScenarios.loadScenarioFromNetwork(networkFile).getNetwork();
		String shapeFile_city = "/Users/amit/Documents/repos/shared-svn/projects/detailedEval/Net/shapeFromVISUM/urbanSuburban/cityArea.shp";
		String shapeFile_mma = "/Users/amit/Documents/repos/shared-svn/projects/detailedEval/Net/boundaryArea/munichMetroArea_correctedCRS_simplified.shp";
		BufferedWriter writer = IOUtils.getBufferedWriter(dir+"/analysis/linkArealStatistics.txt");
		Tuple<Integer, Double> cityLinkStats = getNumberOfLinksAndTotalDistance(shapeFile_city);
		Tuple<Integer, Double> mmaLinkStats = getNumberOfLinksAndTotalDistance(shapeFile_mma);
		Tuple<Integer, Double> allInclusiveLinkStats = getNumberOfLinksAndTotalDistance(null);
		try {
			writer.write("area \t numberOfLinks \t totalDistanceInKm \n");

			writer.write("cityArea \t"+cityLinkStats.getFirst()+"\t"+cityLinkStats.getSecond()/1000+"\n");
			writer.write("metroArea \t"+mmaLinkStats.getFirst()+"\t"+mmaLinkStats.getSecond()/1000+"\n");
			writer.write("allInclusive \t "+allInclusiveLinkStats.getFirst()+"\t"+allInclusiveLinkStats.getSecond()/1000+"\n");

			writer.close();
		} catch (Exception e) {
			throw new RuntimeException("Data is not written in the file. Reason - "+e);
		}
	}

	private Tuple<Integer, Double> getNumberOfLinksAndTotalDistance (String polygonShapeFile){
		boolean isFiltering = !(polygonShapeFile == null) ;
		Collection<SimpleFeature> features = null;
		
		if ( isFiltering ) features = new ShapeFileReader().readFileAndInitialize(polygonShapeFile);
		
		int numberOfLinks = 0;
		double distance = 0;
		for (Link l : network.getLinks().values()){
			if(isFiltering) {
				if (GeometryUtils.isLinkInsideCity(features, l) ){
					numberOfLinks++;
					distance += l.getLength();
				}
			} else {
				numberOfLinks++;
				distance += l.getLength();
			}
		}
		return new Tuple<Integer, Double>(numberOfLinks, distance);
	}
}
