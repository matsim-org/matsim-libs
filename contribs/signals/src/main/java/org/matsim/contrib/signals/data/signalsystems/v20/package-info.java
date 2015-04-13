/**
 * <h2>Signal Systems Data</h2>
 * 
 * The data classes of this package can be used to model the physical part of a signal systems, i.e.
 * <ul>
 *   <li> The existence of a signal system and defines its Id </li>
 *   <li> The signals belonging to the system </li> 
 * </ul>
 * <p>
 * The classes correspond to the data format defined in signalSystems_v2.0.xsd for that a reader
 * and a writer can be found in this package. 
 * </p>
 * <p>
 * The top-level container and central entry point is the interface <code>SignalSystemsData</code>. There
 * a factory for the components of the container can be found. 
 * </p>
 * <h3>Modelling Signals</h3>
 * 
 * Signals are modelled by the interface <code>SignalData</code>. A signal may control a complete link or one or
 * more specific lanes on a link. Furthermore control can be restricted to certain turning moves, i.e.
 * outgoing links of the node at the downstream end of the link the signal stands on. For example a link 
 * may have no lanes but two signals, one for left turns and one for right turns.
 * 
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
package org.matsim.contrib.signals.data.signalsystems.v20;