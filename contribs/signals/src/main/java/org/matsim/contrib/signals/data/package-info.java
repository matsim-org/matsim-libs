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
 *	<p>
 *	For each component one can find a xml schema in the dtd folder of MATSim, a jaxb xml api in the package org.matsim.jaxb and a 
 *	subpackage of this package containing classes for convenient data generation/access/processing within MATSim. Each of the subpackages 
 * 	contains an own package-info.java that documents the functionality of the component.
 * </p>	
 * <p>
 * All components of the data model are grouped beyond a single interface called SignalsData. The classes SignalsScenarioLoader
 * and SignalsScenarioWriter in this package can be used together with the SignalsData container for convenient file IO.
 *</p>
 *<p>
 *	Note that with a little programming it is also possible to use the signal model without the data model. Of course all 
 *	components can be considered "optional" in this case, but may be used standalone.
 *</p>
 * <h3>Usage</h3>
 *
 * A code example for signalsystems demand generation code can be found in the
 * playground.dgrether.koehlerstrehlersignal.DgKoehlerStrehler2010ScenarioGenerator class.
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
 * 
 * @see org.matsim.contrib.signals.data.signalsystems.v20
 * @see org.matsim.contrib.signals.data.signalgroups.v20
 * @see org.matsim.contrib.signals.data.signalcontrol.v20
 * @see org.matsim.contrib.signals.data.ambertimes.v10
 * @see org.matsim.contrib.signals.data.intergreens.v10
 */
package org.matsim.contrib.signals.data;