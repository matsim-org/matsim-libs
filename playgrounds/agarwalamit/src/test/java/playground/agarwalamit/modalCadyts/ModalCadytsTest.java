/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.agarwalamit.modalCadyts;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Provider;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.cadyts.general.CadytsScoring;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ControlerConfigGroup.MobsimType;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup.VehiclesSource;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.ControlerDefaultsModule;
import org.matsim.core.controler.ControlerI;
import org.matsim.core.controler.Injector;
import org.matsim.core.controler.NewControlerModule;
import org.matsim.core.controler.corelisteners.ControlerDefaultCoreListenersModule;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.qsim.ActivityEngine;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.TeleportationEngine;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.agents.DefaultAgentFactory;
import org.matsim.core.mobsim.qsim.agents.PopulationAgentSource;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngine;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.scenario.ScenarioByConfigModule;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.CharyparNagelActivityScoring;
import org.matsim.core.scoring.functions.CharyparNagelAgentStuckScoring;
import org.matsim.core.scoring.functions.CharyparNagelLegScoring;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;
import org.matsim.core.scoring.functions.CharyparNagelScoringParametersForPerson;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;

import playground.agarwalamit.mixedTraffic.counts.MultiModeCountsControlerListener;
import playground.agarwalamit.multiModeCadyts.CountsInserter;
import playground.agarwalamit.multiModeCadyts.ModalCadytsContext;
import playground.agarwalamit.multiModeCadyts.ModalCountsReader;
import playground.agarwalamit.multiModeCadyts.ModalLink;

/**
 * amit after CadytsCarIT in cadyts contrib.
 */
public class ModalCadytsTest {

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public final void testCalibrationAsScoring() throws IOException {

		final double beta=30. ;
		final int lastIteration = 20 ;

		String inputDir = this.utils.getClassInputDirectory();
		String outputDir = this.utils.getOutputDirectory();

		final Config config = createTestConfig(inputDir, outputDir);

		List<String> mainModes = Arrays.asList("car","bike");
		config.qsim().setMainModes(mainModes );

//		config.qsim().setVehiclesSource(VehiclesSource.modeVehicleTypesFromVehiclesData);
		// I switched this off for this piece of code since the vehicles are added as mode vehicles directly into the agent source.  
		// modeVehicleTypesFromVehiclesData could be achieved by instead inserting them into the vehicles container
		// from the scenario.  This might be preferred, but I don't want to modify the test here.  kai, nov'16

		config.controler().setLastIteration(lastIteration);

		config.planCalcScore().setBrainExpBeta(beta);

		StrategySettings strategySettings = new StrategySettings() ;
		strategySettings.setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta.toString());
		strategySettings.setWeight(1.0);
		config.strategy().addStrategySettings(strategySettings);

		// ===

		CountsInserter jcg = new CountsInserter();		
		jcg.processInputFile( inputDir+"/countsCarBike.txt" );
		jcg.run();
		Counts<ModalLink> modalLinkCounts = jcg.getModalLinkCounts();
		Map<String, ModalLink> modalLinkContainer = jcg.getModalLinkContainer();

