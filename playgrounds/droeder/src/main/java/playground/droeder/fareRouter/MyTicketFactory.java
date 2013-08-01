/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.droeder.fareRouter;

import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.utils.objectattributes.ObjectAttributes;


/**
 * @author droeder
 *
 */
class MyTicketFactory implements TicketFactory{
	
	/**
	 * use this tag to put a boolean to the persons custom-attributes
	 */
	public static String USEFLATRATE = "flatrate";

	@SuppressWarnings("unused")
	private static final Logger log = Logger
			.getLogger(MyTicketFactory.class);
	private ObjectAttributes lineAttribs;

	private Map<String, Double> ticketType2Fare;

	public MyTicketFactory(ObjectAttributes lineAttribs, Map<String, Double> ticketType2Fare) {
		this.lineAttribs = lineAttribs;
		this.ticketType2Fare = ticketType2Fare;
	}

	@Override
	public Ticket createTicket(Id routeId, Id lineId, Person person, Double time, Double expectedTravelTime) {
		String[] allowedTickets = ((String) this.lineAttribs.getAttribute(routeId.toString(), TicketMachineImpl.ALLOWEDTICKETS)).split(",");
		// first check if the person owns a flatrate
		if((Boolean) person.getCustomAttributes().get(USEFLATRATE)){
			// now check if the line allows the flatrate
			for(String type: allowedTickets){
				if(type.equals(FlatRate.NAME)){
					// it does allow the flatrate, thus we use it
					return new FlatRate(this.ticketType2Fare.get(FlatRate.NAME));
				}
			}
		}
		Ticket theCheapestTicket = null, tempTicket;
		Double price = Double.MAX_VALUE;
		// now find the cheapest ticket
		for(String type: allowedTickets){
			if(type.equals(TwoHourTicket.NAME) && (expectedTravelTime <= (2 * 3600))){
				tempTicket = new TwoHourTicket(this.ticketType2Fare.get(type), time);
			}else if (type.equals(ThirtyMinTicket.NAME) && (expectedTravelTime <= (30 * 60))){
				tempTicket = new ThirtyMinTicket(this.ticketType2Fare.get(type), time);
			}else if(type.equals(SingleBoardingTicket.NAME)){
				tempTicket = new SingleBoardingTicket((Double) this.lineAttribs.getAttribute(routeId.toString(), SingleBoardingTicket.NAME), lineId, routeId);
			}else{
				continue;
			}
			if((tempTicket.getOriginalFare() < price)){
				price = tempTicket.getOriginalFare();
				theCheapestTicket = tempTicket;
			}
		}
		if(theCheapestTicket == null){
			throw new RuntimeException("there should be at least one ticket allowed");
		}
		return theCheapestTicket;
	}

	@Override
	public Ticket upgrade(Ticket ticketToUpgrade, Id routeId, Id lineId, Person person, Double time, Double expectedTravelTime, Double travelledDistance) {
		// we're upgrading only the TwentyMinTicket 
		if(ticketToUpgrade instanceof ThirtyMinTicket){
			String[] allowedTickets = ((String) this.lineAttribs.getAttribute(routeId.toString(), TicketMachineImpl.ALLOWEDTICKETS)).split(",");
			for(String type: allowedTickets){
				if(type.equals(TwoHourTicket.NAME)){
					// a two-hour-ticket is allowed, thus we can upgrade. but we have to increase the timeToExpire for ninety minutes.
					Double newTimeToExpire = ((ThirtyMinTicket) ticketToUpgrade).getTimeToExpire() + ( 90 * 60);
					if(newTimeToExpire > (time + expectedTravelTime)){
						// we charge only the difference of the fare, assuming the upgraded ticket is more expensive than the old one
						return new TwoHourTicket(this.ticketType2Fare.get(type) - this.ticketType2Fare.get(ThirtyMinTicket.NAME), newTimeToExpire - (2*3600));
					}
				}
			}
		}
		return null;
	}
	
	
}

class SingleBoardingTicket implements Ticket{
	
	public static String NAME = SingleBoardingTicket.class.getSimpleName();
	
	private Double fare;

	private Id routeId;

	private Id lineId;
	
	private boolean paid = false;

	public SingleBoardingTicket(Double fare, Id lineId, Id routeId){
		this.fare = fare;
		this.lineId = lineId;
		this.routeId = routeId;
	}

