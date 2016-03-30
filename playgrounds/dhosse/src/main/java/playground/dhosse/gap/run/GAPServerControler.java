package playground.dhosse.gap.run;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Provider;

import jsprit.core.problem.cost.VehicleRoutingActivityCosts.Time;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.jfree.util.Log;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.carsharing.config.CarsharingConfigGroup;
import org.matsim.contrib.carsharing.config.FreeFloatingConfigGroup;
import org.matsim.contrib.carsharing.config.OneWayCarsharingConfigGroup;
import org.matsim.contrib.carsharing.config.TwoWayCarsharingConfigGroup;
import org.matsim.contrib.carsharing.control.listeners.CarsharingListener;
import org.matsim.contrib.carsharing.qsim.CarsharingQsimFactory;
import org.matsim.contrib.carsharing.replanning.RandomTripToCarsharingStrategy;
import org.matsim.contrib.carsharing.router.OneWayCarsharingRoutingModule;
import org.matsim.contrib.carsharing.scoring.CarsharingScoringFunctionFactory;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl.Builder;
import org.matsim.core.replanning.modules.ReRoute;
import org.matsim.core.replanning.modules.SubtourModeChoice;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule.DefaultStrategy;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.MainModeIdentifierImpl;
import org.matsim.core.router.NetworkRouting;
import org.matsim.core.router.TripRouter;
import org.matsim.core.scenario.ScenarioUtils;

import playground.dhosse.gap.Global;

import com.google.inject.Inject;

/**
 * 
 * @author dhosse
 *
 */
public class GAPServerControler {

	/**
	 * 
	 * @param args
	 */
	public static void main(String args[]) {
		
		Logger.getLogger( "org.matsim.core.controler.Injector" ).setLevel(Level.OFF);

		run(args[0]);

	}

	/**
	 * Runs the GP scenario.
	 */
	private static void run(String configfile) {

		// create a new config and a new scenario and load it
		final Config config = ConfigUtils.loadConfig(configfile,
				new CarsharingConfigGroup(), new OneWayCarsharingConfigGroup(),
				new TwoWayCarsharingConfigGroup(), new FreeFloatingConfigGroup());
		
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);
		
		for(ActivityParams pars : config.planCalcScore().getActivityParams()){
			pars.setMinimalDuration(Time.UNDEFINED);
		}
		
		config.plansCalcRoute().setInsertingAccessEgressWalk(false);

		final Scenario scenario = ScenarioUtils.loadScenario(config);
		
		config.plansCalcRoute().setInsertingAccessEgressWalk(true);

		mapPersonsToCarLinks(scenario);
		
		// create a new controler
		final Controler controler = new Controler(scenario);
		
		controler.addOverridingModule(new AbstractModule() {

			@Override
			public void install() {
				addTravelTimeBinding(TransportMode.ride).to(networkTravelTime());
				addTravelDisutilityFactoryBinding(TransportMode.ride).to(carTravelDisutilityFactoryKey());
				addRoutingModuleBinding(TransportMode.ride).toProvider(new NetworkRouting(TransportMode.ride));
				addEventHandlerBinding().toInstance(new ZugspitzbahnFareHandler(controler));

			}
		});
		
		addChangeExp(controler, 0.7);
		addRouteChoice(controler, 0.1);
		addModeChoice(controler, 0.1);

		addCarsharing(controler, 0.1);
		

