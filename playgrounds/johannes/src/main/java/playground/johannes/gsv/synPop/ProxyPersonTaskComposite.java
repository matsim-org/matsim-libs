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

package playground.johannes.gsv.synPop;

import playground.johannes.sna.util.Composite;
import playground.johannes.synpop.data.PlainPerson;

/**
 * @author johannes
 *
 */
public class ProxyPersonTaskComposite extends Composite<ProxyPersonTask> implements	ProxyPersonTask {

	/* (non-Javadoc)
	 * @see playground.johannes.gsv.synPop.ProxyPersonTask#apply(playground.johannes.synpop.data.PlainPerson)
	 */
	@Override
	public void apply(PlainPerson person) {
		for(ProxyPersonTask task : components) {
			task.apply(person);
		}

	}

}
