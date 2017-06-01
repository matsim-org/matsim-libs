package playground.balac.contribs.carsharing;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.carsharing.config.CarsharingConfigGroup;
import org.matsim.contrib.carsharing.control.listeners.CarsharingListener;
import org.matsim.contrib.carsharing.events.handlers.PersonArrivalDepartureHandler;
import org.matsim.contrib.carsharing.manager.CarsharingManagerInterface;
import org.matsim.contrib.carsharing.manager.CarsharingManagerNew;
import org.matsim.contrib.carsharing.manager.demand.CurrentTotalDemandImpl;
import org.matsim.contrib.carsharing.manager.demand.DemandHandler;
import org.matsim.contrib.carsharing.manager.demand.membership.MembershipContainer;
import org.matsim.contrib.carsharing.manager.demand.membership.MembershipReader;
import org.matsim.contrib.carsharing.manager.routers.RouteCarsharingTrip;
import org.matsim.contrib.carsharing.manager.routers.RouteCarsharingTripImpl;
import org.matsim.contrib.carsharing.manager.routers.RouterProvider;
import org.matsim.contrib.carsharing.manager.routers.RouterProviderImpl;
import org.matsim.contrib.carsharing.manager.supply.CarsharingSupplyContainer;
import org.matsim.contrib.carsharing.manager.supply.CarsharingSupplyInterface;
import org.matsim.contrib.carsharing.manager.supply.costs.CompanyCosts;
import org.matsim.contrib.carsharing.manager.supply.costs.CostCalculation;
import org.matsim.contrib.carsharing.manager.supply.costs.CostsCalculatorContainer;
import org.matsim.contrib.carsharing.models.ChooseTheCompany;
import org.matsim.contrib.carsharing.models.ChooseVehicleType;
import org.matsim.contrib.carsharing.models.KeepingTheCarModel;
import org.matsim.contrib.carsharing.qsim.CarsharingQsimFactoryNew;
import org.matsim.contrib.carsharing.readers.CarsharingXmlReaderNew;
import org.matsim.contrib.carsharing.replanning.CarsharingSubtourModeChoiceStrategy;
import org.matsim.contrib.carsharing.replanning.RandomTripToCarsharingStrategy;
import org.matsim.contrib.carsharing.runExample.CarsharingUtils;
import org.matsim.contrib.carsharing.runExample.ComputationTime;
import org.matsim.contrib.carsharing.scoring.CarsharingScoringFunctionFactory;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.config.groups.GlobalConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

import playground.balac.contribs.carsharing.coststructures.CostStructure1;
import playground.balac.contribs.carsharing.coststructures.CostStructure2;
import playground.balac.contribs.carsharing.coststructures.CostStructureTwoWay;
import playground.balac.contribs.carsharing.models.ChooseTheCompanyPriceBased;
import playground.balac.contribs.carsharing.models.KeepTheVehicleModel;
import playground.balac.contribs.carsharing.models.VehicleTypeChoice;

public class RunCarsharing {

	public static void main(String[] args) {

		Logger.getLogger( "org.matsim.core.controler.Injector" ).setLevel(Level.OFF);

		Config config = ConfigUtils.loadConfig(args[0]);
		
		((ControlerConfigGroup)config.getModules().get("controler")).
		setOutputDirectory(((ControlerConfigGroup)config.getModules().get("controler")).getOutputDirectory() 
				 + "_" + args[1] + "_" + args[2]);		
		
		((GlobalConfigGroup)config.getModules().get("global")).setRandomSeed(Long.parseLong(args[1]));
		for (StrategySettings s :((StrategyConfigGroup)config.getModules().get("strategy")).getStrategySettings()) {
			if (s.getStrategyName().equals("CarsharingSubtourModeChoiceStrategy"))
					s.setWeight(Double.parseDouble(args[2]));
		}		
		
		CarsharingUtils.addConfigModules(config);

		final Scenario sc = ScenarioUtils.loadScenario(config);

		final Controler controler = new Controler( sc );
		int i = 0;
		for (Person person : sc.getPopulation().getPersons().values()) {
			Boolean b = false;
			if (i % 150 == 0)
				b = true;
			person.getAttributes().putAttribute("bulky", b);
			i++;
		}
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
		
		final CostsCalculatorContainer costsCalculatorContainer = createCompanyCostsStructure(carsharingCompanies);
		
		final CarsharingListener carsharingListener = new CarsharingListener();
		final CarsharingSupplyInterface carsharingSupplyContainer = new CarsharingSupplyContainer(controler.getScenario());
		carsharingSupplyContainer.populateSupply();
		final KeepingTheCarModel keepingCarModel = new KeepTheVehicleModel();
		final ChooseTheCompany chooseCompany = new ChooseTheCompanyPriceBased();
		final ChooseVehicleType chooseCehicleType = new VehicleTypeChoice();
		final RouterProvider routerProvider = new RouterProviderImpl();
		final CurrentTotalDemandImpl currentTotalDemand = new CurrentTotalDemandImpl(controler.getScenario().getNetwork());
		final CarsharingManagerInterface carsharingManager = new CarsharingManagerNew();
		final RouteCarsharingTrip routeCarsharingTrip = new RouteCarsharingTripImpl();
		
		//===adding carsharing objects on supply and demand infrastructure ===
		
		controler.addOverridingModule(new AbstractModule() {

			@Override
			public void install() {
				bind(KeepingTheCarModel.class)
				.toInstance(keepingCarModel);
				bind(ChooseTheCompany.class).toInstance(chooseCompany);
				bind(ChooseVehicleType.class).toInstance(chooseCehicleType);
				bind(RouterProvider.class).toInstance(routerProvider);
				bind(CurrentTotalDemandImpl.class).toInstance(currentTotalDemand);
				bind(RouteCarsharingTrip.class).toInstance(routeCarsharingTrip);
				bind(CostsCalculatorContainer.class).toInstance(costsCalculatorContainer);
				bind(MembershipContainer.class).toInstance(memberships);
			    bind(CarsharingSupplyInterface.class).toInstance(carsharingSupplyContainer);
			    bind(CarsharingManagerInterface.class).toInstance(carsharingManager);
			    bind(DemandHandler.class).asEagerSingleton();
			    bind(ComputationTime.class).asEagerSingleton();

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
				bindScoringFunctionFactory().to(CarsharingScoringFunctionFactory.class);		      
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
	
 public static CostsCalculatorContainer createCompanyCostsStructure(Set<String> companies) {
		
		CostsCalculatorContainer companyCostsContainer = new CostsCalculatorContainer();
		
		for (String s : companies) {
			
			Map<String, CostCalculation> costCalculations = new HashMap<String, CostCalculation>();
			
			//=== here customizable cost structures come in ===
			//===what follows is just an example!! and should be modified according to the study at hand===
			if (s.equals("Mobility"))
				costCalculations.put("freefloating", new CostStructure1());		
			else {
				costCalculations.put("freefloating", new CostStructure1());
				costCalculations.put("twoway", new CostStructureTwoWay());

			}
			CompanyCosts companyCosts = new CompanyCosts(costCalculations);
			
			companyCostsContainer.getCompanyCostsMap().put(s, companyCosts);
		}
		
		return companyCostsContainer;
		
	}

}
