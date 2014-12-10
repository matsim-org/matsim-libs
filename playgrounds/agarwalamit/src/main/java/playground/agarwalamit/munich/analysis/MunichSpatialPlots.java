/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

import java.util.Map;
import java.util.SortedMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.emissions.types.WarmPollutant;

import playground.agarwalamit.analysis.LoadMyScenarios;
import playground.agarwalamit.analysis.emission.EmissionLinkAnalyzer;
import playground.agarwalamit.analysis.spatial.GeneralGrid.GridType;
import playground.agarwalamit.analysis.spatial.SpatialDataInputs;
import playground.agarwalamit.analysis.spatial.SpatialInterpolation;

import com.vividsolutions.jts.geom.Point;

/**
 * @author amit
 */

public class MunichSpatialPlots {

	private static Map<Double,Map<Id<Link>,SortedMap<String,Double>>> linkEmissions;

	public static void main(String[] args) {
		String runDir = "/Users/amit/Documents/repos/runs-svn/detEval/emissionCongestionInternalization/output/1pct/run9/";
		String bau = runDir+"/baseCaseCtd";

		// setting of input data
		SpatialDataInputs inputs = new SpatialDataInputs("line",bau);
		inputs.setGridInfo(GridType.SQUARE, 150);
		inputs.setShapeFile("/Users/amit/Documents/repos/shared-svn/projects/detailedEval/Net/shapeFromVISUM/urbanSuburban/cityArea.shp");

		// set bounding box, smoothing radius and targetCRS if different.
		//		inputs.setTargetCRS(MGC.getCRS("EPSG:20004"));
		//		inputs.setBoundingBox(4452550.25, 4479483.33, 5324955.00, 5345696.81);
		//		inputs.setSmoothingRadius(500.);

		SpatialInterpolation plot = new SpatialInterpolation(inputs,runDir+"/analysis/spatialPlots/");

		EmissionLinkAnalyzer emsLnkAna = new EmissionLinkAnalyzer(LoadMyScenarios.getSimulationEndTime(inputs.initialCaseConfig), inputs.initialCaseEmissionEventsFile, 1);
		emsLnkAna.init();
		emsLnkAna.preProcessData();
		emsLnkAna.postProcessData();
		linkEmissions = emsLnkAna.getLink2TotalEmissions();

		Scenario sc = LoadMyScenarios.loadScenarioFromNetwork(inputs.initialCaseNetworkFile);
		double sumEmission =0;

		for(double time :linkEmissions.keySet()){
			for(Id<Link> id : linkEmissions.get(time).keySet()){
				Link l =sc.getNetwork().getLinks().get(id);
				if(plot.isInResearchArea(l)){
					plot.processLink(l,  100 * linkEmissions.get(time).get(id).get(WarmPollutant.NO2.toString()));
					sumEmission += (linkEmissions.get(time).get(id).get(WarmPollutant.NO2.toString()) * 100. );
				}
			}
			//write here for different time intervals
		}

		plot.writeRData();
		SpatialDataInputs.LOG.info("Total NO2 emissions from link emission map is "+sumEmission);

		double cellWeights =0;
		for(Point p: plot.getCellWeights().keySet()){
			cellWeights += plot.getCellWeights().get(p);
		}
		SpatialDataInputs.LOG.info("Total NO2 emissions from cell weights  is "+cellWeights);
	}

}
