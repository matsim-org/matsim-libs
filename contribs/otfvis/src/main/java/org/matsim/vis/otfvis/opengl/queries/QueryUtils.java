/* *********************************************************************** *
 * project: org.matsim.*
 * QueryUtils
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
package org.matsim.vis.otfvis.opengl.queries;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;


/**
 * @author dgrether
 *
 */
public class QueryUtils {
	
	private static final Logger log = Logger.getLogger(QueryUtils.class);
	
	/**
	 * Method that can be used to parse multiple matsim.org Ids from the query input text field.
	 */
	static <T> List<Id<T>> parseIds(String idString, Class<T> type){
		log.info("Query got Id String: " + idString);
		List<Id<T>> ids = new ArrayList<>();
		if (idString.contains(",")){
			for (String i : idString.split(",")) {
				ids.add(Id.create(i.trim(), type));
			}
		}
		else {
			ids.add(Id.create(idString.trim(), type));
		}
		return ids;
	}
	

}
