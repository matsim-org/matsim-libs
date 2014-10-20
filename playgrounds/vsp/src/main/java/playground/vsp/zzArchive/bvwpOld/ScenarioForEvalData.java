/* *********************************************************************** *
 * project: org.matsim.*
 * ValuesByODRelation.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.vsp.zzArchive.bvwpOld;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

class ScenarioForEvalData {
		private Map<String,Values> values = new TreeMap<>();
		ScenarioForEvalData() {
//			for ( Id id : values.keySet() ) {
//				Values vals = new Values() ;
//				values.put( id, vals ) ;
//			}
		}
		ScenarioForEvalData createDeepCopy() {
			ScenarioForEvalData nnn = new ScenarioForEvalData() ;
			for ( String id : values.keySet() ) {
				Values oldValues = this.getByODRelation(id) ;
				Values newValues = oldValues.createDeepCopy() ;
				nnn.values.put( id, newValues ) ;
			}
			return nnn ;
		}
		Values getByODRelation( String id ) {
			return values.get(id) ;
		}
		void setValuesForODRelation( String id , Values tmp ) {
			values.put( id, tmp ) ;
		}
		Set<String> getAllRelations() {
			return Collections.unmodifiableSet(values.keySet()) ;
		}
	}