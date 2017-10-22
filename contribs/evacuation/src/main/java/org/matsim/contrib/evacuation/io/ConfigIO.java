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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.evacuation.control.Controller;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkChangeEvent.ChangeValue;
import org.matsim.core.network.io.NetworkChangeEventsWriter;
import org.matsim.core.utils.misc.Time;

/**
 * all i/origin functions involving the configuration files
 * 
 * @author wdoering
 *
 */
public class ConfigIO
{

	public static synchronized boolean saveRoadClosures(Controller controller, HashMap<Id<Link>, String> roadClosures, String changeEventsFile)
	{
		
		Scenario scenario = controller.getScenario();

		if (roadClosures.size() > 0)
		{

			//			scenario.getConfig().network().setTimeVariantNetwork(true);
			scenario.getConfig().network().setChangeEventsInputFile("changeEvents.xml");
			new ConfigWriter(scenario.getConfig()).write(controller.getConfigFilePath());

			// Since this caused too many problems, I am now disallowing to change the time variant network 
			// after the scenario has been loaded.  I tried to set this to true now just before the scenario
			// is loaded into Controller.  Might be better to have a deep copy method for config, but we don't 
			// have that. kai, oct'17
			
			// create change event
			Collection<NetworkChangeEvent> evs = new ArrayList<>();

			Iterator<Entry<Id<Link>, String>> it = roadClosures.entrySet().iterator();
			while (it.hasNext())
			{
				Entry<Id<Link>, String> pairs = it.next();

				Id<Link> currentId = pairs.getKey();
				String timeString = pairs.getValue();

				try
				{
					double time = Time.parseTime(timeString);
					NetworkChangeEvent ev = new NetworkChangeEvent(time);
					 ev.setFreespeedChange(new
					 ChangeValue(NetworkChangeEvent.ChangeType.ABSOLUTE_IN_SI_UNITS, 0));
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
