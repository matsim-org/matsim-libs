package org.matsim.contrib.carsharing.relocation.controler;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.carsharing.config.CarsharingConfigGroup;
import org.matsim.contrib.carsharing.config.CarsharingVehicleRelocationConfigGroup;
import org.matsim.contrib.carsharing.control.listeners.CarsharingListener;
import org.matsim.contrib.carsharing.events.handlers.PersonArrivalDepartureHandler;
import org.matsim.contrib.carsharing.manager.CarsharingManagerInterface;
import org.matsim.contrib.carsharing.manager.CarsharingManagerNew;
import org.matsim.contrib.carsharing.manager.demand.CurrentTotalDemand;
import org.matsim.contrib.carsharing.manager.demand.CurrentTotalDemandImpl;
import org.matsim.contrib.carsharing.manager.demand.DemandHandler;
import org.matsim.contrib.carsharing.manager.demand.VehicleChoiceAgent;
import org.matsim.contrib.carsharing.manager.demand.VehicleChoiceAgentImpl;
import org.matsim.contrib.carsharing.manager.demand.membership.MembershipContainer;
import org.matsim.contrib.carsharing.manager.demand.membership.MembershipReader;
import org.matsim.contrib.carsharing.manager.routers.RouteCarsharingTrip;
import org.matsim.contrib.carsharing.manager.routers.RouteCarsharingTripImpl;
import org.matsim.contrib.carsharing.manager.routers.RouterProvider;
import org.matsim.contrib.carsharing.manager.routers.RouterProviderImpl;
import org.matsim.contrib.carsharing.manager.supply.CarsharingSupplyContainer;
import org.matsim.contrib.carsharing.manager.supply.CarsharingSupplyInterface;
import org.matsim.contrib.carsharing.manager.supply.costs.CostsCalculatorContainer;
import org.matsim.contrib.carsharing.models.ChooseTheCompany;
import org.matsim.contrib.carsharing.models.ChooseVehicleType;
import org.matsim.contrib.carsharing.models.ChooseVehicleTypeExample;
import org.matsim.contrib.carsharing.models.KeepingTheCarModel;
import org.matsim.contrib.carsharing.models.KeepingTheCarModelExample;
import org.matsim.contrib.carsharing.readers.CarsharingXmlReaderNew;
import org.matsim.contrib.carsharing.relocation.demand.CarsharingVehicleRelocationContainer;
import org.matsim.contrib.carsharing.relocation.events.MobismBeforeSimStepRelocationListener;
import org.matsim.contrib.carsharing.relocation.events.SetupListener;
import org.matsim.contrib.carsharing.relocation.events.handlers.DemandDistributionHandler;
import org.matsim.contrib.carsharing.relocation.infrastructure.AverageDemandRelocationListener;
import org.matsim.contrib.carsharing.relocation.listeners.CarSharingDemandTracker;
import org.matsim.contrib.carsharing.relocation.listeners.KmlWriterListener;
import org.matsim.contrib.carsharing.relocation.replanning.RelocationPlanStrategyModule;
import org.matsim.contrib.carsharing.relocation.utils.ChooseTheCompanyPriceBased;
import org.matsim.contrib.carsharing.relocation.utils.FFVehiclesRentalsWriterListener;
import org.matsim.contrib.carsharing.replanning.CarsharingSubtourModeChoiceStrategy;
import org.matsim.contrib.carsharing.replanning.RandomTripToCarsharingStrategy;
import org.matsim.contrib.carsharing.router.CarsharingRoute;
import org.matsim.contrib.carsharing.router.CarsharingRouteFactory;
import org.matsim.contrib.carsharing.runExample.CarsharingUtils;
import org.matsim.contrib.carsharing.scoring.CarsharingScoringFunctionFactory;
import org.matsim.contrib.dvrp.router.DvrpRoutingNetworkProvider;
import org.matsim.contrib.dvrp.trafficmonitoring.DvrpTravelTimeModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;

import com.google.inject.Key;
import com.google.inject.name.Names;

