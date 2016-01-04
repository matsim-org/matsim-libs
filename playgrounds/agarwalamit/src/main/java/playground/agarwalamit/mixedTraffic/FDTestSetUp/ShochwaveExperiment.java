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
package playground.agarwalamit.mixedTraffic.FDTestSetUp;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.groups.QSimConfigGroup.LinkDynamics;
import org.matsim.core.config.groups.QSimConfigGroup.TrafficDynamics;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkChangeEvent.ChangeType;
import org.matsim.core.network.NetworkChangeEvent.ChangeValue;
import org.matsim.core.network.NetworkChangeEventFactory;
import org.matsim.core.network.NetworkChangeEventFactoryImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author amit
 */

public class ShochwaveExperiment {
	
	private static final String runDir = "../../../../repos/shared-svn/projects/mixedTraffic/triangularNetwork/run313/singleModes/withoutHoles/car_SW/";

	public static void main(String[] args) {

		InputsForFDTestSetUp inputs = new InputsForFDTestSetUp();
		inputs.setTravelModes(new String [] {"car"});
		inputs.setModalSplit(new String [] {"1.0"}); //in pcu
		inputs.setTrafficDynamics(TrafficDynamics.queue);
		inputs.setLinkDynamics(LinkDynamics.FIFO);
		inputs.setTimeDependentNetwork(true);
		
		GenerateFundamentalDiagramData generateFDData = new GenerateFundamentalDiagramData(inputs);
		generateFDData.setRunDirectory(runDir);
		generateFDData.setReduceDataPointsByFactor(Integer.valueOf(40));
		generateFDData.setIsPlottingDistribution(false);
		generateFDData.setIsUsingLiveOTFVis(false);
		generateFDData.setIsWritingEventsFileForEachIteration(true);
		
		//set flow capacity of the base link to zero for 1 min.
		Scenario sc = generateFDData.getScenario();
		sc.getConfig().qsim().setStuckTime(10*3600);
		
		ScenarioUtils.loadScenario(sc);
		Link baseLink = sc.getNetwork().getLinks().get(Id.createLinkId(0));
		
		
		double flowCapBefore = baseLink.getCapacity();
		NetworkChangeEventFactory cef = new NetworkChangeEventFactoryImpl() ;
		{
			NetworkChangeEvent event = cef.createNetworkChangeEvent(20.*60.) ;
			event.setFlowCapacityChange(new ChangeValue(ChangeType.ABSOLUTE, 0.0));
			event.addLink(baseLink);
			((NetworkImpl)sc.getNetwork()).addNetworkChangeEvent(event);
		}
		{
			NetworkChangeEvent event = cef.createNetworkChangeEvent(20.*60.+60*5) ;
			event.setFlowCapacityChange(new ChangeValue(ChangeType.ABSOLUTE, flowCapBefore/3600.)); // value should be in pcu/s
			event.addLink(baseLink);
			((NetworkImpl)sc.getNetwork()).addNetworkChangeEvent(event);
		}
		generateFDData.run();
	}
}
