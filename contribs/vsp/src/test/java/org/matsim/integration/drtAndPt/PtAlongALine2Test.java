package org.matsim.integration.drtAndPt;

import static java.util.stream.Collectors.toList;
import static org.matsim.core.config.groups.PlansCalcRouteConfigGroup.ModeRoutingParams;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.contrib.drt.optimizer.insertion.extensive.ExtensiveInsertionSearchParams;
import org.matsim.contrib.drt.routing.DrtRoute;
import org.matsim.contrib.drt.routing.DrtRouteFactory;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.DrtConfigs;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.drt.run.MultiModeDrtModule;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.contrib.dvrp.run.DvrpQSimComponents;
import org.matsim.contrib.otfvis.OTFVisLiveModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ModeParams;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup.AccessEgressType;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.ActivityEngineModule;
import org.matsim.core.mobsim.qsim.ActivityEngineWithWakeup;
import org.matsim.core.mobsim.qsim.PreplanningEngine;
import org.matsim.core.mobsim.qsim.components.QSimComponentsConfigGroup;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.facilities.ActivityFacility;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehiclesFactory;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import ch.sbb.matsim.config.SwissRailRaptorConfigGroup;
import ch.sbb.matsim.config.SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet;

//@RunWith(Parameterized.class)
public class PtAlongALine2Test {
	private static final Logger log = Logger.getLogger(PtAlongALine2Test.class);

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	enum DrtMode {none, teleportBeeline, teleportBasedOnNetworkRoute, full, withPrebooking}

	private DrtMode drtMode = DrtMode.withPrebooking;
	private boolean drt2 = true;
	private boolean drt3 = true;

	// !! otfvis does not run within parameterized test :-( !!

	//	@Parameterized.Parameters(name = "{index}: DrtMode={0}")
	//	// the convention is that the output of the method marked by "@Parameters" is taken as input to the constructor
	//	// before running each test. kai, jul'16
	//	public static Collection<Object[]> createTestParams(){
	//		return Arrays.asList( new Object[][]{
	//				{ DrtMode.full },
	//				{ DrtMode.withPrebooking }
	//		} );
	//	}
	//
	//	public PtAlongALine2Test( DrtMode drtMode ) {
	//		this.drtMode = drtMode;
	//	}

	// !! otfvis does not run within parameterized test :-( !!

