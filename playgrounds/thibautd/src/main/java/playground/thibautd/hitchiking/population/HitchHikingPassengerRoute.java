///* *********************************************************************** *
// * project: org.matsim.*
// * HitchHikingPassengerRoute.java
// *                                                                         *
// * *********************************************************************** *
// *                                                                         *
// * copyright       : (C) 2012 by the members listed in the COPYING,        *
// *                   LICENSE and WARRANTY file.                            *
// * email           : info at matsim dot org                                *
// *                                                                         *
// * *********************************************************************** *
// *                                                                         *
// *   This program is free software; you can redistribute it and/or modify  *
// *   it under the terms of the GNU General Public License as published by  *
// *   the Free Software Foundation; either version 2 of the License, or     *
// *   (at your option) any later version.                                   *
// *   See also COPYING, LICENSE and WARRANTY file                           *
// *                                                                         *
// * *********************************************************************** */
//package playground.thibautd.hitchiking.population;
//
//import org.matsim.api.core.v01.Id;
//import org.matsim.core.basic.v01.IdImpl;
//import org.matsim.core.population.routes.AbstractRoute;
//import org.matsim.core.population.routes.GenericRoute;
//import org.matsim.core.utils.misc.Time;
//
//import playground.thibautd.hitchiking.HitchHikingConstants;
//
///**
// * @author thibautd
// */
//public class HitchHikingPassengerRoute extends AbstractRoute implements GenericRoute {
//	private static final String PU_DO_SEP = "|";
//	private static final String LINK_TIME_SEP = ",";
//
//	private double accessWalkTime = Time.UNDEFINED_TIME;
//	private Id puLinkId = null;
//	private Id doLinkId = null;
//	private double egressWalkTime = Time.UNDEFINED_TIME;
//
//	public HitchHikingPassengerRoute(
//			final Id startLinkId,
//			final Id endLinkId) {
//		super(startLinkId, endLinkId);
//	}
//
//	@Override
//	public void setRouteDescription(
//			final Id startLinkId,
//			final String routeDescription,
//			final Id endLinkId) {
//		setStartLinkId( startLinkId );
//		setEndLinkId( endLinkId );
//
//		try {
//			String[] split = routeDescription.trim().split( PU_DO_SEP );
//
//			String[] linkTime =  split[0].split( LINK_TIME_SEP ) ;
//			puLinkId = new IdImpl( linkTime[0].trim() );
//			accessWalkTime = Double.parseDouble( linkTime[1] );
//
//			linkTime =  split[1].split( LINK_TIME_SEP ) ;
//			doLinkId = new IdImpl( linkTime[0].trim() );
//			egressWalkTime = Double.parseDouble( linkTime[1] );
//		}
//		catch (Exception e) {
//			throw new RuntimeException( "Ill specified description: "+routeDescription , e );
//		}
//	}
//
//	@Override
//	public String getRouteDescription() {
//		return puLinkId + PU_DO_SEP + doLinkId;
//	}
//
//	@Override
//	public String getRouteType() {
//		return HitchHikingConstants.PASSENGER_MODE;
//	}
//
//	public Id getPickUpLinkId() {
//		return puLinkId;
//	}
//
//	public Id getDropOffLinkId() {
//		return doLinkId;
//	}
//
//	public double getWalkAccessTime() {
//		return accessWalkTime;
//	}
//
//	public double getWalkEgressTime() {
//		return egressWalkTime;
//	}
//}
