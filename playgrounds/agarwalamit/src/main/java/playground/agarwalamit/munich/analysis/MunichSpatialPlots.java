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

import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.emissions.types.WarmPollutant;

import playground.agarwalamit.analysis.LoadMyScenarios;
import playground.agarwalamit.analysis.congestion.CongestionLinkAnalyzer;
import playground.agarwalamit.analysis.emission.EmissionLinkAnalyzer;
import playground.agarwalamit.analysis.spatial.GeneralGrid.GridType;
import playground.agarwalamit.analysis.spatial.SpatialDataInputs;
import playground.agarwalamit.analysis.spatial.SpatialInterpolation;

import com.vividsolutions.jts.geom.Point;

/**
 * @author amit
 */

public class MunichSpatialPlots {
	
	String runDir = "/Users/amit/Documents/repos/runs-svn/detEval/emissionCongestionInternalization/output/1pct/run9/";
	String bau = runDir+"/baseCaseCtd";
	String policyScenario = runDir+"/ci";

	public static void main(String[] args) {
		MunichSpatialPlots plots = new MunichSpatialPlots();
		plots.writeCongestionCells();
//		plots.writeEmissionCells();
	}

	public void writeCongestionCells(){
		Map<Double, Map<Id, Double>> linkDelaysBau = new HashMap<>();
		Map<Double, Map<Id, Double>> linkDelaysPolicy = new HashMap<>();

		// setting of input data
		SpatialDataInputs inputs = new SpatialDataInputs("line",bau);
		inputs.setGridInfo(GridType.HEX, 500);
		inputs.setShapeFile("/Users/amit/Documents/repos/shared-svn/projects/detailedEval/Net/shapeFromVISUM/urbanSuburban/cityArea.shp");

		SpatialInterpolation plot = new SpatialInterpolation(inputs,runDir+"/analysis/spatialPlots/");
		
		Scenario sc = LoadMyScenarios.loadScenarioFromNetwork(inputs.initialCaseNetworkFile);

		CongestionLinkAnalyzer emsLnkAna = new CongestionLinkAnalyzer(LoadMyScenarios.getSimulationEndTime(inputs.initialCaseConfig), inputs.initialCaseEventsFile, 1); 
		emsLnkAna.init(sc);
		emsLnkAna.preProcessData();
		emsLnkAna.postProcessData();
		linkDelaysBau = emsLnkAna.getCongestionPerLinkTimeInterval();

		if(inputs.isComparing){
			emsLnkAna = new CongestionLinkAnalyzer(LoadMyScenarios.getSimulationEndTime(inputs.compareToCaseConfig), inputs.compareToCaseEventsFile, 1);
			emsLnkAna.init(LoadMyScenarios.loadScenarioFromNetwork(inputs.compareToCaseNetwork));
			emsLnkAna.preProcessData();
			emsLnkAna.postProcessData();
			linkDelaysPolicy = emsLnkAna.getCongestionPerLinkTimeInterval();
		}
		double sumDelays =0;

		for(double time :linkDelaysBau.keySet()){
			for(Link l : sc.getNetwork().getLinks().values()){
				Id<Link> id = l.getId();

				if(plot.isInResearchArea(l)){

					double delays = 0;

					if(inputs.isComparing){

						double linkDelayBau =0;
						double linkDelayPolicy =0;

						if(linkDelaysBau.get(time).containsKey(id) && linkDelaysPolicy.get(time).containsKey(id)) {
							linkDelayBau = 100 * linkDelaysBau.get(time).get(id);
							linkDelayPolicy = 100 * linkDelaysPolicy.get(time).get(id);
						} else if(linkDelaysBau.get(time).containsKey(id)){
							linkDelayBau = 100 * linkDelaysBau.get(time).get(id);
						} else if(linkDelaysPolicy.get(time).containsKey(id)){
							linkDelayPolicy = 100 * linkDelaysPolicy.get(time).get(id);
						}
						delays = linkDelayPolicy - linkDelayBau;

					} else {

						if(linkDelaysBau.get(time).containsKey(id)) delays = 100 * linkDelaysBau.get(time).get(id);
						else delays =0;
					}

					plot.processLink(l,  delays);
					sumDelays += (delays);
				}
			}
		}

		plot.writeRData("delays");
		SpatialDataInputs.LOG.info("Total delays from link emission map is "+sumDelays);

		double cellWeights =0;
		for(Point p: plot.getCellWeights().keySet()){
			cellWeights += plot.getCellWeights().get(p);
		}
		SpatialDataInputs.LOG.info("Total delays from cell weights  is "+cellWeights);

	}

	
	public void writeEmissionCells(){
		Map<Double,Map<Id<Link>,SortedMap<String,Double>>> linkEmissionsBau = new HashMap<>();
		Map<Double,Map<Id<Link>,SortedMap<String,Double>>> linkEmissionsPolicy = new HashMap<>();
		
		// setting of input data
		SpatialDataInputs inputs = new SpatialDataInputs("line",bau);
		inputs.setGridInfo(GridType.HEX, 500);
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
		linkEmissionsBau = emsLnkAna.getLink2TotalEmissions();

		if(inputs.isComparing){
			emsLnkAna = new EmissionLinkAnalyzer(LoadMyScenarios.getSimulationEndTime(inputs.compareToCaseConfig), inputs.compareToCaseEmissionEventsFile, 1);
			emsLnkAna.init();
			emsLnkAna.preProcessData();
			emsLnkAna.postProcessData();
			linkEmissionsPolicy = emsLnkAna.getLink2TotalEmissions();
		}


		Scenario sc = LoadMyScenarios.loadScenarioFromNetwork(inputs.initialCaseNetworkFile);
		double sumEmission =0;

		for(double time :linkEmissionsBau.keySet()){
			for(Link l : sc.getNetwork().getLinks().values()){
				Id<Link> id = l.getId();

				if(plot.isInResearchArea(l)){

					double emiss = 0;

					if(inputs.isComparing){

						double linkEmissionBau =0;
						double linkEmissionPolicy =0;

						if(linkEmissionsBau.get(time).containsKey(id) && linkEmissionsPolicy.get(time).containsKey(id)) {
							linkEmissionBau = 100 * linkEmissionsBau.get(time).get(id).get(WarmPollutant.NO2.toString());
							linkEmissionPolicy = 100 * linkEmissionsPolicy.get(time).get(id).get(WarmPollutant.NO2.toString());
						} else if(linkEmissionsBau.get(time).containsKey(id)){
							linkEmissionBau = 100 * linkEmissionsBau.get(time).get(id).get(WarmPollutant.NO2.toString());
						} else if(linkEmissionsPolicy.get(time).containsKey(id)){
							linkEmissionPolicy = 100 * linkEmissionsPolicy.get(time).get(id).get(WarmPollutant.NO2.toString());
						}
						emiss = linkEmissionPolicy - linkEmissionBau;

					} else {

						if(linkEmissionsBau.get(time).containsKey(id)) emiss = 100 * linkEmissionsBau.get(time).get(id).get(WarmPollutant.NO2.toString());
						else emiss =0;
					}

					plot.processLink(l,  emiss);
					sumEmission += (emiss);
				}
			}
		}

		plot.writeRData("NO2");
		SpatialDataInputs.LOG.info("Total NO2 emissions from link emission map is "+sumEmission);

		double cellWeights =0;
		for(Point p: plot.getCellWeights().keySet()){
			cellWeights += plot.getCellWeights().get(p);
		}
		SpatialDataInputs.LOG.info("Total NO2 emissions from cell weights  is "+cellWeights);

	}

}
