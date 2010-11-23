/**
 *  This package contains interfaces and classes that provide functionality to plug a signal system model 
 *  in the MATSim framework.
 *  
 *  <h2>Usage</h2>
 *  
 *  If input files for signal systems are already available, simulation of traffic lights can be enabled via MATSim config options:
 *  <ol>
 *    <li>Set the parameter <code>useSignalsystems</code> to <code>true</code> in the 
 *    config module <code>scenario</code></li>
 *    <li>Set at least three input file names in the config module <code>signalsystems</code>:
 *    <ol>
 *    	<li> parameter name: <code>signalsystems</code> value: path to a file in the <code>signalSystems_v2.0.xsd</code> file format </li>
 *    	<li> parameter name: <code>signalgroups</code> value: path to a file in the <code>signalGroups_v2.0.xsd</code> file format </li>
 *    	<li> parameter name: <code>signalcontrol</code> value: path to a file in the <code>signalControl_v2.0.xsd</code> file format </li>
 *    	<li> parameter name: <code>ambertimes</code> (optional) value: path to a file in the <code>amberTimes_v1.0.xsd</code> file format </li>
 *    </ol>
 *    </li>
 *  </ol>
 *  
 *  An example of such a configuration can be found in the input folder of the SignalSystemsIntegrationTest. If your data is in an older file format have a look at the
 *  package playground.dgrether.signalsystems.data.conversion for converters to the required file formats.
 *  
 *  <h2>Data Model description</h2>
 *  
 *  Documentation of the data model can be found in the package-info.java of the subpackage data. If the corresponding files are set in the matsim config and
 *  signalsystems are enabled the data is loaded by the matsim ScenarioLoader into the Scenario and can be retrieved by calling 
 *  scenario.getScenarioElement(SignalsData.class). Thereby scenario is an instance of Scenario.
 *  
 *  <h2>Model description</h2>
 *  
 *  <h3>Overview</h3>
 *  
 *  The model consists of several logical layers:
 *  
 *	<ul>
 *		<li>A data layer</li>
 *		<li>A model layer</li>
 *		<li>A layer to couple the model to the mobility simulation</li>
 *		<li>A layer to build the other layers</li>
 *	</ul>
 * 
 * Each layer can be found in a subpackage of this package, namely:
 * 
 * <ul>
 *		<li><code>signalsystems.data</code> the package containing the data layer</li>
 *		<li><code>signalsystems.model </code> the package containing the model layer</li>
 *		<li><code>signalsystems.mobsim</code>the package containing the layer to couple the model to the mobility simulation</li>
 *		<li><code>signalsystems.builder </code>the package containing the layer to build the other layers</li>
 *	</ul>
 * 
 * Each layer is documented separately in the package-info.java of the subpackage.
 * <p>
 * In addition to the main layers, the package contains some utility classes. Furthermore in the 
 * <code>controler</code> package interfaces and default implementations can be found that
 *  plug the model into the MATSim Controler.
 * </p>
 * 
 * <h3>Extensibility</h3>
 * 
 * The layers of the model can be used separately and independently from each other. The default implementation 
 * uses all layers of the model in this package. The building layer is used to couple the other layers together. If you want to exchange or extend 
 * a certain part of the model have a look at the factory and builders of the default implementation that create the components of
 * interest. Implement your customized factories and builders using or extending the provided interfaces and replace 
 * the factories/builders of the default implementation by your instances to get your extension or customization into the model. 
 * Also have a look at the package-info.java documentation in the subpackages to get an idea how the components work and what can be 
 * customized.
 *
 * An example of an adaptive SignalControll implementation can be found in the package 
 * playground.dgrether.signalsystems.roedergershenson in the playground project. 
 * 
 *  @see org.matsim.signalsystems.data.package-info.java
 *  @see org.matsim.signalsystems.model.package-info.java
 *  @see org.matsim.signalsystems.builder.package-info.java
 *  @see org.matsim.signalsystems.mobsim.package-info.java
 *  @see org.matsim.signalsystems.controler.package-info.java
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
package org.matsim.signalsystems;

