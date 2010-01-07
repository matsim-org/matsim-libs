/**
 *  This package contains classes to add lanes to links.
 * <p> 
 * A link may contain one or more lanes, that may have the following layout
 * (Dashes stand for the borders of the QueueLink, equal signs (===) depict one lane, 
 * plus signs (+) symbolize a decision point where one lane splits into several lanes):
 * <pre>
 * ----------------
 * ================
 * ----------------
 *</pre>
 *
 *<pre>
 * ----------------
 *          =======
 * ========+
 *          =======
 * ----------------
 *</pre>
 *
 *<pre>
 * ----------------
 *         ========
 * =======+========
 *         ========
 * ----------------
 * </pre>
 * 
 * 
 * The following layouts are not allowed:
 * <pre>
 * ----------------
 * ================
 * ================
 * ----------------
 *</pre>
 *
 *<pre>
 * ----------------
 * =======
 *        +========
 * =======
 * ----------------
 * </pre> 
 * </p> 
 * <p> 
 * All lane information is given by the top-level container LaneDefinitions. 
 * 
 * A lane is added to a link by adding a LaneToLinkAssignment instance to the container.
 * For each link one LaneToLinkAssignment instance is needed, that holds all Lane 
 * instances for this specific link.
 * </p>
 * <p> 
 * There is no extra test package for the lanes implementation. However
 * tests for lanes can be found in the signalsystems test package.
 * </p>
 * <h2>Usage restrictions:</h2>
 * <ul>
 *   <li> Each link's lanes must cover all toLinks of the link's toNode within the toLink information
 *   of the lanes. </li>
 * </ul>
 * 
 * <h2>Package Maintainer(s):</h2>
 * <ul>
 *   <li>Dominik Grether</li>
 * </ul>
 * 
 * Changes by non-maintainers are prohibited. Patches very welcome!
 */
package org.matsim.lanes;