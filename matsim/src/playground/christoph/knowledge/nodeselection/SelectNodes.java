/* *********************************************************************** *
 * project: org.matsim.*
 * SelectNodes.java
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

/**
 * @author Christoph Dobler
 * 
 * Ziel: Knoten eines Netzwerks anhand vorgegebener Regeln auswählen und 
 * zu einer übergebenen ArrayList hinzufügen.
 * 
 * Beispiel: Alle Knoten die innerhalb eines vorgegebenen Abstandes zu einem
 * Zielknoten liegen sollen gefunden und zurück gegeben werden.
 */

package playground.christoph.knowledge.nodeselection;

import java.util.ArrayList;

import org.matsim.network.Node;

public interface SelectNodes {

	/**
	 * Some Links are selected and added to the ArrayList< Node > nodeList.
	 */
	public void getNodes(ArrayList<Node> nodeList);
	
	/**
	 * @return A new created ArrayList of Nodes.
	 */
	public ArrayList<Node> getNodes();
	
}