		com.google.inject.Injector injector = Injector.createInjector(config, new AbstractModule() {
			@Override
			public void install() {
				install(new NewControlerModule());
				install(new ControlerDefaultCoreListenersModule());
				install(AbstractModule.override(Collections.singleton(new ControlerDefaultsModule()), new AbstractModule() {
					@Override
					public void install() {

						bindMobsim().toProvider(MultipleModeVehiclesQSimFactory.class);

						bind(Key.get(new TypeLiteral<Counts<ModalLink>>(){}, Names.named("calibration"))).toInstance(modalLinkCounts);
						bind(Key.get(new TypeLiteral<Map<String,ModalLink>>(){})).toInstance(modalLinkContainer);

						bind(ModalCadytsContext.class).asEagerSingleton();
						addControlerListenerBinding().to(ModalCadytsContext.class);


						bindScoringFunctionFactory().toInstance(new ScoringFunctionFactory() {
							@Inject private CharyparNagelScoringParametersForPerson parameters;
							@Inject private Network network;
							@Inject ModalCadytsContext cadytsContext;
							@Override
							public ScoringFunction createNewScoringFunction(Person person) {
								final CharyparNagelScoringParameters params = parameters.getScoringParameters(person);

								SumScoringFunction scoringFunctionAccumulator = new SumScoringFunction();
								scoringFunctionAccumulator.addScoringFunction(new CharyparNagelLegScoring(params, network));
								scoringFunctionAccumulator.addScoringFunction(new CharyparNagelActivityScoring(params)) ;
								scoringFunctionAccumulator.addScoringFunction(new CharyparNagelAgentStuckScoring(params));

								final CadytsScoring<ModalLink> scoringFunction = new CadytsScoring<>(person.getSelectedPlan(), config, cadytsContext);
								final double cadytsScoringWeight = beta*30.;
								scoringFunction.setWeightOfCadytsCorrection(cadytsScoringWeight) ;
								scoringFunctionAccumulator.addScoringFunction(scoringFunction );

								return scoringFunctionAccumulator;
							}
						});

						this.addControlerListenerBinding().to(MultiModeCountsControlerListener.class);
					}
				}));
				install(new ScenarioByConfigModule());
			}
		});
		ControlerI controler = injector.getInstance(ControlerI.class);
		controler.run();

		//scenario data  test
		Scenario scenario = injector.getInstance(Scenario.class);
		Assert.assertEquals("Different number of links in network.", scenario.getNetwork().getLinks().size() , 23 );
		Assert.assertEquals("Different number of nodes in network.", scenario.getNetwork().getNodes().size() , 15 );

		Assert.assertNotNull("Population is null.", scenario.getPopulation());

		Assert.assertEquals("Num. of persons in population is wrong.", scenario.getPopulation().getPersons().size(), 9);
		Assert.assertEquals("Scale factor is wrong.", scenario.getConfig().counts().getCountsScaleFactor(), 1.0, MatsimTestUtils.EPSILON);

		//counts
		Assert.assertEquals("Count file is wrong.", scenario.getConfig().counts().getCountsFileName(), inputDir + "countsCarBike.xml");

		// check for car
		String mode = mainModes.get(0);
		Count<ModalLink> count =  modalLinkCounts.getCount( Id.create( mode.concat(ModalLink.getModeLinkSplittor()).concat("19") , ModalLink.class ) );
		Assert.assertEquals("CsId is wrong.", count.getCsLabel() , "link_19");
		Assert.assertEquals("Volume of hour 6 is wrong", count.getVolume(7).getValue(), 5.0 , MatsimTestUtils.EPSILON);
		Assert.assertEquals("Max count volume is wrong.", count.getMaxVolume().getValue(), 5.0 , MatsimTestUtils.EPSILON);

		String outCounts = outputDir + "ITERS/it." + lastIteration + "/" + lastIteration + ".multiMode_hourlyCounts.txt";
		ModalCountsReader reader = new ModalCountsReader(outCounts);
		double[] simValues;
		double realValue;

		Id<Link> locId11 = Id.create(11, Link.class);
		simValues = reader.getSimulatedValues(locId11, mode);
		realValue = getCountRealValue(modalLinkCounts, locId11, mode, 7); //6-7 am
		Assert.assertEquals("Volume of hour 6 is wrong", 0.0, simValues[6], MatsimTestUtils.EPSILON);  
		Assert.assertEquals("Volume of hour 6 is wrong", 0.0, realValue, MatsimTestUtils.EPSILON);

