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

package playground.johannes.studies.matrix2014.sim;

import org.matsim.contrib.common.collections.Composite;
import playground.johannes.synpop.sim.Hamiltonian;
import playground.johannes.synpop.sim.data.CachedPerson;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author johannes
 */
public class MutableHamiltonianComposite extends Composite<Hamiltonian> implements Hamiltonian {

    private List<ThetaProvider> providers = new ArrayList<>();

    public void addComponent(Hamiltonian h) {
        super.addComponent(h);
        providers.add(null);
    }

    public void addComponent(Hamiltonian h, ThetaProvider provider) {
        super.addComponent(h);
        providers.add(provider);
    }

    public void removeComponent(Hamiltonian h) {
        int idx = components.indexOf(h);
        super.removeComponent(h);
        providers.remove(idx);
    }

//    /*
//     * TODO: hide access?
//     */
//    public List<Hamiltonian> getComponents() {
//        return components;
//    }

    @Override
    public double evaluate(Collection<CachedPerson> population) {
        double sum = 0;

        for (int i = 0; i < components.size(); i++) {
            sum += providers.get(i).getTheta() * components.get(i).evaluate(population);
        }

        return sum;
    }
}
