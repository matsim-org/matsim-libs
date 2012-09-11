/* *********************************************************************** *
 * project: org.matsim.*
 * VisumNetwork.java
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

package playground.acmarmol.matsim2030.network;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.visum.VisumNetwork;

/**
 * personalized version of  {@link org.matsim.visum.VisumNetwork}: added mode types 
 * 
 * @author acmarmol
 * 
 */
public class MyVisumNetwork extends VisumNetwork{

	private final Map<Id, ModeType> modeTypes = new HashMap<Id, ModeType>();
	private int language;
		
	public void addModeType(final ModeType modeType) {
		ModeType oldModeType = modeTypes.put(modeType.id, modeType);
		if (oldModeType != null) {
			throw new IllegalArgumentException("Duplicate mode type.");
		}
	}
	


	public int getLanguage() {
		return language;
	}



	public void setLanguage(int language) {
		this.language = language;
	}



	public static class ModeType {
		public final Id id;
		public final String name;
		public final String type;
		public final String pcu;

		public ModeType(Id id, String name, String type, String pcu) {
			super();
			this.id = id;
			this.name = name;
			this.type = type;
			this.pcu = pcu;
		}

	}

	public Map<Id, ModeType> getModeTypes(){
		return this.modeTypes;
	}
	
}

