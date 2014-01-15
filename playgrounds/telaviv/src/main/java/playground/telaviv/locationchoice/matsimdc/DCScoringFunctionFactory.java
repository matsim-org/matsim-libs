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

package playground.telaviv.locationchoice.matsimdc;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.locationchoice.bestresponse.DestinationChoiceBestResponseContext;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionAccumulator;
import org.matsim.core.scoring.functions.CharyparNagelActivityScoring;
import org.matsim.core.scoring.functions.CharyparNagelAgentStuckScoring;
import org.matsim.core.scoring.functions.CharyparNagelLegScoring;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;
//import org.matsim.core.trafficmonitoring.TravelTimeCalculatorFactoryImpl;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

import playground.telaviv.locationchoice.CalculateDestinationChoice;
import playground.telaviv.zones.ZoneMapping;

public class DCScoringFunctionFactory extends org.matsim.core.scoring.functions.CharyparNagelScoringFunctionFactory {
	private final Controler controler;
	private DestinationChoiceBestResponseContext dcContext;
	private Config config;	
//	private int iteration = -1;
	private ZoneMapping zoneMapping;
	private CalculateDestinationChoice dcCalculator;
	private final static Logger log = Logger.getLogger(DCScoringFunctionFactory.class);
	private boolean initialized = false;

	public DCScoringFunctionFactory(Config config, Controler controler, DestinationChoiceBestResponseContext dcContext) {
		super(config.planCalcScore(), controler.getNetwork());
		this.controler = controler;
		this.dcContext = dcContext;
		this.config = config;
		log.info("creating DCScoringFunctionFactory");
	}
	
	private void initialize() {
		// should not play a role here when iteration number is exactly set. 
		
//		not necessary anymore as the constant factors do not change over the iterations:
//		if (this.iteration != this.controler.getIterationNumber()) {
//			this.iteration = this.controler.getIterationNumber();
			this.zoneMapping = new ZoneMapping(this.dcContext.getScenario(), TransformationFactory.getCoordinateTransformation("EPSG:2039", "WGS84"));
			this.dcCalculator = new CalculateDestinationChoice(this.dcContext.getScenario());
			this.dcCalculator.calculateVTODForDCModule();
			
			// actually not necessary here:
//			this.dcCalculator.calculateDynamicFactors(
//					new TravelTimeCalculatorFactoryImpl().createTravelTimeCalculator(this.dcContext.getScenario().getNetwork(), 
//							this.dcContext.getScenario().getConfig().travelTimeCalculator()).getLinkTravelTimes());
//					
//			this.dcCalculator.calculateTotalFactors();			
//		}
	}
		
	@Override
	public ScoringFunction createNewScoringFunction(Plan plan) {
		if (!this.initialized) {
			this.initialize();
			this.initialized = true;
		}
		
		ScoringFunctionAccumulator scoringFunctionAccumulator = new ScoringFunctionAccumulator();
		
		CharyparNagelActivityScoring scoringFunction = new DCActivityScoringFunction(
					(PlanImpl)plan, 
					this.dcContext.getFacilityPenalties(), 
					dcContext,
					this.zoneMapping,
					this.dcCalculator);
		scoringFunctionAccumulator.addScoringFunction(scoringFunction);		
		scoringFunctionAccumulator.addScoringFunction(new CharyparNagelLegScoring(new CharyparNagelScoringParameters(config.planCalcScore()), controler.getNetwork()));
		scoringFunctionAccumulator.addScoringFunction(new CharyparNagelAgentStuckScoring(new CharyparNagelScoringParameters(config.planCalcScore())));
		return scoringFunctionAccumulator;
	}
}
