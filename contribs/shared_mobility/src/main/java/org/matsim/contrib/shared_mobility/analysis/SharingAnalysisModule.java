package org.matsim.contrib.shared_mobility.analysis;

import org.matsim.core.controler.AbstractModule;
/**
 * @author steffenaxer
 */
public class SharingAnalysisModule extends AbstractModule {

	@Override
	public void install() {
		bind(SharingLegCollectorImpl.class).asEagerSingleton();
		bind(SharingLegCollector.class).to(SharingLegCollectorImpl.class);
		addEventHandlerBinding().to(SharingLegCollectorImpl.class);
		addControlerListenerBinding().to(SharingLegCollectorImpl.class);
		addControlerListenerBinding().to(SharingStatisticsAnalyzer.class);
	}
}
