/**
 * Integrates automatic calibration upon public transport line occupancies using Cadyts into MATSim.
 *
 * <h2>Entry point / How to use</h2>
 * <ul>
 * <li>Use <code>org.matsim.contrib.cadyts.pt.CadytsPtPlanStrategy</code> as a replanning strategy.</li>
 * <li>Add the following configuration parameters:<br />
 *     <pre>
 * &lt;module name="cadytsPt"&gt;
 *   &lt;!-- The first hour of the day to be used for calibration (start counting hours with 1, not 0) --&gt;
 *   &lt;param name="startHour" value="5" /&gt;
 *
 *   %lt;!-- The last hour of the day to be used for calibration (start counting hours with 1, not 0) --&gt;
 *   &lt;param name="endHour" value="20" /&gt;
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
 * &lt;/module&gt;
 *     </pre>
 *   These parameters are defined in {@link org.matsim.contrib.cadyts.pt.CadytsPtConfigGroup}
 *
 * </li>
 * <li>Typically, {@link org.matsim.contrib.cadyts.pt.CadytsPtPlanStrategy} should be the only
 *     plan strategy being used. So it is advised to first run the simulation until every
 *     agent has a few (different) plans, and then do some iterations using only the
 *     calibration strategy.</li>
 * </ul>
 *
 */
package org.matsim.contrib.cadyts.pt;

