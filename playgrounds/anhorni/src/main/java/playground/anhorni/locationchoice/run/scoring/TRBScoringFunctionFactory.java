/* *********************************************************************** *
 * project: org.matsim.*
 * TRBScoringFunctionFactory.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package playground.anhorni.locationchoice.run.scoring;

import org.apache.log4j.Logger;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionAccumulator;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.facilities.ActivityFacilitiesImpl;
import org.matsim.facilities.ActivityFacility;

import playground.anhorni.locationchoice.preprocess.facilities.FacilityQuadTreeBuilder;

public class TRBScoringFunctionFactory extends org.matsim.core.scoring.functions.CharyparNagelScoringFunctionFactory {

	private ActivityScoringFunction scoringFunction = null;
	private ShoppingScoreAdditionals shoppingScoreAdditionals;
	private double sign = 1.0;
	private boolean sizeScore = false;
	private boolean densityScore = false;
	private boolean shoppingCentersScore = false;
	private QuadTree<ActivityFacility> shopQuadTree;
	private final Controler controler;
	private PlanCalcScoreConfigGroup config;

	private final static Logger log = Logger.getLogger(TRBScoringFunctionFactory.class);

	public TRBScoringFunctionFactory(PlanCalcScoreConfigGroup config, Controler controler) {
        super(config, controler.getScenario().getNetwork());
		this.controler = controler;
		this.config = config;
		this.init();
	}

	private void init() {
			this.sign = Double.parseDouble(this.controler.getConfig().getModule("trb_scoring").getValue("sign"));
			log.info("Sign: " + this.sign);

			this.sizeScore = Boolean.parseBoolean(
					this.controler.getConfig().getModule("trb_scoring").getValue("sizeScore"));
			log.info("Size scoring: " + this.sizeScore);

			this.densityScore = Boolean.parseBoolean(
					this.controler.getConfig().getModule("trb_scoring").getValue("densityScore"));
			log.info("Density scoring: " + this.densityScore);

			this.shoppingCentersScore = Boolean.parseBoolean(
					this.controler.getConfig().getModule("trb_scoring").getValue("shoppingCentersScore"));
			log.info("Shopping center scoring: " + this.shoppingCentersScore);

        this.shoppingScoreAdditionals = new ShoppingScoreAdditionals(this.controler.getScenario().getActivityFacilities());
	}

	private void initQuadTree() {
		// trees
		FacilityQuadTreeBuilder treeBuilder = new FacilityQuadTreeBuilder();
        this.shopQuadTree = treeBuilder.buildFacilityQuadTree("shop", (ActivityFacilitiesImpl) controler.getScenario().getActivityFacilities());
		this.shoppingScoreAdditionals.setShopQuadTree(shopQuadTree);
	}

	public ScoringFunction getNewScoringFunction(PlanImpl plan) {

		if (this.shopQuadTree == null) {
			this.initQuadTree();
		}

		ScoringFunctionAccumulator scoringFunctionAccumulator = new ScoringFunctionAccumulator();

		this.scoringFunction = new ActivityScoringFunction(plan, CharyparNagelScoringParameters.getBuilder(config).create(), this.controler.getScenario().getActivityFacilities());
		this.scoringFunction.setSign(this.sign);
		this.scoringFunction.setSizeScore(this.sizeScore);
		this.scoringFunction.setDensityScore(this.densityScore);
		this.scoringFunction.setShoppingCentersScore(this.shoppingCentersScore);
		this.scoringFunction.setShoppingScoreAdditionals(this.shoppingScoreAdditionals);

		scoringFunctionAccumulator.addScoringFunction(this.scoringFunction);
		scoringFunctionAccumulator.addScoringFunction(
				new org.matsim.core.scoring.functions.CharyparNagelLegScoring(CharyparNagelScoringParameters.getBuilder(config).create(), controler.getScenario().getNetwork()));
		scoringFunctionAccumulator.addScoringFunction(
				new org.matsim.core.scoring.functions.CharyparNagelMoneyScoring(CharyparNagelScoringParameters.getBuilder(config).create()));
		scoringFunctionAccumulator.addScoringFunction(
				new org.matsim.core.scoring.functions.CharyparNagelAgentStuckScoring(CharyparNagelScoringParameters.getBuilder(config).create()));

		return scoringFunctionAccumulator;
	}

	public ActivityScoringFunction getActivities() {
		return scoringFunction;
	}
}
