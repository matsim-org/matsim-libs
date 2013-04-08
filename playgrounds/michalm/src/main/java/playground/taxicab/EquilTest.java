/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.taxicab ;

import org.matsim.core.config.*;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.*;
import org.matsim.core.config.groups.VspExperimentalConfigGroup.VspExperimentalConfigKey;
import org.matsim.core.controler.Controler;

public class EquilTest {

	public static void main(String[] args){	
		Config config = ConfigUtils.createConfig() ;
		config.network().setInputFile("../../matsim/examples/equil/network.xml") ;
		config.plans().setInputFile("../../matsim/examples/equil/plans-w-taxi.xml") ;
		
		config.vspExperimental().addParam(VspExperimentalConfigKey.vspDefaultsCheckingLevel, VspExperimentalConfigGroup.ABORT ) ;
		config.vspExperimental().setUsingOpportunityCostOfTimeInPtRouting(true) ;
		
		config.controler().setMobsim("qsim") ;
		config.addQSimConfigGroup(new QSimConfigGroup()) ;
		config.getQSimConfigGroup().setEndTime(50000.) ;
		config.getQSimConfigGroup().setVehicleBehavior("teleport") ; 

		{
			ActivityParams actParams = new ActivityParams("w") ;
			actParams.setTypicalDuration(8*3600.);
			config.planCalcScore().addActivityParams(actParams) ;
		}
		{
			ActivityParams actParams = new ActivityParams("h") ;
			actParams.setTypicalDuration(16*3600.);
			config.planCalcScore().addActivityParams(actParams) ;
		}
		
		config.controler().setLastIteration(0) ;
		
		final Controler controler = new Controler(config) ;
		controler.setOverwriteFiles(true) ;
		controler.addControlerListener(new MyControlerListener()) ;
		controler.run();
	}

}
