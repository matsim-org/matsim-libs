/* *********************************************************************** *
 * project: org.matsim.*
 * SnapshotWriter.java
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

package org.matsim.vis.snapshotwriters;

/**
 * Interface to implement custom snapshot writers. A snapshot contains information
 * about agents (location, speed) at a specific moment in time. Depending on the
 * format, multiple snapshots can be stored in one file. A snapshot writer should
 * implement a custom constructor to initialize it. For each snapshot, first the
 * method <code>beginSnapshot()</code> will be called, followed by several calls
 * to <code>addAgent()</code> and a final call to <code>endSnapshot()</code>. When
 * no more snapshots will have to be written, <code>finish()</code> is called.
 *
 * @author mrieser
 */
public interface SnapshotWriter {

		/**
		 * Tells the snapshot writer that a new snapshot begins at the specified time.
		 *
		 * @param time The time of the snapshot.
		 */
		public void beginSnapshot(double time);

		/**
		 * Tells the snapshot writer that no more vehicles will be added to the current snapshot.
		 */
		public void endSnapshot();

		/**
		 * Adds an agent to the current snapshot.
		 *
		 * @param position The position, id, and speed of the agent.
		 */
		public void addAgent(AgentSnapshotInfo position);

		/**
		 * Tells the snapshot writer that no more snapshots will be added ("destructor").
		 */
		public void finish();

}
