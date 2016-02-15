/* *********************************************************************** *
 * project: org.matsim.*
 * DefaultTravelCostCalculatorFactoryImpl
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.anhorni.surprice;

import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.roadpricing.RoadPricingConfigGroup;
import org.matsim.roadpricing.RoadPricingReaderXMLv1;
import org.matsim.roadpricing.RoadPricingSchemeImpl;
import org.matsim.utils.objectattributes.ObjectAttributes;

/**
 * @author anhorni
 *
 */
public class SurpriceTravelDisutilityFactoryImpl implements TravelDisutilityFactory {
	
	private String day;
	private AgentMemories memories;
	private  ObjectAttributes preferences;
	private MatsimServices controler;
	
	public SurpriceTravelDisutilityFactoryImpl(String day, AgentMemories memories,  ObjectAttributes preferences, MatsimServices controler) {
		this.day = day;
		this.memories = memories;
		this.preferences = preferences;
		this.controler = controler;
	}

	@Override
	public TravelDisutility createTravelDisutility(TravelTime timeCalculator, PlanCalcScoreConfigGroup cnScoringGroup) {
		RoadPricingSchemeImpl scheme = new RoadPricingSchemeImpl();
		RoadPricingReaderXMLv1 rpReader = new RoadPricingReaderXMLv1(scheme);
		try {
            rpReader.parse(ConfigUtils.addOrGetModule(controler.getConfig(), RoadPricingConfigGroup.GROUP_NAME, RoadPricingConfigGroup.class).getTollLinksFile());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return new SurpriceTravelDisutility(timeCalculator, cnScoringGroup, this.day, this.memories, this.preferences, scheme,
				Boolean.parseBoolean(this.controler.getConfig().findParam(Surprice.SURPRICE_RUN, "useRoadPricing")));
	}
}
