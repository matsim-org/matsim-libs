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
import playground.agarwalamit.analysis.spatial.SpatialDataInputs;
import playground.agarwalamit.analysis.spatial.SpatialInterpolation;

import com.vividsolutions.jts.geom.Point;

/**
 * @author amit
 */

public class MunichSpatialPlots {

	private static Map<Double,Map<Id<Link>,SortedMap<String,Double>>> linkEmissions;

	public static void main(String[] args) {

		SpatialInterpolation plot = new SpatialInterpolation();


		EmissionLinkAnalyzer emsLnkAna = new EmissionLinkAnalyzer(LoadMyScenarios.getSimulationEndTime(SpatialDataInputs.BAUConfig), SpatialDataInputs.BAUEmissionEventsFile, 1);
		emsLnkAna.init();
		emsLnkAna.preProcessData();
		emsLnkAna.postProcessData();
		linkEmissions = emsLnkAna.getLink2TotalEmissions();

		Scenario sc = LoadMyScenarios.loadScenarioFromNetwork(SpatialDataInputs.BAUNetwork);
		double sumEmission =0;

		for(double time :linkEmissions.keySet()){
			for(Id<Link> id : linkEmissions.get(time).keySet()){
				Link l =sc.getNetwork().getLinks().get(id);
				if(plot.isInResearchArea(l)){
					plot.processLink(l,  SpatialDataInputs.scalingFactor * linkEmissions.get(time).get(id).get(WarmPollutant.NO2.toString()));
					sumEmission += (linkEmissions.get(time).get(id).get(WarmPollutant.NO2.toString()) * SpatialDataInputs.scalingFactor );
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
