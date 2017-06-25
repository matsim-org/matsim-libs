/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package playground.agarwalamit.opdyts;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import floetteroed.opdyts.DecisionVariable;
import floetteroed.opdyts.DecisionVariableRandomizer;
import floetteroed.opdyts.ObjectiveFunction;
import floetteroed.opdyts.convergencecriteria.ConvergenceCriterion;
import floetteroed.opdyts.convergencecriteria.FixedIterationNumberConvergenceCriterion;
import floetteroed.opdyts.searchalgorithms.RandomSearch;
import floetteroed.opdyts.searchalgorithms.SelfTuner;
import opdytsintegration.MATSimSimulator2;
import opdytsintegration.MATSimStateFactory;
import opdytsintegration.car.DifferentiatedLinkOccupancyAnalyzer;
import opdytsintegration.pt.PTOccupancyAnalyzer;
import opdytsintegration.utils.MATSimConfiguredFactories;
import opdytsintegration.utils.OpdytsConfigGroup;
import opdytsintegration.utils.TimeDiscretization;
import org.matsim.api.core.v01.Scenario;

/**
 * Created by amit on 15.06.17.
 */

public class MATSimOpdytsIntegrationRunner<U extends DecisionVariable>  {

    private final MATSimConfiguredFactories delegate;

    // probably, cant inject scenario because it is required before (MATSim) controler
    public MATSimOpdytsIntegrationRunner(final Scenario scenario){
        this.delegate = new MATSimConfiguredFactories((OpdytsConfigGroup)scenario.getConfig().getModules().get(OpdytsConfigGroup.GROUP_NAME));

        this.timeDiscretization = this.delegate.newTimeDiscretization();
        this.convergenceCriterion = this.delegate.newFixedIterationNumberConvergenceCriterion();
        this.selfTuner = this.delegate.newSelfTuner();
        this.scenario = scenario;
    }

    private TimeDiscretization timeDiscretization;
    private SelfTuner selfTuner;
    private FixedIterationNumberConvergenceCriterion convergenceCriterion;
    private final Scenario scenario;

    private MATSimSimulator2<U> matSimSimulator2;

    // trying to mimize the opdyts infrastructure outside this Class
    @Deprecated
    public TimeDiscretization newTimeDiscretization() {
        throw new UnsupportedOperationException("not implmented yet.");
    }

    @Deprecated
    public SelfTuner newSelfTuner() {
        throw new UnsupportedOperationException("not implmented yet.");
    }

    @Deprecated
    public RandomSearch newRandomSearch(MATSimSimulator2 matsim, DecisionVariableRandomizer randomizer, DecisionVariable initialDecisionVariable, ConvergenceCriterion convergenceCriterion, ObjectiveFunction objectiveFunction) {
        throw new UnsupportedOperationException("not implmented yet.");
    }

    @Deprecated
    public FixedIterationNumberConvergenceCriterion newFixedIterationNumberConvergenceCriterion() {
        throw new UnsupportedOperationException("not implmented yet.");
    }

    public MATSimSimulator2<U> newMATSimSimulator(final MATSimStateFactory<U> stateFactory) {
        this.matSimSimulator2 = new MATSimSimulator2<>(stateFactory, scenario, this.timeDiscretization);

        // the name is not necessarily exactly same as network modes in MATSim PlansCalcRouteConfigGroup.
        // Here, this means, which needs to be counted on the links.
        // cant take network modes from PlansCalcRouteConfigGroup because this may have additional modes in there
        // however, this must be same as analyzeModes in TravelTimeCalculatorConfigGroup
        Set<String> networkModes = new HashSet<>(scenario.getConfig().qsim().getMainModes());

        // add for network modes
        if (networkModes.size() > 0.) {
            matSimSimulator2.addSimulationStateAnalyzer(new DifferentiatedLinkOccupancyAnalyzer.Provider(this.timeDiscretization, networkModes,
                    new LinkedHashSet<>(scenario.getNetwork().getLinks().keySet())));
        }

        // add for non-network modes
//        Set<String> teleportationModes =  new HashSet<>(scenario.getConfig().plansCalcRoute().getModeRoutingParams().keySet());
//        if (teleportationModes.size()>0) {
            //TODO : dont have information about zones here
//            new TeleportationModeOccupancyAnalyzerFactory(timeDiscretization, teleportationModes, relevantZones);
//        }

        // add for pt as network modes
        if (scenario.getConfig().transit().isUseTransit()) {
            matSimSimulator2.addSimulationStateAnalyzer(new PTOccupancyAnalyzer.Provider(this.timeDiscretization,
                    new HashSet<>(scenario.getTransitSchedule().getFacilities().keySet())) );
        }

        return matSimSimulator2;
    }

    public void run(final DecisionVariableRandomizer randomizer,
                    final DecisionVariable initialDecisionVariable,
                    final ObjectiveFunction objectiveFunction) {
        RandomSearch randomSearch = delegate.newRandomSearch(matSimSimulator2,
                randomizer,
                initialDecisionVariable,
                convergenceCriterion,
                objectiveFunction);

        OpdytsConfigGroup opdytsConfigGroup = (OpdytsConfigGroup) scenario.getConfig().getModules().get(OpdytsConfigGroup.GROUP_NAME);

        if ( Double.isFinite(opdytsConfigGroup.getUniformityGapWeight()) &&  Double.isFinite( opdytsConfigGroup.getEquilibriumGapWeight() ) ) {
            randomSearch.run(opdytsConfigGroup.getUniformityGapWeight(), opdytsConfigGroup.getEquilibriumGapWeight(), selfTuner);
        } else {
            randomSearch.run(selfTuner);
        }
    }

    public TimeDiscretization getTimeDiscretization() {
        return this.timeDiscretization;
    }
}
