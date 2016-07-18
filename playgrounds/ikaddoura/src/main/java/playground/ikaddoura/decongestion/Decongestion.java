/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.ikaddoura.decongestion;

import org.apache.log4j.Logger;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;

import playground.ikaddoura.decongestion.DecongestionConfigGroup.TollingApproach;
import playground.ikaddoura.decongestion.data.DecongestionInfo;
import playground.ikaddoura.decongestion.routing.TollTimeDistanceTravelDisutilityFactory;
import playground.ikaddoura.decongestion.tollSetting.DecongestionTollSetting;
import playground.ikaddoura.decongestion.tollSetting.DecongestionTollingBangBang;
import playground.ikaddoura.decongestion.tollSetting.DecongestionTollingPID;
import playground.ikaddoura.decongestion.tollSetting.old.DecongestionTollingV8;

/**
* @author ikaddoura
*/

public class Decongestion {
	private static final Logger log = Logger.getLogger(Decongestion.class);

	private final DecongestionInfo info;
	private final Controler controler;
	
	public Decongestion(DecongestionInfo info) {
		this.info = info;
		this.controler = new Controler(info.getScenario());
		prepare();
	}

	private void prepare() {
								
		DecongestionTollSetting tollSettingApproach = null;
		
		if (info.getDecongestionConfigGroup().getTOLLING_APPROACH().equals(TollingApproach.V8)) {
			tollSettingApproach = new DecongestionTollingV8(info);
			
		} else if (info.getDecongestionConfigGroup().getTOLLING_APPROACH().equals(TollingApproach.BangBang)) {
			tollSettingApproach = new DecongestionTollingBangBang(info);
		
		} else if (info.getDecongestionConfigGroup().getTOLLING_APPROACH().equals(TollingApproach.PID)) {
			tollSettingApproach = new DecongestionTollingPID(info);	
			
		} else if (info.getDecongestionConfigGroup().getTOLLING_APPROACH().equals(TollingApproach.NoPricing)) {
			
			info.getDecongestionConfigGroup().setTOLL_ADJUSTMENT(0.0);
			info.getDecongestionConfigGroup().setINITIAL_TOLL(0.0);
			info.getDecongestionConfigGroup().setUPDATE_PRICE_INTERVAL(Integer.MAX_VALUE);
			info.getDecongestionConfigGroup().setTOLERATED_AVERAGE_DELAY_SEC(Double.MAX_VALUE);			
			tollSettingApproach = new DecongestionTollingV8(info);
			
		} else {
			throw new RuntimeException("Unknown decongestion toll setting approach. Aborting...");
		}
		
		// decongestion pricing
		final DecongestionControlerListener decongestion = new DecongestionControlerListener(info, tollSettingApproach);		
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				this.addControlerListenerBinding().toInstance(decongestion);
			}
		});
		
		// toll-adjusted routing
		final TollTimeDistanceTravelDisutilityFactory travelDisutilityFactory = new TollTimeDistanceTravelDisutilityFactory(info, info.getScenario().getConfig().planCalcScore());
		travelDisutilityFactory.setSigma(0.);
		controler.addOverridingModule(new AbstractModule(){
			@Override
			public void install() {
				this.bindCarTravelDisutilityFactory().toInstance( travelDisutilityFactory );
			}
		});		
	}

	public void run() {	
		
        controler.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists);
		controler.run();
		
		log.info("Decongestion simulation run completed.");
	}

	public Controler getControler() {
		return controler;
	}

}

