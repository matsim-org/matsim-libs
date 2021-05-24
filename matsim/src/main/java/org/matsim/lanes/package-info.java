
/* *********************************************************************** *
 * project: org.matsim.*
 * package-info.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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
 * Adding lanes to a link means the following:
 * <ul>
 *   <li>The link is split in two parts: The original lane at the beginning of the link and
 *   the lane part at it's end.</li>
 *   <li>The original lane preserves the properties of the link without lanes. </li>
 *   <li>The link may be widened or narrowed at its end due to the lanes. </li>
 *   <li>Bottleneck is at the end of the original lane. This may have the consequence that
 *   spillback occurs faster.</li>
 * </ul>
 * </p>
 * 
 * <p> 
 * All lane information is given by the top-level container Lanes. 
 * 
 * A lane is added to a link by adding a LaneToLinkAssignment instance to the container.
 * For each link one LaneToLinkAssignment instance is needed, that holds all Lane 
 * instances for this specific link.
 * </p>
 * <p> 
 * There is no extra test package for the lanes implementation. However
 * tests for lanes can be found in the signals contrib test package.
 * </p>
 * <h2>Usage restrictions:</h2>
 * <ul>
 *   <li> Each link's lanes must cover all toLinks of the link's toNode within the toLink information
 *   of the lanes. </li>
 *   <li> Exactly one lane of each link has to have the link length, i.e. start at the beginning of the link ('original lane'). </li>
 *   <li> Each link's lanes except the original lane must have the same length. </li>
 * </ul>
 * 
 * <h2>Package Maintainer(s):</h2>
 * <ul>
 *   <li>Theresa Thunig</li>
 * </ul>
 * 
 * Changes by non-maintainers are prohibited. Patches containing bugfixes or extensions however very welcome!
 */
package org.matsim.lanes;