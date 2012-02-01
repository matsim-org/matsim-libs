package playground.tnicolai.matsim4opus.scenario;

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
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.utils.misc.PopulationUtils;

public class ZurichUtilitiesZurichBigRoads {

	/** logger */
	private static final Logger log = Logger.getLogger(ZurichUtilitiesZurichBigRoads.class);

	/** scenario/test case identifier */
	private static String CLOSE_UETLIBERGTUNNEL = "uetlibergtunnel";
	private static String CLOSE_BIRMENSDORFERSTRASSE_OUTERRIM = "birmsdorferstrasseouterrim"; // former birmsdorferstrasse
	private static String CLOSE_BIRMENSDORFERSTRASSE_CITYCENTER = "birmsdorferstrassecitycenter";
	private static String CLOSE_SCHWAMENDINGERTUNNEL = "schwamendingertunnel";
	private static String CLOSE_MILCHBUCKTUNNEL = "milchbucktunnel";
	private static String CLOSE_GUBRISTTUNNEL = "gubristtunnel";
	
	/** links to remove */
	private static ArrayList<Id> linksToRemove = null;

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
				removeBirmensdorferstrasseOuterRim(network);
			else if (scenarioArray[i]
					.equalsIgnoreCase(CLOSE_BIRMENSDORFERSTRASSE_CITYCENTER))
				removeBirmensdorferstrasseCity(network);
			else if (scenarioArray[i]
					.equalsIgnoreCase(CLOSE_MILCHBUCKTUNNEL))
				removeMilchbuckTunnel(network);
			else if (scenarioArray[i]
					.equalsIgnoreCase(CLOSE_GUBRISTTUNNEL))
				removeGubristTunnel(network);
		}
	}

	/**
	 * removes the Uetlibergtunnel links from MATSim network
	 * 
	 * @param network
	 */
	private static void removeUetliBergTunnel(final Network network) {

		log.info("Closing/removing Uetlibertunnel from network ...");
		
		linksToRemove = new ArrayList<Id>();
		
		linksToRemove.add(new IdImpl(26236));
		linksToRemove.add(new IdImpl(26267));

		// remove links from network
		applyScenario(network);
		log.info("Done closing Uetlibertunnel!");
	}

	/**
	 * removes the Birmensdorferstrasse links from MATSim network
	 * 
	 * @param network
	 */
	private static void removeBirmensdorferstrasseOuterRim(final Network network) {

		log.info("Closing/removing Birmensdorferstrasse (outer rim) from network ...");
		
		linksToRemove = new ArrayList<Id>();
		
		linksToRemove.add(new IdImpl(17480));
		linksToRemove.add(new IdImpl(17481));

		// remove links from network
		applyScenario(network);
		log.info("Done closing Birmensdorferstrasse (outer rim)!");
	}
	
	/**
	 * removes the Birmensdorferstrasse next to city center links from MATSim network
	 * 
	 * @param network
	 */
	private static void removeBirmensdorferstrasseCity(final Network network) {

		log.info("Closing/removing Birmensdorferstrasse (near city center) from network ...");
		
		linksToRemove = new ArrayList<Id>();
		
		linksToRemove.add(new IdImpl(7889));
		linksToRemove.add(new IdImpl(178));

		// remove links from network
		applyScenario(network);
		log.info("Done closing Birmensdorferstrasse (near city center)!");
	}

	/**
	 * removes the Schwamendingertunnel links from MATSim network
	 * 
	 * @param network
	 */
	private static void removeSchwamendingerTunnel(final Network network) {
		log.info("Closing/removing Schwamendingertunnel from network ...");

		linksToRemove = new ArrayList<Id>();
		
		linksToRemove.add(new IdImpl(41507));
		linksToRemove.add(new IdImpl(3598));

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
		log.info("Closing/removing Milchbucktunnel from network ...");

		linksToRemove = new ArrayList<Id>();
		
		linksToRemove.add(new IdImpl(24698));
		linksToRemove.add(new IdImpl(24697));

		// remove links from network
		applyScenario(network);
		log.info("Done closing Milchbucktunnel!");
	}
	
	/**
	 * removes the Gubristtunnel links from MATSim network
	 * 
	 * @param network
	 */
	private static void removeGubristTunnel(final Network network){
		log.info("Closing/removing Gubristtunnel from network ...");

		linksToRemove = new ArrayList<Id>();
		
		linksToRemove.add(new IdImpl(37190));
		linksToRemove.add(new IdImpl(20341));

		// remove links from network
		applyScenario(network);
		log.info("Done closing Gubristtunnel!");
	}
	
	/**
	 * Takes a link list and removes them from the network to apply a certain, predefined scenario
	 * @param network
	 * @param linkList
	 */
	private static void applyScenario(final Network network){
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
	private static void existsInNetwork(final Network network){
		
		log.info("Looking whether all links for road closure exist in network ...");
		
		if(linksToRemove == null){
			log.info("No links defined! Exit.");
			return;
		}
		
		Map<Id, ? extends Link> networkLinks = network.getLinks();
		Iterator<Id> linkIterator = linksToRemove.iterator();
		
		while(linkIterator.hasNext()){
			Id id = linkIterator.next();
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
	private static void removeLinks(final Network network){
		
		if(linksToRemove == null)
			return;
		
		Iterator<Id> linkIterator = linksToRemove.iterator();
		
		while(linkIterator.hasNext()){
			Id id = linkIterator.next();
			
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
							Iterator<Id> linkIterator = linksToRemove.iterator();
							boolean setRouteToNull = false;
							
							while(linkIterator.hasNext()){
								Id linkId = linkIterator.next();
								
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
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// for testing
	}

}
