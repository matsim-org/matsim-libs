/**
 * 
 */
package playground.qiuhan.sa;

import java.util.Iterator;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * resets the links with zero capacity and zero freespeed, so that they have the
 * same capacity and freespeed like their opposite linsks.
 * 
 * @author Q. SUN
 * 
 */
public class Expander4NetworkWithNullCapLinks {
	/**
	 * @param currentLinkId
	 * @return the Id of the link in the opposite direction, while it is not
	 *         checked, if the link in the opposite direction exists in the
	 *         network
	 */
	public static Id<Link> getLinkIdInOppositeDirection(Id<Link> currentLinkId) {
		String crtLinkIdStr = currentLinkId.toString();
		if (crtLinkIdStr.endsWith("R")) {
			return Id.create(crtLinkIdStr.substring(0,
					crtLinkIdStr.lastIndexOf("R")), Link.class);
		} else {
			return Id.create(crtLinkIdStr + "R", Link.class);
		}

	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String inputNetworkFile = "output/matsimNetwork/network.multimodal2.xml", //
		outputNetworkFile = "output/matsimNetwork/network.multimodal2wncl.xml";

		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils
				.createConfig());
		new MatsimNetworkReader(scenario).readFile(inputNetworkFile);
		Network network = scenario.getNetwork();
		// double capPeriod_h = network.getCapacityPeriod() / 3600d;

		Map<Id<Link>, ? extends Link> links = network.getLinks();

		for (Iterator<LinkImpl> linkIt = (Iterator<LinkImpl>) network
				.getLinks().values().iterator(); linkIt.hasNext();) {
			LinkImpl link = linkIt.next();
			if (link.getCapacity() == 0d) {
				LinkImpl oppositeLink = (LinkImpl) links
						.get(getLinkIdInOppositeDirection(link.getId()));
				if (oppositeLink != null) {
					System.out.println("The opposite link\t"
							+ oppositeLink.getId() + "\tof link\t"
							+ link.getId() + "\twas found.");
					double oppositeCap = oppositeLink.getCapacity();
					if (oppositeCap > 0d) {
						link.setCapacity(oppositeCap);
						System.out
								.println("The capacity per network capperiod of link\t"
										+ link.getId()
										+ "\twas set to\t"
										+ oppositeCap + "\t.");
					} else {
						System.out.println("The opposite link\t"
								+ oppositeLink.getId()
								+ "\thas capacity per network capperiod\t"
								+ oppositeCap + "\t<=0d.");
					}
				} else {
					System.out.println("The opposite link with Id\t"
							+ oppositeLink.getId()
							+ "\twas not found in the network.");
				}
			}
		}
		new NetworkWriter(network).write(outputNetworkFile);
	}

}
