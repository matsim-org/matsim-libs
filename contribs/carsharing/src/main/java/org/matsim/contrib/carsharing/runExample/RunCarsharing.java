package org.matsim.contrib.carsharing.runExample;


import java.util.Set;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.carsharing.config.CarsharingConfigGroup;
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
import org.matsim.contrib.carsharing.models.ChooseTheCompanyExample;
import org.matsim.contrib.carsharing.models.ChooseVehicleType;
import org.matsim.contrib.carsharing.models.ChooseVehicleTypeExample;
import org.matsim.contrib.carsharing.models.KeepingTheCarModel;
import org.matsim.contrib.carsharing.models.KeepingTheCarModelExample;
import org.matsim.contrib.carsharing.qsim.CarsharingQsimFactoryNew;
import org.matsim.contrib.carsharing.readers.CarsharingXmlReaderNew;
import org.matsim.contrib.carsharing.replanning.CarsharingSubtourModeChoiceStrategy;
import org.matsim.contrib.carsharing.replanning.RandomTripToCarsharingStrategy;
import org.matsim.contrib.carsharing.scoring.CarsharingScoringFunctionFactory;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;

import com.google.inject.Provides;
import com.google.inject.Singleton;


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
		
		final Scenario scenario = controler.getScenario();
		CarsharingXmlReaderNew reader = new CarsharingXmlReaderNew(scenario.getNetwork());
		
		final CarsharingConfigGroup configGroup = (CarsharingConfigGroup)
				scenario.getConfig().getModule( CarsharingConfigGroup.GROUP_NAME );

		reader.readFile(configGroup.getvehiclelocations());
		
		Set<String> carsharingCompanies = reader.getCompanies().keySet();
		
		MembershipReader membershipReader = new MembershipReader();
		
		membershipReader.readFile(configGroup.getmembership());

		final MembershipContainer memberships = membershipReader.getMembershipContainer();
		
		final CostsCalculatorContainer costsCalculatorContainer = CarsharingUtils.createCompanyCostsStructure(carsharingCompanies);
		
		final CarsharingListener carsharingListener = new CarsharingListener();
		//final CarsharingSupplyInterface carsharingSupplyContainer = new CarsharingSupplyContainer(controler.getScenario());
		
		final KeepingTheCarModel keepingCarModel = new KeepingTheCarModelExample();
		final ChooseTheCompany chooseCompany = new ChooseTheCompanyExample();
		final ChooseVehicleType chooseCehicleType = new ChooseVehicleTypeExample();
		final RouterProvider routerProvider = new RouterProviderImpl();
		final CurrentTotalDemand currentTotalDemand = new CurrentTotalDemandImpl(controler.getScenario().getNetwork());
		//final CarsharingManagerInterface carsharingManager = new CarsharingManagerNew();
		final RouteCarsharingTrip routeCarsharingTrip = new RouteCarsharingTripImpl();
		final VehicleChoiceAgent vehicleChoiceAgent = new VehicleChoiceAgentImpl();
		//===adding carsharing objects on supply and demand infrastructure ===
		
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
			    bind(CarsharingManagerInterface.class).to(CarsharingManagerNew.class);
			    bind(VehicleChoiceAgent.class).toInstance(vehicleChoiceAgent);
			    bind(DemandHandler.class).asEagerSingleton();
			}			
			@Provides @Singleton
			CarsharingSupplyContainer provideCarsharingSupplyContainer(Scenario scenario) {
			    return new CarsharingSupplyContainer(scenario);
			}
			
			
		
		});		
		
		//=== carsharing specific replanning strategies ===
		
		controler.addOverridingModule( new AbstractModule() {
			@Override
			public void install() {
				this.addPlanStrategyBinding("RandomTripToCarsharingStrategy").to( RandomTripToCarsharingStrategy.class ) ;
				this.addPlanStrategyBinding("CarsharingSubtourModeChoiceStrategy").to( CarsharingSubtourModeChoiceStrategy.class ) ;
			}
		});
		
		//=== adding qsimfactory, controller listeners and event handlers
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bindMobsim().toProvider(CarsharingQsimFactoryNew.class);
		        addControlerListenerBinding().toInstance(carsharingListener);
		        addControlerListenerBinding().to(CarsharingManagerNew.class);		        
				//bindScoringFunctionFactory().to(CarsharingScoringFunctionFactory.class);		      
		        addEventHandlerBinding().to(PersonArrivalDepartureHandler.class);
		        addEventHandlerBinding().to(DemandHandler.class);
			}
		});
		//=== adding carsharing specific scoring factory ===
		controler.addOverridingModule(new AbstractModule() {
			
			@Override
			public void install() {
				        
				bindScoringFunctionFactory().to(CarsharingScoringFunctionFactory.class);	
			}
		});

		//=== routing moduels for carsharing trips ===

		controler.addOverridingModule(CarsharingUtils.createRoutingModule());			
	}

}
