/* *********************************************************************** *
 * project: org.matsim.*
 * DgWithindayQPersonAgent
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package playground.dgrether.satellic;

// ExperimentalBasicWithindayAgent is no longer. kai, feb'14

/**
 * @author dgrether
 *
 */
//public class DgWithindayQPersonAgent extends ExperimentalBasicWithindayAgent {
//
//	private static final Logger log = Logger.getLogger(DgWithindayQPersonAgent.class);
//	public static DgWithindayQPersonAgent createDgWithindayQPersonAgent(
//			Person p, Netsim simulation, Random r) {
//		DgWithindayQPersonAgent agent = new DgWithindayQPersonAgent(p, simulation, r);
//		return agent;
//	}
//
//	private Random random;
//
//	private DgWithindayQPersonAgent(Person p, Netsim simulation, Random r) {
//		super(p, simulation);
//		random = r;
//	}
//
//	/**
//	 * Returns the next link the vehicle will drive along.
//	 *
//	 * @return The next link the vehicle will drive on, or null if an error has happened.
//	 */
//	@Override
//	public Id chooseNextLinkId() {
//		Id currentLinkId  = this.getCurrentLinkId();
////		if (currentLinkId == null){
////			return super.chooseNextLinkId();
////		}
//		Id destinationLinkId = this.getDestinationLinkId();
//		NetsimNetwork qnet = this.getMobsim().getNetsimNetwork();
//		if (currentLinkId == destinationLinkId){
//				return null;
//		}
//		NetsimLink currentQLink = qnet.getNetsimLinks().get(currentLinkId);
//		Map<Id, ? extends Link> outlinks = currentQLink.getLink().getToNode().getOutLinks();
//		List<Link> outLinksList = new ArrayList<Link>();
////		log.error("outlinks.size " + outlinks.size());
//
//		double outLinksCapacitySum = 0.0;
//		if (outlinks.values().size() == 1){
//			Link outLink = outlinks.values().iterator().next();
//			this.resetCaches();
//			return outLink.getId();
//		}
//		else {
//			for (Link outLink : outlinks.values()){
//				if (!outLink.getToNode().getId().equals(currentQLink.getLink().getFromNode().getId())){
//					outLinksList.add(outLink);
//					outLinksCapacitySum += outLink.getCapacity(this.getMobsim().getSimTimer().getTimeOfDay());
//				}
//			}
//		}
//		double randomNumber = random.nextDouble() * outLinksCapacitySum;
//		double selectedCapacity = 0.0;
////		log.error("outlinkslist.size " + outLinksList.size());
//		for (Link outLink : outLinksList){
//			selectedCapacity += outLink.getCapacity(this.getMobsim().getSimTimer().getTimeOfDay());
////			log.error("selectedCap: " + selectedCapacity + " randomNumber: " + randomNumber);
//			if (selectedCapacity >= randomNumber){
//				this.resetCaches();
//				return outLink.getId();
//			}
//		}
//		throw new IllegalStateException("selectedCapacity: " + selectedCapacity + " randomNumber: " + randomNumber
//				+ " outLinksList.size() " + outLinksList.size() + " outlinks.size() " + outlinks.size());
////		
////		
////		int nextLinkNr = (int) (random.nextDouble() * outLinksList.size());
////		Link nextLink = outLinksList.get(nextLinkNr);
////		this.cachedNextLinkId = nextLink.getId();
////		return this.cachedNextLinkId;
//	}
//	
//}
