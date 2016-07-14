/* *********************************************************************** *
 * project: org.matsim.*
 * MyMapViewer.java
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

package org.matsim.contrib.evacuation.io;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.evacuation.control.Controller;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkChangeEvent.ChangeValue;
import org.matsim.core.network.NetworkChangeEventFactory;
import org.matsim.core.network.NetworkChangeEventFactoryImpl;
import org.matsim.core.network.NetworkChangeEventsWriter;
import org.matsim.core.utils.misc.Time;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

/**
 * all i/origin functions involving the configuration files
 * 
 * @author wdoering
 *
 */
public class ConfigIO
{

	public static synchronized boolean saveRoadClosures(Controller controller, HashMap<Id<Link>, String> roadClosures)
	{
		
		Scenario scenario = controller.getScenario();
		String scenarioPath = controller.getScenarioPath();
		String configFile = controller.getConfigFilePath();

		if (roadClosures.size() > 0)
		{

			scenario.getConfig().network().setTimeVariantNetwork(true);
			String changeEventsFile = scenarioPath + "/networkChangeEvents.xml";
			scenario.getConfig().network().setChangeEventsInputFile(changeEventsFile);
			new ConfigWriter(scenario.getConfig()).write(configFile);
			
			// create change event
			Collection<NetworkChangeEvent> evs = new ArrayList<NetworkChangeEvent>();
			NetworkChangeEventFactory fac = new NetworkChangeEventFactoryImpl();

			Iterator<Entry<Id<Link>, String>> it = roadClosures.entrySet().iterator();
			while (it.hasNext())
			{
				Entry<Id<Link>, String> pairs = it.next();

				Id<Link> currentId = pairs.getKey();
				String timeString = pairs.getValue();

				try
				{
					double time = Time.parseTime(timeString);
					NetworkChangeEvent ev = fac.createNetworkChangeEvent(time);
					 ev.setFreespeedChange(new
					 ChangeValue(NetworkChangeEvent.ChangeType.ABSOLUTE, 0));
//					ev.setFlowCapacityChange(new ChangeValue(NetworkChangeEvent.ChangeType.ABSOLUTE, 0));

					ev.addLink(scenario.getNetwork().getLinks().get(currentId));
					evs.add(ev);
				} catch (Exception e)
				{
					e.printStackTrace();
				}

			}

			NetworkChangeEventsWriter writer = new NetworkChangeEventsWriter();
			if (changeEventsFile.endsWith(".xml"))
			{
				writer.write(changeEventsFile, evs);
			} else
			{
				writer.write(changeEventsFile + ".xml", evs);
			}
			
			return true;

		}
		return false;

	}

}