	@Test
	public void testPtAlongALineWithRaptorAndDrtServiceArea() {
		// Towards some understanding of what is going on here:
		// * In many situations, a good solution is that drt drives to some transit stop, and from there directly to the destination.  The swiss rail
		// raptor will return a cost "infinity" of such a solution, in which case the calling method falls back onto transit_walk.
		// * If "walk" is defined as intermodal access, then swiss rail raptor will call the correct RoutingModule, but afterwards change the mode of all
		// legs to non_network_mode.

		Config config = PtAlongALineTest.createConfig(utils.getOutputDirectory());

		// === GBL: ===

		config.controler().setLastIteration(0);

		// === ROUTER: ===

		config.plansCalcRoute().setAccessEgressType(AccessEgressType.accessEgressModeToLink);

		config.qsim().setVehiclesSource(QSimConfigGroup.VehiclesSource.modeVehicleTypesFromVehiclesData);
		// (as of today, will also influence router. kai, jun'19)

		if (drtMode == DrtMode.teleportBeeline) {// (configure teleportation router)
			config.plansCalcRoute()
					.addModeRoutingParams(
							new ModeRoutingParams().setMode(TransportMode.drt).setTeleportedModeSpeed(100. / 3.6));
			if (drt2) {
				config.plansCalcRoute()
						.addModeRoutingParams(
								new ModeRoutingParams().setMode("drt2").setTeleportedModeSpeed(100. / 3.6));
			}
			if (drt3) {
				config.plansCalcRoute()
						.addModeRoutingParams(
								new ModeRoutingParams().setMode("drt3").setTeleportedModeSpeed(100. / 3.6));
			}
			// teleportation router for walk or bike is automatically defined.
		} else if (drtMode == DrtMode.teleportBasedOnNetworkRoute) {// (route as network route)
			Set<String> networkModes = new HashSet<>();
			networkModes.add(TransportMode.drt);
			if (drt2) {
				networkModes.add("drt2");
			}
			if (drt3) {
				networkModes.add("drt3");
			}
			config.plansCalcRoute().setNetworkModes(networkModes);
		}

		config.plansCalcRoute()
				.addModeRoutingParams(new ModeRoutingParams().setMode("walk").setTeleportedModeSpeed(5. / 3.6));

		// set up walk2 so we don't need walk in raptor:
		config.plansCalcRoute()
				.addModeRoutingParams(new ModeRoutingParams().setMode("walk2").setTeleportedModeSpeed(5. / 3.6));

		// === RAPTOR: ===
		{
			//config.transit().setRoutingAlgorithmType(TransitRoutingAlgorithmType.DijkstraBased); // we'll set up SwissRailRaptor ourself, so disable it here to prevent conflicts
			SwissRailRaptorConfigGroup configRaptor = ConfigUtils.addOrGetModule(config,
					SwissRailRaptorConfigGroup.class);

			if (drtMode != DrtMode.none) {
				configRaptor.setUseIntermodalAccessEgress(true);

				//					paramSetXxx.setMode( TransportMode.walk ); // this does not work because sbb raptor treats it in a special way
				configRaptor.addIntermodalAccessEgress(new IntermodalAccessEgressParameterSet().setMode("walk2")
						.setMaxRadius(1000000)
						.setInitialSearchRadius(1000000)
						.setSearchExtensionRadius(10000));
				// (in principle, walk as alternative to drt will not work, since drt is always faster.  Need to give the ASC to the router!  However, with
				// the reduced drt network we should be able to see differentiation.)

				// drt
				configRaptor.addIntermodalAccessEgress(
						new IntermodalAccessEgressParameterSet().setMode(TransportMode.drt)
								.setMaxRadius(1000000)
								.setInitialSearchRadius(1000000)
								.setSearchExtensionRadius(10000));

				if (drt2) {
					//				paramSetDrt2.setPersonFilterAttribute( null );
					//				paramSetDrt2.setStopFilterAttribute( null );
					configRaptor.addIntermodalAccessEgress(new IntermodalAccessEgressParameterSet().setMode("drt2")
							.setMaxRadius(1000000)
							.setInitialSearchRadius(1000000)
							.setSearchExtensionRadius(10000));
				}
				if (drt3) {
					//				paramSetDrt2.setPersonFilterAttribute( null );
					//				paramSetDrt2.setStopFilterAttribute( null );
					configRaptor.addIntermodalAccessEgress(new IntermodalAccessEgressParameterSet().setMode("drt3")
							.setMaxRadius(1000000)
							.setInitialSearchRadius(1000000)
							.setSearchExtensionRadius(10000));
				}
			}

		}

		// === SCORING: ===

		double margUtlTravPt = config.planCalcScore().getModes().get(TransportMode.pt).getMarginalUtilityOfTraveling();
		if (drtMode != DrtMode.none) {
			// (scoring parameters for drt modes)
			config.planCalcScore()
					.addModeParams(new ModeParams(TransportMode.drt).setMarginalUtilityOfTraveling(margUtlTravPt));
			if (drt2) {
				config.planCalcScore()
						.addModeParams(new ModeParams("drt2").setMarginalUtilityOfTraveling(margUtlTravPt));
			}
			if (drt3) {
				config.planCalcScore()
						.addModeParams(new ModeParams("drt3").setMarginalUtilityOfTraveling(margUtlTravPt));
			}
		}
		config.planCalcScore().addModeParams(new ModeParams("walk2").setMarginalUtilityOfTraveling(margUtlTravPt));

		// === QSIM: ===

		config.qsim().setSimStarttimeInterpretation(QSimConfigGroup.StarttimeInterpretation.onlyUseStarttime);
		// yy why?  kai, jun'19

		// === DRT: ===

		if (drtMode == DrtMode.full || drtMode == DrtMode.withPrebooking) {
			// (configure full drt if applicable)

			DvrpConfigGroup dvrpConfig = ConfigUtils.addOrGetModule(config, DvrpConfigGroup.class);
			dvrpConfig.setNetworkModes(ImmutableSet.copyOf(Arrays.asList(TransportMode.drt, "drt2", "drt3")));

			MultiModeDrtConfigGroup mm = ConfigUtils.addOrGetModule(config, MultiModeDrtConfigGroup.class);
			{
				DrtConfigGroup drtConfigGroup = new DrtConfigGroup().setMode(TransportMode.drt)
						.setMaxTravelTimeAlpha(2.0)
						.setMaxTravelTimeBeta(5. * 60.)
						.setStopDuration(60.)
						.setMaxWaitTime(Double.MAX_VALUE)
						.setRejectRequestIfMaxWaitOrTravelTimeViolated(false)
						.setUseModeFilteredSubnetwork(true)
						.setAdvanceRequestPlanningHorizon(99999);
				drtConfigGroup.addParameterSet(new ExtensiveInsertionSearchParams());
				mm.addParameterSet(drtConfigGroup);

			}
			if (drt2) {
				DrtConfigGroup drtConfigGroup = new DrtConfigGroup().setMode("drt2")
						.setMaxTravelTimeAlpha(1.3)
						.setMaxTravelTimeBeta(5. * 60.)
						.setStopDuration(60.)
						.setMaxWaitTime(Double.MAX_VALUE)
						.setRejectRequestIfMaxWaitOrTravelTimeViolated(false)
						.setUseModeFilteredSubnetwork(true);
				drtConfigGroup.addParameterSet(new ExtensiveInsertionSearchParams());
				mm.addParameterSet(drtConfigGroup);
			}
			if (drt3) {
				DrtConfigGroup drtConfigGroup = new DrtConfigGroup().setMode("drt3")
						.setMaxTravelTimeAlpha(1.3)
						.setMaxTravelTimeBeta(5. * 60.)
						.setStopDuration(60.)
						.setMaxWaitTime(Double.MAX_VALUE)
						.setRejectRequestIfMaxWaitOrTravelTimeViolated(false)
						.setUseModeFilteredSubnetwork(true);
				drtConfigGroup.addParameterSet(new ExtensiveInsertionSearchParams());
				mm.addParameterSet(drtConfigGroup);
			}

			for (DrtConfigGroup drtConfigGroup : mm.getModalElements()) {
				DrtConfigs.adjustDrtConfig(drtConfigGroup, config.planCalcScore(), config.plansCalcRoute());
			}
		}

		if (drtMode == DrtMode.withPrebooking) {
			QSimComponentsConfigGroup qsimComponentsConfig = ConfigUtils.addOrGetModule(config,
					QSimComponentsConfigGroup.class);
			List<String> components = qsimComponentsConfig.getActiveComponents();
			components.remove(ActivityEngineModule.COMPONENT_NAME);
			components.add(ActivityEngineWithWakeup.COMPONENT_NAME);
			qsimComponentsConfig.setActiveComponents(components);
		}

		// === VSP: ===

		config.vspExperimental().setVspDefaultsCheckingLevel(VspExperimentalConfigGroup.VspDefaultsCheckingLevel.warn);

		// ### SCENARIO: ###

		Scenario scenario = new PtAlongALineFixture().createScenario(config, 30);

		// Add Test Agents to Scenario
		PopulationFactory pf = scenario.getPopulation().getFactory();

		List<String> testAgents = Arrays.asList("2-3", "50-51", "300-301", "550-551", "690-691", "800-801");
		for (String str : testAgents) {
			Person testAgent = pf.createPerson(Id.createPersonId("agent" + str));
			scenario.getPopulation().addPerson(testAgent);
			Plan plan = pf.createPlan();
			testAgent.addPlan(plan);

			Id<ActivityFacility> homeFacilityId = Id.create(str, ActivityFacility.class);
			Activity home = pf.createActivityFromActivityFacilityId("dummy", homeFacilityId);
			home.setEndTime(7. * 3600. + 1.);
			plan.addActivity(home);

			Leg leg = pf.createLeg(TransportMode.pt);
			leg.setDepartureTime(7. * 3600.);
			leg.setTravelTime(1800.);
			plan.addLeg(leg);

			Activity shop = pf.createActivityFromActivityFacilityId("dummy",
					Id.create("999-1000", ActivityFacility.class));
			plan.addActivity(shop);
		}

		if (drtMode == DrtMode.full || drtMode == DrtMode.withPrebooking) {
			scenario.getPopulation()
					.getFactory()
					.getRouteFactories()
					.setRouteFactory(DrtRoute.class, new DrtRouteFactory());
		}
		if (drtMode == DrtMode.withPrebooking) {
			for (Person person : scenario.getPopulation().getPersons().values()) {
				person.getSelectedPlan()
						.getAttributes()
						.putAttribute(PreplanningEngine.PREBOOKING_OFFSET_ATTRIBUTE_NAME, 7200.);
			}
		}

		// add drt modes to the car links' allowed modes in their respective service area
		PtAlongALineTest.addModeToAllLinksBtwnGivenNodes(scenario.getNetwork(), 0, 400, TransportMode.drt);
		if (drt2) {
			PtAlongALineTest.addModeToAllLinksBtwnGivenNodes(scenario.getNetwork(), 700, 1000, "drt2");
		}
		if (drt3) {
			PtAlongALineTest.addModeToAllLinksBtwnGivenNodes(scenario.getNetwork(), 500, 600, "drt3");
		}
		// TODO: reference somehow network creation, to ensure that these link ids exist

		// The following is also for the router! kai, jun'19
		VehiclesFactory vf = scenario.getVehicles().getFactory();
		if (drt2) {
			scenario.getVehicles()
					.addVehicleType(
							vf.createVehicleType(Id.create("drt2", VehicleType.class)).setMaximumVelocity(25. / 3.6));
		}
		if (drt3) {
			scenario.getVehicles()
					.addVehicleType(
							vf.createVehicleType(Id.create("drt3", VehicleType.class)).setMaximumVelocity(25. / 3.6));
		}
		scenario.getVehicles()
				.addVehicleType(vf.createVehicleType(Id.create(TransportMode.drt, VehicleType.class))
						.setMaximumVelocity(25. / 3.6));

		// (does not work without; I don't really know why. kai)
		scenario.getVehicles()
				.addVehicleType(vf.createVehicleType(Id.create(TransportMode.car, VehicleType.class))
						.setMaximumVelocity(25. / 3.6));

		//		scenario.getPopulation().getPersons().values().removeIf( person -> !person.getId().toString().equals( "3" ) );

		// ### CONTROLER: ###

		Controler controler = new Controler(scenario);

		if (drtMode == DrtMode.full || drtMode == DrtMode.withPrebooking) {
			controler.addOverridingModule(new DvrpModule());
			controler.addOverridingModule(new MultiModeDrtModule());
			if (drt2 && drt3) {
				controler.configureQSimComponents(DvrpQSimComponents.activateModes(TransportMode.drt, "drt2", "drt3"));
			} else if (drt2) {
				controler.configureQSimComponents(DvrpQSimComponents.activateModes(TransportMode.drt, "drt2"));
			} else if (drt3) {
				controler.configureQSimComponents(DvrpQSimComponents.activateModes(TransportMode.drt, "drt3"));
			} else {
				controler.configureQSimComponents(DvrpQSimComponents.activateModes(TransportMode.drt));
			}
		}
		if (drtMode == DrtMode.withPrebooking) {
			controler.addOverridingModule(new AbstractModule() {
				@Override
				public void install() {
					installQSimModule(new AbstractQSimModule() {
						@Override
						protected void configureQSim() {
							this.addQSimComponentBinding(ActivityEngineWithWakeup.COMPONENT_NAME)
									.to(ActivityEngineWithWakeup.class)
									.in(Singleton.class);
						}
					});
				}
			});
		}

		// TODO: avoid really writing out these files. However so far it is unclear how
		// to configure DRT and load the vehicles otherwise
		controler.addOverridingModule(
				PtAlongALineTest.createGeneratedFleetSpecificationModule(TransportMode.drt, "DRT-", 30,
						Id.createLinkId("0-1"), 1));
		if (drt2) {
			controler.addOverridingModule(PtAlongALineTest.createGeneratedFleetSpecificationModule("drt2", "DRT2-", 10,
					Id.createLinkId("999-1000"), 4));
		}
		if (drt3) {
			controler.addOverridingModule(PtAlongALineTest.createGeneratedFleetSpecificationModule("drt3", "DRT3-", 10,
					Id.createLinkId("500-501"), 4));
		}

		controler.addOverridingModule(
				PtAlongALineTest.createGeneratedFleetSpecificationModule(TransportMode.drt, "DRT-", 30,
						Id.createLinkId("0-1"), 1));

		if ("true".equals(System.getProperty("runOTFVis"))) {
			// This will start otfvis
			controler.addOverridingModule(new OTFVisLiveModule());
			// !! does not work together with parameterized tests :-( !!
		}

		controler.run();

		/*
		 * TODO: Asserts:
		 * All agents go from some randomly chosen link to the transit stop at the far right.
		 *
		 * Nobody should use DRT2, because it only connects to that transit stop at the right.
		 *
		 * People on the left should use DRT to go to the left stop or towards the middle stop (and walk the distance
		 * between the end of the DRT service area and the middle stop).
		 *
		 * People between the middle stop and the right stop should use DRT3 or even walk into the DRT3 service area
		 * (=walk in the wrong direction to access the fast drt mode to access a fast pt trip instead of slowly walking
		 * the whole distance to the destination). At some point walking towards the DRT3 area becomes less attractive
		 * thanh walking directly to the right transit stop, so agents start to walk directly (as the pt router found no
		 * route at all including a pt leg, it returned instead a direct walk).
		 *
		 */
		for (String agent : testAgents) {
			System.out.println("\n\n**** AGENT : " + agent);
			List<PlanElement> pes = controler.getScenario()
					.getPopulation()
					.getPersons()
					.get(Id.createPersonId("agent" + agent))
					.getSelectedPlan()
					.getPlanElements();
			pes.stream().filter(s -> s instanceof Leg).forEach(s -> System.out.println(s.toString()));
		}

		/** Case 1: The agent starts on link 2-3. While the agent is in the drt service area, s/he is so close to the pt
		 * stop that it doesn't make sense for him/her to use drt as an intermodal access to get the pt. Therefore the
		 * agent will just walk to the pt stop.
		 */

		List<PlanElement> planFullCase1 = controler.getScenario()
				.getPopulation()
				.getPersons()
				.get(Id.createPersonId("agent" + testAgents.get(0)))
				.getSelectedPlan()
				.getPlanElements();
		List<Leg> planLegCase1 = planFullCase1.stream()
				.filter(pe -> pe instanceof Leg)
				.map(pe -> (Leg)pe)
				.collect(toList());
		Assert.assertTrue("Incorrect Mode, case 1", planLegCase1.get(0).getMode().contains("walk"));
		Assert.assertTrue("Incorrect Mode, case 1", planLegCase1.get(1).getMode().equals("pt"));
		Assert.assertTrue("Incorrect Mode, case 1", planLegCase1.get(2).getMode().contains("walk"));

		/**
		 * Case 2a: Agent starts at Link "50-51". This is within the drt service area. Agent is expected use drt as
		 * intermodal access to the left pt stop. The plan should be walk -> drt -> walk -> pt -> walk
		 */

		List<PlanElement> planFullCase2a = controler.getScenario()
				.getPopulation()
				.getPersons()
				.get(Id.createPersonId("agent" + testAgents.get(1)))
				.getSelectedPlan()
				.getPlanElements();
		List<Leg> planLegCase2a = planFullCase2a.stream()
				.filter(pe -> pe instanceof Leg)
				.map(pe -> (Leg)pe)
				.collect(toList());
		Assert.assertTrue("Incorrect Mode, case 2a", planLegCase2a.get(0).getMode().contains("walk"));
		Assert.assertTrue("Incorrect Mode, case 2a", planLegCase2a.get(1).getMode().equals("drt"));
		Assert.assertTrue("Incorrect Mode, case 2a", planLegCase2a.get(2).getMode().contains("walk"));
		Assert.assertTrue("Incorrect Mode, case 2a", planLegCase2a.get(3).getMode().equals("pt"));
		Assert.assertTrue("Incorrect Mode, case 2a", planLegCase2a.get(4).getMode().contains("walk"));

		/**
		 * Case 2b: Agent starts at Link "300-301". This is within the drt service area. Agent is expected use drt as
		 * intermodal access to the middle pt stop. The plan should be walk -> drt -> walk -> pt -> walk
		 */
		List<PlanElement> planFullCase2b = controler.getScenario()
				.getPopulation()
				.getPersons()
				.get(Id.createPersonId("agent" + testAgents.get(2)))
				.getSelectedPlan()
				.getPlanElements();
		List<Leg> planLegCase2b = planFullCase2b.stream()
				.filter(pe -> pe instanceof Leg)
				.map(pe -> (Leg)pe)
				.collect(toList());
		Assert.assertTrue("Incorrect Mode, case 2b", planLegCase2b.get(0).getMode().contains("walk"));
		Assert.assertTrue("Incorrect Mode, case 2b", planLegCase2b.get(1).getMode().equals("drt"));
		Assert.assertTrue("Incorrect Mode, case 2b", planLegCase2b.get(2).getMode().contains("walk"));
		Assert.assertTrue("Incorrect Mode, case 2b", planLegCase2b.get(3).getMode().equals("pt"));
		Assert.assertTrue("Incorrect Mode, case 2b", planLegCase2b.get(4).getMode().contains("walk"));

		/**
		 * Case 2c: Agent starts at Link "550-551". This is within the drt3 service area. Agent is expected use drt3 as
		 * intermodal access to the middle pt stop. The plan should be walk -> drt3 -> walk -> pt -> walk
		 */
		List<PlanElement> planFullCase2c = controler.getScenario()
				.getPopulation()
				.getPersons()
				.get(Id.createPersonId("agent" + testAgents.get(3)))
				.getSelectedPlan()
				.getPlanElements();
		List<Leg> planLegCase2c = planFullCase2c.stream()
				.filter(pe -> pe instanceof Leg)
				.map(pe -> (Leg)pe)
				.collect(toList());
		Assert.assertTrue("Incorrect Mode, case 2c", planLegCase2c.get(0).getMode().contains("walk"));
		Assert.assertTrue("Incorrect Mode, case 2c", planLegCase2c.get(1).getMode().equals("drt3"));
		Assert.assertTrue("Incorrect Mode, case 2c", planLegCase2c.get(2).getMode().contains("walk"));
		Assert.assertTrue("Incorrect Mode, case 2c", planLegCase2c.get(3).getMode().equals("pt"));
		Assert.assertTrue("Incorrect Mode, case 2c", planLegCase2c.get(4).getMode().contains("walk"));

		/**
		 * Case 3a: Agent starts at Link "690-691". This is not within any drt service areas. Agent is expected to use
		 * transit_walk to get to his/her destination.
		 */
		List<PlanElement> planFullCase3a = controler.getScenario()
				.getPopulation()
				.getPersons()
				.get(Id.createPersonId("agent" + testAgents.get(4)))
				.getSelectedPlan()
				.getPlanElements();
		List<Leg> planLegCase3a = planFullCase3a.stream()
				.filter(pe -> pe instanceof Leg)
				.map(pe -> (Leg)pe)
				.collect(toList());
		Assert.assertTrue("Incorrect Mode, case 3a", planLegCase3a.get(0).getMode().equals("walk"));

		/**
		 * Case 3b: Agent starts at Link "800-801". This is within the drt2 service area. Agent is NOT expected to utilize
		 * drt2, since s/he is on a pt trip, and can only use drt as access/egress to a pt stop. Therefore, the agent is
		 * agent is expected to use transit_walk to get to his/her destination.
		 */
		List<PlanElement> planFullCase3b = controler.getScenario()
				.getPopulation()
				.getPersons()
				.get(Id.createPersonId("agent" + testAgents.get(5)))
				.getSelectedPlan()
				.getPlanElements();
		List<Leg> planLegCase3b = planFullCase3b.stream()
				.filter(pe -> pe instanceof Leg)
				.map(pe -> (Leg)pe)
				.collect(toList());
		Assert.assertTrue("Incorrect Mode, case 3b", planLegCase3b.get(0).getMode().equals("walk"));

	}

