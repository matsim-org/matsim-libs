/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package org.matsim.contrib.noise;

import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import org.apache.log4j.Logger;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;

/**
* @author ikaddoura
*/
public final class NoiseComputationModule extends AbstractModule {
	private static final Logger log = Logger.getLogger(NoiseComputationModule.class);


	@Override
	public void install() {

		final Multibinder<NoiseVehicleType> noiseVehicleTypeMultibinder = Multibinder.newSetBinder(this.binder(), NoiseVehicleType.class);



		NoiseConfigGroup noiseParameters = ConfigUtils.addOrGetModule(this.getConfig(), NoiseConfigGroup.class);

		this.bind(NoiseContext.class).to(NoiseContextImpl.class).in( Singleton.class );
		this.bind(BarrierContext.class).in(Singleton.class);
		this.bind(ShieldingContext.class).in(Singleton.class);
		this.bind(ReflectionContext.class).in(Singleton.class);

		switch (noiseParameters.getNoiseComputationMethod()) {
			case RLS90:
				this.bind(NoiseEmission.class).to(RLS90NoiseEmission.class);
				this.bind(NoiseImmission.class).to(RLS90NoiseImmission.class);
				this.bind(NoiseVehicleIdentifier.class).to(RLS90NoiseVehicleIdentifier.class);
				this.bind(ShieldingCorrection.class).to(RLS90ShieldingCorrection.class);
				for(RLS90VehicleType type: RLS90VehicleType.values()) {
					noiseVehicleTypeMultibinder.addBinding().toInstance(type);
				}
				break;
			case RLS19:
				this.bind(NoiseEmission.class).to(RLS19NoiseEmission.class);
				this.bind(NoiseImmission.class).to(RLS19NoiseImmission.class);
				this.bind(NoiseVehicleIdentifier.class).to(RLS19NoiseVehicleIdentifier.class);
				this.bind(ShieldingCorrection.class).to(RLS19ShieldingCorrection.class);
				for(RLS19VehicleType type: RLS19VehicleType.values()) {
					noiseVehicleTypeMultibinder.addBinding().toInstance(type);
				}
				this.bind(RoadSurfaceContext.class).in(Singleton.class);
				this.bind(IntersectionContext.class).in(Singleton.class);

				break;
			default:
				throw new IllegalStateException("Unrecognized noise computation method: " + noiseParameters.getNoiseComputationMethod());
		}

		this.bind(NoiseDamageCalculation.class).in(Singleton.class);

		this.bind(NoiseTimeTracker.class).in(Singleton.class); // needed!
		this.addEventHandlerBinding().to(NoiseTimeTracker.class);
		
		if (noiseParameters.isUseActualSpeedLevel()) {
			this.bind(LinkSpeedCalculation.class).in( Singleton.class ) ;
			this.addEventHandlerBinding().to(LinkSpeedCalculation.class);
		}
		
		if (noiseParameters.isComputePopulationUnits()) {
			this.bind(PersonActivityTracker.class).in( Singleton.class ) ;
			this.addEventHandlerBinding().to(PersonActivityTracker.class);
		}
				
		if (noiseParameters.isInternalizeNoiseDamages()) {

			this.bind(NoisePricingHandler.class).in( Singleton.class ) ;
			this.addEventHandlerBinding().to(NoisePricingHandler.class);

			log.info("Internalizing noise damages. This requires that the default travel disutility is replaced by a travel distuility which accounts for noise tolls.");
		}
		
		this.addControlerListenerBinding().to(NoiseCalculationOnline.class);
	}

}

