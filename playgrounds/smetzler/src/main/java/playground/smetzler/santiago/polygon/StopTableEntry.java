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

package playground.smetzler.santiago.polygon;

import org.matsim.api.core.v01.Coord;

/**
 * Reads the 2013 stop table format with header
 * 
 * Codigo|CodigoUsuario|Comuna|Nombre|Sentido|FilaSuperior|FilaInferior|GrupoParada|X|Y|Latitud|Longitud|censal_1992|comunas|diseno_563|diseno_777|eod_2001|eod_2006|estraus_264|estraus_404|estraus_410|estraus_618|zonas_6
 * 
 * 
 * @author aneumann
 *
 */
public class StopTableEntry {
	
	final String stopId;
	final String stopIdPublic;
	final String comuna;
	final String name;
	final String orientation;
	final String crossingStreetA;
	final String crossingStreetB;
	final String stopArea;
	final Coord coordCartesian;
	final Coord coordLatLong;
	final String censal1992;
	final String comunas;
	final String diseno563;
	final String diseno777;
	final String eod2001;
	final String eod2006;
	final String estraus264;
	final String estraus404;
	final String estraus410;
	final String estraus618;
	final String zonas6;
	
	public StopTableEntry(String stopId, String stopIdPublic, String comuna,
			String name, String orientation, String crossingStreetA,
			String crossingStreetB, String stopArea, Coord coordCartesian,
			Coord coordLatLong, String censal1992, String comunas,
			String diseno563, String diseno777, String eod2001, String eod2006,
			String estraus264, String estraus404, String estraus410,
			String estraus618, String zonas6) {
		super();
		this.stopId = stopId;
		this.stopIdPublic = stopIdPublic;
		this.comuna = comuna;
		this.name = name;
		this.orientation = orientation;
		this.crossingStreetA = crossingStreetA;
		this.crossingStreetB = crossingStreetB;
		this.stopArea = stopArea;
		this.coordCartesian = coordCartesian;
		this.coordLatLong = coordLatLong;
		this.censal1992 = censal1992;
		this.comunas = comunas;
		this.diseno563 = diseno563;
		this.diseno777 = diseno777;
		this.eod2001 = eod2001;
		this.eod2006 = eod2006;
		this.estraus264 = estraus264;
		this.estraus404 = estraus404;
		this.estraus410 = estraus410;
		this.estraus618 = estraus618;
		this.zonas6 = zonas6;
	}
}
