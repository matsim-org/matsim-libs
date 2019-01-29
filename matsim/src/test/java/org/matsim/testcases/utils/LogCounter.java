/* *********************************************************************** *
 * project: org.matsim.*
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

package org.matsim.testcases.utils;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;

/**
 * A helper class to be used as logger for counting the number of log-messages per log-level.
 * <br />
 * To be used in the following way:
 * <pre>
 *   LogCounter logger = new LogCounter(Level.WARN);
 *   logger.activiate()
 *   // do your stuff
 *   logger.deactiviate();
 *   System.out.println("number of warnings: " + logger.getWarnCount());
 * </pre>
 * It is important that the logger is deactivated at the end of your test! If not, it
 * will also count the log-messages of all other tests running afterwards, slowing down
 * the test execution.
 *
 * @author mrieser
 */
public class LogCounter extends AppenderSkeleton {
	private int cntFATAL = 0;
	private int cntERROR = 0;
	private int cntWARN = 0;
	private int cntINFO = 0;
	private int cntDEBUG = 0;
	private int cntTRACE = 0;

	public LogCounter() {
		this(Level.ALL);
	}

	public LogCounter(Level treshold) {
		this.setThreshold(treshold);
	}

	@Override
	protected void append(final LoggingEvent event) {
		if (event.getLevel() == Level.FATAL) this.cntFATAL++;
		if (event.getLevel() == Level.ERROR) this.cntERROR++;
		if (event.getLevel() == Level.WARN) this.cntWARN++;
		if (event.getLevel() == Level.INFO) this.cntINFO++;
		if (event.getLevel() == Level.DEBUG) this.cntDEBUG++;
		if (event.getLevel() == Level.TRACE) this.cntTRACE++;
	}

	public void close() {
	}

	public boolean requiresLayout() {
		return false;
	}

	public int getFatalCount() {
		return this.cntFATAL;
	}

	public int getErrorCount() {
		return this.cntERROR;
	}

	public int getWarnCount() {
		return this.cntWARN;
	}

	public int getInfoCount() {
		return this.cntINFO;
	}

	public int getDebugCount() {
		return this.cntDEBUG;
	}

	public int getTraceCount() {
		return this.cntTRACE;
	}

	public void resetCounts() {
		this.cntFATAL = 0;
		this.cntERROR = 0;
		this.cntWARN = 0;
		this.cntINFO = 0;
		this.cntDEBUG = 0;
		this.cntTRACE = 0;
	}

	public void activiate() {
		Logger.getRootLogger().addAppender(this);
	}

	public void deactiviate() {
		Logger.getRootLogger().removeAppender(this);
	}
}
