/**
 *  This package contains interfaces and classes that provide functionality to plug a signal system model 
 *  in the MATSim framework.
 *  
 *  <h2>Usage</h2>
 *  
 *  If input files for signal systems are already available, simulation of traffic lights can be enabled via MATSim config options.
 *  <p>
 *  Examples of such configurations can be found as input of the code examples and tests in this package.
 *  </p>
 *  
 *  <h2>Data Model description</h2>
 *  
 *  If the corresponding input files are set in the matsim config and
 *  useSignalsystems are enabled the data is loaded into the Scenario and can be retrieved by calling 
 *  <code>(SignalsData) scenario.getScenarioElement(SignalsData.ELEMENT_NAME)</code>.
 *  
 *  <h2>Model description</h2>
 *  
 *  <h3>Overview</h3>
 *  
 *  The model consists of several logical layers that can be found as subpackages of this package.
 *  
 *  For more information see
 *  <ul>
 *   <li>Grether, D. S., 2014. Extension of a multi-agent transport simulation for traffic signal control and air transport systems. Ph.D. thesis, TU Berlin. </li>
 * </ul>
 *  
 * <h3>Extensibility</h3>
 * 
 * The layers of the model can be used separately and independently from each other. The default implementation 
 * uses all layers of the model in this package. The building layer is used to couple the other layers together. If you want to exchange or extend 
 * a certain part of the model have a look at the factory and builders of the default implementation that create the components of
 * interest. Implement your customized factories and builders using or extending the provided interfaces and replace 
 * the factories/builders of the default implementation by your instances to get your extension or customization into the model. 
 * <p>
 * An example of an adaptive signal control implementation and an example of an actuated signal control implementation can be found in 
 * this package (in the controller subpackage). The code examples also point to this implementations. 
 * See also the following publications, for more information on these traffic-responsive signals:
 * <ul>
 * 	<li>Grether, D., Bischoff, J., Nagel, K., 2011. Traffic-actuated signal control: Simulation of the user benefits in a big event real-world scenario. In: 2nd International Conference on Models and Technologies for ITS, Leuven, Belgium.</li>
 *   <li>K&uuml;hnel, N., Thunig, T., Nagel, K., 2018. Implementing an adaptive traffic signal control algorithm in an agent-based transport simulation. Procedia Computer Science 130, 894–899.</li>
 * 	<li>Thunig, T., K&uuml;hnel, N., Nagel, K., 2018. Adaptive traffic signal control for real-world scenarios in agent-based transport simulations. Transportation Research Procedia.</li>
 * </ul>
 * </p>
 *  
 * <h2>Package Maintainer(s):</h2>
 * <ul>
 *   <li>Theresa Thunig</li>
 * </ul>
 * 
 * Changes by non-maintainers are prohibited. Patches very welcome!
 * 
 * @author dgrether
 * @author tthunig
 */
package org.matsim.contrib.signals;

import org.matsim.contrib.signals.data.SignalsData;
