/**
 *  This package contains classes to add lanes at the end of links.
 *  There is no extra test package for the lanes implementation. However
 *  tests for lanes can be found in the signalsystems test package.
 *
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