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

package playground.andreas.bvg4;

import org.matsim.api.core.v01.Id;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.vehicles.Vehicle;

/**
 * Corresponds to Golden Spool File fahrt_ist
 * 
 * @author aneumann
 *
 */
public class FahrtEvent {
	
	// Tagged VERSION
	private final int rblDate;
	
	// Tagged LSK
	private final int kurs;
	
	// Tagged IST_ABFAHRT
	private final String departureDateIst;
	private final double departureTimeIst;
	
	// Tagged ZEITBASIS
	private final String zeitBasis;
	
	// Tagged FAHRZEUGNUMMER
	private final Id<Vehicle> vehId;
	
	// Tagged LINIE
	private final Id<TransitLine> lineId;
	
	// Tagged ROUTE
	private final Id<TransitRoute> routeId;
	
	// Tagged DISTANZ_ROUTE_IST
	private final int distanceRouteIst;
	
	// Tagged FAHRZEIT_IST
	private final int travelTimeIst;
	
	// Tagged FAHRTART_DISPO
	private final boolean fahrtArtDispo;
	
	// Tagged STATUS_LOKALISIERUNG
	private final boolean statusLokalisierung;
	
	// Tagged STATUS_ERFASST
	private final boolean statusErfasst;
	
	// Tagged UEBERTRAGUNGSFEHLER
	private final boolean transmissionError;
	
	public FahrtEvent(int rblDate, int kurs, String departureDateIst, double departureTimeIst, String zeitBasis,
			Id<Vehicle> vehId, Id<TransitLine> lineId, Id<TransitRoute> routeId, int distanceRouteIst, int travelTimeIst,
			boolean fahrtArtDispo, boolean statusLokalisierung, boolean statusErfasst, boolean transmissionError){
		
		this.rblDate = rblDate;
		this.kurs = kurs;
		this.departureDateIst = departureDateIst;
		this.departureTimeIst = departureTimeIst;
		this.zeitBasis = zeitBasis;
		this.vehId = vehId;
		this.lineId = lineId;
		this.routeId = routeId;
		this.distanceRouteIst = distanceRouteIst;
		this.travelTimeIst = travelTimeIst;
		this.fahrtArtDispo = fahrtArtDispo;
		this.statusLokalisierung = statusLokalisierung;
		this.statusErfasst = statusErfasst;
		this.transmissionError = transmissionError;		
	}

	public int getRblDate() {
		return rblDate;
	}

	public int getKurs() {
		return kurs;
	}

	public String getDepartureDateIst() {
		return departureDateIst;
	}

	public double getDepartureTimeIst() {
		return departureTimeIst;
	}

	public String getZeitBasis() {
		return zeitBasis;
	}

	public Id<Vehicle> getVehId() {
		return vehId;
	}

	public Id<TransitLine> getLineId() {
		return lineId;
	}

	public Id<TransitRoute> getRouteId() {
		return routeId;
	}

	public int getDistanceRouteIst() {
		return distanceRouteIst;
	}

	public int getTravelTimeIst() {
		return travelTimeIst;
	}

	public boolean isFahrtArtDispo() {
		return fahrtArtDispo;
	}

	public boolean isStatusLokalisierung() {
		return statusLokalisierung;
	}

	public boolean isStatusErfasst() {
		return statusErfasst;
	}

	public boolean isTransmissionError() {
		return transmissionError;
	}
	
	@Override
	public String toString() {
		StringBuffer strB = new StringBuffer();
		strB.append(this.rblDate); strB.append(", ");
		strB.append(this.kurs); strB.append(", ");
		strB.append(this.departureDateIst); strB.append(", ");
		strB.append(this.departureTimeIst); strB.append(", ");
		strB.append(this.zeitBasis); strB.append(", ");
		strB.append(this.vehId); strB.append(", ");
		strB.append(this.lineId); strB.append(", ");
		strB.append(this.routeId); strB.append(", ");
		strB.append(this.distanceRouteIst); strB.append(", ");
		strB.append(this.travelTimeIst); strB.append(", ");
		strB.append(this.fahrtArtDispo); strB.append(", ");
		strB.append(this.statusLokalisierung); strB.append(", ");
		strB.append(this.statusErfasst); strB.append(", ");
		strB.append(this.transmissionError); strB.append(", ");
		return strB.toString();
	}
}
