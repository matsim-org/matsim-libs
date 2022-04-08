package org.matsim.contrib.shared_mobility.analysis;

import org.matsim.contrib.shared_mobility.run.SharingConfigGroup;
import org.matsim.contrib.shared_mobility.run.SharingMode;
import org.matsim.contrib.shared_mobility.run.SharingModes;
import org.matsim.contrib.shared_mobility.run.SharingServiceConfigGroup;
import org.matsim.contrib.shared_mobility.service.SharingUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.modal.AbstractModalModule;

import com.google.inject.Inject;
/**
 * @author steffenaxer
 */
public class SharingAnalysisModule extends AbstractModule {

	@Inject
	SharingConfigGroup sharingConfig;

	@Override
	public void install() {
		bind(SharingLegCollectorImpl.class).asEagerSingleton();
		bind(SharingLegCollector.class).to(SharingLegCollectorImpl.class);
		addEventHandlerBinding().to(SharingLegCollectorImpl.class);
		addControlerListenerBinding().to(SharingLegCollectorImpl.class);
		addControlerListenerBinding().to(SharingStatisticsAnalyzer.class);

		// Install SharingVehicleStatusTimeProfileCollectorProvider for each service
		for (SharingServiceConfigGroup serviceConfig : sharingConfig.getServices()) {
			install(new AbstractModalModule<SharingMode>(SharingUtils.getServiceMode(serviceConfig), SharingModes::mode) {
				@Override
				public void install() {
					bindModal(SharingVehicleStatusTimeProfileCollectorProvider.class).toProvider(modalProvider(getter -> {
						MatsimServices matsimServices = getter.get(MatsimServices.class);
						VehicleStateCollector vehicleStateCollector = getter.getModal(VehicleStateCollector.class);
						return new SharingVehicleStatusTimeProfileCollectorProvider(vehicleStateCollector, matsimServices, SharingUtils.getServiceMode(serviceConfig));
					}));
					addMobsimListenerBinding().toProvider(modalKey(SharingVehicleStatusTimeProfileCollectorProvider.class));
				}
			});
		}
	}
}
