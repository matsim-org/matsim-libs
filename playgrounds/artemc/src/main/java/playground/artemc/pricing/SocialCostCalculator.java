/* *********************************************************************** *
 * project: org.matsim.*
 * SocialCostCalculator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.artemc.pricing;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;
import playground.artemc.analysis.IterationTableWriter;
import playground.artemc.socialCost.SocialCostWriter;

import java.io.IOException;
import java.util.*;

/**
 * TODO Analysis:
 * - performed Trips (Duration and Distance)
 * - Trip Duration vs. Trip Distance (Mean and Distribution)
 * 
 * Please note that this code is very experimental, therefore have a critical
 * look at the results and check whether they are feasible or not. If you think
 * you found a bug, feel free to contact me!
 * 
 * @author cdobler
 * @author achakirov
 * 
 */
public class SocialCostCalculator implements IterationStartsListener, IterationEndsListener, AfterMobsimListener,
PersonDepartureEventHandler, PersonArrivalEventHandler,
LinkEnterEventHandler, LinkLeaveEventHandler {

	private static Logger log = Logger.getLogger(SocialCostCalculator.class);

	private int travelTimeBinSize;

	public int getNumSlots() {
		return numSlots;
	}

	private int numSlots;
	private Network network;
	private EventsManager events;
	private Controler controler;
	private TravelTime travelTime;
	private double endTime = 30 * 3600;

	double marginalUtilityOfMoney;
	double opportunityCostOfCarTravel; 


	/*
	 * Blur the Social Cost to speed up the relaxation process. Values between
	 * 0.0 and 1.0 are valid. 0.0 means the old value will be kept, 1.0 means
	 * the old value will be totally overwritten.
	 */
	private final double blendFactor;;

	/*
	 * This is a lookup table because currently the TransportMode of a Leg
	 * is only included in the DepartureEvents but not in the
	 * LinkEnter/LinkLeave Events.
	 */
	private Set<String> transportModes; // TransportModes which create congestion and therefore have to be respected
	private Set<Id> activeAgents; // AgentId - Agents that perform a Leg with a TransportMode contained in transportModes

	public Map<Id<Link>, SocialCostsData> getSocialCostsMap() {
		return socialCostsMap;
	}

	private Map<Id<Link>, SocialCostsData> socialCostsMap; // LinkId
	private Map<Id, LinkTrip> activeTrips; // LinkId
	private List<LinkTrip> performedTrips;

	/*
	 * only for Analysis
	 */
	private Map<Id, LegTrip> activeLegs; // AgentId
	private List<LegTrip> performedLegs;

	private List<Double> meanSocialCosts = new ArrayList<Double>();
	private List<Double> medianSocialCosts = new ArrayList<Double>();
	private List<Double> quantil25PctSocialCosts = new ArrayList<Double>();
	private List<Double> quantil75PctSocialCosts = new ArrayList<Double>();

	private List<Double> meanNormalizedSocialCosts = new ArrayList<Double>();
	private List<Double> medianNormalizedSocialCosts = new ArrayList<Double>();
	private List<Double> quantil25PctNormalizedSocialCosts = new ArrayList<Double>();
	private List<Double> quantil75PctNormalizedSocialCosts = new ArrayList<Double>();

	public SocialCostCalculator(final Network network, EventsManager events, TravelTime travelTime, Controler controler, double blendFactor) {
		this(network, 5 * 60, 30 * 3600, events, travelTime, controler, blendFactor); // default timeslot-duration: 15 minutes
	}

	public SocialCostCalculator(final Network network, final int timeslice, EventsManager events, TravelTime travelTime, Controler controler, double blendFactor) {
		this(network, timeslice, 30 * 3600, events, travelTime, controler, blendFactor); // default: 30 hours at most
	}

	public SocialCostCalculator(Network network, int timeslice, int maxTime, EventsManager events, TravelTime travelTime, Controler controler, double blendFactor) {
		this.travelTimeBinSize = timeslice;
		this.numSlots = (maxTime / this.travelTimeBinSize) + 1;
		this.network = network;
		this.events = events;
		this.travelTime = travelTime;
		this.controler = controler;
		this.blendFactor = blendFactor;

		this.marginalUtilityOfMoney = controler.getConfig().planCalcScore().getMarginalUtilityOfMoney();
		this.opportunityCostOfCarTravel = - controler.getConfig().planCalcScore().getTraveling_utils_hr() + controler.getConfig().planCalcScore().getPerforming_utils_hr();

		init();
	}

	private void init() {
		transportModes = new HashSet<String>();
		transportModes.add(TransportMode.car);

		socialCostsMap = new HashMap<Id<Link>, SocialCostsData>();

		for (Link link : network.getLinks().values()) {
			SocialCostsData scd = new SocialCostsData();
			scd.link = link;
			scd.socialCosts = new double[this.numSlots];
			scd.freeSpeedTravelTime = travelTime.getLinkTravelTime(link, 0.0, null, null);

			/*
			 * We have to do some blurring here: Imagine an Agent travels over a
			 * Link at FreeSpeed but has to wait one second before it can leave
			 * the Link. The Algorithm would detect this as congestion, which is
			 * not true. Therefore we increase the "FreeSpeedTravelTime" and
			 * assume that each Trip over a Link was traveled with FreeSpeed
			 * as long as its TravelTime was shorter than this
			 * PseudoFreeSpeedTravelTime.
			 */
			double pseudoFreeSpeedTravelTime = scd.freeSpeedTravelTime;
			double pseudoFactor = Math.max(pseudoFreeSpeedTravelTime * 0.01, 1.0); // 1%, but at least 2 seconds
			pseudoFreeSpeedTravelTime = pseudoFreeSpeedTravelTime + pseudoFactor;

			scd.pseudoFreeSpeedTravelTime = pseudoFreeSpeedTravelTime;
			socialCostsMap.put(link.getId(), scd);
		}

		activeAgents = new HashSet<Id>();
		activeTrips = new HashMap<Id, LinkTrip>();
		performedTrips = new ArrayList<LinkTrip>();

		activeLegs = new HashMap<Id, LegTrip>();
		performedLegs = new ArrayList<LegTrip>();
	}

	@Override
		 public void notifyIterationStarts(final IterationStartsEvent event) {

	}

	@Override
	public void handleEvent(final LinkEnterEvent event) {
		/*
		 * Return, if the Agent is on a Leg which does not create congestion.
		 */
		if (!activeAgents.contains(event.getPersonId())) return;

		LinkTrip linkTrip = new LinkTrip();
		linkTrip.person_id = event.getPersonId();
		linkTrip.link_id = event.getLinkId();
		linkTrip.enterTime = event.getTime();

		activeTrips.put(event.getPersonId(), linkTrip);

		/*
		 * Analysis
		 */
		LegTrip legTrip = activeLegs.get(event.getPersonId());
		if (legTrip == null) {
			log.error("LegTrip was not found!");
			return;
		}
		legTrip.linkTrips.add(linkTrip);
	}

	@Override
	public void handleEvent(final LinkLeaveEvent event) {
		/*
		 * Return, if the Agent is on a Leg which does not create congestion.
		 */
		if (!activeAgents.contains(event.getPersonId())) return;

		LinkTrip linkTrip = activeTrips.get(event.getPersonId());

		if (linkTrip == null) {
			log.error("LinkTrip was not found!");
			return;
		}

		linkTrip.leaveTime = event.getTime();
		performedTrips.add(linkTrip);
	}

	@Override
	public void handleEvent(final PersonDepartureEvent event) {
		/*
		 * If the TransportMode does not create any congestion (e.g. walk,
		 * bike), it is ignored.
		 */
		String transportMode = event.getLegMode();
		if (!transportModes.contains(transportMode)) return;
		if(event.getPersonId().toString().startsWith("pt")) return;

		LinkTrip linkTrip = new LinkTrip();
		linkTrip.person_id = event.getPersonId();
		linkTrip.link_id = event.getLinkId();
		linkTrip.enterTime = event.getTime();

		activeTrips.put(event.getPersonId(), linkTrip);
		activeAgents.add(event.getPersonId());

		/*
		 * Analysis
		 */
		LegTrip legTrip = new LegTrip();
		legTrip.person_id = event.getPersonId();
		legTrip.departureTime = event.getTime();
		activeLegs.put(event.getPersonId(), legTrip);
	}

	@Override
	public void handleEvent(final PersonArrivalEvent event) {
		/*
		 * Try to remove the Agent from the Active Set. If he was not
		 * active, the performed Leg created no congestion and therefore
		 * there is no LinkTrip object.
		 */
		boolean wasActive = activeAgents.remove(event.getPersonId());
		if(!wasActive) return;

		LinkTrip linkTrip = activeTrips.remove(event.getPersonId());

		if (linkTrip == null) {
			log.error("LinkTrip was not found!");
			return;
		}

		linkTrip.leaveTime = event.getTime();
		performedTrips.add(linkTrip);

		/*
		 * Analysis
		 */
		LegTrip legTrip = activeLegs.remove(event.getPersonId());
		if (legTrip == null) {
			log.error("LegTrip was not found!");
			return;
		}
		legTrip.arrivalTime = event.getTime();
		performedLegs.add(legTrip);
	}

	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {
		/*
		 * If there are still active Trips left -> end them
		 */
		for (LinkTrip trip : activeTrips.values()) {
			trip.leaveTime = this.endTime;
			this.performedTrips.add(trip);
		}
		activeTrips.clear();

		updateSocCosts(event.getIteration());

		// printSocialCosts();

		calcSocCosts();

		calcStatistics();

		calcLegStatistics();
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		writeSocialCostsPlot(event);
	}

	@Override
	public void reset(final int iteration) {		
		activeAgents.clear();
		activeTrips.clear();
		performedTrips.clear();
		activeLegs.clear();
		performedLegs.clear();
	}

	private int getTimeSlotIndex(final double time) {
		int slice = ((int) time) / travelTimeBinSize;
		if (slice >= numSlots) slice = numSlots - 1;
		return slice;
	}

	private double calcSocCosts(Id link_id, double enterTime) {

		double socialCosts = 0.0;
		int enterIndex = getTimeSlotIndex(enterTime);

		// if it is the last time slot
		if (enterIndex == numSlots - 1) {
			socialCosts = socialCostsMap.get(link_id).socialCosts[enterIndex];
		} else {
			double beginEnterSlot = enterIndex * travelTimeBinSize;

			/*
			 * if linkTrip.enterTime == beginEnterSlot -> fraction = 1.0 if
			 * linkTrip.enterTime == endEnterSlot -> fraction = 0.0
			 */
			double fraction = 1 - ((enterTime - beginEnterSlot) / travelTimeBinSize);

			socialCosts = fraction * socialCostsMap.get(link_id).socialCosts[enterIndex]
					+ (1 - fraction) * socialCostsMap.get(link_id).socialCosts[enterIndex + 1];

		}

		socialCosts = (opportunityCostOfCarTravel * socialCosts / 3600) / marginalUtilityOfMoney;	

		return socialCosts;
	}

	private void calcSocCosts() {
		double totalSocialCosts = 0.0;

		for (LinkTrip linkTrip : performedTrips) {

			double socialCosts = calcSocCosts(linkTrip.link_id, linkTrip.enterTime);
			// convert from seconds to CHF (currently defined as 6 CHF/hour)
			//			socialCosts = socialCosts / 600;

			if (socialCosts <= 0.0) continue;	
			//PersonMoneyEvent e = new PersonMoneyEvent(0.5 * (linkTrip.enterTime + linkTrip.leaveTime), linkTrip.person_id, -socialCosts);
			//this.events.processEvent(e);
			totalSocialCosts = totalSocialCosts + socialCosts;
		}

		log.info("Total social costs caused: " + totalSocialCosts);
	}

	private void calcStatistics() {

		// Get a DescriptiveStatistics instance
		DescriptiveStatistics tripStats = new DescriptiveStatistics();		
		DescriptiveStatistics tripStatsNormalized = new DescriptiveStatistics();

		// Add the data from the array
		for (LegTrip legTrip : performedLegs) {
			double distance = 0.0;
			double cost = 0.0;
			for (LinkTrip linkTrip : legTrip.linkTrips) {
				double socialCosts = calcSocCosts(linkTrip.link_id, linkTrip.enterTime);
				if (socialCosts > 0.0) cost = cost + socialCosts;
				distance = legTrip.distance + network.getLinks().get(linkTrip.link_id).getLength();
			}
	
			legTrip.distance = distance;
			legTrip.cost = cost;
			
			tripStats.addValue(cost);

			/*
			 * Normalize a legs social cost by dividing them by the leg travel time or leg distance.
			 */
			//double legTravelTime = legTrip.arrivalTime - legTrip.departureTime;
			if (cost > 0.0 && legTrip.distance > 0.0) tripStatsNormalized.addValue(cost / legTrip.distance);
		}

		// Compute some statistics
		double sum = tripStats.getSum();
		double mean = tripStats.getMean();
		double std = tripStats.getStandardDeviation();
		double median = tripStats.getPercentile(50);
		double quantile25 = tripStats.getPercentile(25);
		double quantile75 = tripStats.getPercentile(75);

		double sumNormalized = tripStatsNormalized.getSum();
		double meanNormalized = tripStatsNormalized.getMean();
		double stdNormalized = tripStatsNormalized.getStandardDeviation();
		double medianNormalized = tripStatsNormalized.getPercentile(50);
		double quantile25Normalized = tripStatsNormalized.getPercentile(25);
		double quantile75Normalized = tripStatsNormalized.getPercentile(75);

		log.info("Sum of all leg costs: " + sum);
		log.info("Mean leg costs: " + mean);
		log.info("Standard deviation: " + std);
		log.info("Median leg costs: " + median);
		log.info("25% quantile leg costs: " + quantile25);
		log.info("75% quantile leg costs: " + quantile75);

		log.info("Normalized sum of all leg costs: " + sumNormalized);
		log.info("Normalized mean leg costs: " + meanNormalized);
		log.info("Normalized standard deviation: " + stdNormalized);
		log.info("Normalized median leg costs: " + medianNormalized);
		log.info("Normalized 25% quantile leg costs: " + quantile25Normalized);
		log.info("Normalized 75% quantile leg costs: " + quantile75Normalized);

		meanSocialCosts.add(mean);
		medianSocialCosts.add(median);
		quantil25PctSocialCosts.add(quantile25);
		quantil75PctSocialCosts.add(quantile75);

		meanNormalizedSocialCosts.add(meanNormalized);
		medianNormalizedSocialCosts.add(medianNormalized);
		quantil25PctNormalizedSocialCosts.add(quantile25Normalized);
		quantil75PctNormalizedSocialCosts.add(quantile75Normalized);
	}

	private void calcLegStatistics() {
		double totalDistance = 0.0;
		double totalTravelTime = 0.0;

		for (LegTrip legTrip : performedLegs) {
			for (LinkTrip linkTrip : legTrip.linkTrips) {
				totalDistance = totalDistance + network.getLinks().get(linkTrip.link_id).getLength();
			}
			totalTravelTime = totalTravelTime + (legTrip.arrivalTime - legTrip.departureTime);
		}

		log.info("Total Legs: " + performedLegs.size());
		log.info("Total Distance: " + totalDistance);
		log.info("Total Travel Time: " + totalTravelTime);
	}

	private void writeSocialCostsPlot(IterationEndsEvent event) {

		// We have a data set for each iteration. We start counting by 0, therefore we add 1.
		int dataLength = event.getIteration() + 1;

		String fileName = null;

		SocialCostWriter writer = new SocialCostWriter(event.getIteration());

		double[] meanData = new double[dataLength];
		double[] medianData = new double[dataLength];
		double[] quantil25Data = new double[dataLength];
		double[] quantil75Data = new double[dataLength];

		for (int i = 0; i < dataLength; i++) {
			meanData[i] = meanSocialCosts.get(i);
			medianData[i] = medianSocialCosts.get(i);
			quantil25Data[i] = quantil25PctSocialCosts.get(i);
			quantil75Data[i] = quantil75PctSocialCosts.get(i);
		}

		fileName = event.getControler().getControlerIO().getOutputFilename("socialCosts");
		writer.writeGraphic(fileName + ".png", "social costs (money units per leg)", meanData, medianData, quantil25Data, quantil75Data);
		writer.writeTable(fileName + ".txt", meanData, medianData, quantil25Data, quantil75Data);


		for (int i = 0; i < dataLength; i++) {
			meanData[i] = meanNormalizedSocialCosts.get(i);
			medianData[i] = medianNormalizedSocialCosts.get(i);
			quantil25Data[i] = quantil25PctNormalizedSocialCosts.get(i);
			quantil75Data[i] = quantil75PctNormalizedSocialCosts.get(i);
		}

		fileName = event.getControler().getControlerIO().getOutputFilename("normalizedSocialCosts");
		writer.writeGraphic(fileName + ".png", "social costs (money units per leg, normalized)", meanData, medianData, quantil25Data, quantil75Data);
		writer.writeTable(fileName + ".txt", meanData, medianData, quantil25Data, quantil75Data);

		//Write link toll statistics
		String filenameLinkStats = this.controler.getControlerIO().getIterationFilename(event.getIteration(), "linkTollStats.txt");
		String[] linkTableColumns = new String[this.numSlots+1];
		linkTableColumns[0] = "LinkId";
		for(int i=1;i<=numSlots;i++){
			linkTableColumns[i] = Integer.toString((i-1)*this.travelTimeBinSize);
		}

		try{
			IterationTableWriter linkTable = new IterationTableWriter(filenameLinkStats,linkTableColumns);
			String[] linkTollData = new String[this.numSlots+1];
			for (SocialCostsData data : this.socialCostsMap.values()) 
			{
				linkTollData[0] = data.link.getId().toString();
				for (int k = 0; k < this.numSlots; k++) 
				{
					linkTollData[k+1] = Double.toString(calcSocCosts(data.link.getId(), k*this.travelTimeBinSize));
				}
				linkTable.addData(linkTollData);
			}
			
			linkTable.finish();
		} catch (IOException e) {
			e.printStackTrace();
		}	

		//Write trip statistics
		String tripTableColumns[] = {"tripID","startTime","duration","distance","toll"};
		HashMap<Id,Integer> personTrips = new HashMap<Id,Integer>();		
		try {
			String filenameTripStats = this.controler.getControlerIO().getIterationFilename(event.getIteration(), "tripStats.txt");
			IterationTableWriter tripTable = new IterationTableWriter(filenameTripStats,tripTableColumns);
			Double tripDistance;
			Double tripDuration;
			Double cost;
			
			String[] dataLine = new String[5];
			for (LegTrip legTrip : performedLegs) {

				if(personTrips.containsKey(legTrip.person_id)){
					personTrips.put(legTrip.person_id, personTrips.get(legTrip.person_id)+1);
				}
				else{
					personTrips.put(legTrip.person_id, 1);
				}

				tripDuration = legTrip.arrivalTime - legTrip.departureTime;

				dataLine[0] = legTrip.person_id.toString()+"_"+personTrips.get(legTrip.person_id);
				dataLine[1] = String.valueOf(legTrip.departureTime);
				dataLine[2] = String.valueOf(tripDuration);
				dataLine[3] = String.valueOf(legTrip.distance);
				dataLine[4] = String.valueOf(legTrip.cost);
				tripTable.addData(dataLine);
			}
			tripTable.finish();
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}

	private void updateSocCosts(int iteration) {

		log.info("Updating social cost...");
		
		for (SocialCostsData data : this.socialCostsMap.values()) {
			int ke = this.numSlots; // ke = K

			// for k = K-1 .. 0
			for (int k = this.numSlots - 1; k >= 0; k--) {
				/*
				 * if ta(k) = tfree then ke = k We have to use "<=" because we
				 * use a PseudoFreeSpeedTravelTime!
				 */
				if (travelTime.getLinkTravelTime(data.link, k * travelTimeBinSize, null, null) <= data.pseudoFreeSpeedTravelTime) ke = k;

				// Ca(k) = max(0, (ke - k)*T - tfree)
				double socialCost = (ke - k) * travelTimeBinSize - data.freeSpeedTravelTime;
				if (socialCost < 0.0) {
					socialCost = 0.0;
				}
				else{
					System.out.println();
				}
				// data.socialCosts[k] = socialCost;

				// If it is the first iteration, there is no old value, therefore use this iterations value. 
				double oldValue;
				if (iteration == 0) oldValue = socialCost;
				else oldValue = data.socialCosts[k];
				double blendedOldValue = (1 - blendFactor) * oldValue;
				double blendedNewValue = blendFactor * socialCost;
				data.socialCosts[k] = blendedOldValue + blendedNewValue;
			}

			//SavitzkyGolayFilter sgf = new SavitzkyGolayFilter(5,data.socialCosts, true);
			//data.socialCosts = sgf.appllyFilter();
		}
	}

	private void printSocialCosts() {
		for (SocialCostsData data : this.socialCostsMap.values()) {
			String arrayString = "";
			for (double d : data.socialCosts)
				arrayString = arrayString + " " + d;
			log.info(data.link.getId() + " " + arrayString);
		}
	}

	public static class SocialCostsData {
		Link link;
		double[] socialCosts;
		double freeSpeedTravelTime; // TODO: make this time-dependent
		double pseudoFreeSpeedTravelTime; // TODO: make this time-dependent
	}

	private static class LinkTrip {
		Id person_id;
		Id link_id;
		double enterTime;
		double leaveTime;
	}

	private static class LegTrip {
		Id person_id;
		double departureTime;
		double arrivalTime;
		List<LinkTrip> linkTrips = new ArrayList<LinkTrip>();
		double cost;
		double distance;
	}
}