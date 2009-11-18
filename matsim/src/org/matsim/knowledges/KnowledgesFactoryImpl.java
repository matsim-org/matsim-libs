/* *********************************************************************** *
 * project: org.matsim.*
 * KnowledgesBuilderImpl
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

import org.matsim.api.basic.v01.Id;


/**
 * This is just a refactoring of the current code within matsim.
 * It is NOT THE RECOMMENDED WAY TO WRITE CODE.
 * See api for better examples.
 * @author dgrether
 *
 */
public class KnowledgesFactoryImpl implements KnowledgesFactory {

	private KnowledgesImpl knowledge;

	public KnowledgesFactoryImpl(KnowledgesImpl k){
		this.knowledge = k;
	}

	/**
	 * This method is not always creating an object. If already one exists
	 * for the given personId the existing object will be returned.
	 */
	public KnowledgeImpl createKnowledge(Id personId, String desc) {
		if (!this.knowledge.getKnowledgesByPersonId().containsKey(personId)){
			KnowledgeImpl k = new KnowledgeImpl();
			k.setDescription(desc);
			this.knowledge.getKnowledgesByPersonId().put(personId, k);
			return k;
		}
		return this.knowledge.getKnowledgesByPersonId().get(personId);
	}

}
