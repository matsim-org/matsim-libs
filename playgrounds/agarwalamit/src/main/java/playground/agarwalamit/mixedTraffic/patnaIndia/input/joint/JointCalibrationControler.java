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
package playground.agarwalamit.mixedTraffic.patnaIndia.input.joint;

import java.io.File;
import java.util.HashSet;
import java.util.Map;
import javax.inject.Inject;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.cadyts.general.CadytsConfigGroup;
import org.matsim.contrib.cadyts.general.CadytsScoring;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ModeParams;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ScoringParameterSet;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.ScenarioConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.*;
import org.matsim.core.utils.collections.CollectionUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.counts.Counts;
import playground.agarwalamit.analysis.StatsWriter;
import playground.agarwalamit.analysis.modalShare.ModalShareControlerListener;
import playground.agarwalamit.analysis.tripTime.ModalTravelTimeControlerListener;
import playground.agarwalamit.analysis.modalShare.ModalShareEventHandler;
import playground.agarwalamit.analysis.modalShare.ModalShareFromEvents;
import playground.agarwalamit.analysis.tripTime.ModalTravelTimeAnalyzer;
import playground.agarwalamit.analysis.tripTime.ModalTripTravelTimeHandler;
import playground.agarwalamit.mixedTraffic.counts.MultiModeCountsControlerListener;
import playground.agarwalamit.mixedTraffic.patnaIndia.scoring.PtFareEventHandler;
import playground.agarwalamit.mixedTraffic.patnaIndia.utils.PatnaPersonFilter;
import playground.agarwalamit.mixedTraffic.patnaIndia.utils.PatnaPersonFilter.PatnaUserGroup;
import playground.agarwalamit.mixedTraffic.patnaIndia.utils.PatnaUtils;
import playground.agarwalamit.multiModeCadyts.CountsInserter;
import playground.agarwalamit.multiModeCadyts.ModalCadytsContext;
import playground.agarwalamit.multiModeCadyts.ModalLink;

/**
 * @author amit
 */

public class JointCalibrationControler {

	private static String inputLocation = PatnaUtils.INPUT_FILES_DIR;
	private static boolean isUsingCadyts = true;

	private static final String CONFIG_FILE = "../../../../repos/runs-svn/patnaIndia/run108/jointDemand/calibration/"+PatnaUtils.PCU_2W.toString()+"pcu/input/config.xml.gz";
	private static String OUTPUT_DIR = "../../../../repos/runs-svn/patnaIndia/run108/jointDemand/calibration/"+PatnaUtils.PCU_2W.toString()+"pcu/c0/";

