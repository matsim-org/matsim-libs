/*
 *   *********************************************************************** *
 *   project: org.matsim.*
 *   *********************************************************************** *
 *                                                                           *
 *   copyright       : (C)  by the members listed in the COPYING,        *
 *                     LICENSE and WARRANTY file.                            *
 *   email           : info at matsim dot org                                *
 *                                                                           *
 *   *********************************************************************** *
 *                                                                           *
 *     This program is free software; you can redistribute it and/or modify  *
 *     it under the terms of the GNU General Public License as published by  *
 *     the Free Software Foundation; either version 2 of the License, or     *
 *     (at your option) any later version.                                   *
 *     See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                           *
 *   ***********************************************************************
 *
 */

package org.matsim.contrib.freight;

import org.matsim.contrib.freight.controler.CarrierModule;
import org.matsim.contrib.freight.controler.CarrierPlanStrategyManagerFactory;
import org.matsim.contrib.freight.controler.CarrierScoringFunctionFactory;
import org.matsim.contrib.freight.usecases.chessboard.CarrierScoringFunctionFactoryImpl;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.AllowsConfiguration;

public class Freight{
	// yyyy todo:
	// * introduce freight config group		=> DONE by oct' 07 '19,	tschlenther
	// * read freight input files in module => DONE by oct' 07 '19,	tschlenther
	// * repair execution path where config instead of scenario is given to controler

	public static void configure( AllowsConfiguration ao ) {
//		Gbl.assertIf( ao instanceof Controler);  // we need the scenario; otherwise find other way
//		Controler controler = (Controler) ao;
//		FreightConfigGroup freightConfig = ConfigUtils.addOrGetModule( controler.getConfig(), FreightConfigGroup.class );;
//		if ( true ){
//			freightConfig.setTimeWindowHandling( FreightConfigGroup.TimeWindowHandling.enforceBeginnings );
//		} else{
//			freightConfig.setTimeWindowHandling( FreightConfigGroup.TimeWindowHandling.ignore );
//		}
		ao.addOverridingModule( new CarrierModule( ) ) ;

		ao.addOverridingModule( new AbstractModule(){
			@Override
			public void install(){
				bind( CarrierScoringFunctionFactory.class ).to( CarrierScoringFunctionFactoryImpl.class  ) ;

				// yyyy in the long run, needs to be done differently (establish strategy manager as fixed infrastructure; have user code register strategies there).
				// kai/kai, jan'21
				bind( CarrierPlanStrategyManagerFactory.class ).toInstance( () -> null );
			}
		} ) ;
		
	}

}
