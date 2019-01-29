/**
 * <h2>Signal Groups</h2>
 * 
 * The data classes of this package can be used to model groups of signals.
 * For each signal system defined in the SignalSystemsData container the 
 * signals of the system can be grouped. A SignalGroupData consists of:
 * <ul>
 *   <li> The Id of the SignalGroupData </li>
 *   <li> The Id of the signal system the group belongs to</li>
 *   <li> The signals that are grouped</li> 
 * </ul>
 * 
 * Signals that are in one group have a common onset and dropping in the fixed-time control model
 * of the <code>org.matsim.contrib.signals.model</code> package. A typical case to group signals is 
 * for example to group the signal controlling traffic in direction straight ahead and the signal for 
 * the opposite direction. 
 * <p>
 * The Events used by the signal model are thrown when a signal group is changing its state.
 * </p>
 * <p>
 * The classes correspond to the data format defined in signalGroups_v2.0.xsd for that a reader
 * and a writer can be found in this package. 
 * </p>
 * <p>
 * The top-level container and central entry point is the interface <code>SignalGroupsData</code>. There
 * a factory for the components of the container can be found. 
 * </p>
 * You can find a convenience method in the <code>org.matsim.contrib.signals.SignaUtils</code> class
 * that creates a signal group for each signal of a signalsystem and adds it to the signal groups container.
 * 
 * <h2>Package Maintainer(s):</h2>
 * <ul>
 *   <li>Dominik Grether</li>
 * </ul>
 * 
 * Changes by non-maintainers are prohibited. Patches very welcome!
 * 
 * @author dgrether
 */
package org.matsim.contrib.signals.data.signalgroups.v20;