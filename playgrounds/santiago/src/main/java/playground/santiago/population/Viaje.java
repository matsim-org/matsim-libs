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

import java.util.LinkedList;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.misc.Time;

public class Viaje {
	
	private String id;
	
	private LinkedList<Etapa> etapas = new LinkedList<Etapa>();
	
	private String comunaOrigen;
	private String comunaDestino;
	
	private Coord origin;
	private Coord destination;
	
	private String proposito;
	
	private double endTime = 0.;
	private double startTime = 0.;
	
	private int currentIdx = 0;
	
	public Viaje(String id, String comunaOrigen, String comunaDestino, String originX, String originY, String destinationX, String destinationY, String proposito, String startTime, String endTime){

		this.id = id;
		
		this.comunaOrigen = comunaOrigen;
		this.comunaDestino = comunaDestino;
		
		if(!originX.equals("") && !originY.equals("")){
			this.origin = new Coord(Double.parseDouble(originX), Double.parseDouble(originY));
		}
		
		if(!destinationX.equals("") && !destinationY.equals("")){
			this.destination = new Coord(Double.parseDouble(destinationX), Double.parseDouble(destinationY));
		}
		
		this.proposito = proposito;
		
		if(!startTime.equals("")){
			this.startTime = Time.parseTime(startTime);
		} else{
			this.startTime = -1;
		}
		
		if(!endTime.equals("")){
			this.endTime = Time.parseTime(endTime);
		} else{
			this.endTime = -1;
		}
		
	}
	
	public void addEtapa(Etapa etapa){
		
		this.etapas.addLast(etapa);
		
	}
	
	public String getId(){
		return this.id;
	}

	public LinkedList<Etapa> getEtapas() {
		return etapas;
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

	public String getProposito() {
		return proposito;
	}

	public double getEndTime() {
		return endTime;
	}

	public double getStartTime() {
		return startTime;
	}

}
