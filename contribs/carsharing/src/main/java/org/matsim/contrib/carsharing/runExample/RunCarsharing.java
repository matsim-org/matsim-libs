package org.matsim.contrib.carsharing.runExample;


import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.carsharing.control.listeners.CarsharingListener;
import org.matsim.contrib.carsharing.control.listeners.CarsharingMobsimListener;
import org.matsim.contrib.carsharing.events.handlers.PersonArrivalDepartureHandler;
import org.matsim.contrib.carsharing.manager.CSPersonVehicle;
import org.matsim.contrib.carsharing.manager.CSPersonVehiclesContainer;
import org.matsim.contrib.carsharing.manager.CarsharingManager;
import org.matsim.contrib.carsharing.manager.routers.RouteFreefloatingTrip;
import org.matsim.contrib.carsharing.manager.routers.RouteOneWayTrip;
import org.matsim.contrib.carsharing.manager.routers.RouterProvider;
import org.matsim.contrib.carsharing.manager.routers.RouterProviderImpl;
import org.matsim.contrib.carsharing.models.KeepingTheCarModel;
import org.matsim.contrib.carsharing.models.KeepingTheCarModelExample;
import org.matsim.contrib.carsharing.qsim.CarSharingVehiclesNew;
import org.matsim.contrib.carsharing.qsim.CarsharingQsimFactoryNew;
import org.matsim.contrib.carsharing.replanning.CarsharingSubtourModeChoiceStrategy;
import org.matsim.contrib.carsharing.replanning.RandomTripToCarsharingStrategy;
import org.matsim.contrib.carsharing.scoring.CarsharingScoringFunctionFactory;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.mobsim.framework.listeners.FixedOrderSimulationListener;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.withinday.mobsim.MobsimDataProvider;
import org.matsim.withinday.replanning.identifiers.tools.ActivityReplanningMap;

/** 
 * @author balac
 */

public class RunCarsharing {

	public static void main(String[] args) {
		Logger.getLogger( "org.matsim.core.controler.Injector" ).setLevel(Level.OFF);

		final Config config = ConfigUtils.loadConfig(args[0]);
		
		if(Integer.parseInt(config.getModule("qsim").getValue("numberOfThreads")) > 1)
			Logger.getLogger( "org.matsim.core.controler" ).warn("Carsharing contrib is not stable for parallel qsim!! If the error occures please use 1 as the number of threads.");
		
		CarsharingUtils.addConfigModules(config);

		final Scenario sc = ScenarioUtils.loadScenario(config);

		final Controler controler = new Controler( sc );
		
		installCarSharing(controler);
		
		controler.run();


	}

	public static void installCarSharing(final Controler controler) {
		
		
		final CarSharingVehiclesNew carsharingVehcilesData = new CarSharingVehiclesNew(controler.getScenario());
		carsharingVehcilesData.readVehicleLocations();
		final KeepingTheCarModel keepingCarModel = new KeepingTheCarModelExample();
		final RouterProvider routerProvider = new RouterProviderImpl();
		final CSPersonVehicle pesronVehiclesContainer = new CSPersonVehiclesContainer();
		controler.addOverridingModule(new AbstractModule() {

			@Override
			public void install() {
				bind(KeepingTheCarModel.class)
				.toInstance(keepingCarModel);
				bind(RouterProvider.class).toInstance(routerProvider);
				bind(CSPersonVehicle.class).toInstance(pesronVehiclesContainer);
				bind(RouteFreefloatingTrip.class).asEagerSingleton();
				bind(RouteOneWayTrip.class).asEagerSingleton();

			}			
		});		
		
		//=== end of adding the model ===
		
		controler.addOverridingModule( new AbstractModule() {
			@Override
			public void install() {
				this.addPlanStrategyBinding("RandomTripToCarsharingStrategy").to( RandomTripToCarsharingStrategy.class ) ;
				this.addPlanStrategyBinding("CarsharingSubtourModeChoiceStrategy").to( CarsharingSubtourModeChoiceStrategy.class ) ;
			}
		});
		
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
		        bind(FixedOrderSimulationListener.class).asEagerSingleton();
				bindMobsim().toProvider(CarsharingQsimFactoryNew.class);
		        addMobsimListenerBinding().to(FixedOrderSimulationListener.class);
		        addControlerListenerBinding().to(CarsharingListener.class);
		        addControlerListenerBinding().to(CarsharingManager.class);
		        bind(MobsimDataProvider.class).asEagerSingleton();
		        
				//setting up the scoring function factory, inside different scoring functions are set-up
				bindScoringFunctionFactory().to(CarsharingScoringFunctionFactory.class);
		        bind(ActivityReplanningMap.class).asEagerSingleton();
		        addMobsimListenerBinding().to(CarsharingMobsimListener.class) ;
		        bind(CarSharingVehiclesNew.class).toInstance(carsharingVehcilesData);
		        bind(CarsharingManager.class).asEagerSingleton();
		        addEventHandlerBinding().to(PersonArrivalDepartureHandler.class);


			}
		});

		controler.addOverridingModule(CarsharingUtils.createModule());			
	}

}
