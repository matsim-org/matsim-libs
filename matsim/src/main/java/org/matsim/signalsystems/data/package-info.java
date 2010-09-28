/**
 * <h2>Data model</h2>
 * 
 * The data model consists of several mandatory and optional components:
 * 
 *	<ul>
 *		<li>signalsystems (mandatory)</li>
 *		<li>signalgroups (mandatory)</li>
 *		<li>signalcontrol (mandatory)</li>
 *		<li>ambertimes (optional)</li>
 *		<li>intergreentimes (optional)</li>
 *	</ul>
 *	
 *	For each component one can find a xml schema in the dtd folder of MATSim, a jaxb xml api in the package org.matsim.jaxb and a 
 *	subpackage of this package containing classes for convenient data generation/access/processing within MATSim. Each of the subpackages 
 * 	contains an own package-info.java that documents the functionality of the component.
 *	
 *	
 * TODO describe top level container/writer
 *	
 *	Note that with a little programming it is also possible to use the signal model without the data model. Of course all 
 *	components can be considered "optional" in this case, but may be used standalone.
 *
 * <h2>Usage restrictions:</h2>
 * <ul>
 *   <li> Do not use yet.</li>
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
package org.matsim.signalsystems.data;