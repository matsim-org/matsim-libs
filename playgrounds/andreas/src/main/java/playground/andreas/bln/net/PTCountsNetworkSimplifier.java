package playground.andreas.bln.net;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map.Entry;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.network.algorithms.NetworkCalcTopoType;
import org.matsim.core.population.routes.LinkNetworkRouteFactory;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.CountsWriter;
import org.matsim.counts.Volume;
import org.matsim.transitSchedule.TransitScheduleReaderV1;
import org.matsim.transitSchedule.api.TransitLine;
import org.matsim.transitSchedule.api.TransitRoute;
import org.matsim.transitSchedule.api.TransitSchedule;
import org.matsim.transitSchedule.api.TransitScheduleWriter;
import org.matsim.transitSchedule.api.TransitStopFacility;
import org.xml.sax.SAXException;

/**
 * Simplifies a given network, by merging links.
 *
 * @author aneumann
 *
 */
public class PTCountsNetworkSimplifier {

	private static final Logger log = Logger.getLogger(PTCountsNetworkSimplifier.class);
	private boolean mergeLinkStats = false;
	private TransitSchedule transitSchedule;
	private TreeSet<String> linksNeededByTransitSchedule = null;
	private Network network;
	
	private String netInFile;
	private String scheduleInFile;
	private String netOutFile;
	private String scheduleOutFile;
	private Set<Integer> nodeTypesToMerge = new TreeSet<Integer>();
	private HashMap<String, String> shortNameMap;
	private Counts inCounts;
	private Counts outCounts = new Counts();
	private String countsOutFile;
	
	
	public PTCountsNetworkSimplifier(String netInFile, String scheduleInFile, String netOutFile, String scheduleOutFile, HashMap<String,String> shortNameMap, Counts counts, String countsOutFile){
		this.netInFile = netInFile;
		this.scheduleInFile = scheduleInFile;
		this.netOutFile = netOutFile;
		this.scheduleOutFile = scheduleOutFile;
		this.shortNameMap = shortNameMap;
		this.inCounts = counts;
		this.countsOutFile = countsOutFile;
		
		// set some nonsense, cause writer allows for empty fields, but reader complains
		this.outCounts.setYear(2009);
		this.outCounts.setName("hab ich nicht");
		this.outCounts.setLayer("hab ich keinen");
	}	

