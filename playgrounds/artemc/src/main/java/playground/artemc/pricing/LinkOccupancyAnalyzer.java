/* *********************************************************************** *
 * project: org.matsim.*
 * VolumesAnalyzer.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import playground.artemc.utils.Writer;

import java.util.*;

/**
 * Counts the number of vehicles leaving a link, aggregated into time bins of a specified size.
 *
 * @author mrieser
 */
public class LinkOccupancyAnalyzer implements LinkEnterEventHandler, LinkLeaveEventHandler, PersonDepartureEventHandler, VehicleEntersTrafficEventHandler, PersonArrivalEventHandler, AfterMobsimListener {

	private final static Logger log = Logger.getLogger(LinkOccupancyAnalyzer.class);
	private final int timeBinSize;
	private final int maxTime;
	private final int maxSlotIndex;
	private final HashMap<Id<Link>, int[]> links;

	// for multi-modal support
	private final boolean observeModes;
	private final Map<Id<Person>, String> enRouteModes;
	private final Map<Id<Link>, Map<String, int[]>> linksPerMode;

	public LinkOccupancyAnalyzer(final int timeBinSize, final int maxTime, final Network network) {
		this(timeBinSize, maxTime, network, true);
	}

	public LinkOccupancyAnalyzer(final int timeBinSize, final int maxTime, final Network network, boolean observeModes) {
		this.timeBinSize = timeBinSize;
		this.maxTime = maxTime;
		this.maxSlotIndex = (this.maxTime/this.timeBinSize) + 1;

		this.links = new HashMap<Id<Link>, int[]>((int) (network.getLinks().size() * 1.1), 0.95f);


		this.observeModes = observeModes;
		if (this.observeModes) {
			this.enRouteModes = new HashMap<>();
			this.linksPerMode = new HashMap<Id<Link>, Map<String, int[]>>((int) (network.getLinks().size() * 1.1), 0.95f);
		} else {
			this.enRouteModes = null;
			this.linksPerMode = null;
		}
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if (observeModes) {
			enRouteModes.put(event.getPersonId(), event.getLegMode());
		}
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		if(this.links.get(event.getLinkId()) == null){
			this.links.put(event.getLinkId(), new int[this.maxTime+1]);
			Arrays.fill(this.links.get(event.getLinkId()), 0);
		}

		this.links.get(event.getLinkId())[(int) event.getTime()] = this.links.get(event.getLinkId())[(int) event.getTime()] + 1;
	}

	@Override
	public void handleEvent(VehicleEntersTrafficEvent event) {
		if(this.links.get(event.getLinkId()) == null){
			this.links.put(event.getLinkId(), new int[this.maxTime+1]);
			Arrays.fill(this.links.get(event.getLinkId()), 0);
		}

		this.links.get(event.getLinkId())[(int) event.getTime()] = this.links.get(event.getLinkId())[(int) event.getTime()] + 1;
	}
	
	@Override
	public void handleEvent(final LinkLeaveEvent event) {

		if (this.links.get(event.getLinkId()) == null) {
		//	throw new RuntimeException("LinkLeaveEvent before LinkEnterEvent - this shouldn't happen!");
		}
		this.links.get(event.getLinkId())[(int) event.getTime()] = this.links.get(event.getLinkId())[(int) event.getTime()] - 1;
	}

	@Override
	public void handleEvent(final PersonArrivalEvent event) {

		if (this.links.get(event.getLinkId()) != null && event.getLegMode().equals("car")) {
			this.links.get(event.getLinkId())[(int) event.getTime()] = this.links.get(event.getLinkId())[(int) event.getTime()] - 1;
		}
	}

	public void caculateLinkOccupancy() {
		for (Id<Link> link : this.links.keySet()) {
			int changes[] = this.links.get(link);
			for(int i=1;i<changes.length;i++){
				this.links.get(link)[i] = this.links.get(link)[i-1] + changes[i];
			}
		}
	}

	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {
		for (Id<Link> link : this.links.keySet()) {
			int changes[] = this.links.get(link);
			for(int i=1;i<changes.length;i++){
				this.links.get(link)[i] = this.links.get(link)[i-1] + changes[i];
//				if(changes[i]>239){
//					System.out.println(link.toString()+": "+this.links.get(link)[i-1]+" + "+this.links.get(link)[i]);
//				}
			}
		}

		writeToFileLinkOccupancy(event.getControler().getConfig().controler().getOutputDirectory());
	}

	private void writeToFileLinkOccupancy(String outputDirectory){

		Writer writer = new Writer();
		writer.creteFile(outputDirectory+"/linksOccupancy.csv");

		for (Id<Link> link : this.links.keySet()) {
			String line = link.toString();
//			int[] data = this.links.get(link);
//			for(int i=0;i<data.length;i++)
//			{
//				if(i>25200 && i < 30000)
//				line = line + ","+data[i];
//			}
			for(int i=0;i<maxSlotIndex;i++){
				double occupancy = getAverageLinkOccupancy(link, i*this.timeBinSize);
//				if(i>30 && i < 50 && link.toString().equals("14")) {
//					System.out.println(occupancy);
//				}
				line = line + ","+occupancy;
			}
			writer.writeLine(line);
		}
		writer.close();

	}

