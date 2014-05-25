/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * Link2Shape.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2014 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */

package playground.mzilske.teach.tasks2014;

import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.utils.gis.matsim2esri.network.Links2ESRIShape;

public class Link2Shape {
	
	public static void main(String[] args) {
//		Config config = ConfigUtils.createConfig();
//		config.network().setInputFile("/Users/michaelzilske/wurst/network_joschka.xml");
////		config.plans().setInputFile("input/plans.xml");
//		Scenario scenario = ScenarioUtils.loadScenario(config);
//		XY2Links xy2Links = new XY2Links((NetworkImpl) scenario.getNetwork());
//		xy2Links.run(scenario.getPopulation());

        Links2ESRIShape.main(
                new String[]{"/Users/michaelzilske/wurst/network_joschka.xml","/Users/michaelzilske/wurst/networkline.shp","/Users/michaelzilske/wurst/networkpoly.shp",
                        TransformationFactory.WGS84_UTM33N});


//        SelectedPlans2ESRIShape plans2Shape = new SelectedPlans2ESRIShape(scenario.getPopulation(), scenario.getNetwork(), MGC.getCRS(crs), "input");
//		plans2Shape.setWriteActs(true);
//		plans2Shape.setWriteLegs(false);
//		plans2Shape.write();
//
//		config.plans().setInputFile("output/output_plans.xml.gz");
//		scenario = ScenarioUtils.loadScenario(config);
//		plans2Shape = new SelectedPlans2ESRIShape(scenario.getPopulation(), scenario.getNetwork(), MGC.getCRS(crs), "output");
//		plans2Shape.setWriteActs(false);
//		plans2Shape.setWriteLegs(true);
//		plans2Shape.write();
//
	}

}