		Id<Link> locId12 = Id.create("12", Link.class);
		simValues = reader.getSimulatedValues(locId12, mode);
		realValue = getCountRealValue(modalLinkCounts, locId12, mode, 7);
		Assert.assertEquals("Volume of hour 6 is wrong", 0.0, simValues[6], MatsimTestUtils.EPSILON);
		Assert.assertEquals("Volume of hour 6 is wrong", 0.0, realValue , MatsimTestUtils.EPSILON);

		Id<Link> locId19 = Id.create("19", Link.class);
		simValues = reader.getSimulatedValues(locId19, mode);
		realValue = getCountRealValue(modalLinkCounts, locId19, mode, 7); 
		Assert.assertEquals("Volume of hour 6 is wrong", 5.0, simValues[6], MatsimTestUtils.EPSILON); 
		Assert.assertEquals("Volume of hour 6 is wrong", 5.0, realValue, MatsimTestUtils.EPSILON);

		Id<Link> locId21 = Id.create("21", Link.class);
		simValues = reader.getSimulatedValues(locId21, mode);
		realValue = getCountRealValue(modalLinkCounts, locId21, mode, 7);
		Assert.assertEquals("Volume of hour 6 is wrong", 5.0, simValues[6], MatsimTestUtils.EPSILON);
		Assert.assertEquals("Volume of hour 6 is wrong", 5.0, realValue, MatsimTestUtils.EPSILON);

		// check for bike 
		mode = mainModes.get(1);
		count =  modalLinkCounts.getCount( Id.create( mode.concat(ModalLink.getModeLinkSplittor()).concat("11") , ModalLink.class ) );
		//		Assert.assertEquals("Occupancy counts description is wrong", modalLinkCounts.getDescription(), "counts values for equil net");
		Assert.assertEquals("CsId is wrong.", count.getCsLabel() , "link_11");
		Assert.assertEquals("Volume of hour 6 is wrong", count.getVolume(7).getValue(), 4.0 , MatsimTestUtils.EPSILON);
		Assert.assertEquals("Max count volume is wrong.", count.getMaxVolume().getValue(), 4.0 , MatsimTestUtils.EPSILON);

		outCounts = outputDir + "ITERS/it." + lastIteration + "/" + lastIteration + ".multiMode_hourlyCounts.txt";
		reader = new ModalCountsReader(outCounts);

		locId11 = Id.create(11, Link.class);
		simValues = reader.getSimulatedValues(locId11, mode);
		realValue = getCountRealValue(modalLinkCounts, locId11, mode, 7);
		Assert.assertEquals("Volume of hour 6 is wrong", 4.0, simValues[6], MatsimTestUtils.EPSILON);  
		Assert.assertEquals("Volume of hour 6 is wrong", 4.0, realValue, MatsimTestUtils.EPSILON);

		locId12 = Id.create(12, Link.class);
		simValues = reader.getSimulatedValues(locId12, mode);
		realValue = getCountRealValue(modalLinkCounts, locId12, mode, 7);
		Assert.assertEquals("Volume of hour 6 is wrong", 0.0, simValues[6], MatsimTestUtils.EPSILON);
		Assert.assertEquals("Volume of hour 6 is wrong", 0.0, realValue , MatsimTestUtils.EPSILON);

		locId19 = Id.create(19, Link.class);
		simValues = reader.getSimulatedValues(locId19, mode);
		realValue = getCountRealValue(modalLinkCounts, locId19, mode, 7); 
		Assert.assertEquals("Volume of hour 6 is wrong", 0.0, simValues[6], MatsimTestUtils.EPSILON); 
		Assert.assertEquals("Volume of hour 6 is wrong", 0.0, realValue, MatsimTestUtils.EPSILON);

