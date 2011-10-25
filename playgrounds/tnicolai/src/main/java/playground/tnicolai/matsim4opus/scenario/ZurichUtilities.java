package playground.tnicolai.matsim4opus.scenario;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.NetworkImpl;

public class ZurichUtilities {

	/** logger */
	private static final Logger log = Logger.getLogger(ZurichUtilities.class);

	/** scenario/test case identifier */
	private static String CLOSE_UETLIBERGTUNNEL = "uetlibergtunnel";
	private static String CLOSE_BIRMSDORFERSTRASSE = "birmsdorferstrasse";
	private static String CLOSE_SCHWAMENDINGERTUNNEL = "schwamendingertunnel";

	/**
	 * This modifies the MATSim network according to the given test parameter in
	 * the MATSim config file (from UrbanSim)
	 */
	public static void modifyNetwork(final NetworkImpl network, final String[] scenarioArray) {

		for (int i = 0; i < scenarioArray.length; i++) {

			if (scenarioArray[i].equalsIgnoreCase(CLOSE_UETLIBERGTUNNEL))
				removeUetliBergTunnel(network);
			else if (scenarioArray[i]
					.equalsIgnoreCase(CLOSE_SCHWAMENDINGERTUNNEL))
				removeSchwamendingerTunnel(network);
			else if (scenarioArray[i]
					.equalsIgnoreCase(CLOSE_BIRMSDORFERSTRASSE))
				removeBirmensdorferstrasse(network);
		}
	}

	/**
	 * removes the Uetlibergtunnel links from MATSim network
	 * 
	 * @param network
	 */
	private static void removeUetliBergTunnel(final NetworkImpl network) {

		log.info("Removing Uetlibertunnel from network ...");
		
		ArrayList<IdImpl> uetlibergTunnelLinkList = new ArrayList<IdImpl>() {
			private static final long serialVersionUID = 1L;
			{
				add(new IdImpl(108150));
				add(new IdImpl(121962));
			}
		};
		// remove links from network
		applyScenario(network, uetlibergTunnelLinkList);
		log.info("Done removing Uetlibertunnel!");
	}

	/**
	 * removes the Birmensdorferstrasse links from MATSim network
	 * 
	 * @param network
	 */
	private static void removeBirmensdorferstrasse(final NetworkImpl network) {

		log.info("Removing Birmensdorferstrasse from network ...");
		
		ArrayList<IdImpl> birmensdorferStrasseLinkList = new ArrayList<IdImpl>() {
			private static final long serialVersionUID = 1L;
			{
				add(new IdImpl(125464));
				add(new IdImpl(125460));
			}
		};
		// remove links from network
		applyScenario(network, birmensdorferStrasseLinkList);
		log.info("Done removing Birmensdorferstrasse!");
	}

	private static void removeSchwamendingerTunnel(final NetworkImpl network) {
		log.info("Removing Schwamendingertunnel from network ...");

		ArrayList<IdImpl> schwamendingerTunnelLinkList = new ArrayList<IdImpl>() {
			private static final long serialVersionUID = 1L;
			{
				add(new IdImpl(109024));
				add(new IdImpl(65583));
				add(new IdImpl(65582));
				add(new IdImpl(17692));
				add(new IdImpl(128604));
				add(new IdImpl(113201));
				add(new IdImpl(109059));
				add(new IdImpl(109060));
				add(new IdImpl(109061));
				add(new IdImpl(109021));
			}
		};
		// remove links from network
		applyScenario(network, schwamendingerTunnelLinkList);
		log.info("Done removing Schwamendingertunnel!");
	}
	
	/**
	 * Takes a link list and removes them from the network to apply a certain, predefined scenario
	 * @param network
	 * @param linkList
	 * @return true if successful
	 */
	private static boolean applyScenario(final NetworkImpl network, final ArrayList<IdImpl> linkList){
		// check whether all links exist in network
		existsInNetwork(network, linkList);
		// remove links
		removeLinks(network, linkList);
		// check if links were actually removed
		boolean linksRemoved = !existsInNetwork(network, linkList);
		
		return linksRemoved;
	}
	
	/**
	 * Checks whether the links exist in the network
	 * @param network
	 * @param linkList
	 * @return true if all links exist in the network
	 */
	private static boolean existsInNetwork(final NetworkImpl network, final ArrayList<IdImpl> linkList){
		
		boolean linksExist = true;
		
		Map<Id, Link> networkLinks = network.getLinks();
		Iterator<IdImpl> linkIterator = linkList.iterator();
		
		while(linkIterator.hasNext()){
			IdImpl id = linkIterator.next();
			if(networkLinks.containsKey(id))
				log.info("Link found in network: " + id.toString());
			else{
				linksExist = false;
				log.warn("Link not found in network: " + id.toString());
			}
		}
		return linksExist;
	}

	/**
	 * Removes links from the network
	 * @param network
	 * @param linkList
	 */
	private static void removeLinks(final NetworkImpl network, final ArrayList<IdImpl> linkList){
		
		Iterator<IdImpl> linkIterator = linkList.iterator();
		
		while(linkIterator.hasNext()){
			IdImpl id = linkIterator.next();
			network.removeLink( id );
			log.info("Removing link: " + id.toString());
		}
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {

	}

}