	@Test
	public void intermodalAccessEgressPicksWrongVariant() {
		// outdated comment:
		// this test fails because it picks a
		//    drt-nonNetworkWalk-nonNetworkWalk-drt
		//  trip over a faster
		//    drt-nonNetworkWalk-pt-nonNetworkWalk-drt
		// trip.  Commenting out the method
		//    handleTransfers(...)
		// in SwissRailRaptorCore
		//         if (hasIntermodalAccess) {
		//            // allow transfering from the initial stop to another one if we have intermodal access,
		//            // as not all stops might be intermodal
		//            handleTransfers(true, parameters);
		//        }
		// makes it pass.  I have no idea why, or if this would be a good direction to go for a fix.  kai, jul'19

		// does now work with these lines of code in SwissRailRaptorCore (which solve problems in other tests) gleich, aug'19

		Config config = PtAlongALineTest.createConfig(utils.getOutputDirectory());

		// === GBL: ===

		config.controler().setLastIteration(0);

		// === ROUTER: ===

		config.plansCalcRoute().setAccessEgressType(PlansCalcRouteConfigGroup.AccessEgressType.accessEgressModeToLink);

		config.qsim().setVehiclesSource(QSimConfigGroup.VehiclesSource.modeVehicleTypesFromVehiclesData);
		// (as of today, will also influence router. kai, jun'19)

		config.plansCalcRoute().setNetworkModes(new HashSet<>(Arrays.asList(TransportMode.drt, "drt2")));

		// set up walk2 so we don't use faulty walk in raptor:
		config.plansCalcRoute().addModeRoutingParams(new ModeRoutingParams("walk2").setTeleportedModeSpeed(5. / 3.6));

		config.plansCalcRoute()
				.addModeRoutingParams(new ModeRoutingParams(TransportMode.walk).setTeleportedModeSpeed(0.));
		// (when specifying "walk2", all default routing params are cleared.  However, swiss rail raptor needs "walk" to function. kai, feb'20)

		// === RAPTOR: ===
		{
			SwissRailRaptorConfigGroup configRaptor = ConfigUtils.addOrGetModule(config,
					SwissRailRaptorConfigGroup.class);
			configRaptor.setUseIntermodalAccessEgress(true);

			// "walk":
			configRaptor.addIntermodalAccessEgress(new IntermodalAccessEgressParameterSet().setMode("walk2")
					.setMaxRadius(1000000)
					.setInitialSearchRadius(1000000)
					.setSearchExtensionRadius(10000));
			// (in principle, walk as alternative to drt will not work, since drt is always faster.  Need to give the ASC to the router!  However, with
			// the reduced drt network we should be able to see differentiation.)

			// when we constructed this test, this did not work with "walk" since sbb raptor treats the walk mode in some special way.
			// don't know if this got cleaned up.  kai, feb'20

			// drt
			configRaptor.addIntermodalAccessEgress(new IntermodalAccessEgressParameterSet().setMode(TransportMode.drt)
					.setMaxRadius(1000000)
					.setInitialSearchRadius(1000000)
					.setSearchExtensionRadius(10000));

			if (drt2) {
				configRaptor.addIntermodalAccessEgress(new IntermodalAccessEgressParameterSet().setMode("drt2")
						.setMaxRadius(1000000)
						.setInitialSearchRadius(1000000)
						.setSearchExtensionRadius(10000));

				//				paramSetDrt2.setPersonFilterAttribute( null );
				//				paramSetDrt2.setStopFilterAttribute( null );
			}
		}

		// === SCORING: ===
		{
			double margUtlTravPt = config.planCalcScore()
					.getModes()
					.get(TransportMode.pt)
					.getMarginalUtilityOfTraveling();
			config.planCalcScore()
					.addModeParams(new ModeParams(TransportMode.drt).setMarginalUtilityOfTraveling(margUtlTravPt));
			config.planCalcScore().addModeParams(new ModeParams("drt2").setMarginalUtilityOfTraveling(margUtlTravPt));
			config.planCalcScore().addModeParams(new ModeParams("walk2").setMarginalUtilityOfTraveling(margUtlTravPt));
		}
		// === QSIM: ===

		config.qsim().setSimStarttimeInterpretation(QSimConfigGroup.StarttimeInterpretation.onlyUseStarttime);
		// yy why?  kai, jun'19

		config.qsim().setMainModes(Arrays.asList(TransportMode.car, TransportMode.drt, "drt2"));
		// yyyy buses use the car network and so that needs to be defined as network mode.   !!!! :-( :-(

		// === VSP: ===

		config.vspExperimental().setVspDefaultsCheckingLevel(VspExperimentalConfigGroup.VspDefaultsCheckingLevel.warn);

		// ### SCENARIO: ###

		Scenario scenario = new PtAlongALineFixture().createScenario(config, 100);

		PtAlongALineTest.addModeToAllLinksBtwnGivenNodes(scenario.getNetwork(), 0, 400, TransportMode.drt);
		PtAlongALineTest.addModeToAllLinksBtwnGivenNodes(scenario.getNetwork(), 600, 1000, "drt2");

		// The following is for the _router_, not the qsim!  kai, jun'19
		VehiclesFactory vf = scenario.getVehicles().getFactory();
		if (drt2) {
			scenario.getVehicles()
					.addVehicleType(
							vf.createVehicleType(Id.create("drt2", VehicleType.class)).setMaximumVelocity(25. / 3.6));
		}
		scenario.getVehicles()
				.addVehicleType(vf.createVehicleType(Id.create(TransportMode.drt, VehicleType.class))
						.setMaximumVelocity(25. / 3.6));

		scenario.getVehicles()
				.addVehicleType(vf.createVehicleType(Id.create(TransportMode.car, VehicleType.class))
						.setMaximumVelocity(100. / 3.6));
		// (does not work without; I don't really know why. kai)

		scenario.getPopulation().getPersons().values().removeIf(person -> !person.getId().toString().equals("3"));

		// ### CONTROLER: ###

		Controler controler = new Controler(scenario);

		// This will start otfvis.  Comment in if desired.
		//		controler.addOverridingModule( new OTFVisLiveModule() );

		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				this.addControlerListenerBinding().toInstance(new IterationEndsListener() {
					@Inject
					private Population population;

					@Override
					public void notifyIterationEnds(IterationEndsEvent event) {
						for (Person person : population.getPersons().values()) {

							// output to help with debugging:
							log.warn("");
							log.warn("selected plan for personId=" + person.getId());
							for (PlanElement planElement : person.getSelectedPlan().getPlanElements()) {
								log.warn(planElement);
							}
							log.warn("");

							// the trip should contain a true pt leg but does not:
							List<Leg> legs = TripStructureUtils.getLegs(person.getSelectedPlan());
							boolean problem = true;
							for (Leg leg : legs) {
								if (TransportMode.pt.equals(leg.getMode())) {
									problem = false;
									break;
								}
							}
							Assert.assertFalse(problem);

						}
					}
				});
			}
		});

		controler.run();
	}

	@Test
	@Ignore // this test is failing because raptor treats "walk" in a special way.  kai, jul'19
	public void networkWalkDoesNotWorkWithRaptor() {
		// test fails with null pointer exception

		Config config = PtAlongALineTest.createConfig(utils.getOutputDirectory());

		// === GBL: ===

		config.controler().setLastIteration(0);

		// === ROUTER: ===

		config.plansCalcRoute().setAccessEgressType(AccessEgressType.accessEgressModeToLink);

		config.qsim().setVehiclesSource(QSimConfigGroup.VehiclesSource.modeVehicleTypesFromVehiclesData);
		// (as of today, will also influence router. kai, jun'19)

		// remove teleportation walk router:
		config.plansCalcRoute().removeModeRoutingParams(TransportMode.walk);

		// add network walk router:
		Set<String> networkModes = new HashSet<>(config.plansCalcRoute().getNetworkModes());
		networkModes.add(TransportMode.walk);
		config.plansCalcRoute().setNetworkModes(networkModes);

		// === RAPTOR: ===
		{
			SwissRailRaptorConfigGroup configRaptor = ConfigUtils.addOrGetModule(config,
					SwissRailRaptorConfigGroup.class);

			configRaptor.setUseIntermodalAccessEgress(true);
			{
				// Xxx
				IntermodalAccessEgressParameterSet paramSetXxx = new IntermodalAccessEgressParameterSet();
				paramSetXxx.setMode(TransportMode.walk);
				paramSetXxx.setMaxRadius(1000000);
				configRaptor.addIntermodalAccessEgress(paramSetXxx);
			}

		}

		// ### SCENARIO: ###

		Scenario scenario = new PtAlongALineFixture().createScenario(config, 100);

		PtAlongALineTest.addModeToAllLinksBtwnGivenNodes(scenario.getNetwork(), 0, 1000, TransportMode.walk);

		// The following is in particular for the _router_, not the qsim!  kai, jun'19
		VehiclesFactory vf = scenario.getVehicles().getFactory();
		{
			VehicleType vehType = vf.createVehicleType(Id.create(TransportMode.walk, VehicleType.class));
			vehType.setMaximumVelocity(4. / 3.6);
			scenario.getVehicles().addVehicleType(vehType);
		}

		// ### CONTROLER: ###

		Controler controler = new Controler(scenario);

		// This will start otfvis.  Comment out if not needed.
		//		controler.addOverridingModule( new OTFVisLiveModule() );

		controler.run();
	}

}
