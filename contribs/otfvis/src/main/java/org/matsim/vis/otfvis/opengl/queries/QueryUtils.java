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
import org.matsim.core.basic.v01.IdImpl;


/**
 * @author dgrether
 *
 */
public class QueryUtils {
	
	private static final Logger log = Logger.getLogger(QueryUtils.class);
	
	/**
	 * Method that can be used to parse multiple matsim.org Ids from the query input text field.
	 */
	static List<Id> parseIds(String idString){
		log.info("Query got Id String: " + idString);
		List<Id> ids = new ArrayList<Id>();
		if (idString.contains(",")){
			for (String i : idString.split(",")) {
				ids.add(new IdImpl(i.trim()));
			}
		}
		else {
			ids.add(new IdImpl(idString.trim()));
		}
		for (Id id : ids){
			log.info("  parsed id: " + id);
		}
		return ids;
	}
	

}
