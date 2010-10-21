/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.christoph.network.mapping;

import java.util.List;

import org.matsim.api.core.v01.Id;

/*
 * Every time Nodes and / or Links form a SubNetwork are
 * transformed this Transformation should be stored in a
 * Mapping Object.
 */
public abstract class Mapping {

	public static int mappingId = 0;

	private Id id;

	public void setId(Id id)
	{
		this.id = id;
	}

	public Id getId()
	{
		return id;
	}

	public abstract Object getInput();

	public abstract Object getOutput();

	public abstract double getLength();

	public abstract List<MappingInfo> getMappedObjects();
}
