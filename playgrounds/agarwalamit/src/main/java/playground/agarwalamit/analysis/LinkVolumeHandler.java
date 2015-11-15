/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.agarwalamit.analysis;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.network.Link;

/**
 * @author amit
 */
public class LinkVolumeHandler implements LinkEnterEventHandler {

	private final Logger logger = Logger.getLogger(LinkVolumeHandler.class);
	private Map<Id<Link>, Map<Integer,Double>> linkId2Time2Volume = new HashMap<Id<Link>, Map<Integer,Double>>();

	public LinkVolumeHandler () {
		this.logger.info("Starting volume count on links.");
		reset(0);
	}
	@Override
	public void reset(int iteration) {
		this.linkId2Time2Volume.clear();
	}

	private int getSlot(double time){
		return (int)time/3600;
	}
	@Override
	public void handleEvent(LinkEnterEvent event) {
		int slotInt = getSlot(event.getTime());
		Map<Integer, Double> volsTime = new HashMap<Integer, Double>();

		if(this.linkId2Time2Volume.containsKey(event.getLinkId())){
			volsTime =	this.linkId2Time2Volume.get(event.getLinkId());
			if(volsTime.containsKey(slotInt)) {
				double counter = (volsTime.get(slotInt));
				double newCounter = counter+1;
				volsTime.put(slotInt, newCounter);
				this.linkId2Time2Volume.put(event.getLinkId(), volsTime);
			}
			else {
				volsTime.put(slotInt, 1.0);
				this.linkId2Time2Volume.put(event.getLinkId(), volsTime);
			} 
		}else {
			volsTime.put(slotInt, 1.0);
			this.linkId2Time2Volume.put(event.getLinkId(), volsTime);
		}
	}

	public Map<Id<Link>, Map<Integer, Double>> getLinkId2TimeSlot2LinkVolume(){
		return this.linkId2Time2Volume;
	}
}