		locId21 = Id.create(21, Link.class);
		simValues = reader.getSimulatedValues(locId21, mode);
		realValue = getCountRealValue(modalLinkCounts, locId21, mode, 8); // bike is slow; will enter link 21 after 7am
		Assert.assertEquals("Volume of hour 7 is wrong", 4.0, simValues[7], MatsimTestUtils.EPSILON);
		Assert.assertEquals("Volume of hour 7 is wrong", 4.0, realValue, MatsimTestUtils.EPSILON);
	}

	//--------------------------------------------------------------
	private double getCountRealValue(Counts<ModalLink> counts, Id<Link> linkId, String mode, int hour) {
		Count<ModalLink> count =  counts.getCount( Id.create( mode.concat(ModalLink.getModeLinkSplittor()).concat(linkId.toString()) , ModalLink.class ) );
		return count.getVolume(hour).getValue();
	}

	private static class MultipleModeVehiclesQSimFactory implements Provider<Mobsim> {

		@Inject Scenario scenario;
		@Inject EventsManager eventsManager;

		@Override
		public Mobsim get() {

			QSimConfigGroup conf = scenario.getConfig().qsim();
			if (conf == null) {
				throw new NullPointerException("There is no configuration set for the QSim. Please add the module 'qsim' to your config file.");
			}

			// construct the QSim:
			QSim qSim = new QSim(scenario, eventsManager);

			// add the activity engine:
			ActivityEngine activityEngine = new ActivityEngine(eventsManager, qSim.getAgentCounter());
			qSim.addMobsimEngine(activityEngine);
			qSim.addActivityHandler(activityEngine);

			// add the netsim engine:
			QNetsimEngine netsimEngine = new QNetsimEngine(qSim) ;
			qSim.addMobsimEngine(netsimEngine);
			qSim.addDepartureHandler(netsimEngine.getDepartureHandler());

			TeleportationEngine teleportationEngine = new TeleportationEngine(scenario, eventsManager);
			qSim.addMobsimEngine(teleportationEngine);

			AgentFactory agentFactory = new DefaultAgentFactory(qSim);

			PopulationAgentSource agentSource = new PopulationAgentSource(scenario.getPopulation(), agentFactory, qSim);
			Map<String, VehicleType> modeVehicleTypes = new HashMap<>();

			VehicleType car = VehicleUtils.getFactory().createVehicleType(Id.create("car", VehicleType.class));
			car.setMaximumVelocity(100.0/3.6);
			car.setPcuEquivalents(1.0);
			modeVehicleTypes.put("car", car);

			VehicleType bike = VehicleUtils.getFactory().createVehicleType(Id.create("bike", VehicleType.class));
			bike.setMaximumVelocity(50.0/3.6);
			bike.setPcuEquivalents(0.25);
			modeVehicleTypes.put("bike", bike);

			agentSource.setModeVehicleTypes(modeVehicleTypes);

			qSim.addAgentSource(agentSource);
			return qSim ;
		}
	}

	private static Config createTestConfig(String inputDir, String outputDir) {
		Config config = ConfigUtils.createConfig() ;
		config.global().setRandomSeed(4711) ;
		config.network().setInputFile(inputDir + "network.xml") ;
		config.plans().setInputFile(inputDir + "plans.xml") ;
		config.controler().setFirstIteration(1) ;
		config.controler().setLastIteration(10) ;
		config.controler().setOutputDirectory(outputDir) ;
		config.controler().setWriteEventsInterval(1) ;
		config.controler().setMobsim(MobsimType.qsim.toString()) ;
		config.qsim().setFlowCapFactor(1.) ;
		config.qsim().setStorageCapFactor(1.) ;
		config.qsim().setStuckTime(10.) ;
		config.qsim().setRemoveStuckVehicles(false) ;
		{
			ActivityParams params = new ActivityParams("h") ;
			config.planCalcScore().addActivityParams(params ) ;
			params.setTypicalDuration(12*60*60.) ;
		}
		{
			ActivityParams params = new ActivityParams("w") ;
			config.planCalcScore().addActivityParams(params ) ;
			params.setTypicalDuration(8*60*60.) ;
		}
		config.counts().setInputFile(inputDir + "countsCarBike.xml");
		return config;
	}
}