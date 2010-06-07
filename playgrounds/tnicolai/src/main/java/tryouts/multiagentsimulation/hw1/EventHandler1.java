/* *********************************************************************** *
 * project: org.matsim.*
 * ControlerListener1.java
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

/**
 * 
 */
package tryouts.multiagentsimulation.hw1;

import java.util.HashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.charts.XYLineChart;

/**
 * @author thomas
 *
 */
public class EventHandler1 implements LinkLeaveEventHandler{
	
	private static HashMap<Id, LinkCount> hm;
	private static int totalCount = 0;
	
	/**
	 * 
	 */
	@Override
	public void reset(int iteration){
		hm = new HashMap<Id, LinkCount>();
	}

	/**
	 * 
	 */
	@Override
	public void handleEvent(LinkLeaveEvent event) {		
		Id linkID = event.getLinkId();
		double time = event.getTime();
		LinkCount lc;
		
		if(! hm.containsKey(linkID)){
			hm.put(linkID, new LinkCount(linkID));
		}
		lc = hm.get(linkID);
		lc.increase();		// update counter
		lc.setTime(time);	// set current time (indicates last time a vehicle moved over a link)
		
		totalCount++;
	}
	
	public void writeChart(String filename) {
		
		int numberOfLinks = 23;
		double linkCounts[] = new double[numberOfLinks];
		double linkId[] = new double[numberOfLinks]; 
		double lastTime[] = new double[numberOfLinks];
		
		for(int i = 1; i <= numberOfLinks; i++){
			
			linkCounts[i-1] = 0;
			linkId[i-1] = i;
			
			if(hm.containsKey(new IdImpl(i) )){
				linkCounts[i-1] = hm.get( new IdImpl(i) ).getCount();
				lastTime[i-1] = hm.get( new IdImpl(i) ).getLastTime()/3600;
				LinkCount lc = hm.get(new IdImpl(i));
				System.out.println("LinkID " + lc.getId() + ", Count " + lc.getCount() + ", lastTime " + lc.getLastTime()/3600);
			}
		}
		System.out.println("Total count: " + totalCount);
		
		// configuring chart
		XYLineChart chart = new XYLineChart("Traffic link", "links", "counts");
		chart.addSeries("counts",linkId, linkCounts);
		chart.addSeries("time of last vehicle", linkId, lastTime);
		
		chart.saveAsPng(filename, 1920, 1080);
	}

	public class LinkCount{
		private Id id;
		private int count;
		private double lastTime;
		
		public LinkCount(Id i){
			count = 0;
			lastTime = 0.0;
			id = i;
		}
		
		public int getCount(){
			return count;
		}
		
		public double getLastTime(){
			return lastTime;
		}
		
		public void increase(){
			count++;
		}
		
		public void setTime(final double time){
			lastTime = time;
		}
		
		public Id getId(){
			return id;
		}
	}

}

