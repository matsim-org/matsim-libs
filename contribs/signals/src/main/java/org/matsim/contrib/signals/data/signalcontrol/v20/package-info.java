/**
 * <h2>Signal Control</h2>
 * 
 * The interface of this package can be used to store and create information about the 
 * controllers of the signal systems. Primarily it serves as container for a customizeable identifier
 * for all signal control algorithms used.
 * 
 * <p>
 * As a default control algorithm the package can be used to model a fixed-time control behavior. 
 * Therefore a signal plan is created containing settings for signal groups that specify an onset and  
 * the dropping for a signal group. The plan specifies further  an offset and a cycle time.
 * </p>
 * <p>
 * In general a fixed-time controller of a signal system may have several plans. However there is
 * no mechanism introduced yet, that implements plan selection, even if the model already has
 * data fields for daytime based selection. Thus always exactly one of the defined plans is used. This 
 * plan is however unspecified, use only one plan per signal system. The next release will
 * support a better behavior.
 * </p>
 * <p>
 * The classes correspond to the data format defined in signalControl_v2.0.xsd for that a reader
 * and a writer can be found in this package. 
 * </p>
 * <p>
 * The top-level container and central entry point is the interface <code>SignalControlData</code>. There
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
package org.matsim.contrib.signals.data.signalcontrol.v20;