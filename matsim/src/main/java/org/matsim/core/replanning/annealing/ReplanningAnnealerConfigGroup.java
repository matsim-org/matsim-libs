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

package org.matsim.core.replanning.annealing;

import java.util.EnumMap;
import java.util.Map;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ReflectiveConfigGroup;

/**
 * @author davig
 */

public class ReplanningAnnealerConfigGroup extends ReflectiveConfigGroup {

    public static final String GROUP_NAME = "ReplanningAnnealer";
    private static final String ACTIVATE_ANNEALING_MODULE = "activateAnnealingModule";
    private boolean activateAnnealingModule = false;

    public ReplanningAnnealerConfigGroup() {
        super(GROUP_NAME);
    }

    @StringGetter(ACTIVATE_ANNEALING_MODULE)
    public boolean isActivateAnnealingModule() {
        return activateAnnealingModule;
    }

    @StringSetter(ACTIVATE_ANNEALING_MODULE)
    public void setActivateAnnealingModule(boolean activateAnnealingModule) {
        this.activateAnnealingModule = activateAnnealingModule;
    }

    @Override
    public Map<String, String> getComments() {
        final Map<String, String> comments = super.getComments();
        comments.put(ACTIVATE_ANNEALING_MODULE, "Activate the scaling of replanning modules using an annealing approach rather than fixed rates.");
        return comments;
    }

    @Override
    public ConfigGroup createParameterSet(final String type) {
        if (AnnealingVariable.GROUP_NAME.equals(type)) {
            return new AnnealingVariable();
        }
        throw new IllegalArgumentException(type);
    }

    @Override
    protected void checkParameterSet(final ConfigGroup module) {
        if (!AnnealingVariable.GROUP_NAME.equals(module.getName())) {
            throw new IllegalArgumentException(module.getName());
        }
        if (!(module instanceof AnnealingVariable)) {
            throw new RuntimeException("unexpected class for module " + module);
        }
    }

    @Override
    public void addParameterSet(final ConfigGroup set) {
        if (!AnnealingVariable.GROUP_NAME.equals(set.getName())) {
            throw new IllegalArgumentException(set.getName());
        }
        addAnnealingVariable((AnnealingVariable) set);
    }

    public Map<AnnealParameterOption, AnnealingVariable> getAnnealingVariables() {
        final EnumMap<AnnealParameterOption, AnnealingVariable> map =
                new EnumMap<>(AnnealParameterOption.class);
        for (ConfigGroup pars : getParameterSets(AnnealingVariable.GROUP_NAME)) {
            final AnnealParameterOption name = ((AnnealingVariable) pars).getAnnealParameter();
            final AnnealingVariable old = map.put(name, (AnnealingVariable) pars);
            if (old != null) {
                throw new IllegalStateException("several parameter sets for variable " + name);
            }
        }
        return map;
    }

    public void addAnnealingVariable(final AnnealingVariable params) {
        final AnnealingVariable previous = this.getAnnealingVariables().get(params.getAnnealParameter());
        if (previous != null) {
            final boolean removed = removeParameterSet(previous);
            if (!removed) {
                throw new RuntimeException("problem replacing annealing variable");
            }
        }
        super.addParameterSet(params);
    }

    public enum AnnealOption {linear, geometric, exponential, msa, sigmoid, disabled}

    public enum AnnealParameterOption {
        globalInnovationRate, BrainExpBeta, PathSizeLogitBeta, learningRate
    }

    public static class AnnealingVariable extends ReflectiveConfigGroup {

        public static final String GROUP_NAME = "AnnealingVariable";
        private static final String START_VALUE = "startValue";
        private static final String END_VALUE = "endValue";
        private static final String ANNEAL_TYPE = "annealType";
        private static final String DEFAULT_SUBPOP = "defaultSubpopulation";
        private static final String ANNEAL_PARAM = "annealParameter";
        private static final String HALFLIFE = "halfLife";
        private static final String SHAPE_FACTOR = "shapeFactor";
        private String defaultSubpop = null;
        private Double startValue = null;
        private double endValue = 0.0001;
        private double shapeFactor = 0.9;
        private double halfLife = 100.0;
        private AnnealOption annealType = AnnealOption.disabled;
        private AnnealParameterOption annealParameter = AnnealParameterOption.globalInnovationRate;

        public AnnealingVariable() {
            super(GROUP_NAME);
        }

        @StringGetter(START_VALUE)
        public Double getStartValue() {
            return this.startValue;
        }

        @StringSetter(START_VALUE)
        public void setStartValue(Double startValue) {
            this.startValue = startValue;
        }

        @StringGetter(END_VALUE)
        public double getEndValue() {
            return this.endValue;
        }

        @StringSetter(END_VALUE)
        public void setEndValue(double endValue) {
            this.endValue = endValue;
        }

        @StringGetter(ANNEAL_TYPE)
        public AnnealOption getAnnealType() {
            return this.annealType;
        }

        @StringSetter(ANNEAL_TYPE)
        public void setAnnealType(String annealType) {
            this.annealType = AnnealOption.valueOf(annealType);
        }

        public void setAnnealType(AnnealOption annealType) {
            this.annealType = annealType;
        }

        @StringGetter(DEFAULT_SUBPOP)
        public String getDefaultSubpopulation() {
            return this.defaultSubpop;
        }

        @StringSetter(DEFAULT_SUBPOP)
        public void setDefaultSubpopulation(String defaultSubpop) {
            this.defaultSubpop = defaultSubpop;
        }

        @StringGetter(ANNEAL_PARAM)
        public AnnealParameterOption getAnnealParameter() {
            return this.annealParameter;
        }

        @StringSetter(ANNEAL_PARAM)
        public void setAnnealParameter(String annealParameter) {
            this.annealParameter = AnnealParameterOption.valueOf(annealParameter);
        }

        public void setAnnealParameter(AnnealParameterOption annealParameter) {
            this.annealParameter = annealParameter;
        }

        @StringGetter(HALFLIFE)
        public double getHalfLife() {
            return this.halfLife;
        }

        @StringSetter(HALFLIFE)
        public void setHalfLife(double halfLife) {
            this.halfLife = halfLife;
        }

        @StringGetter(SHAPE_FACTOR)
        public double getShapeFactor() {
            return this.shapeFactor;
        }

        @StringSetter(SHAPE_FACTOR)
        public void setShapeFactor(double shapeFactor) {
            this.shapeFactor = shapeFactor;
        }

        @Override
        public Map<String, String> getComments() {
            Map<String, String> map = super.getComments();
            map.put(HALFLIFE,
                    "this parameter enters the exponential and sigmoid formulas. May be an iteration or a share, i.e. 0.5 for halfLife at 50% of iterations. Exponential: startValue / exp(it/halfLife)");
            map.put(SHAPE_FACTOR, "sigmoid: 1/(1+e^(shapeFactor*(it - halfLife))); geometric: startValue * shapeFactor^it; msa: startValue / it^shapeFactor");
            map.put(ANNEAL_TYPE, "options: linear, exponential, geometric, msa, sigmoid and disabled (no annealing).");
            map.put(ANNEAL_PARAM,
                    "list of config parameters that shall be annealed. Currently supported: globalInnovationRate, BrainExpBeta, PathSizeLogitBeta, learningRate. Default is globalInnovationRate");
            map.put(DEFAULT_SUBPOP, "subpopulation to have the global innovation rate adjusted. Not applicable when annealing with other parameters.");
            map.put(START_VALUE, "start value for annealing.");
            map.put(END_VALUE, "final annealing value. When the annealing function reaches this value, further results remain constant.");
            return map;
        }
    }

}