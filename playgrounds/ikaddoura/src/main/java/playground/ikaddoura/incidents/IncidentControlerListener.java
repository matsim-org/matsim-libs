/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.ikaddoura.incidents;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.NetworkChangeEventsParser;

/**
* @author ikaddoura
*/

public class IncidentControlerListener implements IterationStartsListener {
	private static final Logger log = Logger.getLogger(IncidentControlerListener.class);

	private List<String> networkChangeEventsFiles = null;
	private int dayCounter = 0;
	private Controler controler;	
		
	public IncidentControlerListener(Controler controler, List<String> networkChangeEventsFiles) {
		this.networkChangeEventsFiles = networkChangeEventsFiles;
		this.controler = controler;
	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		
		if (dayCounter == networkChangeEventsFiles.size()) {
			dayCounter = 0;
		}
		String nce = networkChangeEventsFiles.get(dayCounter);

		log.info("Setting network change events for the next iteration: " + nce);
						
		List<NetworkChangeEvent> events = new ArrayList<>() ;
		new NetworkChangeEventsParser(controler.getScenario().getNetwork(), events).readFile(nce);;
				
		Network network = controler.getScenario().getNetwork();
		NetworkUtils.getNetworkChangeEvents(network).clear();
		NetworkUtils.setNetworkChangeEvents(network,events);
		event.getServices().getConfig().network().setChangeEventsInputFile(nce);
	}

}

