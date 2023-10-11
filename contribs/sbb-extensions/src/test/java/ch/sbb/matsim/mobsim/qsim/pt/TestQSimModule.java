/* *********************************************************************** *
 * project: org.matsim.* 												   *
 *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2023 by the members listed in the COPYING,        *
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
package ch.sbb.matsim.mobsim.qsim.pt;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import org.matsim.core.config.Config;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.pt.*;
import org.matsim.core.replanning.ReplanningContext;

/**
 * The deterministic transit simulation ({@link SBBTransitQSimEngine}) requires access to the current iteration number to determine if link-events should be created or not. This iteration number is
 * accessible using {@link ReplanningContext}. In order to run the tests, we have a special test module that registers an instance of a ReplanningContext in order to run the tests successfully. This
 * module does exactly that: it registers a special ReplanningContext for the tests.
 *
 * @author mrieser / SBB
 */
public class TestQSimModule extends AbstractQSimModule {

    public final DummyReplanningContext context;

    public TestQSimModule(Config config) {
        this.context = new DummyReplanningContext();
    }

    @Override
    protected void configureQSim() {
        bind(ReplanningContext.class).toInstance(context);
        bind(TransitStopHandlerFactory.class).to(ComplexTransitStopHandlerFactory.class);
		bind(TransitDriverAgentFactory.class).to(DefaultTransitDriverAgentFactory.class);
    }

	@Provides
	@Singleton
	public TransitStopAgentTracker transitStopAgentTracker(QSim qSim) {
		return new TransitStopAgentTracker(qSim.getEventsManager());
	}

    public static final class DummyReplanningContext implements ReplanningContext {

        private int iteration = 0;

        @Override
        public int getIteration() {
            return this.iteration;
        }

        public void setIteration(int iteration) {
            this.iteration = iteration;
        }
    }
}
