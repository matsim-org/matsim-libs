/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.juliakern.responsibilityOffline;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.emissions.types.ColdPollutant;
import org.matsim.contrib.emissions.types.WarmPollutant;


public interface DistributionConfiguration {

	public abstract Double getSimulationEndTime();

	public abstract boolean storeResponsibilityEvents();

	public abstract Map<Id<Link>, ? extends Link> getLinks();

	public abstract String getMunichShapeFile();

	public abstract String getEventsFile();

	public abstract String getEmissionFile();

	public abstract String getOutPathStub();

	public abstract double getXmin();

	public abstract double getXmax();

	public abstract double getYmin();

	public abstract double getYmax();

	public abstract int getNoOfTimeBins();

	public abstract int getNumberOfXBins();

	public abstract int getNumberOfYBins();

	public abstract WarmPollutant getWarmPollutant2analyze();

	public abstract ColdPollutant getColdPollutant2analyze();

	public abstract Double getTimeBinSize();

	public abstract Map<Id<Link>, Integer> getLink2yBin();

	public abstract Map<Id<Link>, Integer> getLink2xBin();

}