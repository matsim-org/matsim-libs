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

import java.util.Arrays;

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

public class ShockwaveExperiment {
	
	private static final String RUN_DIR = "../../../../repos/shared-svn/projects/mixedTraffic/triangularNetwork/run313/singleModes/holes/car_SW_kn/";

	public static void main(String[] args) {

		boolean isUsingOTFVis = false;

		InputsForFDTestSetUp inputs = new InputsForFDTestSetUp();
		inputs.setTravelModes(new String [] {"car"});
		inputs.setModalSplit(new String [] {"1.0"}); //in pcu
		inputs.setTrafficDynamics(TrafficDynamics.withHoles);
		inputs.setLinkDynamics(LinkDynamics.FIFO);
		inputs.setTimeDependentNetwork(true);
		inputs.setSnapshotFormats(Arrays.asList( "transims", "otfvis" ));

		GenerateFundamentalDiagramData generateFDData = new GenerateFundamentalDiagramData(inputs);
		generateFDData.setRunDirectory(RUN_DIR);
		generateFDData.setReduceDataPointsByFactor(40);
		generateFDData.setIsPlottingDistribution(false);
		generateFDData.setIsUsingLiveOTFVis(isUsingOTFVis);
		generateFDData.setIsWritingEventsFileForEachIteration(true);

		//set flow capacity of the base link to zero for 1 min.
		Scenario sc = generateFDData.getScenario();
		sc.getConfig().qsim().setStuckTime(10*3600);

		if (! isUsingOTFVis ) { //necessary to avoid placement on link/lane (2-D) if using the data to plot only one-D space.
			sc.getConfig().qsim().setLinkWidthForVis((float)0);
			((NetworkImpl) sc.getNetwork()).setEffectiveLaneWidth(0.);		
		}

		ScenarioUtils.loadScenario(sc);
		Link desiredLink = sc.getNetwork().getLinks().get(Id.createLinkId(1));//baseLink is not chosen to observe some spillover


		double flowCapBefore = desiredLink.getCapacity();
		NetworkChangeEventFactory cef = new NetworkChangeEventFactoryImpl() ;
		{
			NetworkChangeEvent event = cef.createNetworkChangeEvent(20.*60.) ;
			event.setFlowCapacityChange(new ChangeValue(ChangeType.ABSOLUTE, 0.0)); 
			event.addLink(desiredLink);
			((NetworkImpl)sc.getNetwork()).addNetworkChangeEvent(event);
		}
		{
			NetworkChangeEvent event = cef.createNetworkChangeEvent(20.*60.+60*5) ;
			event.setFlowCapacityChange(new ChangeValue(ChangeType.ABSOLUTE, flowCapBefore/3600.)); // value should be in pcu/s
			event.addLink(desiredLink);
			((NetworkImpl)sc.getNetwork()).addNetworkChangeEvent(event);
		}
		generateFDData.run();
	}
}
