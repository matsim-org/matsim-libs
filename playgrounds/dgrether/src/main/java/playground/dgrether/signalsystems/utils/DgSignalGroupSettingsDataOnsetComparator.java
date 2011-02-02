/* *********************************************************************** *
 * project: org.matsim.*
 * DgSignalGroupSettingsDataOnsetComparator
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
package playground.dgrether.signalsystems.utils;

import java.util.Comparator;

import org.matsim.signalsystems.data.signalcontrol.v20.SignalGroupSettingsData;


/**
 * @author dgrether
 *
 */
public class DgSignalGroupSettingsDataOnsetComparator implements Comparator<SignalGroupSettingsData> {

	@Override
	public int compare(SignalGroupSettingsData o1, SignalGroupSettingsData o2) {
		return Integer.valueOf(o1.getOnset()).compareTo(Integer.valueOf(o2.getOnset()));
	}

}
