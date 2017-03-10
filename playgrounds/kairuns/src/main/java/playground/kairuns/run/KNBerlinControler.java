package playground.kairuns.run;

import java.util.Set;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.analysis.kai.KaiAnalysisListener;
import org.matsim.contrib.noise.NoiseConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.consistency.VspConfigConsistencyCheckerImpl;
import org.matsim.core.config.groups.ControlerConfigGroup.MobsimType;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.TypicalDurationScoreComputation;
import org.matsim.core.config.groups.PlansConfigGroup.ActivityDurationInterpretation;
import org.matsim.core.config.groups.QSimConfigGroup.TrafficDynamics;
import org.matsim.core.config.groups.VspExperimentalConfigGroup.VspDefaultsCheckingLevel;
import org.matsim.core.controler.*;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.CollectionUtils;

public final class KNBerlinControler {
	private static final Logger log = Logger.getLogger(KNBerlinControler.class);

	public static double capFactorForEWS = Double.NaN ;
	
	public static enum A100 { base, ba16, ba1617, stork } ;

	public static void main ( String[] args ) {
		final boolean equil = false ;
		final boolean unterLindenQuiet = false ;
		final A100 a100 = A100.base ;

		final boolean assignment = false ;
		final boolean modeChoice = true ;

		OutputDirectoryLogging.catchLogEntries();

		log.info("Starting KNBerlinControler ...") ;

		Config config = prepareConfig(args, assignment, equil, modeChoice, a100);

		// ===

		Scenario scenario = prepareScenario(equil, unterLindenQuiet, config);

		// ===

		AbstractModule overrides = prepareOverrides(assignment);

		// === run everything:

		Controler controler = new Controler( scenario ) ;
		controler.addOverridingModule( overrides );
		controler.run();

		// === post-processing:

		//		computeNoise(sampleFactor, config, scenario);

	}

	public static AbstractModule prepareOverrides(final boolean assignment) {
		AbstractModule overrides = new AbstractModule() {
			@Override public void install() {
				this.addControlerListenerBinding().toInstance( new KaiAnalysisListener() ) ;
				//		controler.addOverridingModule( new OTFVisLiveModule() );
				if ( assignment ) {
					this.addControlerListenerBinding().to( MySpeedProvider.class ) ;
				}
				addTravelTimeBinding(TransportMode.bike).to(BerlinUtils.BikeTravelTime.class);
				addTravelTimeBinding(TransportMode.walk).to(BerlinUtils.WalkTravelTime.class);
//				addTravelTimeBinding(TransportMode.pt).to(BerlinUtils.PtTravelTime.class);
				
				this.bind( PrepareForSim.class ).to( PrepareForSimMultimodalImpl.class ) ;
			}
		} ;
		return overrides;
	}

	public static Scenario prepareScenario(final boolean equil, boolean unterLindenQuiet, Config config) {
		Scenario scenario = ScenarioUtils.loadScenario( config ) ;
		
		if ( !equil ) {
			// modify berlin network:
			for ( Link link : scenario.getNetwork().getLinks().values() ) {
				if ( link.getFreespeed() < 77/3.6 ) {
					if ( link.getCapacity() >= 1001. ) { // cap >= 1000 is nearly everything 
						link.setFreespeed( 1. * link.getFreespeed() );
					}else {
						link.setFreespeed( 0.5 * link.getFreespeed() );
					}
				}
				if ( link.getLength()<100 ) {
					link.setCapacity( 2. * link.getCapacity() ); // double capacity on short links, often roundabouts or short u-turns, etc., with the usual problem
				}
				Set<String> modes = CollectionUtils.stringArrayToSet(new String [] {TransportMode.walk, TransportMode.car, TransportMode.bike, TransportMode.pt }) ;
				// (TransportMode.pt for self-computed pseudo pt)
				link.setAllowedModes(modes);
			}
			// remove bike routes so that they get recomputed as network routes (could store the result afterwards):
			for ( Person person : scenario.getPopulation().getPersons().values() ) {
				for ( Plan plan : person.getPlans() ) {
					for ( Leg leg : TripStructureUtils.getLegs(plan) ) {
						leg.setRoute(null);
					}
				}
			}
			
		}
		if ( equil ) {
			// modify equil plans:
			double time = 6*3600. ;
			for ( Person person : scenario.getPopulation().getPersons().values() ) {
				Plan plan = person.getSelectedPlan() ;
				Activity activity = (Activity) plan.getPlanElements().get(0) ;
				activity.setEndTime(time);
				time++ ;
			}
		}
		if ( unterLindenQuiet ) {
			BerlinUtils.unterLindenQuiet(scenario.getNetwork());
		}

		BerlinUtils.createAndAddModeVehicleTypes(scenario.getVehicles());

		return scenario;
	}

