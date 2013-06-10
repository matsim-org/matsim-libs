/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

/**
 * 
 */
package org.matsim.contrib.matsim4opus.usecases;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.matsim4opus.config.M4UImprovedPseudoPtConfigUtils;
import org.matsim.contrib.matsim4opus.config.modules.ImprovedPseudoPtConfigGroup;
import org.matsim.contrib.matsim4opus.improvedpseudopt.MATSim4UrbanSimRouterFactoryImpl;
import org.matsim.contrib.matsim4opus.improvedpseudopt.PtMatrix;
import org.matsim.contrib.matsim4opus.utils.network.NetworkBoundaryBox;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author nagel
 *
 */
public class ImprovedPseudoPtIntegrationTest {
	@Rule public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	@Ignore
	public void testIntegration() {
		Config config = ConfigUtils.createConfig() ;
		config.addModule( new ImprovedPseudoPtConfigGroup() ) ;
		
		// modify config according to needs
		// ...
		
		
		Controler controler = new Controler(config) ;
		
		PlansCalcRouteConfigGroup plansCalcRoute = controler.getScenario().getConfig().plansCalcRoute();
		
		// determining the bounds minX/minY -- maxX/maxY. For optimal performance of the QuadTree. All pt stops should be evenly distributed within this rectangle.
		NetworkBoundaryBox nbb = new NetworkBoundaryBox();
		nbb.setDefaultBoundaryBox(controler.getNetwork());
		
		PtMatrix ptMatrix = new PtMatrix(controler.getScenario().getNetwork(),
								plansCalcRoute.getTeleportedModeSpeeds().get(TransportMode.walk),
								plansCalcRoute.getTeleportedModeSpeeds().get(TransportMode.pt),
								plansCalcRoute.getBeelineDistanceFactor(),
								nbb.getXMin(), nbb.getYMin(), nbb.getXMax(), nbb.getYMax(),
								M4UImprovedPseudoPtConfigUtils.getConfigModuleAndPossiblyConvert(controler.getScenario().getConfig()));	
		controler.setTripRouterFactory( new MATSim4UrbanSimRouterFactoryImpl(controler, ptMatrix) ); // the car and pt router

		
		controler.run();
		
		// compare some results
		// ...
		
	}
}