	public void run(final Network net, final TransitSchedule tranSched) {
		
		this.network = net;
		this.transitSchedule = tranSched;
		
		TransitScheduleCleaner.removeEmptyLines(this.transitSchedule);
		TransitScheduleCleaner.removeStopsNotUsed(this.transitSchedule);
		this.network = TransitScheduleCleaner.tagTransitLinksInNetwork(this.transitSchedule, this.network);
		
		if(this.nodeTypesToMerge.size() == 0){
			Gbl.errorMsg("No types of node specified. Please use setNodesToMerge to specify which nodes should be merged");
		}

		log.info("running " + this.getClass().getName() + " algorithm...");

		log.info("  checking " + this.network.getNodes().size() + " nodes and " +
				this.network.getLinks().size() + " links for dead-ends...");

		NetworkCalcTopoType nodeTopo = new NetworkCalcTopoType();
		nodeTopo.run(this.network);
		
		TreeSet<String> nodesConnectedToTransitStop = new TreeSet<String>();
		
		int nodesProcessed = 0;
		int nextMessageAt = 2;
		

		for (Node node : this.network.getNodes().values()) {
			
			nodesProcessed++;
			if(nextMessageAt == nodesProcessed){
				log.info(nodesProcessed + " nodes processed so far");
				nextMessageAt = 2 * nodesProcessed;
			}
			
			if(nodesConnectedToTransitStop.contains(node.getId().toString())){
				continue;
			}

			if(this.nodeTypesToMerge.contains(Integer.valueOf(nodeTopo.getTopoType(node)))){

				List<Link> iLinks = new ArrayList<Link> (node.getInLinks().values());

				for (Link iL : iLinks) {
					LinkImpl inLink = (LinkImpl) iL;

					List<Link> oLinks = new ArrayList<Link> (node.getOutLinks().values());

					for (Link oL : oLinks) {
						LinkImpl outLink = (LinkImpl) oL;

						if(inLink != null && outLink != null){
							
							if(!outLink.getToNode().equals(inLink.getFromNode())){
								
								if(!linkNeededByCountsStation(inLink, outLink)){
								
									if(!linkNeededByTransitStop(inLink, outLink)){

										Link link = null;

										if(this.mergeLinkStats){

											// Try to merge both links by guessing the resulting links attributes
											link = this.network.getFactory().createLink(
													new IdImpl(inLink.getId() + "-" + outLink.getId()),
													inLink.getFromNode().getId(),
													outLink.getToNode().getId());

											// length can be summed up
											link.setLength(inLink.getLength() + outLink.getLength());

											// freespeed depends on total length and time needed for inLink and outLink
											link.setFreespeed(
													(inLink.getLength() + outLink.getLength()) /
													(inLink.getFreespeedTravelTime() + outLink.getFreespeedTravelTime())
											);

											// the capacity and the new links end is important, thus it will be set to the minimum
											link.setCapacity(Math.min(inLink.getCapacity(), outLink.getCapacity()));

											// number of lanes can be derived from the storage capacity of both links
											link.setNumberOfLanes((inLink.getLength() * inLink.getNumberOfLanes()
													+ outLink.getLength() * outLink.getNumberOfLanes())
													/ (inLink.getLength() + outLink.getLength())
											);

											//									inLink.getOrigId() + "-" + outLink.getOrigId(),


										} else {

											// Only merge links with same attributes
											if(bothLinksHaveSameLinkStats(inLink, outLink)){
												link = this.network.getFactory().createLink(
														new IdImpl(inLink.getId() + "-" + outLink.getId()),
														inLink.getFromNode().getId(),
														outLink.getToNode().getId());

												link.setLength(inLink.getLength() + outLink.getLength());

												link.setFreespeed(inLink.getFreespeed());


												link.setCapacity(inLink.getCapacity());

												link.setNumberOfLanes(inLink.getNumberOfLanes());

												link.setAllowedModes(inLink.getAllowedModes());											
											}

										}

										if(link != null){
											if(!nodesConnectedToTransitStop.contains(node.getId().toString())){
												if(!nodesConnectedToTransitStop.contains(link.getFromNode().getId().toString())){
													if(!nodesConnectedToTransitStop.contains(link.getToNode().getId().toString())){

														if(inLink.getAllowedModes().contains(TransportMode.pt) || outLink.getAllowedModes().contains(TransportMode.pt)){
															if(removeLinksFromTransitSchedule(link, inLink, outLink)){
																this.network.addLink(link);
																this.network.removeLink(inLink.getId());
																this.network.removeLink(outLink.getId());
															}
														} else {
															this.network.addLink(link);
															this.network.removeLink(inLink.getId());
															this.network.removeLink(outLink.getId());
														}

													}
												}
											}

										}

									} else {
										nodesConnectedToTransitStop.add(node.getId().toString());
									}
								}
							}
						}
					}
				}
			}

		}

		NetworkRemoveUnusedNodes nc = new NetworkRemoveUnusedNodes();
		nc.run(this.network);

		nodeTopo = new NetworkCalcTopoType();
		nodeTopo.run(this.network);

		log.info("  resulting network contains " + this.network.getNodes().size() + " nodes and " +
				this.network.getLinks().size() + " links.");
		log.info("done.");
		
		TransitScheduleCleaner.removeRoutesAndStopsOfLinesWithMissingLinksInNetwork(this.transitSchedule, this.network);
		TransitScheduleCleaner.removeEmptyLines(this.transitSchedule);
		TransitScheduleCleaner.removeStopsNotUsed(this.transitSchedule);
	}

