/* *********************************************************************** *
 * project: org.matsim.*
 * ChoiceSet.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package org.matsim.contrib.common.collections;

import gnu.trove.list.array.TDoubleArrayList;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * A list of options that can be randomly drawn according to their weight. Note JI dec'15: The naming "choice_set_"
 * violates the definition of a set since options can be added multiple times.
 *
 * @author illenberger
 */
public class ChoiceSet<T> {

    private final List<T> options;

    private final TDoubleArrayList weights;

    private final Random random;

    private double weightSum;

    /**
     * Creates a new choice set.
     *
     * @param random a random generator
     */
    public ChoiceSet(Random random) {
        this.random = random;

        this.options = new ArrayList<T>();
        this.weights = new TDoubleArrayList();

        weightSum = 0;
    }

    /**
     * Adds an option to the choice set with weight 1. Options are internally stored in a list, that is, the same option
     * can be added multiple times.
     *
     * @param option an option
     */
    public void addOption(T option) {
        addOption(option, 1.0);
    }

    /**
     * Adds an option to the choice set.
     *
     * @param option an option
     * @param weight a weight > 0
     */
    public void addOption(T option, double weight) {
        if (weight < 0) throw new IllegalArgumentException("Negative weight not allowed.");

        options.add(option);
        weights.add(weight);

        weightSum += weight;
    }

    /**
     * Removes an option from the choice set.
     *
     * @param option the option to remove
     * @return <tt>true</tt> if the options is removed, <tt>false</tt> otherwise
     */
    public boolean removeOption(T option) {
        int idx = options.indexOf(option);
        if (idx >= 0) {
            options.remove(idx);
            double w = weights.removeAt(idx);
            weightSum -= w;
            return true;
        } else {
            return false;
        }
    }

    /**
     * @return the list of options
     */
    public List<T> getOptions() {
        return options;
    }

    /**
     * @return an equally distributed random option
     */
    public T randomChoice() {
        return options.get(random.nextInt(options.size()));
    }

    /**
     * @return an option randomly drawn according to the option's weight
     */
    public T randomWeightedChoice() {
        double weight = random.nextDouble() * weightSum;
        double sum = 0;
        for (int i = 0; i < options.size(); i++) {
            sum += weights.get(i);
            if (weight <= sum)
                return options.get(i);
        }

        return null;
    }
}
