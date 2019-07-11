/**
 * This package provides functionality to simulate different road-pricing scenarios in MATSim.
 * It provides support for different toll schemes, for example distance tolls, cordon tolls and area tolls. 
 * The toll schemes are described in special XML files (<a href="#schemes">see below</a>). All supported toll
 * schemes can be limited to a part of the network and can be time-dependent (that means that the amount
 * agents have to pay for the toll can differ during the simulated day).
 * <br>
 * The specified toll amount should be in respect to the scoring function used. Typically, the 
 * scoring function contains a parameter that defines the marginal utility of money; that parameter is
 * used to convert toll amounts into utilities.
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
 * In the case of a cordon toll, agents have to pay a fixed amount each time 
 * they enter a tolled link from a non-tolled link. The roadpricing type in the 
 * file has to be set to "cordon". The links listed in the roadpricing file are 
 * the "tolled links". There are two possibilities to specify the tolled links:
 * <ul>
 *   <li><strike><b>All the links in the area enclosed by the cordon are 
 *   listed.</b> In this case, agents only pay the toll when entering the cordon 
 *   from the outside, but not when driving from the inside to the outside. This 
 *   has the advantage that the listed links are the same as for an area toll 
 *   within the same area.  DOES NOT WORK CORRECTLY  (in the router).  kai, apr'13 </strike></li>
 *   
 *   <li><b>Only the links that build the cordon are listed.</b> In this case, 
 *   agents have to pay each time they cross the cordon, no matter if from the 
 *   outside or from the inside (assuming both links, incoming and outgoing, 
 *   are listed).</li>
 * </ul>
 * Each time an agent enters a tolled link, while having been on an not-tolled 
 * link before, the agent has to pay the specified amount. The time the agent 
 * enters a link is determining the costs.
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
 * <h4>Distinct toll amounts for single links</h4>
 * For both presented toll schemes, i.e. distance and cordon tolls it is possible to specify a distinct amount
 * of toll for each link. Just include a cost section for each link in the links section of the roadpricing
 * file. In the subsequent example for link 6 the toll specified in the general cost section of the roadpricing
 * file is used while for link 15 those defaults are overwritten by the costs specified within the link's tag.
 * <blockquote><pre>
 *  &lt;links>
 *    &lt;link id="6" />
 *    &lt;link id="15">
 *      &lt;cost start_time="06:00" end_time="10:00" amount="1.00" />
 *      &lt;cost start_time="10:00" end_time="15:00" amount="0.50" />
 *      &lt;cost start_time="15:00" end_time="19:00" amount="1.00" />
 *    &lt;/link>
 *  &lt;/links>
 * </pre></blockquote>
 * 
 * [[I read the above text as if the "amount" entry has, with distance toll, different interpretations when used per link than when used in the 
 * general section.  But when looking through the code, this cannot be substantiated; a per-link amount is multiplied with the link-length
 * with distance toll, and charged as is with the other toll schemes.  kai, nov'14]]
 * 
 * <h4>Link Toll</h4>
 * 
 * In order to reduce the margin of error with some of the schemes, there is now also a "link" scheme (i.e. <code>type="link"</code>), 
 * which just charges per link the amount that is in the file.  This is possibly quite similar to "cordon", except that with cordon is it not
 * clear what happens if you have two links in a sequence that are both tolled (the code might then decide that you are driving "inside"
 * and not charge you on the second link).
 * 
 * <h4><strike>Area Toll</strike></h4>
 * [[Area toll does not work.  The reason is that PlansCalcAreaTollRoute made too many assumptions about plans which are no longer correct, and
 * in consequence the class would need to be re-implemented with current infrastructure.  kn, apr'13 & nov'14]]
 * <strike>In the case of an area toll, agents have to pay a fixed amount when they drive on one of the tolled links,
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
 * </strike>
 *
 * <h3><a name="controler"> RoadPricing with the Controler</a></h3>
 * To simulate road pricing with the default MATSim {@link org.matsim.core.controler.Controler}, prepare the
 * road pricing XML file (more details and examples can be found with the description of each
 * <a href="#schemes">supported toll scheme</a>). Then add the following part to your configuration:
 * <pre>
 * &lt;module name="roadpricing"&gt;
 *   &lt;param name="tollLinksFile" value="path/to/your/roadpricing-file.xml" /&gt;
 * &lt;/module&gt; </pre>
 * 
 *Start MATSim using the following lines of code (check {@link org.matsim.contrib.roadpricing.run.RunRoadPricingExample} in the roadpricing contrib for an up-to-date example):
 * <code>
 * <pre>
 * 	public static void main(String[] args) {

		Config config = ConfigUtils.loadConfig( args[0], new RoadPricingConfigGroup() ) ;

		Scenario scenario = ScenarioUtils.loadScenario(config) ;
		
		Controler controler = new Controler(scenario) ;
		RoadPricing roadPricing = new RoadPricing() ;
		controler.addControlerListener( roadPricing ) ;
		controler.run() ;
	}
 * </pre>
 * </code>
 * 
 * You can also call this from the command line with a release or a nightly build; the syntax is approximately
 * <pre>
 * java -Xmx2000m -cp MATSim.jar:roadpricing-.../roadpricing-...jar org.matsim.roadpricing.run.Main config.xml
 * </pre>
 * 
 * 
 * <h3><a name="limitations">Limitations</a></h3>
 * Currently, the package has the following limitations:
 * <ul>
 *   <li>Only one toll scheme can be simulated at a time.</li>
 *   <li>As far as I understand, there is neither a consistent nor a "random mutation" way to deal with the "flat fee" issue of
 *   a daily area toll: If it makes sense for all <i>individual</i> trips during a day to drive around a toll area, the implementation
 *   will probably not find out that it might have made sense to pay the daily fee for all trips together.  kai, mar'11 -- 
 *   No, this is not entirely correct.  There was originally a provision that enumerated the possible options.  It was, however,
 *   too brittle to work under somewhat arbitrary circumstances, and so did not survive the many changes that this code 
 *   underwent. kai, nov'14</li>
 * </ul>
 *
 * @author mrieser
 */
package org.matsim.contrib.roadpricing;

