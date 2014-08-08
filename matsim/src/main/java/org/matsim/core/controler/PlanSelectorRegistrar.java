/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * PlanSelectorRegistrar.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2014 by the members listed in the COPYING, *
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

package org.matsim.core.controler;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.replanning.selectors.*;

public class PlanSelectorRegistrar {

    private PlanSelectorFactoryRegister register = new PlanSelectorFactoryRegister();

    public PlanSelectorRegistrar() {
        register.register("WorstPlanSelector", new PlanSelectorFactory() {
            @Override
            public WorstPlanForRemovalSelector createPlanSelector(Scenario scenario) {
                return new WorstPlanForRemovalSelector();
            }
        });

        register.register("SelectRandom", new PlanSelectorFactory() {
            @Override
            public GenericPlanSelector createPlanSelector(Scenario scenario) {
                return new RandomPlanSelector();
            }
        });

        register.register("SelectExpBeta", new PlanSelectorFactory() {

            @Override
            public GenericPlanSelector createPlanSelector(Scenario scenario) {
                return new ExpBetaPlanSelector( - scenario.getConfig().planCalcScore().getBrainExpBeta());
            }
        });

        register.register("ChangeExpBeta", new PlanSelectorFactory() {
            @Override
            public GenericPlanSelector createPlanSelector(Scenario scenario) {
                return new ExpBetaPlanChanger( - scenario.getConfig().planCalcScore().getBrainExpBeta());
            }
        });

        register.register("PathSizeLogitSelector", new PlanSelectorFactory() {
            @Override
            public GenericPlanSelector createPlanSelector(Scenario scenario) {
                return new PathSizeLogitSelector(scenario.getConfig().planCalcScore().getPathSizeLogitBeta(), -scenario.getConfig().planCalcScore().getBrainExpBeta(),
                        scenario.getNetwork());
            }
        });
    }

    public PlanSelectorFactoryRegister getFactoryRegister() {
        return register;
    }

}