	public Double getAverageLinkOccupancy(Id<Link> link, double time){

		int startTime = getTimeSlotIndex(time)*this.timeBinSize;
		int endTime = (getTimeSlotIndex(time)+1)*this.timeBinSize;

		int sum=0;
		for(int i = startTime;i<endTime;i++){
			sum = sum + this.links.get(link)[i];

		}

		double occupancy = (double) sum / (double) this.timeBinSize;

		return occupancy;
	}

	public Double getOccupancyPercentage(Id<Link> link, double time, double occupancy){

		int startTime = getTimeSlotIndex(time)*this.timeBinSize;
		int endTime = (getTimeSlotIndex(time)+1)*this.timeBinSize;

		int sum=0;
		for(int i = startTime;i<endTime;i++){
			if(this.links.get(link)[i]>=occupancy){
				sum++;
			}
		}

		double percentage = (double) sum / (double) this.timeBinSize;
		return percentage;
	}


	private int getTimeSlotIndex(final double time) {
		if (time > this.maxTime) {
			return this.maxSlotIndex;
		}
		return ((int)time / this.timeBinSize);
	}



	/**
	 * @param linkId
	 * @return Array containing the number of vehicles leaving the link <code>linkId</code> per time bin,
	 * 		starting with time bin 0 from 0 seconds to (timeBinSize-1)seconds.
	 */
	public int[] getVolumesForLink(final Id<Link> linkId) {
		return this.links.get(linkId);
	}
	
	/**
	 * @param linkId
	 * @param mode
	 * @return Array containing the number of vehicles using the specified mode leaving the link 
	 *  	<code>linkId</code> per time bin, starting with time bin 0 from 0 seconds to (timeBinSize-1)seconds.
	 */
	public int[] getVolumesForLink(final Id<Link> linkId, String mode) {
		if (observeModes) {
			Map<String, int[]> modeVolumes = this.linksPerMode.get(linkId);
			if (modeVolumes != null) return modeVolumes.get(mode);
		} 
		return null;
	}
	
	/*
	 * This procedure is only working if (hour % timeBinSize == 0)
	 * 
	 * Example: 15 minutes bins
	 *  ___________________
	 * |  0 | 1  | 2  | 3  |
	 * |____|____|____|____|
	 * 0   900 1800  2700 3600
		___________________
	 * | 	  hour 0	   |
	 * |___________________|
	 * 0   				  3600
	 * 
	 * hour 0 = bins 0,1,2,3
	 * hour 1 = bins 4,5,6,7
	 * ...
	 * 
	 * getTimeSlotIndex = (int)time / this.timeBinSize => jumps at 3600.0!
	 * Thus, starting time = (hour = 0) * 3600.0
	 */
	public double[] getVolumesPerHourForLink(final Id<Link> linkId) {
		if (3600.0 % this.timeBinSize != 0) log.error("Volumes per hour and per link probably not correct!");
		
		double [] volumes = new double[24];
		for (int hour = 0; hour < 24; hour++) {
			volumes[hour] = 0.0;
		}
		
		int[] volumesForLink = this.getVolumesForLink(linkId);
		if (volumesForLink == null) return volumes;

		int slotsPerHour = (int)(3600.0 / this.timeBinSize);
		for (int hour = 0; hour < 24; hour++) {
			double time = hour * 3600.0;
			for (int i = 0; i < slotsPerHour; i++) {
				volumes[hour] += volumesForLink[this.getTimeSlotIndex(time)];
				time += this.timeBinSize;
			}
		}
		return volumes;
	}

	public double[] getVolumesPerHourForLink(final Id<Link> linkId, String mode) {
		if (observeModes) {
			if (3600.0 % this.timeBinSize != 0) log.error("Volumes per hour and per link probably not correct!");
			
			double [] volumes = new double[24];
			for (int hour = 0; hour < 24; hour++) {
				volumes[hour] = 0.0;
			}
			
			int[] volumesForLink = this.getVolumesForLink(linkId, mode);
			if (volumesForLink == null) return volumes;
	
			int slotsPerHour = (int)(3600.0 / this.timeBinSize);
			for (int hour = 0; hour < 24; hour++) {
				double time = hour * 3600.0;
				for (int i = 0; i < slotsPerHour; i++) {
					volumes[hour] += volumesForLink[this.getTimeSlotIndex(time)];
					time += this.timeBinSize;
				}
			}
			return volumes;
		}
		return null;
	}
	
	/**
	 * @return Set of Strings containing all modes for which counting-values are available.
	 */
	public Set<String> getModes() {
		Set<String> modes = new TreeSet<String>();
		
		for (Map<String, int[]> map : this.linksPerMode.values()) {
			modes.addAll(map.keySet());
		}
		
		return modes;
	}
	
	/**
	 * @return Set of Strings containing all link ids for which counting-values are available.
	 */
	public Set<Id<Link>> getLinkIds() {
		return this.links.keySet();
	}

	@Override
	public void reset(final int iteration) {
		this.links.clear();
		if (observeModes) {
			this.linksPerMode.clear();
			this.enRouteModes.clear();
		}
	}
}
