/* *********************************************************************** *
 * project: org.matsim.*
 * PlansFileSnapshotWriter.java
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

package org.matsim.utils.vis.snapshots.writers;

import org.matsim.gbl.Gbl;
import org.matsim.population.Act;
import org.matsim.population.Person;
import org.matsim.population.PersonImpl;
import org.matsim.population.Plan;
import org.matsim.population.PlanImpl;
import org.matsim.population.Population;
import org.matsim.population.PopulationWriter;
import org.matsim.utils.geometry.CoordImpl;
import org.matsim.utils.misc.Time;

/**
 * Writes the current position of all vehicles into a plans file.
 *
 * @author glaemmel
 *
 */
public class PlansFileSnapshotWriter implements SnapshotWriter {

	private String filePrefix;
	private String fileSuffix;

	private String version = null;
	private String filename = null;

	private double currenttime = -1;

	private Population plans = null;

	public PlansFileSnapshotWriter(String snapshotFilePrefix, String snapshotFileSuffix){
		this.filePrefix = snapshotFilePrefix;
		this.fileSuffix = snapshotFileSuffix;

		this.version = Gbl.getConfig().plans().getOutputVersion();
	}

	public void beginSnapshot(double time) {
		this.plans = new Population(Population.NO_STREAMING);
		this.filename = this.filePrefix + Time.writeTime(time, "-") + "." + this.fileSuffix;
		this.currenttime = time;
	}

	public void endSnapshot() {
		writePlans();
		this.plans = null;
		this.currenttime = -1;
	}

	/**
	 * Writes the position infos as plans to a file using
	 * {@link org.matsim.population.PopulationWriter}
	 */
	private void writePlans() {
		PopulationWriter pw = new PopulationWriter(this.plans, this.filename, this.version);
		pw.write();
	}

	public void addAgent(PositionInfo position) {
		Person pers = new PersonImpl(position.getAgentId());

		Plan plan = new PlanImpl(pers);
		Act actA = new Act("h", new CoordImpl(position.getEasting(), position.getNorthing()),
				position.getLink());
		actA.setEndTime(this.currenttime);
		plan.addAct(actA);
		pers.addPlan(plan);
		this.plans.addPerson(pers);
	}

	public void finish() {
	}

}
