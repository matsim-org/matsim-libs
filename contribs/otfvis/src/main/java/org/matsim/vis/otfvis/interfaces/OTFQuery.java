/* *********************************************************************** *
 * project: org.matsim.*
 * OTFQuery.java
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

package org.matsim.vis.otfvis.interfaces;

import org.matsim.vis.otfvis.SimulationViewForQueries;

/**
 * An interface for the live version of the OTFVis.
 * Implementing this interface enables q query to be send to the actual simulation
 * and be visualized thereafter.<br />
 * Apart from implementing this interface, the query must be made known to
 * the queries drop-down box in the class OTFQueryControlBar.
 * The execution of a query happens in two parts. First the
 * query() method is called on the server side, with all parameters set.
 * Data needs to be collected here. It needs to be stored in a way that allows
 * serialization.
 * The query object is then transported back to the client, where the draw() method
 * is called to visualize whatever data has been collected.
 *
 * @author dstrippgen
 */
public interface OTFQuery {

	/**
	 * Type of Ids that this query will deal with
	 * Right now only LINK and AGENT is used.
	 * The client framework needs this for guessing which ID
	 * to collect for a given query.
	 *
	 * @param Id A String representation of Id
	 */
	public enum Type {AGENT,LINK,OTHER, CLIENT}

	/**
	 * Sets Id as String.
	 * Together with type this indicates which agent/link to query.
	 *
	 * @param Id A String representation of Id
	 */
	public void setId(String id);


	/**
	 * Is called by the OTFServer framework to issue a query into the simulation.
	 * This method should extract the wanted knowledge from the given
	 * sources of information
	 *
	 * @param net The simulation.
	 * @param plans The Population the simulation fed from.
	 * @param quad The quadtree with writer objects.
	 * @return a query containing results, usually this, but not with live queries, as in
	 * this case the result object should change whenever the result changes, as the RI stream
	 * will actually only transport NEW objects.
	 *
	 */
	public void installQuery(SimulationViewForQueries queueModel);

	public void uninstall();


	/**
	 * Everytime the display needs to be refreshed this
	 * method is called for every active Query.
	 *
	 * @return Type of Ids expected
	 */
	public Type getType();
}
