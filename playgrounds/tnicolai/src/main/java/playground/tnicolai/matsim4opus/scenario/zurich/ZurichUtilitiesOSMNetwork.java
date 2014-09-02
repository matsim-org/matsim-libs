package playground.tnicolai.matsim4opus.scenario.zurich;

import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.basic.v01.IdImpl;


public class ZurichUtilitiesOSMNetwork extends ZurichUtilities{

	/** logger */
	private static final Logger log = Logger.getLogger(ZurichUtilitiesOSMNetwork.class);

	/**
	 * This modifies the MATSim network according to the given test parameter in
	 * the MATSim config file (from UrbanSim)
	 */
	public static void modifyNetwork(final Network network, final String[] scenarioArray) {

		for (int i = 0; i < scenarioArray.length; i++) {

			if (scenarioArray[i].equalsIgnoreCase(CLOSE_UETLIBERGTUNNEL))
				removeUetliBergTunnel(network);
			else if (scenarioArray[i]
					.equalsIgnoreCase(CLOSE_SCHWAMENDINGERTUNNEL))
				removeSchwamendingerTunnel(network);
			else if (scenarioArray[i]
					.equalsIgnoreCase(CLOSE_BIRMENSDORFERSTRASSE_OUTERRIM))
				removeBirmensdorferstrasse(network);
			else if (scenarioArray[i]
					.equalsIgnoreCase(CLOSE_MILCHBUCKTUNNEL))
				removeMilchbuckTunnel(network);
		}
	}

	/**
	 * removes the Uetlibergtunnel links from MATSim network
	 * 
	 * @param network
	 */
	private static void removeUetliBergTunnel(final Network network) {

		log.info("Closing Uetlibertunnel from (osm) network ...");
		
		linksToRemove = new ArrayList<Id<Link>>();
		
		linksToRemove.add(new IdImpl(108150));
		linksToRemove.add(new IdImpl(121962));

		// remove links from network
		applyScenario(network);
		log.info("Done closing Uetlibertunnel!");
	}

	/**
	 * removes the Birmensdorferstrasse links from MATSim network
	 * 
	 * @param network
	 */
	private static void removeBirmensdorferstrasse(final Network network) {

		log.info("Closing Birmensdorferstrasse from (osm) network ...");
		
		linksToRemove = new ArrayList<Id<Link>>();
		
		linksToRemove.add(new IdImpl(125464));
		linksToRemove.add(new IdImpl(125460));

		// remove links from network
		applyScenario(network);
		log.info("Done closing Birmensdorferstrasse!");
	}

	private static void removeSchwamendingerTunnel(final Network network) {
		log.info("Closing Schwamendingertunnel from (osm) network ...");

		linksToRemove = new ArrayList<Id<Link>>();
		
		linksToRemove.add(new IdImpl(109024));
		//add(new IdImpl(65583));
		//add(new IdImpl(65582));
		//add(new IdImpl(17692));
		//add(new IdImpl(128604));
		linksToRemove.add(new IdImpl(113201));
		//add(new IdImpl(109059));
		//add(new IdImpl(109060));
		//add(new IdImpl(109061));
		//add(new IdImpl(109021));

		// remove links from network
		applyScenario(network);
		log.info("Done closing Schwamendingertunnel!");
	}

	/**
	 * removes the Milchbucktunnel links from MATSim network
	 * 
	 * @param network
	 */
	private static void removeMilchbuckTunnel(final Network network) {
		log.info("Closing Milchbucktunnel from (osm) network ...");

		linksToRemove = new ArrayList<Id<Link>>();
		
		linksToRemove.add(new IdImpl(21130));
		linksToRemove.add(new IdImpl(21129));

		// remove links from network
		applyScenario(network);
		log.info("Done closing Milchbucktunnel!");
	}
	
//	/**
//	 * Takes a link list and removes them from the network to apply a certain, predefined scenario
//	 * @param network
//	 * @param linkList
//	 */
//	private static void applyScenario(final Network network){
//		// check whether all links exist in network
//		existsInNetwork(network);
//		// close links
//		removeLinks(network);
//	}
//	
//	/**
//	 * Checks whether the links exist in the network
//	 * @param network
//	 * @param linkList
//	 * @return true if all links exist in the network
//	 */
//	private static void existsInNetwork(final Network network){
//		
//		log.info("Looking whether all links for road closure exist in network ...");
//		
//		if(linksToRemove == null){
//			log.info("No links defined! Exit.");
//			return;
//		}
//		
//		Map<Id, ? extends Link> networkLinks = network.getLinks();
//		Iterator<Id> linkIterator = linksToRemove.iterator();
//		
//		while(linkIterator.hasNext()){
//			Id id = linkIterator.next();
//			if(networkLinks.containsKey(id))
//				log.info("Link found in network: " + id.toString());
//			else
//				log.warn("Link not found in network: " + id.toString());
//		}
//	}
//
//	/**
//	 * this closes a link by setting the free speed and capacity to zero 
//	 * and the link length to Double.MAX_VALUE
//	 * 
//	 * @param network
//	 * @param linkList
//	 */
//	private static void removeLinks(final Network network){
//		
//		if(linksToRemove == null)
//			return;
//		
//		Iterator<Id> linkIterator = linksToRemove.iterator();
//		
//		while(linkIterator.hasNext()){
//			Id id = linkIterator.next();
//			
//			network.removeLink( id );
//			log.info("Removed link " + id.toString() + " from network ...");
//		}
//	}
//	
//	/**
//	 * This method deletes plan elements from existing plans when they contain links
//	 * that are removed from the network (see modifyNetwork) to avoid errors during mobsim
//	 * 
//	 * @param population
//	 */
//	public static void deleteRoutesContainingRemovedLinks(final Population population){
//		
//		if(linksToRemove == null)
//			return;
//		
//		int cnt = 0;
//		for (Person person : PopulationUtils.getSortedPersons(population).values()){
//			boolean isModified = false;
//			for(Plan plan : person.getPlans()){
//				for(PlanElement pe: plan.getPlanElements()){
//					if(pe instanceof Leg){
//						Leg leg = (Leg)pe;
//						if(leg.getRoute() != null){
//							
//							NetworkRoute nr = (NetworkRoute)leg.getRoute();
//							Iterator<Id> linkIterator = linksToRemove.iterator();
//							boolean setRouteToNull = false;
//							
//							while(linkIterator.hasNext()){
//								Id linkId = linkIterator.next();
//								
//								for(Id id : nr.getLinkIds()){
//									if(id.compareTo(linkId) == 0){
//										leg.setRoute(null);
//										isModified = true;
//										setRouteToNull = true;
//										break;
//									}			
//								}
//								if(setRouteToNull)
//									break;
//							}
//
//						}
//					}
//				}
//			}
//			if(isModified)
//				cnt++;
//		}
//		if(cnt > 0){
//			log.info("Reseted " + cnt + " persons plans from a population of " + population.getPersons().size()  );
//			log.info("This was necessary because these plans contain removed links!");
//		}
//	}
//	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
	}

}
