/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.johannes.gsv.popsim;

import playground.johannes.sna.util.Composite;
import playground.johannes.synpop.data.PlainPerson;

/**
 * @author johannes
 */
public class HistoSyncComposite extends Composite<HistogramSync> implements HistogramSync {
    @Override
    public void notifyChange(Object attKey, double oldValue, double newValue, PlainPerson person) {
        for(HistogramSync element : this.components) {
            element.notifyChange(attKey, oldValue, newValue, person);
        }
    }
}