	public static Config prepareConfig(String[] args, final boolean assignment, final boolean equil, boolean modeChoice, A100 a100) {
		double sampleFactor;
		if ( equil ) {
			sampleFactor = 1. ;
		} else {
			sampleFactor = 0.02 ;
		}

		// ### config, paths, and related:
		Config config ;
		if ( args!=null && args.length>=1 ) {
			config = ConfigUtils.loadConfig( args[0] ) ;
		} else if ( equil ) {
			config = ConfigUtils.loadConfig( "~/git/matsim/matsim/examples/equil/config.xml" ) ;
			config.plans().setInputFile("plans2000.xml.gz");
		} else {
			config = ConfigUtils.createConfig( new NoiseConfigGroup() ) ;
			switch ( a100 ) {
			case base:
				config.network().setInputFile("~/shared-svn/studies/countries/de/berlin/counts/iv_counts/network-base_ext.xml.gz");
				break;
			case ba16:
				config.network().setInputFile("~/shared-svn/studies/countries/de/berlin/counts/iv_counts/network-ba16_ext.xml.gz") ;
				break;
			case ba1617:
				config.network().setInputFile("~/shared-svn/studies/countries/de/berlin/counts/iv_counts/network-ba16_17_ext.xml.gz") ;
				break;
			case stork:
				config.network().setInputFile("~/shared-svn/studies/countries/de/berlin/counts/iv_counts/network-ba16_17_storkower_ext.xml.gz") ;
				break;
			default:
				throw new RuntimeException("not implemented") ;
			}

//			config.plans().setInputFile("~/kairuns/a100/baseplan_900s_routed.xml.gz") ;
			config.plans().setInputFile("~/kairuns/a100/Aoutput-2016-11-19-triang-w-mode-choice/long-2016-11-23-09hhXX/output_plans.xml.gz") ;
		}

		if ( assignment ) {
			config.network().setTimeVariantNetwork(true);
			capFactorForEWS = 1.4 ;  // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
		}

		config.plans().setRemovingUnneccessaryPlanAttributes(true) ;
		config.plans().setActivityDurationInterpretation(ActivityDurationInterpretation.tryEndTimeThenDuration );
		if ( equil ) {
			config.planCalcScore().setWriteExperiencedPlans(true);
		}

		if ( !equil ) {
			config.counts().setInputFile("~/shared-svn/studies/countries/de/berlin/counts/iv_counts/vmz_di-do.xml" ) ;
			config.counts().setOutputFormat("all");
			config.counts().setCountsScaleFactor(1./sampleFactor);
			config.counts().setWriteCountsInterval(100);
		}

		config.controler().setOutputDirectory( System.getProperty("user.home") + "/kairuns/a100/output" );
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);

		// controler, global, and related:

		final int lastIteration = 100 ;
		config.controler().setFirstIteration(0); // with something like "9" we don't get output events! 
		config.controler().setLastIteration(lastIteration); // with something like "9" we don't get output events! 
		config.controler().setWriteSnapshotsInterval(lastIteration);
		config.controler().setWritePlansInterval(lastIteration);
		config.controler().setWriteEventsInterval(lastIteration);

		config.controler().setWritePlansUntilIteration(-1); 
		config.controler().setWriteEventsUntilIteration(1); 

		config.vspExperimental().setWritingOutputEvents(true); // is actually the default

		config.global().setCoordinateSystem("GK4");
		config.global().setRandomSeed(4711);

		config.strategy().setFractionOfIterationsToDisableInnovation(0.8);
		config.planCalcScore().setFractionOfIterationsToStartScoreMSA(0.8);

		// activity parameters:

		if ( !equil ) {
			BerlinUtils.createActivityParameters(config);
		}
		for ( ActivityParams params : config.planCalcScore().getActivityParams() ) {
			params.setTypicalDurationScoreComputation( TypicalDurationScoreComputation.relative );
		}

		// threads:

		config.global().setNumberOfThreads(6);
		config.qsim().setNumberOfThreads(5);
		config.parallelEventHandling().setNumberOfThreads(1);

		// qsim:

		config.qsim().setEndTime(36*3600);

		config.controler().setMobsim( MobsimType.qsim.toString() );
		config.qsim().setFlowCapFactor( sampleFactor );
		if ( equil ) {
			config.qsim().setStorageCapFactor( Math.pow( sampleFactor, 0.75 ) ); // <== this would be the correct version.
		} else {
			config.qsim().setStorageCapFactor(0.03);
			// leaving this at 0.03 for "berlin" since this was used for all the experiments.  And probably having it a it smaller than usual
			// is good since it spatially extends the jams.  kai, nov'16
		}
		
		
		if ( assignment ) {
			config.qsim().setTrafficDynamics( TrafficDynamics.queue );
			config.qsim().setFlowCapFactor(100);
			config.qsim().setStorageCapFactor(100);
		} else {
			config.qsim().setTrafficDynamics( TrafficDynamics.kinematicWaves); // this means, using with holes AND constraining inflow from maxFlowFromFdiag.

//			if ( config.qsim().getTrafficDynamics()==TrafficDynamics.withHoles ) {
//				config.qsim().setInflowConstraint(InflowConstraint.maxflowFromFdiag);
//			}
		}

		config.qsim().setNumberOfThreads(6);
		config.qsim().setUsingFastCapacityUpdate(true);

		config.qsim().setUsingTravelTimeCheckInTeleportation(true) ;

		//		config.qsim().setUsePersonIdForMissingVehicleId(false);
		// does not work

		// mode parameters:
		if ( !equil ) {
			BerlinUtils.createMultimodalParameters(config);
		}

		// strategy:

		BerlinUtils.setStrategies(config, equil, modeChoice);

		// other:

		config.vspExperimental().setVspDefaultsCheckingLevel( VspDefaultsCheckingLevel.abort );
		if ( equil ) {
			config.vspExperimental().setVspDefaultsCheckingLevel( VspDefaultsCheckingLevel.warn );
		}

		if ( args.length >=1 && args[0]!=null ) {
			ConfigUtils.loadConfig( config, args[0] ) ;
		}

		config.addConfigConsistencyChecker(new VspConfigConsistencyCheckerImpl());
		config.checkConsistency();
		return config;
	}


}