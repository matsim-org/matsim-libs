/* *********************************************************************** *
 * project: org.matsim.*
 * HitchHikingControler.java
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
package playground.thibautd.hitchiking.run;

import com.google.inject.Provider;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.framework.Mobsim;

import playground.thibautd.hitchiking.HitchHikingUtils;
import playground.thibautd.hitchiking.qsim.HitchHikingQsimFactory;
import playground.thibautd.hitchiking.routing.HitchHikingTripRouterFactory;
import playground.thibautd.hitchiking.spotweights.SpotWeighter;

/**
 * @author thibautd
 */
public class HitchHikingControler extends Controler {
	private final SpotWeighter spotWeighter;

	private void loadMyControlerListeners() {
		addControlerListener( new StartupListener() {
			@Override
			public void notifyStartup(final StartupEvent event) {
				setTripRouterFactory(
					new HitchHikingTripRouterFactory(
						event.getControler(),
						HitchHikingUtils.getSpots( getScenario() ),
						spotWeighter,
						HitchHikingUtils.getConfigGroup( getConfig() )));
			}
		});

//		super.loadControlerListeners();
	}


	public HitchHikingControler(
			final Scenario scenario,
			final SpotWeighter spotWeighter) {
		super(scenario);
		this.spotWeighter = spotWeighter;
		
		this.loadMyControlerListeners(); 
		
		throw new RuntimeException( Gbl.LOAD_DATA_IS_NOW_FINAL ) ;
	}

//	@Override
//	protected void loadData() {
//		this.addOverridingModule(new AbstractModule() {
//			@Override
//			public void install() {
//				bindMobsim().toProvider(new Provider<Mobsim>() {
//					@Override
//					public Mobsim get() {
//						return new HitchHikingQsimFactory(HitchHikingControler.this).createMobsim(getScenario(), getEvents());
//					}
//				});
//			}
//		});
//		super.loadData();
//	}

}

