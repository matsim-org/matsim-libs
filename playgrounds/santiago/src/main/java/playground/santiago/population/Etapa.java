/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.santiago.population;

import org.matsim.api.core.v01.Coord;

public class Etapa {
	
	private String comunaOrigen;
	private String comunaDestino;
	
	private Coord origin;
	private Coord destination;
	
	private String mode;
	
	public Etapa(String mode, String comunaOrigen, String comunaDestino, String originX, String originY, String destinationX, String destinationY){
		
		this.mode = mode;
		this.comunaOrigen = comunaOrigen;
		this.comunaDestino = comunaDestino;
		
		if(!originX.equals("") && !originY.equals("")){
			this.origin = new Coord(Double.parseDouble(originX.replace(",", ".")), Double.parseDouble(originY.replace(",", ".")));
		}
		
		if(!destinationX.equals("") && !destinationY.equals("")){
			this.destination = new Coord(Double.parseDouble(destinationX.replace(",", ".")), Double.parseDouble(destinationY.replace(",", ".")));
		}
		
	}

	public String getComunaOrigen() {
		return comunaOrigen;
	}

	public String getComunaDestino() {
		return comunaDestino;
	}

	public Coord getOrigin() {
		return origin;
	}

	public Coord getDestination() {
		return destination;
	}

	public String getMode() {
		return mode;
	}

}
