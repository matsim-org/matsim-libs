/* *********************************************************************** *
 * project: org.matsim.*
 * SignalSystemsDataFactory
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.dgrether.signalsNew.data.v20;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.internal.MatsimFactory;


/**
 * @author dgrether
 *
 */
public interface SignalSystemsDataFactory extends MatsimFactory {

	public SignalSystemData createSignalSystemData(Id systemId);
	
	public SignalGroupData createSignalGroupData(Id groupId);
	
	//TODO how to deal with subtypes???
	public SignalData createSignalData(Id signalId);
	
}