	@Override
	public Double getFare() {
		if(this.paid){
			// The Router already asked for the fare. Do not charge it again
			return 0.;
		}
		this.paid = true;
		return this.fare;
	}

	@Override
	public boolean timeExpired(Double time) {
		//the time will never expire for a single boarding ticket
		return false;
	}

	@Override
	public boolean lineForbidden(Id lineId) {
		// only the line where the ticket was bought is allowed
		return (!lineId.equals(this.lineId));
	}

	@Override
	public boolean routeForbidden(Id routeId) {
		// only the route where the ticket was bought is allowed
		return (!routeId.equals(this.routeId));
	}

	@Override
	public boolean distanceExpired(Double travelledDistance) {
		// the distance will never expire for a single boarding ticket
		return false;
	}

	@Override
	public Double getOriginalFare() {
		return this.fare;
	}

	@Override
	public String getType() {
		return NAME;
	}
}

class TwoHourTicket implements Ticket{
	
	public static String NAME = TwoHourTicket.class.getSimpleName();
	
	private Double expiring;
	private Double fare;
	private boolean paid = false;
	
	public TwoHourTicket(Double fare, Double time){
		this.fare = fare;
		this.expiring = time + (2*3600.);
	}

	@Override
	public Double getFare() {
		if(this.paid){
			// The Router already asked for the fare. Do not charge it again
			return 0.;
		}
		this.paid = true;
		return this.fare;
	}

	@Override
	public boolean timeExpired(Double time) {
		// time expired
		return (time > this.expiring);
	}
	
	

	@Override
	public boolean lineForbidden(Id lineId) {
		// not possible 
		return false;
	}

	@Override
	public boolean routeForbidden(Id routeId) {
		// not possible 
		return false;
	}

	@Override
	public boolean distanceExpired(Double travelledDistance) {
		// not possible 
		return false;
	}

	@Override
	public Double getOriginalFare() {
		return this.fare;
	}
	
	@Override
	public String getType() {
		return NAME;
	}
}

class ThirtyMinTicket implements Ticket{
	
	public static String NAME = ThirtyMinTicket.class.getSimpleName();
	private Double fare;
	private double expiring;
	private boolean paid = false;

	public ThirtyMinTicket(Double fare, Double time){
		this.fare = fare;
		// current time plus 20 minutes
		this.expiring = time + (30 * 60);
	}

	@Override
	public Double getFare() {
		if(this.paid){
			// The Router already asked for the fare. Do not charge it again
			return 0.;
		}
		this.paid = true;
		return this.fare;
	}

	@Override
	public boolean timeExpired(Double time) {
		// time expired
		return (time > this.expiring);
	}

	/**
	 * 
	 * @return
	 */
	public Double getTimeToExpire(){
		return this.expiring;
	}
	
	@Override
	public boolean lineForbidden(Id lineId) {
		// not possible 
		return false;
	}

	@Override
	public boolean routeForbidden(Id routeId) {
		// not possible 
		return false;
	}

	@Override
	public boolean distanceExpired(Double travelledDistance) {
		// not possible 
		return false;
	}

	@Override
	public Double getOriginalFare() {
		return this.fare;
	}
	
	@Override
	public String getType() {
		return NAME;
	}
}

class FlatRate implements Ticket{
	
	public static String NAME = FlatRate.class.getSimpleName();
	private Double fare;

	public FlatRate(Double fare){
		// the fare is prepaid, thus it is not additive in the routing, but maybe we'll need it for scoring?!
		this.fare = fare;
	}

	@Override
	public Double getFare() {
		// the fare is prepaid, thus it is not additive in the routing
		return 0.;
	}

	@Override
	public boolean timeExpired(Double time) {
		// not possible 
		return false;
	}
	


	@Override
	public boolean lineForbidden(Id lineId) {
		// not possible 
		return false;
	}

	@Override
	public boolean routeForbidden(Id routeId) {
		// not possible 
		return false;
	}

	@Override
	public boolean distanceExpired(Double travelledDistance) {
		// not possible 
		return false;
	}

	@Override
	public Double getOriginalFare() {
		return this.fare;
	}
	
	@Override
	public String getType() {
		return NAME;
	}
}

