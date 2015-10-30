package playground.balac.allcsmodestest.controler;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.router.*;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.costcalculators.TravelTimeAndDistanceBasedTravelDisutilityFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;

import playground.balac.allcsmodestest.config.AllCSModesConfigGroup;
import playground.balac.allcsmodestest.controler.listeneronlymembers.CarsharingListener;
import playground.balac.allcsmodestest.qsim.AllCSModesQsimFactory;
import playground.balac.allcsmodestest.scoring.AllCSModesScoringFunctionFactory;
import playground.balac.analysis.TripsAnalyzer;
import playground.balac.freefloating.config.FreeFloatingConfigGroup;
import playground.balac.freefloating.router.FreeFloatingRoutingModule;
import playground.balac.onewaycarsharingredisgned.config.OneWayCarsharingRDConfigGroup;
import playground.balac.onewaycarsharingredisgned.router.OneWayCarsharingRDRoutingModule;
import playground.balac.twowaycarsharingredisigned.config.TwoWayCSConfigGroup;
import playground.balac.twowaycarsharingredisigned.router.TwoWayCSRoutingModule;
import playground.balac.utils.Events2TTCalculator;

public class CSJustMembersControler {

	public static void main(final String[] args) {
		Logger.getLogger( "org.matsim.core.controler.Injector" ).setLevel(Level.OFF);
		
    	final Config config = ConfigUtils.loadConfig(args[0]);
    	OneWayCarsharingRDConfigGroup configGroup = new OneWayCarsharingRDConfigGroup();
    	config.addModule(configGroup);
    	
    	FreeFloatingConfigGroup configGroupff = new FreeFloatingConfigGroup();
    	config.addModule(configGroupff);
    	
    	TwoWayCSConfigGroup configGrouptw = new TwoWayCSConfigGroup();
    	config.addModule(configGrouptw);
    	
    	AllCSModesConfigGroup configGroupAll = new AllCSModesConfigGroup();
    	config.addModule(configGroupAll);
    	
		final Scenario sc = ScenarioUtils.loadScenario(config);
		
		
		final Controler controler = new Controler( sc );

		controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                bindMobsim().toProvider( AllCSModesQsimFactory.class );
            }
        });
		final TravelTimeCalculator travelTimeCalculator = Events2TTCalculator.getTravelTimeCalculator(sc, args[1]);
		
		controler.addOverridingModule(new AbstractModule() {

			@Override
			public void install() {

				addRoutingModuleBinding("twowaycarsharing").toInstance(new TwoWayCSRoutingModule());
				addRoutingModuleBinding("freefloating").toInstance(new FreeFloatingRoutingModule());
				addRoutingModuleBinding("onewaycarsharing").toInstance(new OneWayCarsharingRDRoutingModule());
				bind( TravelTime.class ).toInstance( travelTimeCalculator.getLinkTravelTimes() );
				bind(MainModeIdentifier.class).toInstance(new MainModeIdentifier() {

                    final MainModeIdentifier defaultModeIdentifier = new MainModeIdentifierImpl();
					
					@Override
					public String identifyMainMode(List<? extends PlanElement> tripElements) {

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
                        // if the trip doesn't contain a carsharing leg,
                        // fall back to the default identification method.
                        return defaultModeIdentifier.identifyMainMode( tripElements );
					
					}				
					
				});		
				
			}
			
		});
		
		AllCSModesScoringFunctionFactory allCSModesScoringFunctionFactory = new AllCSModesScoringFunctionFactory(
                  config,
                  sc.getNetwork(), sc);
		controler.setScoringFunctionFactory(allCSModesScoringFunctionFactory);


		Set<String> modes = new TreeSet<String>();
		modes.add("freefloating");
		modes.add("twowaycarsharing");
		modes.add("onewaycarsharing");
		modes.add("car");
		modes.add("walk");
		modes.add("pt");
		modes.add("bike");
		TripsAnalyzer tripsAnalyzer = new TripsAnalyzer(sc.getConfig().getParam("controler", "outputDirectory")+ "/tripsFile",
                sc.getConfig().getParam("controler", "outputDirectory") + "/durationsFile",
                sc.getConfig().getParam("controler", "outputDirectory") + "/distancesFile",
                modes, true, sc.getNetwork());


		controler.addControlerListener(tripsAnalyzer);

		controler.addControlerListener(new CarsharingListener(controler,
                Integer.parseInt(controler.getConfig().getModule("Carsharing").getValue("statsWriterFrequency"))));
		controler.run();


	}
	
}
