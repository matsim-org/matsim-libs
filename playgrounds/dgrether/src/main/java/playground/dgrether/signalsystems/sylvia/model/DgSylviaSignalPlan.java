/* *********************************************************************** *
 * project: org.matsim.*
 * DgSylviaSignalPaln
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.dgrether.signalsystems.sylvia.model;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.signals.model.DatabasedSignalPlan;
import org.matsim.signals.data.signalcontrol.v20.SignalPlanData;
import org.matsim.signals.model.SignalGroup;
import org.matsim.signals.model.SignalPlan;


/**
 * @author dgrether
 *
 */
public class DgSylviaSignalPlan implements SignalPlan {

	private DatabasedSignalPlan delegate;
	private List<DgExtensionPoint> extensionPoints = new ArrayList<DgExtensionPoint>();
	private int maxExtensionTime = 0;
	private int fixedTimeCycle = 0;

	
	public DgSylviaSignalPlan(DatabasedSignalPlan delegate){
		this.delegate = delegate;
	}
	
	@Override
	public List<Id<SignalGroup>> getDroppings(double timeSeconds) {
		return delegate.getDroppings(timeSeconds);
	}

	@Override
	public List<Id<SignalGroup>> getOnsets(double timeSeconds) {
		return delegate.getOnsets(timeSeconds);
	}

	@Override
	public Double getEndTime() {
		return delegate.getEndTime();
	}

	@Override
	public Double getStartTime() {
		return delegate.getStartTime();
	}

	@Override
	public Id<SignalPlan> getId() {
		return delegate.getId();
	}

	@Override
	public Integer getOffset() {
		return delegate.getOffset();
	}

	@Override
	public Integer getCycleTime() {
		return delegate.getCycleTime();
	}
	
	public void addExtensionPoint(DgExtensionPoint extensionPoint){
		this.extensionPoints.add(extensionPoint);
	}

	public List<DgExtensionPoint> getExtensionPoints() {
		return extensionPoints;
	}

	
	public int getMaxExtensionTime() {
		return maxExtensionTime;
	}

	
	public void setMaxExtensionTime(int maxExtensionTime) {
		this.maxExtensionTime = maxExtensionTime;
	}

	
	public int getFixedTimeCycle() {
		return fixedTimeCycle;
	}

	
	public void setFixedTimeCycle(int fixedTimeCycle) {
		this.fixedTimeCycle = fixedTimeCycle;
	}
	
	public SignalPlanData getPlanData(){
		return this.delegate.getPlanData();
	}
	
}
