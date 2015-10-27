package playground.balac.aam.controler;

import java.util.List;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.MainModeIdentifierImpl;
import org.matsim.core.scenario.ScenarioUtils;

import playground.balac.aam.router.AAMRoutingModule;
import playground.balac.aam.scoring.AAMScoringFunctionFactory;


public class AAMControler extends Controler{

	public AAMControler(Scenario scenario) {
		super(scenario);
	}

	public void init(Config config, Network network, Scenario sc) {
		AAMScoringFunctionFactory aAMScoringFunctionFactory = new AAMScoringFunctionFactory(
				      config, 
				      network, sc);
	    this.setScoringFunctionFactory(aAMScoringFunctionFactory); 	
				
		}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		final Config config = ConfigUtils.loadConfig(args[0]);

		
		final Scenario sc = ScenarioUtils.loadScenario(config);
		
		
		final AAMControler controler = new AAMControler( sc );
		
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addRoutingModuleBinding("movingpathways").toInstance(new AAMRoutingModule(sc));
				
				bind(MainModeIdentifier.class).toInstance(new MainModeIdentifier() {
                    final MainModeIdentifier defaultModeIdentifier = new MainModeIdentifierImpl();

                    @Override
                    public String identifyMainMode(
                            final List<? extends PlanElement> tripElements) {
                    	boolean hadMovingPathway = false;
						for ( PlanElement pe : tripElements ) {
							if ( pe instanceof Leg ) {
								final Leg l = (Leg) pe;
								if ( l.getMode().equals( "movingpathways" ) ) {
									hadMovingPathway = true;
								}
								if ( l.getMode().equals( TransportMode.transit_walk ) ) {
									return TransportMode.pt;
								}
							}
						}

						if ( hadMovingPathway ) {
							// there were bike sharing legs but no transit walk
							return "movingpathways";
						}

						return defaultModeIdentifier.identifyMainMode( tripElements );
                    }
                });
			}
		});
				
		controler.init(config, sc.getNetwork(), sc);		
			
		controler.run();
	}

}
