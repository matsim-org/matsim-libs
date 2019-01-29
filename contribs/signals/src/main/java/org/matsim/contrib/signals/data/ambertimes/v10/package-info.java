/**
 * <h2>Amber times</h2>
 * 
 * The data classes of this package can be used to model the red-amber and
 * amber time a signal group is amber between the onset and dropping or vice versa.
 * <p>
 * This can be done globally by setting the defaults in the <code>AmberTimesData</code> instance
 * or by a <code>AmberTimeData</code> object per signal system.
 * </p>
 * <p>
 * The classes correspond to the data format defined in <code>amberTimes_v1.0.xsd</code> for that a reader
 * and a writer can be found in this package. 
 * </p>
 * <p>
 * The top-level container and central entry point is the interface <code>AmberTimesData</code>. There
 * a factory for the components of the container can be found. 
 * </p>

 * <h2>Package Maintainer(s):</h2>
 * <ul>
 *   <li>Dominik Grether</li>
 * </ul>
 * 
 * Changes by non-maintainers are prohibited. Patches very welcome!
 * 
 * @author dgrether
 */
package org.matsim.contrib.signals.data.ambertimes.v10;