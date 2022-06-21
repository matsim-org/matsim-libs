
package org.matsim.contrib.shared_mobility.run;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.shared_mobility.analysis.SharingLegCollectorImpl;
import org.matsim.contrib.shared_mobility.analysis.VehicleStateCollector;
import org.matsim.contrib.shared_mobility.analysis.VehicleStateCollectorImpl;
import org.matsim.contrib.shared_mobility.io.DefaultSharingServiceSpecification;
import org.matsim.contrib.shared_mobility.io.SharingServiceReader;
import org.matsim.contrib.shared_mobility.io.SharingServiceSpecification;
import org.matsim.contrib.shared_mobility.io.validation.FreefloatingServiceValidator;
import org.matsim.contrib.shared_mobility.io.validation.SharingServiceValidator;
import org.matsim.contrib.shared_mobility.io.validation.StationBasedServiceValidator;
import org.matsim.contrib.shared_mobility.routing.FreefloatingInteractionFinder;
import org.matsim.contrib.shared_mobility.routing.InteractionFinder;
import org.matsim.contrib.shared_mobility.routing.SharingRoutingModule;
import org.matsim.contrib.shared_mobility.routing.StationBasedInteractionFinder;
import org.matsim.contrib.shared_mobility.service.SharingNetworkRentalsHandler;
import org.matsim.contrib.shared_mobility.service.SharingService;
import org.matsim.contrib.shared_mobility.service.SharingTeleportedRentalsHandler;
import org.matsim.contrib.shared_mobility.service.SharingUtils;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.modal.AbstractModalModule;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.utils.timing.TimeInterpretation;

import com.google.inject.Singleton;

public class SharingServiceModule extends AbstractModalModule<SharingMode> {
	private final SharingServiceConfigGroup serviceConfig;

	public SharingServiceModule(SharingServiceConfigGroup serviceConfig) {
		super(SharingUtils.getServiceMode(serviceConfig), SharingModes::mode);
		this.serviceConfig = serviceConfig;
	}

	@Override
	public void install() {
		SharingModes.registerSharingMode(binder(), getMode());

		installQSimModule(new SharingQSimServiceModule(serviceConfig));

		bindModal(SharingServiceSpecification.class).toProvider(modalProvider(getter -> {
			SharingServiceSpecification specification = new DefaultSharingServiceSpecification();
			new SharingServiceReader(specification).readURL(
					ConfigGroup.getInputFileURL(getConfig().getContext(), serviceConfig.getServiceInputFile()));
			return specification;
		})).in(Singleton.class);

		bindModal(SharingRoutingModule.class).toProvider(modalProvider(getter -> {
			Scenario scenario = getter.get(Scenario.class);
			RoutingModule accessEgressRoutingModule = getter.getNamed(RoutingModule.class, TransportMode.walk);
			RoutingModule mainModeRoutingModule = getter.getNamed(RoutingModule.class, serviceConfig.getMode());

			InteractionFinder interactionFinder = getter.getModal(InteractionFinder.class);
			TimeInterpretation timeInterpretation = getter.get(TimeInterpretation.class);

			return new SharingRoutingModule(scenario, accessEgressRoutingModule, mainModeRoutingModule,
					interactionFinder, Id.create(serviceConfig.getId(), SharingService.class), timeInterpretation);
		}));

		addRoutingModuleBinding(getMode()).to(modalKey(SharingRoutingModule.class));

		bindModal(FreefloatingInteractionFinder.class).toProvider(modalProvider(getter -> {
			Network network = getter.get(Network.class);
			return new FreefloatingInteractionFinder(network);
		})).in(Singleton.class);

		bindModal(StationBasedInteractionFinder.class).toProvider(modalProvider(getter -> {
			Network network = getter.get(Network.class);
			SharingServiceSpecification specification = getter.getModal(SharingServiceSpecification.class);

			return new StationBasedInteractionFinder(network, specification,
					serviceConfig.getMaximumAccessEgressDistance());
		}));

		bindModal(OutputWriter.class).toProvider(modalProvider(getter -> {
			SharingServiceSpecification specification = getter.getModal(SharingServiceSpecification.class);
			OutputDirectoryHierarchy outputHierarchy = getter.get(OutputDirectoryHierarchy.class);

			return new OutputWriter(Id.create(serviceConfig.getId(), SharingService.class), specification,
					outputHierarchy);
		})).in(Singleton.class);

		addControlerListenerBinding().to(modalKey(OutputWriter.class));

		bindModal(FreefloatingServiceValidator.class).toProvider(modalProvider(getter -> {
			return new FreefloatingServiceValidator(Id.create(serviceConfig.getId(), SharingService.class));
		})).in(Singleton.class);

		bindModal(StationBasedServiceValidator.class).toProvider(modalProvider(getter -> {
			return new StationBasedServiceValidator(Id.create(serviceConfig.getId(), SharingService.class));
		})).in(Singleton.class);

		bindModal(ValidationListener.class).toProvider(modalProvider(getter -> {
			SharingServiceSpecification specification = getter.getModal(SharingServiceSpecification.class);
			SharingServiceValidator validator = getter.getModal(SharingServiceValidator.class);

			return new ValidationListener(Id.create(serviceConfig.getId(), SharingService.class), validator,
					specification);
		}));

		addControlerListenerBinding().to(modalKey(ValidationListener.class));

		bindModal(VehicleStateCollector.class).toProvider(modalProvider(getter -> {
			SharingServiceSpecification specification = getter.getModal(SharingServiceSpecification.class);
			return new VehicleStateCollectorImpl(specification);
		})).in(Singleton.class);

		addEventHandlerBinding().to(modalKey(VehicleStateCollector.class));
		addControlerListenerBinding().to(modalKey(VehicleStateCollector.class));


		// based on the underlying mode and how it is simulated
		// teleported/network we need to bind different rental handler

		if (((QSimConfigGroup) getConfig().getModules().get(QSimConfigGroup.GROUP_NAME)).getMainModes()
				.contains(serviceConfig.getMode())) {
			addEventHandlerBinding().toProvider(modalProvider(getter -> {
				EventsManager eventsManager = getter.get(EventsManager.class);
				Network network = getter.get(Network.class);
				return new SharingNetworkRentalsHandler(eventsManager, serviceConfig, network);
			}));

		} else {
			addEventHandlerBinding().toProvider(modalProvider(getter -> {
				EventsManager eventsManager = getter.get(EventsManager.class);
				return new SharingTeleportedRentalsHandler(eventsManager, serviceConfig);
			}));
		}

		switch (serviceConfig.getServiceScheme()) {
			case Freefloating:
				bindModal(InteractionFinder.class).to(modalKey(FreefloatingInteractionFinder.class));
				bindModal(SharingServiceValidator.class).to(modalKey(FreefloatingServiceValidator.class));
				break;
			case StationBased:
				bindModal(InteractionFinder.class).to(modalKey(StationBasedInteractionFinder.class));
				bindModal(SharingServiceValidator.class).to(modalKey(StationBasedServiceValidator.class));
				break;
			default:
				throw new IllegalStateException();
		}
	}
}
