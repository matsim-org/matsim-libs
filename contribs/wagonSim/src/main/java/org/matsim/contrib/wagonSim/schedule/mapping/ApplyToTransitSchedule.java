package org.matsim.contrib.wagonSim.schedule.mapping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

/**
 * @author mrieser / senozon
 */
public class ApplyToTransitSchedule {

	private final static Logger log = Logger.getLogger(ApplyToTransitSchedule.class);
	private final TransitSchedule schedule;
	
	public ApplyToTransitSchedule(final TransitSchedule schedule) {
		this.schedule = schedule;
	}
	
	public void applyEdits(final List<NetworkEdit> edits) {
		log.info("build replacement map, this may take a while…");
		Map<Id, List<Id>> replacements = buildReplacementMap(edits);
		
		log.info("apply to routes");
		applyToRoutes(replacements);
		log.info("apply to stops");
		applyToStops(replacements);
		log.info("combine stops");
		combineStopsOnSameLinks();
		log.info("finished");
	}
	
	private void applyToRoutes(final Map<Id, List<Id>> replacements) {
		for (TransitLine line : this.schedule.getTransitLines().values()) {
			for (TransitRoute transitRoute : line.getRoutes().values()) {
				NetworkRoute route = transitRoute.getRoute();
				replaceLinksInRoute(route, replacements);
			}
		}
	}

	private void applyToStops(final Map<Id, List<Id>> replacements) {
		for (TransitStopFacility stop : this.schedule.getFacilities().values()) {
			List<Id> r = replacements.get(stop.getLinkId());
			if ((r != null) && !r.isEmpty()) {
				stop.setLinkId(r.get(r.size() - 1));
			}
		}
	}

	private void combineStopsOnSameLinks() {
		List<TransitStopFacility> stopFacilities = new ArrayList<TransitStopFacility>(this.schedule.getFacilities().values());
		Map<Id, List<TransitStopFacility>> stopsPerLink = new HashMap<Id, List<TransitStopFacility>>();
		Map<TransitStopFacility, TransitStopFacility> replacements = new HashMap<TransitStopFacility, TransitStopFacility>();
		for (TransitStopFacility stop : stopFacilities) {
			List<TransitStopFacility> otherStopsOnLink = stopsPerLink.get(stop.getLinkId());
			if (otherStopsOnLink == null) {
				otherStopsOnLink = new ArrayList<TransitStopFacility>();
				stopsPerLink.put(stop.getLinkId(), otherStopsOnLink);
				otherStopsOnLink.add(stop);
			} else {
				boolean foundReplacement = false;
				for (TransitStopFacility otherStop : otherStopsOnLink) {
					if (otherStop.getCoord().equals(stop.getCoord())) {
						replacements.put(stop, otherStop);
						foundReplacement = true;
						break;
					}
				}
				if (!foundReplacement) {
					otherStopsOnLink.add(stop);
				}
			}
		}

		for (TransitLine line : this.schedule.getTransitLines().values()) {
			for (TransitRoute route : line.getRoutes().values()) {
				for (TransitRouteStop stop : route.getStops()) {
					TransitStopFacility replacementStop = replacements.get(stop.getStopFacility());
					if (replacementStop != null) {
						stop.setStopFacility(replacementStop);
					}
				}
			}
		}
		
		for (TransitStopFacility replacedStop : replacements.keySet()) {
			this.schedule.removeStopFacility(replacedStop);
		}
	}
	
	private Map<Id, List<Id>> buildReplacementMap(final List<NetworkEdit> edits) {
		Map<Id, List<Id>> replacements = new HashMap<Id, List<Id>>();

		// collect all links
		for (TransitLine line : this.schedule.getTransitLines().values()) {
			for (TransitRoute route : line.getRoutes().values()) {
				NetworkRoute r = route.getRoute();
				{
					ArrayList<Id> list = new ArrayList<Id>();
					list.add(r.getStartLinkId());
					replacements.put(r.getStartLinkId(), list);
				}
				for (Id id : r.getLinkIds()) {
					ArrayList<Id> list = new ArrayList<Id>();
					list.add(id);
					replacements.put(id, list);
				}
				{
					ArrayList<Id> list = new ArrayList<Id>();
					list.add(r.getEndLinkId());
					replacements.put(r.getEndLinkId(), list);
				}
			}
		}
		
		int cnt = 0;
		for (NetworkEdit edit : edits) {
			if (edit instanceof ReplaceLink) {
				if (cnt % 250 == 0) {
					log.info(cnt);
				}
				cnt++;
				// apply edit
				ReplaceLink r = (ReplaceLink) edit;
				if (r.getReplacementLinkIds().size() == 1 && r.getLinkId().equals(r.getReplacementLinkIds().get(0))) {
					// ignore
				} else {
					Map<Id, List<Id>> edited = new HashMap<Id, List<Id>>();
					for (Map.Entry<Id, List<Id>> e : replacements.entrySet()) {
						Id linkId = e.getKey();
						if (e.getValue().contains(r.getLinkId())) {
							List<Id> mergedIds = new ArrayList<Id>();
							for (Id id : e.getValue()) {
								if (id.equals(r.getLinkId())) {
									mergedIds.addAll(r.getReplacementLinkIds());
								} else {
									mergedIds.add(id);
								}
							}
							edited.put(linkId, mergedIds);
						} else {
							edited.put(linkId, e.getValue());
						}
					}
					replacements = edited;
				}
			}
		}
		
		return replacements;
	}
	
	private void replaceLinksInRoute(final NetworkRoute route, final Map<Id, List<Id>> replacements) {
		List<Id> allLinkIds = new ArrayList<Id>();
		allLinkIds.add(route.getStartLinkId());
		allLinkIds.addAll(route.getLinkIds());
		allLinkIds.add(route.getEndLinkId());
		
		List<Id> newLinkIds = new ArrayList<Id>();
		for (Id id : allLinkIds) {
			List<Id> r = replacements.get(id);
			if (r.isEmpty()) {
				newLinkIds.add(id);
			} else {
				newLinkIds.addAll(r);
			}
		}
		
		int size = newLinkIds.size();
		route.setLinkIds(newLinkIds.get(0), newLinkIds.subList(1, size - 1), newLinkIds.get(size - 1));
	}
}
