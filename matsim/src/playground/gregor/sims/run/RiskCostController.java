/* *********************************************************************** *
 * project: org.matsim.*
 * RiskCostControler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.gregor.sims.run;

import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.core.api.population.Population;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.trafficmonitoring.TravelTimeCalculatorBuilder;

import playground.gregor.flooding.FloodingReader;
import playground.gregor.flooding.RiskCostFromFloodingData;
import playground.gregor.sims.evacbase.Building;
import playground.gregor.sims.evacbase.BuildingsShapeReader;
import playground.gregor.sims.evacbase.EvacuationNetGenerator;
import playground.gregor.sims.evacbase.EvacuationPlansGenerator;
import playground.gregor.sims.evacbase.EvacuationPopulationFromShapeFileLoader;
import playground.gregor.sims.riskaversion.RiskAverseTravelCostCalculator;
import playground.gregor.sims.riskaversion.RiskCostCalculator;

public class RiskCostController extends Controler {

	private final static Logger log = Logger
			.getLogger(RiskCostController.class);

	private List<Building> buildings;

	public RiskCostController(final String[] args) {
		super(args);
	}

	@Override
	protected void setUp() {

		// TravelTimeAggregatorFactory factory = new
		// TravelTimeAggregatorFactory();
		// factory.setTravelTimeDataPrototype(TravelTimeDataHashMap.class);
		// factory.setTravelTimeAggregatorPrototype(PessimisticTravelTimeAggregator.class);
		// double endTime = this.config.simulation().getEndTime() > 0 ?
		// this.config.simulation().getEndTime() : 30*3600;

		String netcdf = this.config.evacuation().getFloodingDataFile();

		FloodingReader fr = new FloodingReader(netcdf, true);

		RiskCostCalculator rc = new RiskCostFromFloodingData(this.network, fr);

		// RiskCostCalculator rc = new
		// RiskCostFromNetworkChangeEvents(this.network, false);
		this.events.addHandler(rc);
		double endTime = this.config.simulation().getEndTime() > 0 ? this.config
				.simulation().getEndTime()
				: 30 * 3600;
		if (this.travelTimeCalculator == null) {
			this.travelTimeCalculator = TravelTimeCalculatorBuilder
					.createTravelTimeCalculator(this.network, this.config
							.travelTimeCalculator());
		}

		// TravelTimeAndSocialCostCalculator t = new
		// TravelTimeAndSocialCostCalculator(this.network,this.config.controler().getTraveltimeBinSize(),(int)endTime,factory);
		// this.events.removeHandler(this.travelTimeCalculator);
		// this.travelTimeCalculator = t;
		// this.events.addHandler(sc);
		this.travelCostCalculator = new RiskAverseTravelCostCalculator(
				this.travelTimeCalculator, rc);

		// log.info("writing network xml file... ");
		// new NetworkWriter(this.network,
		// getOutputFilename("evacuation_net.xml")).write();
		// log.info("done");

		super.setUp();

		this.strategyManager = loadStrategyManager();
		// this.addControlerListener(sc);
	}

	@Override
	protected NetworkLayer loadNetwork() {
		NetworkLayer net = super.loadNetwork();
		new EvacuationNetGenerator(net, this.config).run();
		return net;
	}

	@Override
	protected Population loadPopulation() {
		if (this.buildings == null) {
			this.buildings = BuildingsShapeReader.readDataFile(this.config
					.evacuation().getBuildingsFile());
		}
		if (this.travelCostCalculator == null) {

		}
		EvacuationPopulationFromShapeFileLoader popGen = new EvacuationPopulationFromShapeFileLoader(
				this.buildings, this.network, this.config);
		// popGen.setTravelCostCalculator(this.travelCostCalculator);
		return popGen.getPopulation();
	}

	public static void main(final String[] args) {
		final Controler controler = new RiskCostController(args);
		controler.setOverwriteFiles(true);
		controler.run();
		System.exit(0);
	}

}
