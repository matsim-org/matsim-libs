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
package playground.vsp.bvwpOld;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;

class ScenarioForEvalData {
		private Map<Id,Values> values = new TreeMap<Id,Values>();
		ScenarioForEvalData() {
//			for ( Id id : values.keySet() ) {
//				Values vals = new Values() ;
//				values.put( id, vals ) ;
//			}
		}
		ScenarioForEvalData createDeepCopy() {
			ScenarioForEvalData nnn = new ScenarioForEvalData() ;
			for ( Id id : values.keySet() ) {
				Values oldValues = this.getByODRelation(id) ;
				Values newValues = oldValues.createDeepCopy() ;
				nnn.values.put( id, newValues ) ;
			}
			return nnn ;
		}
		Values getByODRelation( Id id ) {
			return values.get(id) ;
		}
		void setValuesForODRelation( Id id , Values tmp ) {
			values.put( id, tmp ) ;
		}
		Set<Id> getAllRelations() {
			return Collections.unmodifiableSet(values.keySet()) ;
		}
	}