	private boolean linkNeededByCountsStation(LinkImpl inLink, LinkImpl outLink) {
		String nodeId = inLink.getToNode().getId().toString();
		if(this.shortNameMap.keySet().contains(nodeId)){			
			Id shortNameId = new IdImpl(this.shortNameMap.get(nodeId));
			// toNode is marked as count station, thus inLink is needed			
			if(this.outCounts.getCount(shortNameId) == null){
				Count oldCount = this.inCounts.getCount(shortNameId);
				if(oldCount == null){
					// count station was mapped, but data can not be provided
					return false;
				} else {
					// create new count with correct locId and mirgrate data
					this.outCounts.createCount(inLink.getId(), oldCount.getCsId());
					Count newCount = this.outCounts.getCount(inLink.getId());
					newCount.setCoord(oldCount.getCoord());
					for (Volume volume : oldCount.getVolumes().values()) {
						newCount.createVolume(volume.getHour(), volume.getValue());
					}
				}
			} else {
				// count station is already processed
			}			
			return true;
		} else {
			return false;
		}
	}

	private boolean removeLinksFromTransitSchedule(Link link, LinkImpl inLink, LinkImpl outLink) {
		// first test - links must not be changed if, only one link is part of a route, but the other one not
		for (TransitLine transitLine : this.transitSchedule.getTransitLines().values()) {
			for (TransitRoute transitRoute : transitLine.getRoutes().values()) {
				
				if(transitRoute.getRoute().getLinkIds().contains(inLink.getId()) || transitRoute.getRoute().getLinkIds().contains(inLink.getId())){

					LinkedList<Id> routeLinkIds = new LinkedList<Id>();
					routeLinkIds.add(transitRoute.getRoute().getStartLinkId());
					for (Id id : transitRoute.getRoute().getLinkIds()) {
						routeLinkIds.add(id);
					}
					routeLinkIds.add(transitRoute.getRoute().getEndLinkId());

					for (Iterator<Id> iterator = routeLinkIds.iterator(); iterator.hasNext();) {
						Id id = iterator.next();
						if(id.toString().equalsIgnoreCase(inLink.getId().toString())){
							Id nextId = iterator.next();
							if(nextId.toString().equalsIgnoreCase(outLink.getId().toString())){
								// everything okay
								break;
							} else {
								// inLink and outLink ar not followers, thus they should not be touched
								return false;
							}
						}

					}
				}
			}
		}
		
		// second perform
		
		for (TransitLine transitLine : this.transitSchedule.getTransitLines().values()) {
			for (TransitRoute transitRoute : transitLine.getRoutes().values()) {
				
				if(transitRoute.getRoute().getLinkIds().contains(inLink.getId()) || transitRoute.getRoute().getLinkIds().contains(inLink.getId())){

					LinkedList<Id> routeLinkIds = new LinkedList<Id>();
					routeLinkIds.add(transitRoute.getRoute().getStartLinkId());
					for (Id id : transitRoute.getRoute().getLinkIds()) {
						routeLinkIds.add(id);
					}
					routeLinkIds.add(transitRoute.getRoute().getEndLinkId());

					if(routeLinkIds.contains(inLink.getId()) && routeLinkIds.contains(outLink.getId())){
						routeLinkIds.add(routeLinkIds.indexOf(inLink.getId()), link.getId());
						routeLinkIds.remove(inLink.getId());
						routeLinkIds.remove(outLink.getId());
					}

					NetworkRoute newRoute = (NetworkRoute) new LinkNetworkRouteFactory().createRoute(routeLinkIds.getFirst(), routeLinkIds.getLast());
					Id startLink = routeLinkIds.pollFirst();
					Id endLink = routeLinkIds.pollLast();
					newRoute.setLinkIds(startLink, routeLinkIds, endLink);
					transitRoute.setRoute(newRoute);
				}
			}
		}
		return true;
	}

	private boolean linkNeededByTransitStop(LinkImpl inLink, LinkImpl outLink) {
		
		if(this.linksNeededByTransitSchedule == null){
			this.linksNeededByTransitSchedule = new TreeSet<String>();
			for (TransitStopFacility transitStopFacility : this.transitSchedule.getFacilities().values()) {
				this.linksNeededByTransitSchedule.add(transitStopFacility.getLinkId().toString());
			}			
		}		
		
		if(this.linksNeededByTransitSchedule.contains(inLink.getId().toString())){
			return true;				
		}
		if(this.linksNeededByTransitSchedule.contains(outLink.getId().toString())){
			return true;				
		}			
		
		return false;
	}

