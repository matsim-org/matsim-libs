package playground.wrashid.parkingChoice.freeFloatingCarSharing;

import com.google.inject.name.Names;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.parking.parkingChoice.carsharing.ParkingCoordInfo;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.*;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.vehicles.Vehicle;

import playground.balac.freefloating.config.FreeFloatingConfigGroup;
import playground.balac.freefloating.controler.listener.FFListener;
import playground.balac.freefloating.routerparkingmodule.FreeFloatingRoutingModule;
import playground.balac.onewaycarsharingredisgned.config.OneWayCarsharingRDConfigGroup;
import playground.wrashid.freefloating.qsim.FreeFloatingQsimFactory;
import playground.wrashid.parkingChoice.config.ParkingChoiceConfigGroup;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FreeFloatingWithParkingControler {

public static void main(final String[] args) throws IOException {
		
    	final Config config = ConfigUtils.loadConfig(args[0]);
    	FreeFloatingConfigGroup configGroup = new FreeFloatingConfigGroup();
    	config.addModule(configGroup);
    	ParkingChoiceConfigGroup configGroupP = new ParkingChoiceConfigGroup();
    	config.addModule(configGroupP);
		final Scenario sc = ScenarioUtils.loadScenario(config);
		OneWayCarsharingRDConfigGroup configGroupOW = new OneWayCarsharingRDConfigGroup();
    	config.addModule(configGroupOW);
		
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
		    	Coord coord = new Coord(Double.parseDouble(arr[2]), Double.parseDouble(arr[3]));
			final Coord coord1 = coord;
		    	
		    	
		    	
                Link l = NetworkUtils.getNearestLinkExactly(((Network)controler.getScenario().getNetwork()),coord1);
		    	
		    	for (int k = 0; k < Integer.parseInt(arr[6]); k++) {
		    		ParkingCoordInfo parkingInfo = new ParkingCoordInfo(Id.create(Integer.toString(i), Vehicle.class), l.getCoord());
		    		freefloatingCars.add(parkingInfo);
		    		i++;
		    	}
		    	
		    	s = reader.readLine();
		    	
		    }
		  /*  FreeFloatingScoringFunctionFactory ffScoringFunctionFactory = new FreeFloatingScoringFunctionFactory(
				      config, 
				      sc.getNetwork(), sc);
		    controler.setScoringFunctionFactory(ffScoringFunctionFactory); */

		    //controler.addOverridingModule( new ScoreTrackingModule() );
		  
		    final ParkingModuleWithFFCarSharingZH parkingModule = new ParkingModuleWithFFCarSharingZH(controler, freefloatingCars);
		    
		    controler.addOverridingModule(new AbstractModule() {

				@Override
				public void install() {
			
					bind(ParkingModuleWithFFCarSharingZH.class).toInstance(parkingModule);
					
				}
		    }
		    );
		    controler.addOverridingModule(new AbstractModule() {

				@Override
				public void install() {
					bind(ArrayList.class)
					.annotatedWith(Names.named("initialFFCars"))
					.toInstance(freefloatingCars);			
					
				}
				
			});		
		    controler.addOverridingModule(
		    		new AbstractModule() {

						@Override
						public void install() {
		
							bindMobsim().toProvider(FreeFloatingQsimFactory.class);
				}
			});
		}
		controler.addOverridingModule( new AbstractModule() {
			@Override
			public void install() {
				this.addPlanStrategyBinding("RandomTripToCarsharingStrategy").to( playground.balac.allcsmodestest.replanning.RandomTripToCarsharingStrategy.class ) ;
			}
		});
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
	
		
  controler.addControlerListener(new FFListener(controler));
  controler.run();
	}

}
