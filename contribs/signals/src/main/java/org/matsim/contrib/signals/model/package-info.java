/**
 * <h2>Signal Model</h2>
 * 
 * The signal model implementation consists of several components:
 * <ul>
 *   <li>A SignalSystemsManager instance as central point in the model, providing its components and
 *   with pointers to each other. Furthermore it provides access to the MATSim EventsManager.</li>
 *   <li>An implementation for fixed-time signal control, see below</li>
 *   <li>One or several implementations of the SignalController that may contain algorithms for 
 *   signal control.</li>
 *   <li>A logic to show a red-amber and a amber phase when a signal group changes from red to green.</li>
 *   <li>A factory, i.e. an implementation of SignalSystemsFactory to create  the components of the model.</li>
 * 		<li>An implementation of the <code>Signal</code> interface that serves as connection between
 *				the <code>org.matsim.contrib.signals.model</code> package and the 
 * <code>org.matsim.contrib.signals.mobsim</code> package. 
 * 				Currently there are two implementations:
 *   <ul>
 *     <li><code>DatabasedSignal</code> for a Signal based on the given data.</li>
 *     <li><code>SignalImpl</code> for a Signal created in initialization code </li>
 *   </ul>
 *   </li>
 * </ul>
 * 
 * <h3>Fixed-time Signal Control<h3>
 * Fixed-time or also plan based signal control uses the attributes 
 * <ul>
 *   <li>cycle time</li>
 *   <li>offset</li>
 *   <li>onset</li>
 *   <li>dropping</li>
 * </ul>
 * 
 * Whereby onset is the second the signal group starts its switch to green within a cycle time. This may
 * first trigger a red-amber phase if amber times are defined. At second dropping within the cycle the switch
 * to red is triggered. If amber times are defined red is preceded by a phase where the signal group is shows
 * amber. The offset is the second onset and dropping are shifted in respect to the global simulation time.
 * 
 *	<h2>Package Maintainer(s):</h2>
 *		<ul>
 *			<li>Dominik Grether</li>
 * 		</ul>
 * 
 *	Changes by non-maintainers are prohibited. Patches very welcome!
 * 
 *	@author dgrether
 */
package org.matsim.contrib.signals.model;