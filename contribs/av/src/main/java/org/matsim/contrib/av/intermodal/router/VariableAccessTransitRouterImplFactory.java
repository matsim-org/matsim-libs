/* *********************************************************************** *
 * project: org.matsim.*
 * TransitRouterImplFactory.java
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

package org.matsim.contrib.av.intermodal.router;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.av.intermodal.router.config.VariableAccessConfigGroup;
import org.matsim.contrib.av.intermodal.router.config.VariableAccessModeConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.router.DijkstraFactory;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeAndDisutility;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.pt.router.PreparedTransitSchedule;
import org.matsim.pt.router.TransitRouter;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitRouterImpl;
import org.matsim.pt.router.TransitRouterNetwork;
import org.matsim.pt.router.TransitRouterNetworkTravelTimeAndDisutility;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import com.google.inject.name.Named;

import java.util.HashSet;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

/**
 * yyyyyy cld u pls add a short description what this is meant to do?  kai, jan'17
 * 
 * @author jbischoff
 */
@Singleton
public class VariableAccessTransitRouterImplFactory implements Provider<TransitRouter> {

	private final TransitRouterConfig transitRouterconfig;
	private final PlanCalcScoreConfigGroup planCalcScoreConfig;
	private final TransitRouterNetwork routerNetwork;
	private final PreparedTransitSchedule preparedTransitSchedule;
	private final Config config;
	private final Network network;
	private final Network carnetwork;

	@Inject
	VariableAccessTransitRouterImplFactory(final @Named("variableAccess") TransitSchedule schedule, final Config config, final Network network) {
		this.config = config;
		this.transitRouterconfig = new TransitRouterConfig(config.planCalcScore(),config.plansCalcRoute(),config.transitRouter(),config.vspExperimental());
		this.planCalcScoreConfig = config.planCalcScore();
		planCalcScoreConfig.setLocked();
		this.routerNetwork = TransitRouterNetwork.createFromSchedule(schedule, this.transitRouterconfig.getBeelineWalkConnectionDistance());
		this.preparedTransitSchedule = new PreparedTransitSchedule(schedule);
		this.network = network;
		
		TransportModeNetworkFilter filter = new TransportModeNetworkFilter(network);
		Network net = NetworkUtils.createNetwork();
		HashSet<String> modes = new HashSet<>();
		modes.add(TransportMode.car);
		filter.filter(net, modes);
		this.carnetwork = net;
	}

	@Override
	public TransitRouter get() {
		
				
		VariableAccessConfigGroup vaConfig = (VariableAccessConfigGroup) config.getModule(VariableAccessConfigGroup.GROUPNAME);
		VariableAccessEgressTravelDisutility variableAccessEgressTravelDisutility;
		if (vaConfig.getStyle().equals("fixed")){
		variableAccessEgressTravelDisutility = new FixedDistanceBasedVariableAccessModule(carnetwork,config);
		
		for (ConfigGroup cg: vaConfig.getVariableAccessModeConfigGroups()){
			VariableAccessModeConfigGroup modeconfig = (VariableAccessModeConfigGroup) cg;
			((FixedDistanceBasedVariableAccessModule) variableAccessEgressTravelDisutility).registerMode(modeconfig.getMode(), (int) modeconfig.getDistance(), modeconfig.isTeleported());
		}
		} else if (vaConfig.getStyle().equals("flexible")){
			variableAccessEgressTravelDisutility = new FlexibleDistanceBasedVariableAccessModule(carnetwork,config);
			for (ConfigGroup cg: vaConfig.getVariableAccessModeConfigGroups()){
				VariableAccessModeConfigGroup modeconfig = (VariableAccessModeConfigGroup) cg;
				((FlexibleDistanceBasedVariableAccessModule) variableAccessEgressTravelDisutility).registerMode(modeconfig.getMode(), (int) modeconfig.getDistance(), modeconfig.isTeleported());
			}
		} else {
			throw new RuntimeException("Unsupported Style");
		}
		TransitRouterNetworkTravelTimeAndDisutility ttCalculator = new TransitRouterNetworkTravelTimeAndDisutility(this.transitRouterconfig, this.preparedTransitSchedule);
		return new VariableAccessTransitRouterImpl(this.planCalcScoreConfig, this.transitRouterconfig, this.preparedTransitSchedule, this.routerNetwork, ttCalculator, ttCalculator, variableAccessEgressTravelDisutility, network);
	}
	
}
