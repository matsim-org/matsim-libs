package org.matsim.contrib.ev.withinday;

import org.matsim.contrib.ev.withinday.analysis.WithinDayChargingAnalysisHandler;
import org.matsim.contrib.ev.withinday.analysis.WithinDayChargingAnalysisListener;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.OutputDirectoryHierarchy;

import com.google.inject.Provides;
import com.google.inject.Singleton;

/**
 * This module is the main entry point for within-day electric vehicle charging
 * (WEVC).
 * 
 * @author Sebastian Hörl (sebhoerl), IRT SystemX
 */
public class WithinDayEvModule extends AbstractModule {
	@Override
	public void install() {
		installQSimModule(new WithinDayEvQSimModule());
		addControlerListenerBinding().to(WithinDayChargingAnalysisListener.class);
		addEventHandlerBinding().to(WithinDayChargingAnalysisHandler.class);
	}

	@Provides
	@Singleton
	WithinDayChargingAnalysisListener provideWithinDayChargingAnalysisListener(WithinDayChargingAnalysisHandler handler,
			OutputDirectoryHierarchy outputDirectoryHierarchy) {
		return new WithinDayChargingAnalysisListener(handler, outputDirectoryHierarchy);
	}

	@Provides
	@Singleton
	WithinDayChargingAnalysisHandler provideWithinDayChargingAnalysisHandler() {
		return new WithinDayChargingAnalysisHandler();
	}
}
