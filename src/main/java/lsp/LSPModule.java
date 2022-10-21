/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2022 by the members listed in the COPYING,        *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */

package lsp;

import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.HasPlansAndId;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.contrib.freight.FreightConfigGroup;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.Carriers;
import org.matsim.contrib.freight.controler.CarrierAgentTracker;
import org.matsim.contrib.freight.controler.CarrierScoringFunctionFactory;
import org.matsim.contrib.freight.controler.CarrierStrategyManager;
import org.matsim.contrib.freight.controler.FreightAgentSource;
import org.matsim.contrib.freight.usecases.chessboard.CarrierScoringFunctionFactoryImpl;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.components.QSimComponentsConfigGroup;
import org.matsim.core.replanning.GenericPlanStrategy;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.replanning.selectors.PlanSelector;
import org.matsim.core.scoring.ScoringFunction;

import java.util.List;


public class LSPModule extends AbstractModule {
	private static final Logger log = LogManager.getLogger(LSPModule.class);

//	private final FreightConfigGroup carrierConfig = new FreightConfigGroup();

	@Override
	public void install() {
		FreightConfigGroup freightConfig = ConfigUtils.addOrGetModule(getConfig(), FreightConfigGroup.class);

		bind(LSPControlerListener.class).in(Singleton.class);
		addControlerListenerBinding().to(LSPControlerListener.class);

		bind( CarrierAgentTracker.class ).in( Singleton.class );
		addEventHandlerBinding().to( CarrierAgentTracker.class );

		// this switches on certain qsim components:
		QSimComponentsConfigGroup qsimComponents = ConfigUtils.addOrGetModule(getConfig(), QSimComponentsConfigGroup.class);
		List<String> abc = qsimComponents.getActiveComponents();
		abc.add(FreightAgentSource.COMPONENT_NAME);
		switch (freightConfig.getTimeWindowHandling()) {
			case ignore:
				break;
//			case enforceBeginnings:
////				abc.add( WithinDayActivityReScheduling.COMPONENT_NAME );
//				log.warn("LSP has never hedged against time window openings; this is probably wrong; but I don't know what to do ...");
//				break;
			default:
				throw new IllegalStateException("Unexpected value: " + freightConfig.getTimeWindowHandling());
		}
		qsimComponents.setActiveComponents(abc);

		// this installs qsim components, which are switched on (or not) via the above syntax:
		this.installQSimModule(new AbstractQSimModule() {
			@Override
			protected void configureQSim() {
				this.bind(FreightAgentSource.class).in(Singleton.class);
				this.addQSimComponentBinding(FreightAgentSource.COMPONENT_NAME).to(FreightAgentSource.class);
				switch (freightConfig.getTimeWindowHandling()) {
					case ignore:
						break;
//					case enforceBeginnings:
////						this.addQSimComponentBinding(WithinDayActivityReScheduling.COMPONENT_NAME).to( WithinDayActivityReScheduling.class );
//						log.warn("LSP has never hedged against time window openings; this is probably wrong; but I don't know what to do ...");
//						break;
					default:
						throw new IllegalStateException("Unexpected value: " + freightConfig.getTimeWindowHandling());
				}
			}
		});

		// the scorers are necessary to run a zeroth iteration to the end:
//		bind( CarrierScoringFunctionFactory.class ).to( CarrierScoringFactoryDummyImpl.class );
		bind( CarrierScoringFunctionFactory.class ).to( CarrierScoringFunctionFactoryImpl.class );
		bind( LSPScorerFactory.class ).to( LSPScoringFunctionFactoryDummyImpl.class );

		// for iterations, one needs to replace the following with something meaningful.  If nothing else, there are "empty implementations" that do nothing.  kai, jul'22
		bind( CarrierStrategyManager.class ).toProvider( ()->null );
		bind( LSPStrategyManager.class ).toProvider( ()->null );

		this.addControlerListenerBinding().to( DumpLSPPlans.class );
	}

	@Provides Carriers provideCarriers(LSPControlerListener lspControlerListener ) {
		return lspControlerListener.getCarriersFromLSP();
	}

	private static class LSPScoringFunctionFactoryDummyImpl implements LSPScorerFactory{
		@Override public LSPScorer createScoringFunction( LSP lsp ){
			return new LSPScorer(){
				@Override public double getScoreForCurrentPlan(){
					return Double.NEGATIVE_INFINITY;
				}
				@Override public void setEmbeddingContainer( LSP pointer ){
				}
			};
		}
	}
	private static class CarrierScoringFactoryDummyImpl implements CarrierScoringFunctionFactory {
		@Override public ScoringFunction createScoringFunction( Carrier carrier ){
			return new ScoringFunction(){
				@Override public void handleActivity( Activity activity ){
				}
				@Override public void handleLeg( Leg leg ){
				}
				@Override public void agentStuck( double time ){
				}
				@Override public void addMoney( double amount ){
				}
				@Override public void addScore( double amount ){
				}
				@Override public void finish(){
				}
				@Override public double getScore(){
					return Double.NEGATIVE_INFINITY;
				}
				@Override public void handleEvent( Event event ){
				}
			};
		}
	}
	public static final class LSPStrategyManagerEmptyImpl implements LSPStrategyManager {

		@Override public void addStrategy( GenericPlanStrategy<LSPPlan, LSP> strategy, String subpopulation, double weight ){
			throw new RuntimeException( "not implemented" );
		}
		@Override public void run( Iterable<? extends HasPlansAndId<LSPPlan, LSP>> persons, int iteration, ReplanningContext replanningContext ){
			log.warn("Running iterations without a strategy may lead to unclear results.");// "run" is possible, but will not do anything. kai, jul'22
		}
		@Override public void setMaxPlansPerAgent( int maxPlansPerAgent ){
			throw new RuntimeException( "not implemented" );
		}
		@Override public void addChangeRequest( int iteration, GenericPlanStrategy<LSPPlan, LSP> strategy, String subpopulation, double newWeight ){
			throw new RuntimeException( "not implemented" );
		}
		@Override public void setPlanSelectorForRemoval( PlanSelector<LSPPlan, LSP> planSelector ){
			throw new RuntimeException( "not implemented" );
		}
		@Override public List<GenericPlanStrategy<LSPPlan, LSP>> getStrategies( String subpopulation ){
			throw new RuntimeException( "not implemented" );
		}
		@Override public List<Double> getWeights( String subpopulation ){
			throw new RuntimeException( "not implemented" );
		}
	}

	public static final class DumpLSPPlans implements BeforeMobsimListener {
		@Inject Scenario scenario;
		@Override public void notifyBeforeMobsim( BeforeMobsimEvent event ){
			LSPs lsps = LSPUtils.getLSPs( scenario );
			for( LSP lsp : lsps.getLSPs().values() ){
				log.warn("Dumping plan(s) of [LSP="+lsp.getId() + "] ; [No of plans=" + lsp.getPlans().size() + "]");
				for( LSPPlan plan : lsp.getPlans() ){
					log.warn( "[LSPPlan: " + plan.toString() + "]") ;
				}
				log.warn("Plan(s) of [LSP="+lsp.getId() + "] dumped.");
			}
		}
	}

}
