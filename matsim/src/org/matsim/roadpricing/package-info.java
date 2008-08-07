/**
 * This package provides functionality to simulate different road-pricing scenarios in MATSim.
 * It provides support for different toll schemes, namely distance tolls, cordon tolls and area tolls. The
 * MATSim {@link org.matsim.controler.Controler} has support for the roadpricing package built in, so it
 * only needs to be activated with the corresponding config-settings (<a href="#controler">see below</a>).
 * The toll schemes are described in special XML files (<a href="#schemes">see below</a>). All supported toll
 * schemes can be limited upon a part of the network and can be time-dependent (that means that the amount
 * agents have to pay for the toll can differ during the simulated day).
 * <br>
 * The specified toll amount should be in respect to the scoring function used. Best practice is that the
 * scoring function monetizes the the utility, in that case monetary values can be used for the toll amount.
 *
 * <h3><a name="schemes">Supported Toll Schemes</a></h3>
 * <h4>Distance Toll</h4>
 * In the case of a distance toll, the amount agents have to pay for the toll is linear to the distance they
 * travel in the tolled area. The roadpricing-type must be set to "distance" in the roadpricing file (see
 * below), and all the links that should be tolled must be listed. The costs are per "link length unit": If an
 * agents travels along a link with length set to "100" (which usually means 100 metres for us), the agent
 * will have to pay 100 times the amount specified in the roadpricing file. The time the agent enters a link
 * is the determining time to define the costs.
 * <br>
 * Example Road-Pricing File for a distance toll:
 * <blockquote><pre>
 * &lt;?xml version="1.0" encoding="UTF-8"?>
 * &lt;!DOCTYPE roadpricing SYSTEM "http://www.matsim.org/files/dtd/roadpricing_v1.dtd">
 *
 * &lt;roadpricing type="distance" name="equil-net distance-toll for tests">
 *
 * 	&lt;description>A simple distance toll scheme for the equil-network.&lt;/description>
 *
 * 	&lt;links>
 * 		&lt;link id="6" />
 * 		&lt;link id="15" />
 * 	&lt;/links>
 *
 * 	&lt;!-- amount: [monetary unit] / [link length unit] -->
 * 	&lt;cost start_time="06:00" end_time="10:00" amount="0.00020" />
 * 	&lt;cost start_time="10:00" end_time="15:00" amount="0.00010" />
 * 	&lt;cost start_time="15:00" end_time="19:00" amount="0.00020" />
 *
 * &lt;/roadpricing>
 * </pre></blockquote>
 *
 * <h4>Cordon Toll</h4>
 * In the case of a cordon toll, agents have to pay a fixed amount each time they enter a tolled link from a
 * non-tolled link. The roadpricing type in the file has to be set to "cordon". The links listed in the
 * roadpricing file are the "tolled links". There are two possibilities to specify the tolled links:
 * <ul>
 *   <li><b>All the links in the area enclosed by the cordon are listed.</b> In this case, agents only pay
 *   the toll when entering the cordon from the outside, but not when driving from the inside to the outside.
 *   This has the advantage that the listed links are the same as for an area toll within the same area.</li>
 *   <li><b>Only the links that build the cordon are listed.</b> In this case, agents have to pay each time
 *   they cross the cordon, no matter if from the outside or from the inside (assuming both links, incoming and
 *   outgoing links are listed).</li>
 * </ul>
 * Each time an agent enters a tolled link, while being on an not-tolled link before, the agent has to pay the
 * specified amount. The time the agent enters a link is determining the costs.
 * <br>
 * Example Road-Pricing File for a cordon toll:
 * <blockquote><pre>
 * &lt;?xml version="1.0" encoding="UTF-8"?>
 * &lt;!DOCTYPE roadpricing SYSTEM "http://www.matsim.org/files/dtd/roadpricing_v1.dtd">
 *
 * &lt;roadpricing type="cordon" name="equil-net cordon-toll">
 *
 * 	&lt;description>A simple cordon toll scheme for the equil-network.&lt;/description>
 *
 * 	&lt;links>
 * 		&lt;link id="6" />
 * 		&lt;link id="15" />
 * 	&lt;/links>
 *
 * 	&lt;!-- amount: [monetary unit] / [traveling across a tolled link.] -->
 * 	&lt;cost start_time="06:00" end_time="10:00" amount="1.00" />
 * 	&lt;cost start_time="10:00" end_time="15:00" amount="0.50" />
 * 	&lt;cost start_time="15:00" end_time="19:00" amount="1.00" />
 *
 * &lt;/roadpricing>
 * </pre></blockquote>
 *
 * <h4>Area Toll</h4>
 * In the case of an area toll, agents have to pay a fixed amount when they drive on one of the tolled links,
 * but they have to pay the amount at most once during the simulation. The type must be set to "area" in the
 * roadpricing file. The links listed in the file are the tolled links. The time an agent enters a link is
 * determining if the agent has to be or not.
 * <br>
 * If more than one time-window is used (more than one &lt;cost&gt;-tag), they must all have the same toll
 * amount, as it is not clear how much an agent would need to pay if he is traveling during several the
 * time-windows (the highest amount of them? or the sum of them?).
 *
 * <br>
 * Example Road-Pricing File for an area toll:
 * <blockquote><pre>
 * &lt;?xml version="1.0" encoding="UTF-8"?>
 * &lt;!DOCTYPE roadpricing SYSTEM "http://matsim.org/files/dtd/roadpricing_v1.dtd">
 *
 * &lt;roadpricing type="area" name="equil-net area-toll">
 *
 * 	&lt;description>A simple area toll scheme for the equil-network.&lt;/description>
 *
 * 	&lt;links>
 * 		&lt;link id="6" />
 * 		&lt;link id="15" />
 * 	&lt;/links>
 *
 * 	&lt;!-- amount: [monetary unit] / [simulations] -->
 * 	&lt;cost start_time="06:00" end_time="10:00" amount="2.00" />
 * 	&lt;cost start_time="15:00" end_time="19:00" amount="2.00" />
 *
 * &lt;/roadpricing>
 * </pre></blockquote>
 *
 *
 * <h3><a name="controler">Use RoadPricing with the Controler</a></h3>
 * To simulate road pricing with the default MATSim {@link org.matsim.controler.Controler}, prepare the
 * road pricing XML file (more details and examples can be found with the description of each
 * <a href="#schemes">supported toll scheme</a>). Then add the following part to your configuration:
 * <pre>
 * &lt;module name="roadpricing"&gt;
 *   &lt;param name="tollLinksFile" value="path/to/your/roadpricing-file.xml" /&gt;
 * &lt;/module&gt; </pre>
 * As soon as the parameter <code>tollLinksFile</code> is set, the Controler will load the file and the required
 * classes to simulate the road pricing scenario.
 *
 * <h3><a name="no-controler">Use RoadPricing without the Controler</a></h3>
 * If you plan to use the provided road pricing functionality outside of the Controler, please
 * carefully read the following remarks to correctly setup your road pricing scenario:
 * <ul>
 *   <li>When using a distance or cordon toll scheme, use {@link org.matsim.roadpricing.TollTravelCostCalculator}
 *    as {@link org.matsim.router.util.TravelCost}-object for routers.</li>
 *   <li>When using an area toll, make sure you use {@link org.matsim.roadpricing.PlansCalcAreaTollRoute} as
 *    routing algorithm, together with a non-toll TravelCost.</li>
 * </ul>
 * The {@link org.matsim.controler.Controler} takes care of all of these details, so you only have to care about
 * this if you're not using (or are using a modified version of) the {@link org.matsim.controler.Controler}.
 *
 * <h3><a name="limitations">Limitations</a></h3>
 * Currently, the package has the following limitations:
 * <ul>
 *   <li>Only one toll scheme can be simulated at a time.</li>
 * </ul>
 *
 * @author mrieser
 */
package org.matsim.roadpricing;