		// start of the simulation
		controler.run();

	}

	private static void mapPersonsToCarLinks(final Scenario scenario) {
		
		Network subNetwork = NetworkUtils.createNetwork();
		TransportModeNetworkFilter filter = new TransportModeNetworkFilter(scenario.getNetwork());
		Set<String> modes = new HashSet<>();
		modes.add(TransportMode.car);
		filter.filter(subNetwork, modes);
		
		for(Node n: new HashSet<Node>(subNetwork.getNodes().values())){
			for(Link l: NetworkUtils.getIncidentLinks(n).values()){
				if(l.getFreespeed() > (60 / 3.6)){
					subNetwork.removeLink(l.getId()); //remove links with freespeed > 60kmh
				}
			}
			if(n.getInLinks().size() == 0 && n.getOutLinks().size() == 0){
				subNetwork.removeNode(n.getId()); //remove nodes without connection to links
			}
		}
		new NetworkCleaner().run(subNetwork);
		
		for(Person person : scenario.getPopulation().getPersons().values()){
			
			String subpopulation = (String) scenario.getPopulation().getPersonAttributes().getAttribute(person.getId().toString(), Global.USER_GROUP);
			if(subpopulation != null){
				
				if(subpopulation.equals("CAR_OWNER") || subpopulation.equalsIgnoreCase("LICENSE")){
				
					scenario.getPopulation().getPersonAttributes().putAttribute(person.getId().toString(), "OW_CARD", "true");
					
				}
				
			}
			
			for(PlanElement pe : person.getSelectedPlan().getPlanElements()){
				
				if(pe instanceof ActivityImpl){
					
					ActivityImpl act = (ActivityImpl)pe;
					Id<Link> linkId = act.getLinkId();
					linkId = NetworkUtils.getNearestLink(subNetwork, act.getCoord()).getId();
					act.setLinkId(linkId);
					
				}
				
			}
			
		}
	}
	
	private static void addChangeExp(final Controler controler, double weight){
		
		StrategySettings changeExp = new StrategySettings();
		changeExp.setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta.name());
		changeExp.setSubpopulation(null);
		changeExp.setWeight(weight);
		controler.getConfig().strategy().addStrategySettings(changeExp);
		
		StrategySettings changeExpCarUser = new StrategySettings();
		changeExpCarUser.setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta.name());
		changeExpCarUser.setSubpopulation(Global.GP_CAR);
		changeExpCarUser.setWeight(weight);
		controler.getConfig().strategy().addStrategySettings(changeExpCarUser);
		
		StrategySettings changeExpLicenseOwner = new StrategySettings();
		changeExpLicenseOwner.setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta.name());
		changeExpLicenseOwner.setSubpopulation(Global.LICENSE_OWNER);
		changeExpLicenseOwner.setWeight(weight);
		controler.getConfig().strategy().addStrategySettings(changeExpLicenseOwner);
		
		StrategySettings changeExpCommmuter = new StrategySettings();
		changeExpCommmuter.setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta.name());
		changeExpCommmuter.setSubpopulation(Global.COMMUTER);
		changeExpCommmuter.setWeight(weight);
		controler.getConfig().strategy().addStrategySettings(changeExpCommmuter);
		
	}
	
	private static void addRouteChoice(final Controler controler, double weight){
		
		StrategySettings reRoute = new StrategySettings();
		reRoute.setStrategyName(DefaultStrategy.ReRoute.name());
		reRoute.setSubpopulation(null);
		reRoute.setWeight(weight);
		controler.getConfig().strategy().addStrategySettings(reRoute);
		
		StrategySettings reRouteCarUser = new StrategySettings();
		reRouteCarUser.setStrategyName(DefaultStrategy.ReRoute.name());
		reRouteCarUser.setSubpopulation(Global.GP_CAR);
		reRouteCarUser.setWeight(weight);
		controler.getConfig().strategy().addStrategySettings(reRouteCarUser);
		
		StrategySettings reRouteLicenseOwner = new StrategySettings();
		reRouteLicenseOwner.setStrategyName(DefaultStrategy.ReRoute.name());
		reRouteLicenseOwner.setSubpopulation(Global.LICENSE_OWNER);
		reRouteLicenseOwner.setWeight(weight);
		controler.getConfig().strategy().addStrategySettings(reRouteLicenseOwner);
		
		StrategySettings reRouteCommmuter = new StrategySettings();
		reRouteCommmuter.setStrategyName(DefaultStrategy.ReRoute.name());
		reRouteCommmuter.setSubpopulation(Global.COMMUTER);
		reRouteCommmuter.setWeight(weight);
		controler.getConfig().strategy().addStrategySettings(reRouteCommmuter);
		
	}

	/**
	 * Adds subtour mode choice strategy settings to the controler. These
	 * strategies are configured for different types of subpopulations (persons
	 * with car and license, commuters, persons without license).
	 * 
	 * @param controler
	 */
	private static void addModeChoice(final Controler controler, double weight) {
		
		final String nameCarAvail = DefaultPlanStrategiesModule.DefaultStrategy.SubtourModeChoice.toString()
				.concat("_" + Global.GP_CAR);
		final String nameLicense = DefaultPlanStrategiesModule.DefaultStrategy.SubtourModeChoice.toString()
				.concat("_" + Global.LICENSE_OWNER);
		final String nameRest = DefaultPlanStrategiesModule.DefaultStrategy.SubtourModeChoice.toString()
				.concat("_NO_CAR");
		final String nameCommuter = DefaultPlanStrategiesModule.DefaultStrategy.SubtourModeChoice.toString()
				.concat("_" + Global.COMMUTER);

		StrategySettings carAvail = new StrategySettings();
		carAvail.setStrategyName(nameCarAvail);
		carAvail.setSubpopulation(Global.GP_CAR);
		carAvail.setWeight(weight);
		controler.getConfig().strategy().addStrategySettings(carAvail);

		StrategySettings license = new StrategySettings();
		license.setStrategyName(nameLicense);
		license.setSubpopulation(Global.LICENSE_OWNER);
		license.setWeight(weight);
		controler.getConfig().strategy().addStrategySettings(license);

		StrategySettings nonCarAvail = new StrategySettings();
		nonCarAvail.setStrategyName(nameRest);
		nonCarAvail.setSubpopulation(null);
		nonCarAvail.setWeight(weight);
		controler.getConfig().strategy().addStrategySettings(nonCarAvail);

		StrategySettings commuter = new StrategySettings();
		commuter.setStrategyName(nameCommuter);
		commuter.setSubpopulation(Global.COMMUTER);
		commuter.setWeight(weight);
		controler.getConfig().strategy().addStrategySettings(commuter);

		controler.addOverridingModule(new AbstractModule() {
			
			@Override
			public void install() {
				
				String[] modesCarAvail = new String[]{TransportMode.bike, TransportMode.car, TransportMode.pt,
						TransportMode.walk};
				String[] modesLicenseOwner = new String[]{TransportMode.bike, TransportMode.pt,
						TransportMode.walk};
				String[] modesRest = {TransportMode.bike, TransportMode.pt, TransportMode.walk};
				String[] modesCommuters = {TransportMode.car, TransportMode.pt};
				
				String[] chainBasedModesCar = {TransportMode.bike, TransportMode.car};
				String[] chainBasedModesNoCar = {TransportMode.bike};
				String[] chainBasedModesCommuters = {TransportMode.car};
				
				this.addPlanStrategyBinding(nameCarAvail).toProvider(new SubtourModeChoiceProvider(
						modesCarAvail, chainBasedModesCar));
				this.addPlanStrategyBinding(nameLicense).toProvider(new SubtourModeChoiceProvider(
						modesLicenseOwner, chainBasedModesNoCar));
				this.addPlanStrategyBinding(nameRest).toProvider(new SubtourModeChoiceProvider(
						modesRest, chainBasedModesNoCar));
				this.addPlanStrategyBinding(nameCommuter).toProvider(new SubtourModeChoiceProvider(
						modesCommuters, chainBasedModesCommuters));
				
			}
			
		});
		
	}

	/**
	 * adds carsharing to the scenario
	 * 
	 * @param controler
	 * @param weightForCsStrategies
	 */
	private static void addCarsharing(final Controler controler, double weightForCsStrategies){
		
		// add carsharing to the main (congested) modes
		String[] mainModes = new String[]{"car", "onewaycarsharing"};
		controler.getConfig().qsim().setMainModes(Arrays.asList(mainModes));
		
		//add carsharing strategy settings
		StrategySettings rttcsCar = new StrategySettings();
		rttcsCar.setStrategyName("RandomTripToCarsharingStrategy");
		rttcsCar.setSubpopulation(Global.GP_CAR);
		rttcsCar.setWeight(weightForCsStrategies);
		controler.getConfig().strategy().addStrategySettings(rttcsCar);
		
		StrategySettings rttcsLicense = new StrategySettings();
		rttcsLicense.setStrategyName("RandomTripToCarsharingStrategy");
		rttcsLicense.setSubpopulation(Global.LICENSE_OWNER);
		rttcsLicense.setWeight(weightForCsStrategies);
		controler.getConfig().strategy().addStrategySettings(rttcsLicense);
		
//		StrategySettings csmcCar = new StrategySettings();
//		csmcCar.setStrategyName("CarsharingSubtourModeChoiceStrategy");
//		csmcCar.setSubpopulation(Global.GP_CAR);
//		csmcCar.setWeight(weightForCsStrategies / 2);
//		controler.getConfig().strategy().addStrategySettings(csmcCar);
//		
//		StrategySettings csmcLicense = new StrategySettings();
//		csmcLicense.setStrategyName("CarsharingSubtourModeChoiceStrategy");
//		csmcLicense.setSubpopulation(Global.LICENSE_OWNER);
//		csmcLicense.setWeight(weightForCsStrategies / 2);
//		controler.getConfig().strategy().addStrategySettings(csmcLicense);
		
		controler.addOverridingModule(new AbstractModule() {
			
			@Override
			public void install() {
				
				bindMobsim().toProvider( CarsharingQsimFactory.class );
				bindScoringFunctionFactory().to(CarsharingScoringFunctionFactory.class);
				
				this.addPlanStrategyBinding("RandomTripToCarsharingStrategy").to( 
						RandomTripToCarsharingStrategy.class ) ;
//				this.addPlanStrategyBinding("CarsharingSubtourModeChoiceStrategy").to( 
//						CarsharingSubtourModeChoiceStrategy.class ) ;

				addRoutingModuleBinding("onewaycarsharing").toInstance(new OneWayCarsharingRoutingModule());

				bind(MainModeIdentifier.class).toInstance(new MainModeIdentifier() {
                    final MainModeIdentifier defaultModeIdentifier = new MainModeIdentifierImpl();

                    @Override
                    public String identifyMainMode(
                            final List<? extends PlanElement> tripElements) {
                        for ( PlanElement pe : tripElements ) {
                            if ( pe instanceof Leg && ((Leg) pe).getMode().equals( "twowaycarsharing" ) ) {
                                return "twowaycarsharing";
                            }
                            else if ( pe instanceof Leg && ((Leg) pe).getMode().equals( "onewaycarsharing" ) ) {
                                return "onewaycarsharing";
                            }
                            else if ( pe instanceof Leg && ((Leg) pe).getMode().equals( "freefloating" ) ) {
                                return "freefloating";
                            }
                        }
                        return defaultModeIdentifier.identifyMainMode( tripElements );
                    }
                    
                });
				
			}
			
		});
		
		CarsharingConfigGroup cs = (CarsharingConfigGroup) controler.getConfig().getModule("Carsharing");
		controler.addControlerListener(new CarsharingListener(controler,
				cs.getStatsWriterFrequency() ) ) ;
		
	}

}

/**
 * @author benjamin
 *
 */
final class SubtourModeChoiceProvider implements javax.inject.Provider<PlanStrategy> {
	@Inject Scenario scenario;
	@Inject Provider<TripRouter> tripRouterProvider;
	String[] availableModes;
	String[] chainBasedModes;

	public SubtourModeChoiceProvider(String[] availableModes, String[] chainBasedModes) {
	super();
		this.availableModes = availableModes;
		this.chainBasedModes = chainBasedModes;
	}

	@Override
	public PlanStrategy get() {
		Log.info("Available modes are " + availableModes);
		Log.info("Chain-based modes are " + chainBasedModes);
		final Builder builder = new Builder(new RandomPlanSelector<Plan, Person>());
		builder.addStrategyModule(new SubtourModeChoice(scenario.getConfig().global().getNumberOfThreads(), availableModes, chainBasedModes, false, tripRouterProvider));
		builder.addStrategyModule(new ReRoute(scenario, tripRouterProvider));
		return builder.build();
	}
}