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

package playground.vsp.airPollution.exposure;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;


public class ResponsibilityGridTools {

	private static final Logger LOGGER = org.apache.log4j.Logger.getLogger(ResponsibilityGridTools.class);

	private final Double dist0factor = 0.216;
	private final Double dist1factor = 0.132;
	private final Double dist2factor = 0.029;
	private final Double dist3factor = 0.002;

	private final Double timeBinSize;
	private final int noOfTimeBins;
	private final Map<Double, Map<Id<Link>, Double>> timebin2link2factor;
 
	private final Map<Id<Link>, Integer> links2xCells;
	private final Map<Id<Link>, Integer> links2yCells;
	private final int noOfXCells;
	private final int noOfYCells;

	private final int maxWarnCount = 2;
	private int warnCount = 0;

	/* TODO : adding this switch to inform the user to use first interval handler with normal events file
	 * and provide activity durations and then estiamtes the air pollution exposure costs. This switch should go away after
	 * https://matsim.atlassian.net/browse/MATSIM-634
	 */
	private boolean isDurationsStored = false;
	
	public ResponsibilityGridTools(Double timeBinSize, int noOfTimeBins, GridTools gridTools) {
		this.timeBinSize = timeBinSize;
		this.noOfTimeBins = noOfTimeBins;

		this.timebin2link2factor = new HashMap<>();
		for(int i = 1; i< noOfTimeBins +1; i++){
			timebin2link2factor.put((i* this.timeBinSize), new HashMap<>());
		}

		this.links2xCells = gridTools.getLink2XBins();
		this.links2yCells = gridTools.getLink2YBins();
		this.noOfXCells= gridTools.getNoOfXCells();
		this.noOfYCells= gridTools.getNoOfYCells();
	}

	public Double getFactorForLink(Id<Link> linkId, double time) {

		if (! isDurationsStored && warnCount++ <= maxWarnCount) {
			LOGGER.warn("Make sure, you have stored activity durations by using "+ IntervalHandler.class.getSimpleName()
					+" along with normal events and pass it to the "+ResponsibilityGridTools.class.getSimpleName());
		}

		Double currentTimeBin = getTimeBin(time);
		
		if(timebin2link2factor!=null){
			if(timebin2link2factor.get(currentTimeBin)!=null){
				if(timebin2link2factor.get(currentTimeBin).get(linkId)!=null){
					return timebin2link2factor.get(currentTimeBin).get(linkId);
				}
			}
		}
		return 0.0;
	}
 
	public void resetAndcaluculateRelativeDurationFactors(SortedMap<Double, Double[][]> duration) {
		isDurationsStored = true;
		timebin2link2factor.clear();
		
		// each time bin - generate new map
		for(Double timeBin : duration.keySet()){timebin2link2factor.put(timeBin, new HashMap<Id<Link>, Double>());
		// calculate total durations for each time bin
			Double sumOfCurrentTimeBin = 0.0;
			Double [][] currentDurations = duration.get(timeBin);
			for(int x=0; x< currentDurations.length; x++){
				for(int y=0; y<currentDurations[x].length;y++){
					sumOfCurrentTimeBin += currentDurations[x][y];
				}
			}
			// calculate average for each time bin
			Double averageOfCurrentTimeBin = sumOfCurrentTimeBin/currentDurations.length/currentDurations[0].length;
			// calculate factor for each link for current time bin
			for(Id<Link> linkId: links2xCells.keySet()){
				if (links2yCells.containsKey(linkId)) { // only if in research are
					Double relativeFactorForCurrentLink = getRelativeFactorForCurrentLink(
							averageOfCurrentTimeBin, currentDurations, linkId);
					timebin2link2factor.get(timeBin).put(linkId,relativeFactorForCurrentLink); 
				}
			}
		}		
	}

	private Double getRelativeFactorForCurrentLink(Double averageOfCurrentTimeBin, Double[][] currentDurations, Id<Link> linkId) {
		
		Double relevantDuration = new Double(0.0);
		if(links2xCells.get(linkId)!=null && links2yCells.get(linkId)!=null){
			Cell cellOfLink = new Cell(links2xCells.get(linkId), links2yCells.get(linkId));
			for(int distance =0; distance <= 3; distance++){
				List<Cell> distancedCells = cellOfLink .getCellsWithExactDistance(noOfXCells, noOfYCells, distance);
				for(Cell dc: distancedCells){

					try {
						if (currentDurations[dc.getX()][dc.getY()]!=null) {
							Double valueOfdc = currentDurations[dc.getX()][dc.getY()];
							switch (distance) {
								case 0:
									relevantDuration += dist0factor * valueOfdc;
									break;
								case 1:
									relevantDuration += dist1factor * valueOfdc;
									break;
								case 2:
									relevantDuration += dist2factor * valueOfdc;
									break;
								case 3:
									relevantDuration += dist3factor * valueOfdc;
									break;
							}
						}

					} catch (ArrayIndexOutOfBoundsException e) {
						// nothing to do not in research area
					}
				}
			}
			return relevantDuration / averageOfCurrentTimeBin;
		}
		return 0.0;
	}

	private Double getTimeBin(double time) {
		Double timeBin = Math.ceil(time/timeBinSize)*timeBinSize;
		if(timeBin<=1)timeBin=timeBinSize;
		return timeBin;
	}

	public int getNoOfTimeBins() {
		return this.noOfTimeBins;
	}

	public Double getTimeBinSize() {
		return this.timeBinSize;
	}
}
