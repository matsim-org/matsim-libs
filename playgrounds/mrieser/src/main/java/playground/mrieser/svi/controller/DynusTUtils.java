/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.mrieser.svi.controller;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scoring.functions.ScoringParameters;
import org.matsim.core.utils.misc.StringUtils;
import org.matsim.core.utils.misc.Time;

import com.google.inject.Provider;

import playground.mrieser.svi.converters.DynusTNetworkReader;
import playground.mrieser.svi.data.analysis.DynamicTravelTimeMatrix;

/**
 * @author mrieser
 */
public abstract class DynusTUtils {

	private final static Logger log = Logger.getLogger(DynusTUtils.class);
	
	
	public static final void integrate(final Controler controler) {
		log.info("Integrate functionality required for Dynus-T-Support in MATSim");
		final DynusTConfig dc = new DynusTConfig();
		
		Config config = controler.getConfig();
		dc.setDynusTDirectory(config.getParam("dynus-t", "dynusTDirectory"));
		dc.setModelDirectory(config.getParam("dynus-t", "modelDirectory"));
		dc.setZonesShapeFile(config.getParam("dynus-t", "zonesShpFile"));
		dc.setZoneIdToIndexMappingFile(config.findParam("dynus-t", "zoneIdToIndexMapping"));
		dc.setDemandFactor(Double.parseDouble(config.getParam("dynus-t", "dynusTDemandFactor")));
		dc.setZoneIdAttributeName(config.getParam("dynus-t", "zoneIdAttributeName"));
		dc.setActToZoneMappingFilename(config.getParam("dynus-t", "actToZoneMapping"));
		if (config.findParam("dynus-t", "timeBinSize_min") != null) {
			dc.setTimeBinSize_min(Integer.parseInt(config.getParam("dynus-t", "timeBinSize_min")));
		}
		dc.setPtLinesFile(config.findParam("dynus-t", "ptLinesFile"));

		{
			String param = config.findParam("dynus-t", "extractVehTrajectories");
			if (param != null) {
				String[] parts = StringUtils.explode(param, ',');
				for (String part : parts) {
					String[] times = StringUtils.explode(part, '-');
					dc.addVehTrajectoryExtract(Time.parseTime(times[0].trim()), Time.parseTime(times[1].trim()));
				}
			}
		}
		
		final DynamicTravelTimeMatrix ttMatrix = new DynamicTravelTimeMatrix(600, 30*3600.0); // 10min time bins, at most 30 hours
		
		log.info("Reading DynusT-network..." + dc.getModelDirectory());
		final Network dynusTNetwork = NetworkUtils.createNetwork();
		new DynusTNetworkReader(dynusTNetwork).readFiles(dc.getModelDirectory() + "/xy.dat", dc.getModelDirectory()+ "/network.dat");
		
		controler.addControlerListener(new DynusTControlerListener(dc, ttMatrix, dynusTNetwork));

		boolean useOnlyDynusT = Boolean.parseBoolean(controler.getConfig().getParam("dynus-t", "useOnlyDynusT"));
		if (useOnlyDynusT) {
			controler.addOverridingModule(new AbstractModule() {
				@Override
				public void install() {
					bindMobsim().toProvider(new Provider<Mobsim>() {
						@Override
						public Mobsim get() {
							return new DynusTMobsimFactory(dc, ttMatrix, dynusTNetwork, controler).createMobsim(controler.getScenario(), controler.getEvents());
						}
					});
				}
			});
			log.info("DynusT will be used as exclusive mobility simulation. Make sure that re-routing is *not* enabled as replanning strategy, as it will have no effect.");
			controler.setScoringFunctionFactory(new DynusTScoringFunctionFactory(dc, ttMatrix, dc.getActToZoneMapping(), new ScoringParameters.Builder(config.planCalcScore(), config.planCalcScore().getScoringParameters(null), config.scenario()).build()));
		} else {
			controler.setScoringFunctionFactory(new MixedScoringFunctionFactory(dc, ttMatrix, dc.getActToZoneMapping(), new ScoringParameters.Builder(config.planCalcScore(), config.planCalcScore().getScoringParameters(null), config.scenario()).build()));
		}
	}
}
