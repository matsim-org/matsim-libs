/**
 *  This package contains interfaces and classes that provide functionality to plug a signal system model 
 *  in the MATSim framework.
 *  
 *  <h2>Model description</h2>
 *  
 *  <h3>Overview</h3>
 *  
 *  The model consists of several layers:
 *  
 *	<ul>
 *		<li>A data layer</li>
 *		<li>A model layer</li>
 *		<li>A layer to couple the model to the mobility simulation</li>
 *		<li>A layer to build the other layers</li>
 *	</ul>
 * 
 * Each layer can be found in a subpackage of this package. 
 * 
 * TODO short overview 
 * 
 * In addition to the main layers, the package contains some utility classes and needful helpers to plug 
 * the model into the MATSim Controler and to generate the required/desired input data.
 * 
 * <h3>Extensibility</h3>
 * 
 * The layers of the model can be used separately and independently from each other. The default implementation 
 * uses all layers of the model using the building layer to couple the other layers together. If you want to exchange or extend 
 * a certain part of the model look for the factory and builders of the default implementation that create the components of
 * interest. Implement your customized factories and builders using or extending the provided interfaces and replace 
 * the factories/builders of the default implementation by your instances to get your extension or customization into the model. 
 *  
 * TODO pointer to example
 *  
 * <h2>Usage restrictions:</h2>
 * <ul>
 *   <li> Do not use, yet!</li>
 * </ul>
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
package org.matsim.signalsystems;