/**
 *  This package contains interfaces and classes that provide functionality to plug a signal system model 
 *  in the MATSim framework.
 *  
 *  <h2>Usage</h2>
 *  
 *  If input files for signal systems are already available, simulation of traffic lights can be enabled via MATSim config options:
 *  <ol>
 *    <li>Set the parameter <pre><code>useSignalsystems</code></pre> to <pre><code>true</code></pre> in the 
 *    config module <pre><code>scenario</code></pre></li>
 *    <li>Set at least three input file names in the config module <pre><code>signalsystems</code></pre>:
 *    <ol>
 *    	<li> parameter name: <pre><code>signalsystems</code></pre> value: path to a file in the <pre><code>signalSystems_v2.0.xsd</code></pre> file format </li>
 *    	<li> parameter name: <pre><code>signalgroups</code></pre> value: path to a file in the <pre><code>signalGroups_v2.0.xsd</code></pre> file format </li>
 *    	<li> parameter name: <pre><code>signalcontrol</code></pre> value: path to a file in the <pre><code>signalControl_v2.0.xsd</code></pre> file format </li>
 *    	<li> parameter name: <pre><code>ambertimes</code></pre> (optional) value: path to a file in the <pre><code>amberTimes_v1.0.xsd</code></pre> file format </li>
 *    </ol>
 *    </li>
 *  </ol>
 *  
 *  An example of such a configuration can be found in the input folder of the SignalSystemsIntegrationTest. If your data is in an older file format have a look at the
 *  package playground.dgrether.signalsystems.data.conversion for converters to the required file formats.
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
 *		<li><pre><code>signalsystems.data</code></pre> the package containing the data layer</li>
 *		<li><pre><code>signalsystems.model </code></pre> the package containing the model layer</li>
 *		<li><pre><code>signalsystems.mobsim</code></pre>the package containing the layer to couple the model to the mobility simulation</li>
 *		<li><pre><code>signalsystems.builder </code></pre>the package containing the layer to build the other layers</li>
 *	</ul>
 * 
 * Each layer is documented separately in the package-info.java of the subpackage.
 * 
 * In addition to the main layers, the package contains some utility classes and needful helpers to plug 
 * the model into the MATSim Controler and to generate the required/desired input data.
 * 
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