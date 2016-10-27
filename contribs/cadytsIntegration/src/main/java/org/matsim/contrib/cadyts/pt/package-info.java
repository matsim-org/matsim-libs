/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
/**
 * Integrates automatic calibration upon public transport line occupancies using Cadyts into MATSim.
 *
 * <h2>Entry point / How to use</h2>
 * <ul>
 * <li>Use <code>org.matsim.contrib.cadyts.pt.CadytsPtPlanStrategy</code> as a replanning strategy.</li>
 * <li>Add the following configuration parameters (approximately; <i>this has changed since inception;</i> 
 * check auto-generated config comments; use <i>second</i> config dump for that):
 *     <pre>
 * &lt;module name="cadytsPt"&gt;
 *   &lt;param name="startTime" value="05:00:00" /&gt;
 *
 *   &lt;param name="endTime" value="21:00:00" /&gt;
 *
 *   &lt;!-- Comma-separated list of transit lines to be calibrated. -&gt;
 *   &lt;param name="calibratedLines" value="M44" /&gt;
 *
 *   &lt;param name="writeAnalysisFile" value="false" /&gt;
 *
 *   &lt;!-- see cadyts documentation for the meaning of the following values. --&gt;
 *   &lt;param name="regressionInertia" value="0.95" /&gt;
 *   &lt;param name="minFlowStddevVehH" value="8.0" /&gt;
 *   &lt;param name="freezeIteration" value="2147483647" /&gt;
 *   &lt;param name="preparatoryIterations" value="1" /&gt;
 *   &lt;param name="varianceScale" value="1.0" /&gt;
 *   &lt;param name="useBruteForce" value="true" /&gt;
 *
 * &lt;/module&gt;</pre>
 *   These parameters are defined in {@link org.matsim.contrib.cadyts.general.CadytsConfigGroup}
 *
 * </li>
 * <li> There also needs to be a ptCounts entry, something like:
 * <br>
 * <pre>
 * 	&lt;module name="ptCounts"&gt;
 *		&lt;param name="inputOccupancyCountsFile" value="path-to-counts-file" /&gt;
 *	&lt;/module&gt;</pre>
 * And (obviously) a working ptCounts file.
 * </li>
 * <li> It is a unfortunate that the counts file takes measurements in hourly values, while cadyts takes arbitrary time spans. 
 * (The cadyts convention seems more powerful, thus we did not want to reduce it to the "Counts" convention.)
 * As long as the cadytsPt timeBinSize is set to 3600, things should be straightforward, and there is also (I think) no
 * problem if there are measurements for times outside the cadytsPt startTime/endTime interval. yyyy Unfortunately,
 * I cannot remember how the counts file is interpreted once the cadytsPt timeBinSize is set to something different: Does the
 * Counts file than think in terms of time bins rather than in terms of hours?  In fact, I think not; rather, it is probably as
 * follows: Counts still refer to hours.  If, say, you use timeBinSize of 7200 and start/endTime as 05:00/09:00, then the code
 * will aggregate counts from the 6th and 7th hour into one time bin, etc.  If things do not correspond, the code will probably
 * complain.  See CadytsBuilder.buildCalibrator, since there are some consistency checks.  (kai, oct'12) 
 * <li>Typically, {@link org.matsim.contrib.cadyts.pt.CadytsPtPlanStrategy} should be the only
 *     plan strategy being used. So it is advised to first run the simulation until every
 *     agent has a few (different) plans, and then do some iterations using only the
 *     calibration strategy.</li>
 * </ul>
 *
 */
package org.matsim.contrib.cadyts.pt;
