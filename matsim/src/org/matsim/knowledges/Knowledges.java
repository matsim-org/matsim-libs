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

import java.util.Map;

import org.matsim.api.basic.v01.Id;
/**
 * This is just a refactoring of the current code within matsim.
 * It is NOT THE RECOMMENDED WAY TO WRITE CODE.
 * See api for better examples.
 * @author dgrether
 *
 */
public interface Knowledges {

	public Map<Id, Knowledge> getKnowledgesByPersonId();

	public KnowledgesBuilder getBuilder();

}