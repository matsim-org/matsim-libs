/* *********************************************************************** *
 * project: org.matsim.*
 * TrafficManagement.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.withinday.trafficmanagement;

import java.util.ArrayList;
import java.util.List;




/**
 * @author dgrether
 *
 */
public class TrafficManagement {

	private List<VDSSign> signs;

	private List<Accident> accidents;

	public TrafficManagement() {
		this.signs = new ArrayList<VDSSign>();
		this.accidents = new ArrayList<Accident>();
	}

	public void addVDSSign(final VDSSign vdsSign) {
		this.signs.add(vdsSign);
	}

	public List<VDSSign> getVDSSigns() {
		return this.signs;
	}

	public void updateBeforeSimStrep(final double time) {
		for (VDSSign s : this.signs) {
			s.calculateOutput(time);
		}
	}

	public void addAccident(final Accident accident) {
		this.accidents.add(accident);
	}

	public List<Accident> getAccidents() {
		return this.accidents;
	}

	public void setupIteration(final int iteration) {
		for (VDSSign s : this.signs) {
			s.setupIteration();
		}

	}

	/**
	 * This method is called after the prepareSim() method of the QueueSimulation was
	 * run. It is used to initialize all elements in the TrafficManagement which
	 * depend on a initialized Simulation.
	 */
	public void simulationPrepared() {
		for (VDSSign s : this.signs) {
			s.simulationPrepared();
		}
	}

	public void finishIteration() {
		for (VDSSign s : this.signs) {
			s.finishInteration();
		}
	}

}
