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

package org.matsim.freight.logistics;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.HasPlansAndId;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.components.QSimComponentsConfigGroup;
import org.matsim.core.replanning.GenericPlanStrategy;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.replanning.selectors.PlanSelector;
import org.matsim.freight.carriers.FreightCarriersConfigGroup;
import org.matsim.freight.carriers.controller.*;
import org.matsim.freight.logistics.analysis.LspScoreStatsModule;

public class LSPModule extends AbstractModule {
  private static final Logger log = LogManager.getLogger(LSPModule.class);

  @Override
  public void install() {
    FreightCarriersConfigGroup freightConfig =
        ConfigUtils.addOrGetModule(getConfig(), FreightCarriersConfigGroup.class);

    bind(LSPControllerListener.class).in(Singleton.class);
    addControlerListenerBinding().to(LSPControllerListener.class);

    install(new CarrierModule());
    install(new LspScoreStatsModule());

    // this switches on certain qsim components:
    QSimComponentsConfigGroup qsimComponents =
        ConfigUtils.addOrGetModule(getConfig(), QSimComponentsConfigGroup.class);
    List<String> abc = qsimComponents.getActiveComponents();
    abc.add(FreightAgentSource.COMPONENT_NAME);
    switch (freightConfig.getTimeWindowHandling()) {
      case ignore:
        break;
      case enforceBeginnings:
        ////				abc.add( WithinDayActivityReScheduling.COMPONENT_NAME );
        log.warn(
            "LSP has never hedged against time window openings; this is probably wrong; but I don't know what to do ...");
        //				break;
      default:
        throw new IllegalStateException(
            "Unexpected value: " + freightConfig.getTimeWindowHandling());
    }
    qsimComponents.setActiveComponents(abc);

    // this installs qsim components, which are switched on (or not) via the above syntax:
    this.installQSimModule(
        new AbstractQSimModule() {
          @Override
          protected void configureQSim() {
            this.bind(FreightAgentSource.class).in(Singleton.class);
            this.addQSimComponentBinding(FreightAgentSource.COMPONENT_NAME)
                .to(FreightAgentSource.class);
            switch (freightConfig.getTimeWindowHandling()) {
              case ignore:
                break;
              case enforceBeginnings:
                ////
                //	this.addQSimComponentBinding(WithinDayActivityReScheduling.COMPONENT_NAME).to(
                // WithinDayActivityReScheduling.class );
                log.warn(
                    "LSP has never hedged against time window openings; this is probably wrong; but I don't know what to do ...");
                //						break;
              default:
                throw new IllegalStateException(
                    "Unexpected value: " + freightConfig.getTimeWindowHandling());
            }
          }
        });

    // the scorers are necessary to run a zeroth iteration to the end:
    bind(LSPScorerFactory.class).to(LSPScoringFunctionFactoryDummyImpl.class);

    // for iterations, one needs to replace the following with something meaningful.  If nothing
    // else, there are "empty implementations" that do nothing.  kai, jul'22
    bind(LSPStrategyManager.class).toProvider(() -> null);

    this.addControlerListenerBinding().to(DumpLSPPlans.class);
  }

  private static class LSPScoringFunctionFactoryDummyImpl implements LSPScorerFactory {
    @Override
    public LSPScorer createScoringFunction() {
      return new LSPScorer() {
        @Override
        public double getScoreForCurrentPlan() {
          return Double.NEGATIVE_INFINITY;
        }

        @Override
        public void setEmbeddingContainer(LSP pointer) {}
      };
    }
  }

  public static final class LSPStrategyManagerEmptyImpl implements LSPStrategyManager {

    @Override
    public void addStrategy(
        GenericPlanStrategy<LSPPlan, LSP> strategy, String subpopulation, double weight) {
      throw new RuntimeException("not implemented");
    }

    @Override
    public void run(
        Iterable<? extends HasPlansAndId<LSPPlan, LSP>> persons,
        int iteration,
        ReplanningContext replanningContext) {
      log.warn("Running iterations without a strategy may lead to unclear results."); // "run" is
      // possible, but will not do anything. kai, jul'22
    }

    @Override
    public void setMaxPlansPerAgent(int maxPlansPerAgent) {
      throw new RuntimeException("not implemented");
    }

    @Override
    public void addChangeRequest(int iteration, GenericPlanStrategy<LSPPlan, LSP> strategy, String subpopulation, double newWeight) {
      throw new RuntimeException("not implemented");
    }

    @Override
    public void setPlanSelectorForRemoval(PlanSelector<LSPPlan, LSP> planSelector) {
      throw new RuntimeException("not implemented");
    }

    @Override
    public List<GenericPlanStrategy<LSPPlan, LSP>> getStrategies(String subpopulation) {
      throw new RuntimeException("not implemented");
    }

    @Override
    public List<Double> getWeights(String subpopulation) {
      throw new RuntimeException("not implemented");
    }
  }

  public static final class DumpLSPPlans implements BeforeMobsimListener {
    @Inject Scenario scenario;

    @Override
    public void notifyBeforeMobsim(BeforeMobsimEvent event) {
      LSPs lsps = LSPUtils.getLSPs(scenario);
      for (LSP lsp : lsps.getLSPs().values()) {
        log.info("Dumping plan(s) of [LSP={}] ; [No of plans={}]", lsp.getId(), lsp.getPlans().size());
        for (LSPPlan plan : lsp.getPlans()) {
          log.info("[LSPPlan: {}]", plan.toString());
        }
        log.info("Plan(s) of [LSP={}] dumped.", lsp.getId());
      }
    }
  }
}
