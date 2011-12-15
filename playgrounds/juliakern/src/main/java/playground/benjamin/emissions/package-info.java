package playground.benjamin.emissions;


/**
 * This package provides a tool for exhaust emission calculation based on
 * the ``Handbook on Emission Factors for Road Transport'' (HBEFA), version 3.1 (see <link>{@link http://www.hbefa.net}</link>).
 * 
 * <h2>Usage</h2>
 * Run RunEmissionToolOnline or RunEmissionToolOffline from the example package.
 * <ol>
 * 	<li> RunEmissionToolOnline: Works with network and plan files.  </li>
 * 	<li> RunEmissionToolOffline: Uses already calculated trips from an eventsfile. </li>
 * </ol>
 * In both cases some files concerning the network, emission values, road and vehicle types need to be provided.
 * See the RunEmissionTool classes for details. 
 * 
 * <h2>Model description</h2>
 * The emissions package contains four subpackages:
 * <ol>	<li><code>events</code></li>
 * 		<li><code>example</code></li> 
 *  	<li><code>test</code></li>
 *   	<li><code>types</code></li>
 * </ol>
 * 
 * <h3> Emissions </h3>
 * The main package contains classes and methods to handle the emission input data and create
 * maps to associate the emissions with corresponding vehicle types, speed, parking time, ...
 * 
 * <h3> Events </h3>
 * This class contains extensions of {@link org.matsim.core.api.experimental.events.Event} 
 * to handle different types of emissions as events. <code> ColdEmissionAnalysisModule</code> 
 * calculates emissions after a cold start, <code> WarmEmissionAnalysisModule</code> calculates
 * warm emissions.
 * 
 * <h3> Example</h3>
 * This class contains the RunEmissionTool classes and a control listener which implement
 * some functions from {@link org.matsim.core.controler}.
 * 
 * <h3> Test </h3>
 * <h3> Types </h3>
 * 
 * @author benjamin
 */


class Heinz{
	
}