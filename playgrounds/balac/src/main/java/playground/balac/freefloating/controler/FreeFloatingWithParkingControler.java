package playground.balac.freefloating.controler;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.parking.parkingChoice.carsharing.ParkingCoordInfo;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.router.*;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.vehicles.Vehicle;

import playground.balac.freefloating.config.FreeFloatingConfigGroup;
import playground.balac.freefloating.controler.listener.FFListener;
import playground.balac.freefloating.routerparkingmodule.FreeFloatingRoutingModule;
import playground.balac.freefloating.scoring.FreeFloatingScoringFunctionFactory;
import playground.balac.onewaycarsharingredisgned.router.OneWayCarsharingRDRoutingModule;
import playground.balac.twowaycarsharingredisigned.router.TwoWayCSRoutingModule;

import javax.inject.Provider;


public class FreeFloatingWithParkingControler {
	
	
	public static void main(final String[] args) throws IOException {
		
    	final Config config = ConfigUtils.loadConfig(args[0]);
    	FreeFloatingConfigGroup configGroup = new FreeFloatingConfigGroup();
    	config.addModule(configGroup);
		final Scenario sc = ScenarioUtils.loadScenario(config);
		
		
		final Controler controler = new Controler( sc );

		final FreeFloatingConfigGroup configGroupff = (FreeFloatingConfigGroup)
				sc.getConfig().getModule( FreeFloatingConfigGroup.GROUP_NAME );
		
		
		BufferedReader reader;
		String s;
		
		//read in all the free-floating cars for the parking module
		if (configGroupff.useFeeFreeFloating()) {
		 reader = IOUtils.getBufferedReader(configGroupff.getvehiclelocations());
		    s = reader.readLine();
		    int i = 1;
		    
		    final ArrayList<ParkingCoordInfo> freefloatingCars = new ArrayList<ParkingCoordInfo>();
		    while(s != null) {
		    	
		    	String[] arr = s.split("\t", -1);

                Link l = controler.getScenario().getNetwork().getLinks().get(Id.create(arr[0], Link.class));
		    	
		    	for (int k = 0; k < Integer.parseInt(arr[1]); k++) {
		    		ParkingCoordInfo parkingInfo = new ParkingCoordInfo(Id.create(Integer.toString(i), Vehicle.class), l.getCoord());
		    		freefloatingCars.add(parkingInfo);
		    		i++;
		    	}
		    	
		    	s = reader.readLine();
		    	
		    }
		    
		/*      final ParkingModuleWithFFCarSharingZH parkingModule = new ParkingModuleWithFFCarSharingZH(controler, freefloatingCars);

			controler.addOverridingModule(new AbstractModule() {
				@Override
				public void install() {
					bindMobsim().toProvider(new Provider<Mobsim>() {
						@Override
						public Mobsim get() {
							return new FreeFloatingQsimFactory(sc, controler,
									parkingModule, freefloatingCars).createMobsim(controler.getScenario(), controler.getEvents());
						}
					});
				}
			});*/
		}
		  controler.addOverridingModule(new AbstractModule() {

				@Override
				public void install() {

					addRoutingModuleBinding("freefloating").toInstance(new FreeFloatingRoutingModule());

					bind(MainModeIdentifier.class).toInstance(new MainModeIdentifier() {

	                    final MainModeIdentifier defaultModeIdentifier = new MainModeIdentifierImpl();
						
						@Override
						public String identifyMainMode(List<? extends PlanElement> tripElements) {
							for ( PlanElement pe : tripElements ) {
							 if ( pe instanceof Leg && ((Leg) pe).getMode().equals( "freefloating" ) ) {
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
	
		FreeFloatingScoringFunctionFactory ffScoringFunctionFactory = new FreeFloatingScoringFunctionFactory(
			      config, 
			      sc.getNetwork(), sc);
  controler.setScoringFunctionFactory(ffScoringFunctionFactory); 
  controler.addControlerListener(new FFListener(controler));
  controler.run();
	}

}