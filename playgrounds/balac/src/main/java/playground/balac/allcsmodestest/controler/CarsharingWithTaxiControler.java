package playground.balac.allcsmodestest.controler;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.router.*;
import org.matsim.core.scenario.ScenarioUtils;

import playground.balac.allcsmodestest.config.AllCSModesConfigGroup;
import playground.balac.allcsmodestest.controler.listener.AllCSModesTestListener;
import playground.balac.allcsmodestest.qsim.AllCSModesQsimFactory;
import playground.balac.allcsmodestest.scoring.AllCSModesScoringFunctionFactory;
import playground.balac.analysis.TripsAnalyzer;
import playground.balac.freefloating.config.FreeFloatingConfigGroup;
import playground.balac.freefloating.router.FreeFloatingRoutingModule;
import playground.balac.onewaycarsharingredisgned.config.OneWayCarsharingRDConfigGroup;
import playground.balac.onewaycarsharingredisgned.router.OneWayCarsharingRDRoutingModule;
import playground.balac.taxiservice.config.TaxiserviceConfigGroup;
import playground.balac.taxiservice.router.TaxiserviceRoutingModule;
import playground.balac.twowaycarsharingredisigned.config.TwoWayCSConfigGroup;
import playground.balac.twowaycarsharingredisigned.router.TwoWayCSRoutingModule;

import javax.inject.Provider;

public class CarsharingWithTaxiControler extends Controler{

	public CarsharingWithTaxiControler(Scenario scenario) {
		super(scenario);
	}


	public void init(Config config, Network network, Scenario sc) {
		AllCSModesScoringFunctionFactory allCSModesScoringFunctionFactory = new AllCSModesScoringFunctionFactory(
				      config, 
				      network, sc);
	    this.setScoringFunctionFactory(allCSModesScoringFunctionFactory); 	
				
	    this.loadMyControlerListeners();
		}
	
	  private void loadMyControlerListeners() {  
		  
//	    super.loadControlerListeners();   
	    Set<String> modes = new TreeSet<String>();
	    modes.add("freefloating");
	    modes.add("twowaycarsharing");
	    modes.add("car");
	    modes.add("walk");
	    modes.add("pt");
	    modes.add("bike");
	    modes.add("taxi");
        TripsAnalyzer tripsAnalyzer = new TripsAnalyzer(this.getConfig().getParam("controler", "outputDirectory")+ "/tripsFile",
	    		this.getConfig().getParam("controler", "outputDirectory") + "/durationsFile",
	    		this.getConfig().getParam("controler", "outputDirectory") + "/distancesFile",
	    		modes, true, getScenario().getNetwork());
	    this.addControlerListener(tripsAnalyzer);
	    
	    this.addControlerListener(new AllCSModesTestListener(this,
	    		Integer.parseInt(this.getConfig().getModule("AllCSModes").getValue("statsWriterFrequency"))));
	  }
	public static void main(final String[] args) {
		
    	final Config config = ConfigUtils.loadConfig(args[0]);
    	OneWayCarsharingRDConfigGroup configGroupow = new OneWayCarsharingRDConfigGroup();
    	config.addModule(configGroupow);
    	
    	FreeFloatingConfigGroup configGroupff = new FreeFloatingConfigGroup();
    	config.addModule(configGroupff);
    	
    	TwoWayCSConfigGroup configGrouptw = new TwoWayCSConfigGroup();
    	config.addModule(configGrouptw);
    	
    	AllCSModesConfigGroup configGroupAll = new AllCSModesConfigGroup();
    	config.addModule(configGroupAll);
    	
    	TaxiserviceConfigGroup configGrouptaxi = new TaxiserviceConfigGroup();
    	config.addModule(configGrouptaxi);
    	
		final Scenario sc = ScenarioUtils.loadScenario(config);
		
		
		final CarsharingWithTaxiControler controler = new CarsharingWithTaxiControler( sc );

		controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                bindMobsim().toProvider( AllCSModesQsimFactory.class );
            }
        });

		controler.setTripRouterFactory(
            new javax.inject.Provider<org.matsim.core.router.TripRouter>() {
                @Override
                public TripRouter get() {
                    // this factory initializes a TripRouter with default modules,
                    // taking into account what is asked for in the config

                    // This allows us to just add our module and go.
                    final Provider<TripRouter> delegate = TripRouterFactoryBuilderWithDefaults.createDefaultTripRouterFactoryImpl(controler.getScenario());

                    final TripRouter router = delegate.get();

                    // add our module to the instance
                    router.setRoutingModule(
                        "twowaycarsharing",
                        new TwoWayCSRoutingModule());

                    router.setRoutingModule(
                            "freefloating",
                            new FreeFloatingRoutingModule());

                    router.setRoutingModule(
                            "onewaycarsharing",
                            new OneWayCarsharingRDRoutingModule());

                    router.setRoutingModule(
                            "taxiservice",
                            new TaxiserviceRoutingModule(controler));

                    // we still need to provide a way to identify our trips
                    // as being twowaycarsharing trips.
                    // This is for instance used at re-routing.
                    final MainModeIdentifier defaultModeIdentifier =
                        router.getMainModeIdentifier();
                    router.setMainModeIdentifier(
                            new MainModeIdentifier() {
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
                                        else if ( pe instanceof Leg && ((Leg) pe).getMode().equals( "taxi" ) ) {
                                            return "taxi";
                                        }
                                    }
                                    // if the trip doesn't contain a onewaycarsharing leg,
                                    // fall back to the default identification method.
                                    return defaultModeIdentifier.identifyMainMode( tripElements );
                                }
                            });

                    return router;
                }


            });


		controler.init(config, sc.getNetwork(), sc);

		controler.run();


	}

}
