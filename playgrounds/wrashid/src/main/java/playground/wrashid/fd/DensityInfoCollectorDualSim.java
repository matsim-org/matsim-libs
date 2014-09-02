package playground.wrashid.fd;

/* *********************************************************************** *
 * project: org.matsim.*
 * DensityInfoCollector.java
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

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.parking.lib.GeneralLib;


// TODO: accumulate
public class DensityInfoCollectorDualSim extends AbstractDualSimHandler {

	private int binSizeInSeconds; // set the length of interval
	public HashMap<Id, double[]> density; // define
	private Map<Id<Link>, ? extends Link> filteredEquilNetLinks; // define
	int numberOfProcessedVehicles;

	private boolean isJDEQSim;
	private int calculationTimeBinSize=5;

	public DensityInfoCollectorDualSim(
			Map<Id<Link>, ? extends Link> filteredEquilNetLinks,
			int binSizeInSeconds, boolean isJDEQSim) {
		this.isJDEQSim = isJDEQSim;

		// and give the link set
		this.filteredEquilNetLinks = filteredEquilNetLinks;
		this.binSizeInSeconds = binSizeInSeconds;
	}

	@Override
	public void reset(int iteration) {
		density = new HashMap<Id, double[]>();
	}

	public HashMap<Id, double[]> getLinkDensities() {
		/**
		HashMap<Id, double[]> avgDensity = new HashMap<Id, double[]>();
		
		for (Id linkId : density.keySet()) {
			double[] linkDensity = density.get(linkId);
			
			double[] avgLinkDensity = density.put(linkId, new double[(86400 / binSizeInSeconds) + 1]);
			avgDensity.put(linkId, avgLinkDensity);
			
			
			for (int i=0;i<linkDensity.length;i++){
				
			}
			
		}
		*/
		for (Id linkId : density.keySet()) {
			double[] linkDensity = density.get(linkId);
			
			for (int i = 0; i < linkDensity.length; i++) {
				Link link = filteredEquilNetLinks.get(linkId);
				linkDensity[i]=linkDensity[i]/link.getLength()/link.getNumberOfLanes();
			}
			
		}
		
		
		
		return getAverageDensity(binSizeInSeconds/calculationTimeBinSize);
	}

	@Override
	public boolean isJDEQSim() {
		return isJDEQSim;
	}

	@Override
	public boolean isLinkPartOfStudyArea(Id linkId) {
		return filteredEquilNetLinks.containsKey(linkId);
	}

	@Override
	public void processLeaveLink(Id linkId, Id personId, double enterTime, double leaveTime) {

		if (!density.containsKey(linkId)) {
			density.put(linkId, new double[(86400 / calculationTimeBinSize) + 1]);
		}

		double[] bins = density.get(linkId);

		if (leaveTime < 86400) {
			int startBinIndex = (int) Math.round(Math.floor(GeneralLib
					.projectTimeWithin24Hours(enterTime)
					/ calculationTimeBinSize));
			int endBinIndex = (int) Math.round(Math.floor(GeneralLib
					.projectTimeWithin24Hours(leaveTime) / calculationTimeBinSize));

			for (int i = startBinIndex; i <= endBinIndex; i++) {
				bins[i]++;
			}
		}
		numberOfProcessedVehicles++;
	}
	
	public int getNumberOfProcessedVehicles(){
		return numberOfProcessedVehicles;
	}
	
	private HashMap<Id, double[]> getAverageDensity(int valuesPerBin) {

		HashMap<Id, double[]> avgDensity = new HashMap<Id, double[]>();
		
		for (Id linkId : density.keySet()) {

			double[] linkDensity = density.get(linkId);
			
			double[] avgLinkDensity = new double[(int) Math.ceil(linkDensity.length / valuesPerBin) + 1];
			avgDensity.put(linkId, avgLinkDensity);
			
			int index = 0;
			double sumDensity = 0.0;
			for (int i = 0; i < linkDensity.length; i++) {

				sumDensity += linkDensity[i];
				
				// if all entries of the time bin have been processed
				if ((i+1) % valuesPerBin == 0) {
					avgLinkDensity[index] = sumDensity / valuesPerBin;
					sumDensity = 0.0;
					index++;
				}
			}
		}

		return avgDensity;
	}

}
