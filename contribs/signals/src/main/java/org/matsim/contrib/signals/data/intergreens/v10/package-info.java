/**
 * <h2>Intergreen times</h2>
 * 
 * The data classes of this package can be used to specify intergreen times, 
 * i.e. the time a signal group has to show red before it "is allowed" 
 * to show green, which may depends on the time another signal group switched to red. 
 * There is a logic implemented that checks this intergreen times and throws an 
 * error or warning if the signal control does not comply with the provided data.
 * <p>
 * The classes correspond to the data format defined in <code>intergreenTimes_v1.0.xsd</code> for that a reader
 * and a writer can be found in this package. 
 * </p>
 * <p>
 * The top-level container and central entry point is the interface <code>IntergreenTimesData</code>. There
 * a factory for the components of the container can be found. 
 * </p>
 * <p>
 * For more information see the documentation in <code>contrib/signals/docs/user-guide</code>.
 * </p>
 * 
 * <h2>Package Maintainer(s):</h2>
 * <ul>
 *   <li>Dominik Grether</li>
 * </ul>
 * 
 * Changes by non-maintainers are prohibited. Patches very welcome!
 * 
 * @author dgrether
 * @author tthunig
 */
package org.matsim.contrib.signals.data.intergreens.v10;