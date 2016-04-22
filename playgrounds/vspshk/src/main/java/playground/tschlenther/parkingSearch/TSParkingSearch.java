/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package playground.tschlenther.parkingSearch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import javax.inject.Inject;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.carsharing.config.CarsharingConfigGroup;
import org.matsim.contrib.carsharing.config.FreeFloatingConfigGroup;
import org.matsim.contrib.carsharing.config.OneWayCarsharingConfigGroup;
import org.matsim.contrib.carsharing.config.TwoWayCarsharingConfigGroup;
import org.matsim.contrib.carsharing.control.listeners.CarsharingListener;
import org.matsim.contrib.carsharing.replanning.CarsharingSubtourModeChoiceStrategy;
import org.matsim.contrib.carsharing.replanning.RandomTripToCarsharingStrategy;
import org.matsim.contrib.carsharing.runExample.CarsharingUtils;
import org.matsim.contrib.carsharing.scoring.CarsharingScoringFunctionFactory;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.NetworkConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup.SnapshotStyle;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.AbstractQSimPlugin;
import org.matsim.core.mobsim.qsim.ActivityEnginePlugin;
import org.matsim.core.mobsim.qsim.QSimProvider;
import org.matsim.core.mobsim.qsim.TeleportationPlugin;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.agents.PopulationAgentSource;
import org.matsim.core.mobsim.qsim.changeeventsengine.NetworkChangeEventsPlugin;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.mobsim.qsim.messagequeueengine.MessageQueuePlugin;
import org.matsim.core.mobsim.qsim.pt.TransitEnginePlugin;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEnginePlugin;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.pt.config.TransitConfigGroup;
import com.google.inject.Module;
import com.google.inject.Provides;
import playground.tschlenther.createNetwork.Nikolaus;

/**
 * @author nagel/schlenther
 *
 */
final class TSParkingSearch {
	
	//defines whether car sharing is considered or not
	private static final boolean USE_CS = true;
	
	/* bind the CS and PS combining QSimFactory, inside the PSAndCSPersonDriverAgentImpl gets bound.
	 * the park search logic is mainly to be found in that class, more precisely in chooseNextLinkId()
	 * and isWantingToArriveOnCurrentLink()
	* if CS is not used, we need to bind AgentFactory to ParkingSearchAgentFactory, this gets done in the
	* ParkinSearchPopulationPlugin
	*/
	private static class ParkingSearchQSimModule extends com.google.inject.AbstractModule {
		@Override 
		protected void configure() {
			if(USE_CS){
				bind(Mobsim.class).toProvider( ParkSearchAndCarsharingQsimFactory.class );
				//setting up the scoring function factory, inside different scoring functions are set-up
				bind(ScoringFunctionFactory.class).to(CarsharingScoringFunctionFactory.class);
			}
			else{
				bind(Mobsim.class).toProvider(QSimProvider.class);
			}
		}
		@SuppressWarnings("static-method")
		@Provides 
		Collection<AbstractQSimPlugin> provideQSimPlugins(TransitConfigGroup transitConfigGroup, NetworkConfigGroup networkConfigGroup, Config config) {
			final Collection<AbstractQSimPlugin> plugins = new ArrayList<>();
			plugins.add(new MessageQueuePlugin(config));
			plugins.add(new ActivityEnginePlugin(config));
			plugins.add(new QNetsimEnginePlugin(config));
			if (networkConfigGroup.isTimeVariantNetwork()) {
				plugins.add(new NetworkChangeEventsPlugin(config));
			}
			if (transitConfigGroup.isUseTransit()) {
				plugins.add(new TransitEnginePlugin(config));
			}
			plugins.add(new TeleportationPlugin(config));
			plugins.add(new ParkingSearchPopulationPlugin(config));
			return plugins;
		}
	}
	
	//
	private static class ParkingSearchPopulationPlugin extends AbstractQSimPlugin {
		public ParkingSearchPopulationPlugin(Config config) { super(config); }
		@Override 
		public Collection<? extends Module> modules() {
			Collection<Module> result = new ArrayList<>();
			result.add(new com.google.inject.AbstractModule() {
				@Override
				protected void configure() {
					bind(PopulationAgentSource.class).asEagerSingleton();
					if (getConfig().transit().isUseTransit()) {
						throw new RuntimeException("parking search together with transit is not implemented (should not be difficult)") ;
					} else {
						if(!USE_CS)	bind(AgentFactory.class).to(ParkingSearchAgentFactory.class).asEagerSingleton();
					}
				}
			});
			return result;
		}
		@Override 
		public Collection<Class<? extends AgentSource>> agentSources() {
			Collection<Class<? extends AgentSource>> result = new ArrayList<>();
			result.add(PopulationAgentSource.class);
			return result;
		}
	}
	private static class ParkingSearchAgentFactory implements AgentFactory {
		@Inject Netsim netsim ;
		@Override
		public MobsimAgent createMobsimAgentFromPerson(Person p) {
//			randomly driving around agent, will not find destination
			return new RandomParkingSearchAgent( p.getSelectedPlan(), netsim ) ;
		}
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Config config = ConfigUtils.loadConfig("C:/Users/Work/svn/shared-svn/studies/tschlenther/ParkSearch/input/Parkingconfig.xml") ;
		config.controler().setOutputDirectory("C:/Users/Work/VSP/runs/parkSearch/documentation/noCS");
		config.controler().setOverwriteFileSetting( OverwriteFileSetting.deleteDirectoryIfExists );
		config.controler().setWriteEventsInterval(1);
		config.controler().setWritePlansInterval(1);		
		config.qsim().setSnapshotStyle( SnapshotStyle.queue);
		
