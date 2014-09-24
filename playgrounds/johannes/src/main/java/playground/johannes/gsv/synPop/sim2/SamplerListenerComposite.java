/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.johannes.gsv.synPop.sim2;

import java.util.Collection;

import playground.johannes.gsv.synPop.ProxyPerson;
import playground.johannes.sna.util.Composite;

/**
 * @author johannes
 *
 */
public class SamplerListenerComposite extends Composite<SamplerListener> implements SamplerListener {

	@Override
	public void afterStep(Collection<ProxyPerson> population, ProxyPerson person, boolean accpeted) {
		for(SamplerListener listener : components) {
			listener.afterStep(population, person, accpeted);
		}

	}

	@Override
	public void afterModify(ProxyPerson person) {
		for(SamplerListener listener : components) {
			listener.afterModify(person);
		}
		
	}

}