	public static void main(String[] args) {
		Config config = ConfigUtils.createConfig();

		config.travelTimeCalculator().setFilterModes(true);
		config.travelTimeCalculator().setAnalyzedModes(String.join(",", PatnaUtils.ALL_MAIN_MODES));

		if(args.length>0){
			String dir = "/net/ils4/agarwal/patnaIndia/run108/";
			inputLocation = dir+"/input/";
			ConfigUtils.loadConfig(config, args[0]);
			OUTPUT_DIR = dir+args [1];
			isUsingCadyts = Boolean.valueOf( args[2] );
			config.controler().setOutputDirectory( OUTPUT_DIR );

			config.planCalcScore().getModes().get("car").setConstant(Double.valueOf(args[3]));
			config.planCalcScore().getModes().get("bike").setConstant(Double.valueOf(args[4]));
			config.planCalcScore().getModes().get("motorbike").setConstant(Double.valueOf(args[5]));
			config.planCalcScore().getModes().get("pt").setConstant(Double.valueOf(args[6]));
			config.planCalcScore().getModes().get("walk").setConstant(Double.valueOf(args[7]));

		} else {
			// all utility parameters must be set in  input config file 
			ConfigUtils.loadConfig(config, CONFIG_FILE);
		}

		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setOutputDirectory(OUTPUT_DIR);

		Scenario sc = ScenarioUtils.loadScenario(config);

		// no vehicle info should be present if using VehiclesSource.modeVEhicleTypesFromVehiclesData
		if (!sc.getVehicles().getVehicles().isEmpty()) throw new RuntimeException("Only vehicle types should be loaded if vehicle source "+
				QSimConfigGroup.VehiclesSource.modeVehicleTypesFromVehiclesData +" is assigned.");
		sc.getConfig().qsim().setVehiclesSource(QSimConfigGroup.VehiclesSource.modeVehicleTypesFromVehiclesData);

		final Controler controler = new Controler(sc);
		controler.getConfig().controler().setDumpDataAtEnd(true);

		controler.addOverridingModule(new AbstractModule() { // plotting modal share over iterations
			@Override
			public void install() {
				this.bind(ModalShareEventHandler.class);
				this.addControlerListenerBinding().to(ModalShareControlerListener.class);

				this.bind(ModalTripTravelTimeHandler.class);
				this.addControlerListenerBinding().to(ModalTravelTimeControlerListener.class);

				this.addControlerListenerBinding().to(MultiModeCountsControlerListener.class);
			}
		});

		// adding pt fare system based on distance 
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				this.addEventHandlerBinding().to(PtFareEventHandler.class);
			}
		});

		// for above make sure that util_dist and monetary dist rate for pt are zero.
		ModeParams mp = controler.getConfig().planCalcScore().getModes().get("pt");
		mp.setMarginalUtilityOfDistance(0.0);
		mp.setMonetaryDistanceRate(0.0);

		// add cadyts or income-dependent scoring function only or both
		addScoringFunction(controler,config);
		
		controler.run();

		// delete unnecessary iterations folder here.
		int firstIt = controler.getConfig().controler().getFirstIteration();
		int lastIt = controler.getConfig().controler().getLastIteration();
		for (int index =firstIt+1; index <lastIt; index ++){
			String dirToDel = OUTPUT_DIR+"/ITERS/it."+index;
			Logger.getLogger(JointCalibrationControler.class).info("Deleting the directory "+dirToDel);
			IOUtils.deleteDirectoryRecursively(new File(dirToDel).toPath());
		}

		new File(OUTPUT_DIR+"/analysis/").mkdir();
		String outputEventsFile = OUTPUT_DIR+"/output_events.xml.gz";
		// write some default analysis

		String userGroup = PatnaUserGroup.urban.toString();
		ModalTravelTimeAnalyzer mtta = new ModalTravelTimeAnalyzer(outputEventsFile, userGroup, new PatnaPersonFilter());
		mtta.run();
		mtta.writeResults(OUTPUT_DIR+"/analysis/modalTravelTime_"+userGroup+".txt");

		ModalShareFromEvents msc = new ModalShareFromEvents(outputEventsFile, userGroup, new PatnaPersonFilter());
		msc.run();
		msc.writeResults(OUTPUT_DIR+"/analysis/modalShareFromEvents_"+userGroup+".txt");

		StatsWriter.run(OUTPUT_DIR);
	}

	private static void addScoringFunction(final Controler controler, final Config config){
		
		CountsInserter jcg = new CountsInserter();		
		jcg.processInputFile( inputLocation+"/raw/counts/urbanDemandCountsFile/innerCordon_excl_rckw_shpNetwork.txt" );
		jcg.processInputFile( inputLocation+"/raw/counts/externalDemandCountsFile/outerCordonData_allCounts_shpNetwork.txt" );
		jcg.run();

		Counts<ModalLink> modalLinkCounts = jcg.getModalLinkCounts();
		modalLinkCounts.setYear(2008);
		modalLinkCounts.setName("Patna_counts");
		
		Map<String, ModalLink> modalLinkContainer = jcg.getModalLinkContainer();

		String modes = CollectionUtils.setToString(new HashSet<>(PatnaUtils.EXT_MAIN_MODES));
		config.counts().setAnalyzedModes(modes);
		config.counts().setFilterModes(true);
		config.strategy().setMaxAgentPlanMemorySize(10);

		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bind(Key.get(new TypeLiteral<Counts<ModalLink>>(){}, Names.named("calibration"))).toInstance(modalLinkCounts);
				bind(Key.get(new TypeLiteral<Map<String,ModalLink>>(){})).toInstance(modalLinkContainer);

				bind(ModalCadytsContext.class).asEagerSingleton();
				addControlerListenerBinding().to(ModalCadytsContext.class);
			}
		});

		CadytsConfigGroup cadytsConfigGroup = ConfigUtils.addOrGetModule(config, CadytsConfigGroup.GROUP_NAME, CadytsConfigGroup.class);
		cadytsConfigGroup.setStartTime(0);
		cadytsConfigGroup.setEndTime(24*3600-1);

		// scoring function
		controler.setScoringFunctionFactory(new ScoringFunctionFactory() {
			final ScoringParametersForPerson parameters = new SubpopulationScoringParameters( controler.getScenario() );
			@Inject
             Network network;
			@Inject
             Population population;
			@Inject
             PlanCalcScoreConfigGroup planCalcScoreConfigGroup; // to modify the util parameters
			@Inject
             ScenarioConfigGroup scenarioConfig;
			@Inject
             ModalCadytsContext cContext;
			@Override
			public ScoringFunction createNewScoringFunction(Person person) {
				final ScoringParameters params = parameters.getScoringParameters( person );

				SumScoringFunction sumScoringFunction = new SumScoringFunction();
				sumScoringFunction.addScoringFunction(new CharyparNagelActivityScoring(params)) ;
				sumScoringFunction.addScoringFunction(new CharyparNagelAgentStuckScoring(params));
				
				final CadytsScoring<ModalLink> scoringFunction = new CadytsScoring<>(person.getSelectedPlan(), config, cContext);
				
				if(isUsingCadyts){
					final double cadytsScoringWeight = 15.0;
					scoringFunction.setWeightOfCadytsCorrection(cadytsScoringWeight) ;
					sumScoringFunction.addScoringFunction(scoringFunction );	
				}

				Double ratioOfInc = 1.0;

				if ( PatnaPersonFilter.isPersonBelongsToUrban(person.getId())) { // inc is not available for commuters and through traffic
					Double monthlyInc = (Double) population.getPersonAttributes().getAttribute(person.getId().toString(), PatnaUtils.INCOME_ATTRIBUTE);
					Double avgInc = PatnaUtils.MEADIAM_INCOME;
					ratioOfInc = avgInc/monthlyInc;
				}

				planCalcScoreConfigGroup.setMarginalUtilityOfMoney(ratioOfInc );				

				ScoringParameterSet scoringParameterSet = planCalcScoreConfigGroup.getScoringParameters( null ); // parameters set is same for all subPopulations 

				ScoringParameters.Builder builder = new ScoringParameters.Builder(
						planCalcScoreConfigGroup, scoringParameterSet, scenarioConfig);
				final ScoringParameters modifiedParams = builder.build();

				sumScoringFunction.addScoringFunction(new CharyparNagelLegScoring(modifiedParams, network));
				sumScoringFunction.addScoringFunction(new CharyparNagelMoneyScoring(modifiedParams));
				return sumScoringFunction;
			}
		}) ;
	}
}