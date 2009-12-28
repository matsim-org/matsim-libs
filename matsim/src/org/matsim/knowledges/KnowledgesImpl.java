/* *********************************************************************** *
 * project: org.matsim.*
 * Knowledges
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package org.matsim.knowledges;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;


/**
 * This is just a refactoring of the current code within matsim.
 * It is NOT THE RECOMMENDED WAY TO WRITE CODE.
 * See api for better examples.
 * @author dgrether
 *
 */
public class KnowledgesImpl implements Knowledges {

	private Map<Id, KnowledgeImpl> knowledgeByPersonId = new HashMap<Id, KnowledgeImpl>();
	private KnowledgesFactory factory = new KnowledgesFactoryImpl(this);
	
	public KnowledgesImpl(){
		
	}
	
	/**
	 * @see org.matsim.knowledges.Knowledges#getKnowledgesByPersonId()
	 */
	public Map<Id, KnowledgeImpl> getKnowledgesByPersonId() {
		return this.knowledgeByPersonId;
	}
	
	/**
	 * @see org.matsim.knowledges.Knowledges#getFactory()
	 */
	public KnowledgesFactory getFactory(){
		return this.factory;
	}
}