public class RelocationExample {
	/*public static void main(final String[] args) {
		Logger.getLogger("org.matsim.core.controler.Injector").setLevel(Level.OFF);

		final Config config = ConfigUtils.loadConfig(args[0]);

		// config.global().setRandomSeed(Integer.parseInt(args[1]));
		// config.controler().setOutputDirectory(config.controler().getOutputDirectory()
		// + "_" + args[1]);
		CarsharingUtils.addConfigModules(config);

		CarsharingVehicleRelocationConfigGroup carsharingVehicleRelocationConfigGroup = new CarsharingVehicleRelocationConfigGroup();
		config.addModule(carsharingVehicleRelocationConfigGroup);

		final Scenario sc = ScenarioUtils.loadScenario(config);
		sc.getPopulation().getFactory().getRouteFactories().setRouteFactory(CarsharingRoute.class,
				new CarsharingRouteFactory());
		final Controler controler = new Controler(sc);

		installCarSharing(controler);
		controler.getConfig().controler()
				.setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
		controler.run();
	}

	public static void installCarSharing(final Controler controler) {
		final Scenario scenario = controler.getScenario();
		TransportModeNetworkFilter filter = new TransportModeNetworkFilter(scenario.getNetwork());
		Set<String> modes = new HashSet<>();
		modes.add("car");
		Network networkFF = NetworkUtils.createNetwork();
		filter.filter(networkFF, modes);
		CarsharingXmlReaderNew reader = new CarsharingXmlReaderNew(networkFF);

		final CarsharingConfigGroup configGroup = (CarsharingConfigGroup) scenario.getConfig()
				.getModule(CarsharingConfigGroup.GROUP_NAME);

		// this is necessary to populate the companies list
		reader.readFile(configGroup.getvehiclelocations());
		Set<String> carsharingCompanies = reader.getCompanies().keySet();

		MembershipReader membershipReader = new MembershipReader();
		membershipReader.readFile(configGroup.getmembership());
		final MembershipContainer memberships = membershipReader.getMembershipContainer();

		final CostsCalculatorContainer costsCalculatorContainer = CarsharingUtils
				.createCompanyCostsStructure(carsharingCompanies);

		final CarsharingVehicleRelocationContainer carsharingVehicleRelocation = new CarsharingVehicleRelocationContainer(
				scenario, networkFF);

		final SetupListener setupListener = new SetupListener();
		final CarSharingDemandTracker demandTracker = new CarSharingDemandTracker();
		final CarsharingListener carsharingListener = new CarsharingListener();
		final KmlWriterListener relocationListener = new KmlWriterListener(configGroup.getStatsWriterFrequency());
		final FFVehiclesRentalsWriterListener vehicleRentalsWriterListener = new FFVehiclesRentalsWriterListener(
				configGroup.getStatsWriterFrequency());
		final KeepingTheCarModel keepingCarModel = new KeepingTheCarModelExample();
		final ChooseTheCompany chooseCompany = new ChooseTheCompanyPriceBased();
		final ChooseVehicleType chooseCehicleType = new ChooseVehicleTypeExample();
		final RouterProvider routerProvider = new RouterProviderImpl();
		final CurrentTotalDemand currentTotalDemand = new CurrentTotalDemandImpl(controler.getScenario().getNetwork());
		final CarsharingManagerInterface carsharingManager = new CarsharingManagerNew();
		final RouteCarsharingTrip routeCarsharingTrip = new RouteCarsharingTripImpl();
		final AverageDemandRelocationListener averageDemandRelocationListener = new AverageDemandRelocationListener();
		final VehicleChoiceAgent vehicleChoiceAgent = new VehicleChoiceAgentImpl();
		controler.addOverridingModule(new RelocationQSimModule());
		controler.addOverridingModule(new DvrpTravelTimeModule());
		// ===adding carsharing objects on supply and demand infrastructure ===
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bind(KeepingTheCarModel.class).toInstance(keepingCarModel);
				bind(ChooseTheCompany.class).toInstance(chooseCompany);
				bind(ChooseVehicleType.class).toInstance(chooseCehicleType);
				bind(RouterProvider.class).toInstance(routerProvider);
				bind(CurrentTotalDemand.class).toInstance(currentTotalDemand);
				bind(RouteCarsharingTrip.class).toInstance(routeCarsharingTrip);
				bind(CostsCalculatorContainer.class).toInstance(costsCalculatorContainer);
				bind(MembershipContainer.class).toInstance(memberships);
				bind(CarsharingSupplyInterface.class).to(CarsharingSupplyContainer.class);
				bind(CarsharingManagerInterface.class).toInstance(carsharingManager);
				bind(CarsharingVehicleRelocationContainer.class).toInstance(carsharingVehicleRelocation);
				bind(CarSharingDemandTracker.class).toInstance(demandTracker);
				bind(DemandHandler.class).asEagerSingleton();
				bind(DemandDistributionHandler.class).asEagerSingleton();
				bind(VehicleChoiceAgent.class).toInstance(vehicleChoiceAgent);
				bind(MobismBeforeSimStepRelocationListener.class).asEagerSingleton();
				bind(Network.class).annotatedWith(Names.named("carnetwork")).toInstance(networkFF);

				bind(Network.class).annotatedWith(Names.named(DvrpRoutingNetworkProvider.DVRP_ROUTING)).to(Network.class);
				bind(TravelTime.class).annotatedWith(Names.named("ff"))
						.to(Key.get(TravelTime.class, Names.named(DvrpTravelTimeModule.DVRP_ESTIMATED)));

			}
		});

		// === carsharing specific replanning strategies ===
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				this.addPlanStrategyBinding("RandomTripToCarsharingStrategy").to(RandomTripToCarsharingStrategy.class);
				this.addPlanStrategyBinding("CarsharingSubtourModeChoiceStrategy")
						.to(CarsharingSubtourModeChoiceStrategy.class);
				this.addPlanStrategyBinding("RelocationPlanStrategy").to(RelocationPlanStrategyModule.class);
			}
		});

		// === adding qsimfactory, controller listeners and event handlers
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				// bindMobsim().toProvider(RelocationQsimFactory.class);
				// bindMobsim().toProvider(CarsharingQsimFactoryNew.class);
				// this is now the only MobsimListenerBinding. Should it be
				// handled differently?
				// addMobsimListenerBinding().to(MobismBeforeSimStepRelocationListener.class);
				addControlerListenerBinding().toInstance(carsharingListener);
				addControlerListenerBinding().toInstance(relocationListener);
				addControlerListenerBinding().toInstance(setupListener);
				addControlerListenerBinding().toInstance(demandTracker);
				addControlerListenerBinding().toInstance(vehicleRentalsWriterListener);
				addControlerListenerBinding().toInstance((CarsharingManagerNew) carsharingManager);
				addControlerListenerBinding().toInstance(averageDemandRelocationListener);
				bindScoringFunctionFactory().to(CarsharingScoringFunctionFactory.class);
				addEventHandlerBinding().to(PersonArrivalDepartureHandler.class);
				addEventHandlerBinding().to(DemandHandler.class);
				addEventHandlerBinding().to(DemandDistributionHandler.class);
				addEventHandlerBinding().toInstance(averageDemandRelocationListener);
			}
		});

		// === routing moduels for carsharing trips ===
		controler.addOverridingModule(CarsharingUtils.createRoutingModule());
	}*/
}