		int statsfrequency = 1;
		
		if(USE_CS){
//			prepare car sharing
			String[] modes = new String[2];
			modes[0] = "freefloating";
			modes[1] = "car";
			config.qsim().setMainModes(Arrays.asList(modes));

			// these values must all be set
	    	FreeFloatingConfigGroup ffconfigGroup = new FreeFloatingConfigGroup();
	    	PlanCalcScoreConfigGroup planCalcScore = config.planCalcScore();
	    	ffconfigGroup.setConstantFreeFloating("0");
	    	ffconfigGroup.setDistanceFeeFreeFloating("0");
	    	ffconfigGroup.setTimeFeeFreeFloating("-0.0052");
	    	ffconfigGroup.setTimeParkingFeeFreeFloating("-0.0025");
	    	ffconfigGroup.setUseFeeFreeFloating(true);
			ffconfigGroup.setUtilityOfTravelling(""+ planCalcScore.getModes().get(TransportMode.car).getMarginalUtilityOfTraveling());
	    	ffconfigGroup.setvehiclelocations("C:/Users/Work/svn/shared-svn/studies/tschlenther/ParkSearch/input/VehicleLocations.txt");
	    	ffconfigGroup.setSpecialTimeStart("0");
	    	ffconfigGroup.setSpecialTimeEnd("0");
	    	ffconfigGroup.setSpecialTimeFee("0");
	    	config.addModule(ffconfigGroup);
			
	    	TwoWayCarsharingConfigGroup twconfigGroup = new TwoWayCarsharingConfigGroup();
	    	twconfigGroup.setUseTwoWayCarsharing(false);
	    	config.addModule(twconfigGroup);
	    	
	    	OneWayCarsharingConfigGroup owconfigGroup = new OneWayCarsharingConfigGroup();
	    	owconfigGroup.setUseOneWayCarsharing(false);
	    	config.addModule(owconfigGroup);
			CarsharingConfigGroup csGroup = new CarsharingConfigGroup();
	    	csGroup.setStatsWriterFrequency(""+statsfrequency);
	    	config.addModule(csGroup);
		}
		
		String[] chainModes = new String[1];
		chainModes[0] = "car";
		config.subtourModeChoice().setChainBasedModes(chainModes);
		
		String[] subtourModes = new String[2];
		subtourModes[0] = "freefloating";
		subtourModes[1] = "pt";
		config.subtourModeChoice().setModes(subtourModes);
		config.subtourModeChoice().setConsiderCarAvailability(true);
		
		Scenario scenario = ScenarioUtils.loadScenario( config );
		Nikolaus netCreator = new Nikolaus(scenario);
		netCreator.createNetwork();
		
		//population
		createPopulation(scenario);
		
		Controler controler = new Controler( scenario ) ;

		controler.addOverridingModule( new AbstractModule(){
			@Override public void install() {
				this.install( new ParkingSearchQSimModule() ) ;
			}
		});
		
		//prepare controler
		if(USE_CS){
			controler.addOverridingModule( new AbstractModule() {
				@Override
				public void install() {
					this.addPlanStrategyBinding("RandomTripToCarsharingStrategy").to( RandomTripToCarsharingStrategy.class ) ;
					this.addPlanStrategyBinding("CarsharingSubtourModeChoiceStrategy").to( CarsharingSubtourModeChoiceStrategy.class ) ;
				}
			});
			
			controler.addOverridingModule(CarsharingUtils.createModule());
	
			controler.addControlerListener(new CarsharingListener(controler,
					statsfrequency ) ) ;
		}
		controler.run();
	}

	private static void createPopulation(Scenario scenario) {
		Population population = scenario.getPopulation();
		
		for (int i = 0; i < 12; i++) {
			
			int secondPuls = 0;
			if(i>=5) secondPuls = 15*60;
			// create a person and a plan container
			Person person = population.getFactory().createPerson(Id.createPersonId(i));
			Plan plan = population.getFactory().createPlan();

			// add a start activity at Link1
			Activity startAct = population.getFactory().createActivityFromCoord("h", new Coord(500,0));
	
			// 	8:00 am. plus i seconds
			startAct.setEndTime(8 * 3600 + 5*i + secondPuls);
			plan.addActivity(startAct);

			// add a leg
			Leg leg;
			if(USE_CS && i< 10 ){
			leg = population.getFactory().createLeg("freefloating");
			}
			else{
			leg = population.getFactory().createLeg("car");
			}
			plan.addLeg(leg);		

			// add a drain activity at Link7
			Activity drainAct = population.getFactory().createActivityFromCoord("w", new Coord(-500,500));
			plan.addActivity(drainAct);

			// store information in population
			person.addPlan(plan);
			population.addPerson(person);
		}
	}

}
