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

package playground.wdoering.grips.scenariomanager.control.io;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.network.*;
import org.matsim.core.network.NetworkChangeEvent.ChangeValue;
import org.matsim.core.replanning.DefaultPlanStrategiesModule;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.transitSchedule.TransitScheduleWriterV1;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.vehicles.VehicleWriterV1;
import playground.wdoering.grips.scenariomanager.control.Controller;
import playground.wdoering.grips.v2.ptlines.BusStop;
import playground.wdoering.grips.v2.ptlines.PTLinesGenerator;

import java.util.*;
import java.util.Map.Entry;

/**
 * all i/o functions involving the configuration files
 * 
 * @author wdoering
 *
 */
public class ConfigIO
{

	public static synchronized boolean saveRoadClosures(Controller controller, HashMap<Id, String> roadClosures)
	{
		
		Scenario scenario = controller.getScenario();
		String scenarioPath = controller.getScenarioPath();
		String configFile = controller.getConfigFilePath();
		System.out.println("scenario path: " + scenarioPath);
		System.out.println("rc size: " + roadClosures.size());

		if (roadClosures.size() > 0)
		{

			scenario.getConfig().network().setTimeVariantNetwork(true);
			String changeEventsFile = scenarioPath + "/networkChangeEvents.xml";
			System.out.println("nw change events file: " + changeEventsFile);
			scenario.getConfig().network().setChangeEventInputFile(changeEventsFile);
			new ConfigWriter(scenario.getConfig()).write(configFile);
			
			// create change event
			Collection<NetworkChangeEvent> evs = new ArrayList<NetworkChangeEvent>();
			NetworkChangeEventFactory fac = new NetworkChangeEventFactoryImpl();

			Iterator<Entry<Id,String>> it = roadClosures.entrySet().iterator();
			while (it.hasNext())
			{
				Entry<Id, String> pairs = it.next();

				Id currentId = (Id) pairs.getKey();
				String timeString = (String) pairs.getValue();

				try
				{
					double time = Time.parseTime(timeString);
					NetworkChangeEvent ev = fac.createNetworkChangeEvent(time);
					// ev.setFreespeedChange(new
					// ChangeValue(NetworkChangeEvent.ChangeType.ABSOLUTE, 0));
					ev.setFlowCapacityChange(new ChangeValue(NetworkChangeEvent.ChangeType.ABSOLUTE, 0));

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
	
	public static synchronized boolean savePTLines(Controller controller, Map<Id,BusStop> busStops)
	{
		Config config = controller.getScenario().getConfig();
		
		String scenarioPath = controller.getScenarioPath();
		
		//settings to activate pt simulation
		config.strategy().addParam("maxAgentPlanMemorySize", "3");
		config.strategy().addParam("Module_1", DefaultPlanStrategiesModule.DefaultStrategies.ReRoute.toString());
		config.strategy().addParam("ModuleProbability_1", "0.1");
		config.strategy().addParam("Module_2", DefaultPlanStrategiesModule.DefaultSelectors.ChangeExpBeta.toString());
		config.strategy().addParam("ModuleProbability_2", "0.8");
		config.strategy().addParam("Module_3", DefaultPlanStrategiesModule.DefaultStrategies.ChangeLegMode.toString());
		config.strategy().addParam("ModuleProbability_3", "0.4");
		config.strategy().addParam("ModuleDisableAfterIteration_3", "50");
		
		
		
		config.strategy().addParam("fractionOfIterationsToDisableInnovation", "0.8");
		
		
//		config.strategy().addParam("Module_4", "TransitTimeAllocationMutator");
//		config.strategy().addParam("ModuleProbability_4", "0.3");

		config.setParam("qsim", "startTime", "00:00:00");
		config.setParam("qsim", "endTime", "30:00:00");
		config.setParam("changeLegMode", "modes", "car,pt");
		config.setParam("changeLegMode", "ignoreCarAvailability", "false");
		
		config.setParam("transit", "transitScheduleFile", scenarioPath+"/transitSchedule.xml");
		config.setParam("transit", "vehiclesFile",scenarioPath+"/transitVehicles.xml");
		config.setParam("transit", "transitModes", "pt");

		config.scenario().setUseTransit(true);
		config.scenario().setUseVehicles(true);
		
		String configFile = controller.getConfigFilePath();
		
		new ConfigWriter(config).write(configFile);
		
		PTLinesGenerator gen = new PTLinesGenerator(controller.getScenario(),busStops);
		TransitSchedule schedule = gen.getTransitSchedule();
		
		new NetworkWriter(controller.getScenario().getNetwork()).write(config.network().getInputFile());
		new TransitScheduleWriterV1(schedule).write(scenarioPath+"/transitSchedule.xml");
		new VehicleWriterV1(((ScenarioImpl)controller.getScenario()).getVehicles()).writeFile(scenarioPath+"/transitVehicles.xml");
		
		
		
		return true;
	}
	
	
	

}
