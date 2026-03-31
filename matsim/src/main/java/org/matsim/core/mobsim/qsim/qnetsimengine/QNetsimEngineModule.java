
/* *********************************************************************** *
 * project: org.matsim.*
 * QNetsimEngineModule.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

package org.matsim.core.mobsim.qsim.qnetsimengine;

import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.qnetsimengine.linkspeedcalculator.LinkSpeedCalculator;
import org.matsim.core.mobsim.qsim.qnetsimengine.parking.ParkingSearchTimeCalculator;
import org.matsim.core.mobsim.qsim.qnetsimengine.vehicle_handler.VehicleHandler;

public final class QNetsimEngineModule extends AbstractQSimModule {
	public final static String COMPONENT_NAME = "NetsimEngine";

	/**
	 * The {@link QNetsimEngineModule} fills the following interfaces with bindings:<ul>
	 * <li>{@link QNetsimEngineI}</li>
	 * <li>{@link QNetworkFactory}</li>
	 * <li> (Q){@link NetworkModeDepartureHandler} </li>
	 * </ul>
	 */
	@Override
	protected void configureQSim() {
		// === QNetsimEngine:

		bind(QNetsimEngineI.class).to(QNetsimEngineWithThreadpool.class).in(Singleton.class);
		// (given the "overriding" architecture, this is a default binding which may be overridden later)

		addQSimComponentBinding(COMPONENT_NAME).to(QNetsimEngineI.class);
		// (this will register the MobsimEngine functionality.  necessary since QNetsimEngineI is a MobsimEngine, which needs to be registered.)

		// === QNetworkFactory:

		if (this.getConfig().qsim().isUseLanes()) {
			bind(DefaultQNetworkFactory.class).in(Singleton.class);
			// (provide this as a delegate to QLanesNetworkFactory)
			bind(QNetworkFactory.class).to(QLanesNetworkFactory.class).in(Singleton.class);
		} else {
			bind(QNetworkFactory.class).to(DefaultQNetworkFactory.class).in(Singleton.class);
		}

		// Calculators for QLink:

		Multibinder.newSetBinder(this.binder(), LinkSpeedCalculator.class);
		Multibinder.newSetBinder(this.binder(), VehicleHandler.class);
		Multibinder.newSetBinder(this.binder(), ParkingSearchTimeCalculator.class);
		// (initialize this here so we do not have to hedge against "null".)

//		addLinkSpeedCalculatorBinding().to(...);

		// === departure handler:
		bind(NetworkModeDepartureHandler.class).to(NetworkModeDepartureHandlerDefaultImpl.class).in(Singleton.class);
		// (given the "overriding" architecture, this is a default binding which may be overridden later)

		addQSimComponentBinding(COMPONENT_NAME).to(NetworkModeDepartureHandler.class);
		// (this will register the DepartureHandler functionality.  Necessary since departureHandlers need to be registered.  It will,
		// however, use whatever is bound to the interface, and not necessarily the above binding.)

		// kai, jan'25:

		// I am currently thinking that the NetworkModeDepartureHandler as a separate interface is not needed.  It used to be hardwired into
		// the QNetsimEngine, but that is no longer the case.  Technically, it just does something like
		// qNetsimEngine.getNetsimNetwork.getNetsimLink.letVehicleDepart, so as long as all those classes have the necessary functionality, it
		// does not have to be tightly integrated.

		// The question, in more general terms, is if we need to put things, which are registered as QSimComponents, also behind interfaces.

		// on the other hand, it may be a bit more natural (compared to the controler-related injection architecture) to replace the binding of
		// the interface and rather not touch the QSimComponents if one does not have to.

		// The QSimComponent thing is really not much more than a multibinder, except that one can remove things before everything is plugged
		// together.  In other places, we say something like addTravelTimeBinding()( modeString ).to( ...Impl.class ).  So there we are NOT putting
		// an interface in between.

	}
}