	/**
	 * Specify the types of node which should be merged.
	 *
	 * @param nodeTypesToMerge A Set of integer indicating the node types as specified by {@link NetworkCalcTopoType}
	 * @see NetworkCalcTopoType NetworkCalcTopoType for a list of available classifications.
	 */
	public void setNodesToMerge(Set<Integer> nodeTypesToMerge){
		this.nodeTypesToMerge.addAll(nodeTypesToMerge);
	}

	/**
	 *
	 * @param mergeLinkStats If set true, links will be merged despite their different attributes.
	 *  If set false, only links with the same attributes will be merged, thus preserving as much information as possible.
	 *  Default is set false.
	 */
	public void setMergeLinkStats(boolean mergeLinkStats){
		this.mergeLinkStats = mergeLinkStats;
	}

	// helper

	/**
	 * Compare link attributes. Return whether they are the same or not.
	 */
	private boolean bothLinksHaveSameLinkStats(LinkImpl linkA, LinkImpl linkB){

		boolean bothLinksHaveSameLinkStats = true;

		if(!linkA.getAllowedModes().equals(linkB.getAllowedModes())){ bothLinksHaveSameLinkStats = false; }

		if(linkA.getFreespeed() != linkB.getFreespeed()){ bothLinksHaveSameLinkStats = false; }

		if(linkA.getCapacity() != linkB.getCapacity()){ bothLinksHaveSameLinkStats = false; }

		if(linkA.getNumberOfLanes() != linkB.getNumberOfLanes()){ bothLinksHaveSameLinkStats = false; }

		return bothLinksHaveSameLinkStats;
	}
	
	public static void main(String[] args) {
		PTCountsNetworkSimplifier simplifier = new PTCountsNetworkSimplifier("e:/_out/osm/transit-network_bb_subway.xml", "e:/_out/osm/osm_transitSchedule_subway.xml", "e:/_out/osm/transit-network_bb_subway_simplified_merged.xml", "e:/_out/osm/osm_transitSchedule_subway_merged.xml", null, null, null);
		Set<Integer> nodeTypesToMerge = new TreeSet<Integer>();
		nodeTypesToMerge.add(new Integer(4));
		nodeTypesToMerge.add(new Integer(5));
		simplifier.setNodesToMerge(nodeTypesToMerge);
		simplifier.setMergeLinkStats(false);
		simplifier.simplifyPTNetwork();
	}
	
	public void simplifyPTNetwork(){

		log.info("Start...");
		Scenario scenario = new ScenarioImpl();
		this.network = scenario.getNetwork();
		log.info("Reading " + this.netInFile);
		new MatsimNetworkReader(scenario).readFile(this.netInFile);
					
		ScenarioImpl osmScenario = new ScenarioImpl();
		Config osmConfig = osmScenario.getConfig();		
		osmConfig.scenario().setUseTransit(true);
		osmConfig.scenario().setUseVehicles(true);
		osmConfig.network().setInputFile(this.netInFile);		
		ScenarioLoaderImpl osmLoader = new ScenarioLoaderImpl(osmScenario);
		osmLoader.loadScenario();
	
		log.info("Reading " + this.scheduleInFile);
		try {
			new TransitScheduleReaderV1(osmScenario.getTransitSchedule(), osmScenario.getNetwork()).readFile(this.scheduleInFile);
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		

		log.info("Running simplifier...");
		run(this.network, osmScenario.getTransitSchedule());		
		TransitScheduleCleaner.removeRoutesAndStopsOfLinesWithMissingLinksInNetwork(osmScenario.getTransitSchedule(), this.network);
		
		log.info("Writing network to " + this.netOutFile);
		new NetworkWriter(this.network).write(this.netOutFile);
		try {
			log.info("Writing transit schedule to " + this.scheduleOutFile);
			new TransitScheduleWriter(osmScenario.getTransitSchedule()).writeFile(this.scheduleOutFile);
			log.info("Writing counts file to " + this.countsOutFile);
			new CountsWriter(this.outCounts).write(this.countsOutFile);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}