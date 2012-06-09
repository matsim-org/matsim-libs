/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.wrashid.lib.obj;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class HashMapHashSetConcat<KeyClass,ValueClass> {

	HashMap<KeyClass, HashSet<ValueClass>> map=new HashMap<KeyClass, HashSet<ValueClass>>();
	
	public void put(KeyClass id,ValueClass value){
		if (!map.containsKey(id)) {
			map.put(id, new HashSet<ValueClass>());
		}

		HashSet<ValueClass> hs = map.get(id);
		hs.add(value);
	}
	
	public boolean containsValue(KeyClass id,ValueClass value){
		return map.containsKey(id) && map.get(id).contains(value);
	}
	
	public Set<ValueClass> getValueSet(KeyClass id){
		return map.get(id);
	}
	
	public boolean removeValue(KeyClass id,ValueClass value){
		return map.get(id).remove(value);
	}
	
}
