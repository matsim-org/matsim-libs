/*
 * Copyright (C) 2022 MOIA GmbH - All Rights Reserved
 *
 * You may use, distribute and modify this code under the terms
 * of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License,
 * or (at your option) any later version.
 */
package org.matsim.contrib.drt.extension.fiss;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.qnetsimengine.NetworkModeDepartureHandler;
import org.matsim.core.mobsim.qsim.qnetsimengine.NetworkModeDepartureHandlerDefaultImpl;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngineI;
import org.matsim.core.router.util.TravelTime;

/**
 * @author nkuehnel / MOIA, hrewald
 */
public class FISSQSimModule extends AbstractQSimModule {

    static final public String COMPONENT_NAME = "FISS";

    @Override
    protected void configureQSim() {
	    bind(NetworkModeDepartureHandlerDefaultImpl.class ).in( Singleton.class );
	    // (this binds the above since it will be needed in FISS. )

	    bind( NetworkModeDepartureHandler.class ).to( FISS.class ).in( Singleton.class );
	    // (the above will bind the FISS departure handler correctly.  But not remove the pre-existing NetworkModeDepartureHandlerDefaultImpl as a "second" departure handler.  kai, jan'25)

	    addQSimComponentBinding( COMPONENT_NAME ).to( FISS.class ).in( Singleton.class );
	    // (this will register FISS as a departure handler)

    }

//    @Provides
//    @Singleton
//    FISS provideMultiModalDepartureHandler( MatsimServices matsimServices, QNetsimEngineI qNetsimEngine,
//					    QSimConfigGroup qsimConfig, Scenario scenario, EventsManager eventsManager,
//					    @Named(TransportMode.car) TravelTime travelTime, NetworkModeDepartureHandler networkModeDepartureHandler ) {
//        Config config = scenario.getConfig();
//        FISSConfigGroup fissConfigGroup = ConfigUtils.addOrGetModule(config, FISSConfigGroup.class);
//		return new FISS(matsimServices, qNetsimEngine, scenario, eventsManager, fissConfigGroup, travelTime, networkModeDepartureHandler );
//    }
    // yyyyyy I am not sure if the above @Provides is really necessary.  Could as well inject the FISS class directly.  kai, jan'25

	// kai, jan'25:

	// with the above (which currently does not work), NetworkModeDepartureHandler is bound to FISS, which replaces the
	// NetworkModeDepartureHandlerDefaultImpl, and has the consequence that network modes for which FISS does NOT feel responsible are no longer
	// served.

	// as stated elsewhere, I am not sure if we need NetworkModeDepartureHandler at all.
}
