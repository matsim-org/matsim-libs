package org.matsim.contrib.matsim4urbansim.zurichscenario;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.NetworkRoute;

public class ZurichUtilities {
	
	/** logger */
	private static final Logger log = Logger.getLogger(ZurichUtilities.class);

	/** scenario/test case identifier */
	static String CLOSE_UETLIBERGTUNNEL = "uetlibergtunnel";
	static String CLOSE_BIRMENSDORFERSTRASSE_OUTERRIM = "birmsdorferstrasseouterrim"; // former birmsdorferstrasse
	static String CLOSE_BIRMENSDORFERSTRASSE_CITYCENTER = "birmsdorferstrassecitycenter";
	static String CLOSE_SCHWAMENDINGERTUNNEL = "schwamendingertunnel";
	static String CLOSE_MILCHBUCKTUNNEL = "milchbucktunnel";
	static String CLOSE_GUBRISTTUNNEL = "gubristtunnel";
	
	/** links to remove */
	static ArrayList<Id<Link>> linksToRemove = null;
	
	/**
	 * Takes a link list and removes them from the network to apply a certain, predefined scenario
	 * @param network
	 * @param linkList
	 */
	static void applyScenario(final Network network){
		// check whether all links exist in network
		existsInNetwork(network);
		// close links
		removeLinks(network);
	}
	
	/**
	 * Checks whether the links exist in the network
	 * @param network
	 * @param linkList
	 * @return true if all links exist in the network
	 */
	static void existsInNetwork(final Network network){
		
		log.info("Looking whether all links for road closure exist in network ...");
		
		if(linksToRemove == null){
			log.info("No links defined! Exit.");
			return;
		}
		
		Map<Id<Link>, ? extends Link> networkLinks = network.getLinks();
		Iterator<Id<Link>> linkIterator = linksToRemove.iterator();
		
		while(linkIterator.hasNext()){
			Id<Link> id = linkIterator.next();
			if(networkLinks.containsKey(id))
				log.info("Link found in network: " + id.toString());
			else
				log.warn("Link not found in network: " + id.toString());
		}
	}

	/**
	 * this closes a link by setting the free speed and capacity to zero 
	 * and the link length to Double.MAX_VALUE
	 * 
	 * @param network
	 * @param linkList
	 */
	static void removeLinks(final Network network){
		
		if(linksToRemove == null)
			return;
		
		Iterator<Id<Link>> linkIterator = linksToRemove.iterator();
		
		while(linkIterator.hasNext()){
			Id<Link> id = linkIterator.next();
			
			network.removeLink( id );
			log.info("Removed link " + id.toString() + " from network ...");
		}
	}
	
	/**
	 * This method deletes plan elements from existing plans when they contain links
	 * that are removed from the network (see modifyNetwork) to avoid errors during mobsim
	 * 
	 * @param population
	 */
	public static void deleteRoutesContainingRemovedLinks(final Population population){
		
		if(linksToRemove == null)
			return;
		
		int cnt = 0;
		for (Person person : PopulationUtils.getSortedPersons(population).values()){
			boolean isModified = false;
			for(Plan plan : person.getPlans()){
				for(PlanElement pe: plan.getPlanElements()){
					if(pe instanceof Leg){
						Leg leg = (Leg)pe;
						if(leg.getRoute() != null){
							
							NetworkRoute nr = (NetworkRoute)leg.getRoute();
							Iterator<Id<Link>> linkIterator = linksToRemove.iterator();
							boolean setRouteToNull = false;
							
							while(linkIterator.hasNext()){
								Id<Link> linkId = linkIterator.next();
								
								for(Id id : nr.getLinkIds()){
									if(id.compareTo(linkId) == 0){
										leg.setRoute(null);
										isModified = true;
										setRouteToNull = true;
										break;
									}			
								}
								if(setRouteToNull)
									break;
							}

						}
					}
				}
			}
			if(isModified)
				cnt++;
		}
		if(cnt > 0){
			log.info("Reseted " + cnt + " persons plans from a population of " + population.getPersons().size()  );
			log.info("This was necessary because these plans contain removed links!");
		}
	}

}
