/* *********************************************************************** *
 /* ********************************************************************** *
 * project: org.matsim.*												   *
 * WarmEmissionAnalysisModule.java									       *
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
 *                                                                         *
 * *********************************************************************** */
package org.matsim.contrib.emissions;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.emissions.events.WarmEmissionEvent;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;

import java.util.*;

import static org.matsim.contrib.emissions.HbefaTrafficSituation.*;
import static org.matsim.contrib.emissions.utils.EmissionsConfigGroup.EmissionsComputationMethod.AverageSpeed;
import static org.matsim.contrib.emissions.utils.EmissionsConfigGroup.EmissionsComputationMethod.StopAndGoFraction;

/**
 * @author benjamin
 *
 */
public final class WarmEmissionAnalysisModule implements LinkEmissionsCalculator{
	// cannot make non-public: used at least twice outside package.  kai, jan'19

	private static final Logger logger = Logger.getLogger(WarmEmissionAnalysisModule.class);

	private final Map<HbefaWarmEmissionFactorKey, HbefaWarmEmissionFactor>  avgHbefaWarmTable;
	private final Map<HbefaWarmEmissionFactorKey, HbefaWarmEmissionFactor> detailedHbefaWarmTable;
	private final Map<HbefaRoadVehicleCategoryKey, Map<HbefaTrafficSituation, Double>> hbefaRoadTrafficSpeeds;
	private final Set<Pollutant> warmPollutants;

	private final EventsManager eventsManager;
	private final EmissionsConfigGroup ecg;

	private int vehAttributesNotSpecifiedCnt = 0;

    // The following was tested to slow down significantly, therefore counters were commented out:
//	Set<Id> vehAttributesNotSpecified = Collections.synchronizedSet(new HashSet<Id>());
//	Set<Id> vehicleIdSet = Collections.synchronizedSet(new HashSet<Id>());

	private int freeFlowCounter = 0;
	private int saturatedCounter = 0;
	private int heavyFlowCounter = 0;
	private int stopGoCounter = 0;
	private int fractionCounter = 0;
	private int emissionEventCounter = 0;
	
	private double kmCounter = 0.0;
	private double freeFlowKmCounter = 0.0;
	private double heavyFlowKmCounter = 0.0;
	private double saturatedKmCounter = 0.0;
	private double stopGoKmCounter = 0.0;

	public WarmEmissionAnalysisModule(
			Map<HbefaWarmEmissionFactorKey, HbefaWarmEmissionFactor> avgHbefaWarmTable,
			Map<HbefaWarmEmissionFactorKey, HbefaWarmEmissionFactor> detailedHbefaWarmTable,
			Map<HbefaRoadVehicleCategoryKey, Map<HbefaTrafficSituation, Double>> hbefaRoadTrafficSpeeds,
			Set<Pollutant> warmPollutants, EventsManager eventsManager, EmissionsConfigGroup ecg ){
		this.ecg = ecg;

		Gbl.assertIf( avgHbefaWarmTable!=null || detailedHbefaWarmTable!=null );
		this.avgHbefaWarmTable = avgHbefaWarmTable;
		this.detailedHbefaWarmTable = detailedHbefaWarmTable;
		this.hbefaRoadTrafficSpeeds = hbefaRoadTrafficSpeeds;
		this.warmPollutants = warmPollutants;

		Gbl.assertNotNull( eventsManager );
		this.eventsManager = eventsManager;

	}

	void reset() {
		logger.info("resetting counters...");
		vehAttributesNotSpecifiedCnt = 0;

		freeFlowCounter = 0;
		saturatedCounter = 0;
		heavyFlowCounter = 0;
		stopGoCounter = 0;
		emissionEventCounter = 0;
		
		kmCounter = 0.0;
		freeFlowKmCounter = 0.0;
		heavyFlowKmCounter = 0.0;
		saturatedKmCounter = 0.0;
		fractionCounter = 0;
		stopGoKmCounter = 0.0;
	}

	void throwWarmEmissionEvent( double leaveTime, Id<Link> linkId, Id<Vehicle> vehicleId, Map<Pollutant, Double> warmEmissions ){
		Event warmEmissionEvent = new WarmEmissionEvent(leaveTime, linkId, vehicleId, warmEmissions);
		this.eventsManager.processEvent(warmEmissionEvent);
	}
	@Override
	public Map<Pollutant, Double> checkVehicleInfoAndCalculateWarmEmissions( Vehicle vehicle, Link link, double travelTime ){
		return checkVehicleInfoAndCalculateWarmEmissions( vehicle.getType(), vehicle.getId(), link, travelTime );
	}

	/*package-private*/ Map<Pollutant, Double> checkVehicleInfoAndCalculateWarmEmissions( VehicleType vehicleType, Id<Vehicle> vehicleId,
											      Link link, double travelTime ) {
		{
			String hbefaVehicleTypeDescription = EmissionUtils.getHbefaVehicleDescription( vehicleType, this.ecg );
			// (this will, importantly, repair the hbefa description in the vehicle type. kai/kai, jan'20)
			Gbl.assertNotNull( hbefaVehicleTypeDescription );
		}
		Tuple<HbefaVehicleCategory, HbefaVehicleAttributes> vehicleInformationTuple = EmissionUtils.convertVehicleDescription2VehicleInformationTuple(vehicleType );
		Gbl.assertNotNull( vehicleInformationTuple );

		if (vehicleInformationTuple.getFirst() == null){
			throw new RuntimeException("Vehicle category for vehicle " + vehicleType + " is not valid. " +
					"Please make sure that requirements for emission vehicles in " +
					EmissionsConfigGroup.GROUP_NAME + " config group are met. Aborting...");
		}

		double freeVelocity = link.getFreespeed(); //TODO: what about time dependence

		Map<Pollutant, Double> warmEmissions
				= calculateWarmEmissions( vehicleId, travelTime, EmissionUtils.getHbefaRoadType( link ), freeVelocity, link.getLength(), vehicleInformationTuple );

		// a basic apporach to introduce emission reduced cars:
		// yy this should be deprecated. kai, jan'20
//		if( ecg.getEmissionEfficiencyFactor()!=1. ) {
//			warmEmissions = rescaleWarmEmissions(warmEmissions, ecg.getEmissionEfficiencyFactor();
//		}
		return warmEmissions;
	}

//	static Map<Pollutant, Double> rescaleWarmEmissions( Map<Pollutant, Double> warmEmissions, double emissionEfficiencyFactor ) {
//		Map<Pollutant, Double> rescaledWarmEmissions = new HashMap<>();
//		for( Pollutant wp : warmEmissions.keySet()){
//			Double orgValue = warmEmissions.get(wp);
//			Double rescaledValue = emissionEfficiencyFactor * orgValue;
//			rescaledWarmEmissions.put(wp, rescaledValue);
//		}
//		return rescaledWarmEmissions;
//	}

	private static int cnt =10;
	private Map<Pollutant, Double> calculateWarmEmissions( Id<Vehicle> vehicleId, double travelTime_sec, String roadType, double freeVelocity_ms,
							       double linkLength_m, Tuple<HbefaVehicleCategory, HbefaVehicleAttributes> vehicleInformationTuple ) {

		Map<Pollutant, Double> warmEmissionsOfEvent = new HashMap<>();

//		if(vehicleInformationTuple.getFirst().equals(HbefaVehicleCategory.HEAVY_GOODS_VEHICLE)){
//			efkey.setHbefaVehicleCategory(HbefaVehicleCategory.HEAVY_GOODS_VEHICLE);
//		} else if (vehicleInformationTuple.getFirst().equals(HbefaVehicleCategory.MOTORCYCLE)) {
//			efkey.setHbefaVehicleCategory(HbefaVehicleCategory.MOTORCYCLE);
//		} else if(vehicleInformationTuple.getFirst().equals(HbefaVehicleCategory.ZERO_EMISSION_VEHICLE)) {
//			for (WarmPollutant warmPollutant : warmPollutants) {
//				warmEmissionsOfEvent.put( warmPollutant, 0.0 );
//			}
//			// I am doubtful that the above is useful ... e.g. tire emissions will also exist for electric vehicles.  kai, jan'20
//			return warmEmissionsOfEvent;
//		} else {
//			efkey.setHbefaVehicleCategory(HbefaVehicleCategory.PASSENGER_CAR);
//		}

		// the above had a fall-through to passenger car ... which for, e.g., bicycles would be totally wrong.  Thus new version follows, which
		// should crash if enum not known:

		// fallback vehicle types that we cannot or do not want to map onto a hbefa vehicle type:
		if ( vehicleInformationTuple.getFirst()==HbefaVehicleCategory.NON_HBEFA_VEHICLE ) {
			for ( Pollutant warmPollutant : warmPollutants) {
				warmEmissionsOfEvent.put( warmPollutant, 0.0 );
				// yyyyyy todo replace by something more meaningful. kai, jan'20
			}
			if ( cnt >0 ) {
				logger.warn( "Just encountered non hbefa vehicle; currently, this code is setting the emissions of such vehicles to zero.  " +
							     "Might be necessary to find a better solution for this.  kai, jan'20" );
				cnt--;
				if ( cnt ==0 ) {
					logger.warn( Gbl.FUTURE_SUPPRESSED );
				}
			}
			return warmEmissionsOfEvent;
		}

		// translate vehicle information type into factor key.  yyyy maybe combine these two? kai, jan'20
		HbefaWarmEmissionFactorKey efkey = new HbefaWarmEmissionFactorKey();
		efkey.setHbefaVehicleCategory( vehicleInformationTuple.getFirst() );
		efkey.setHbefaRoadCategory( roadType );
		if(this.detailedHbefaWarmTable != null){
			HbefaVehicleAttributes hbefaVehicleAttributes = new HbefaVehicleAttributes();
			hbefaVehicleAttributes.setHbefaTechnology(vehicleInformationTuple.getSecond().getHbefaTechnology());
			hbefaVehicleAttributes.setHbefaSizeClass(vehicleInformationTuple.getSecond().getHbefaSizeClass());
			hbefaVehicleAttributes.setHbefaEmConcept(vehicleInformationTuple.getSecond().getHbefaEmConcept());
			efkey.setHbefaVehicleAttributes(hbefaVehicleAttributes);
		}


		double averageSpeed_kmh = (linkLength_m / 1000) / (travelTime_sec / 3600);

		// hedge against odd average speeds:
		if(averageSpeed_kmh <= 0.0){
			throw new RuntimeException("Average speed has been calculated to 0.0 or a negative value. Aborting...");
		}
		if ((averageSpeed_kmh - freeVelocity_ms * 3.6) > 1.0){
			if (ecg.handlesHighAverageSpeeds()) {
				logger.warn("averageSpeed was capped from " + averageSpeed_kmh + " to" + freeVelocity_ms * 3.6 );
				averageSpeed_kmh = freeVelocity_ms * 3.6;
			} else {
				throw new RuntimeException("Average speed has been calculated to be greater than free flow speed; this might produce negative warm emissions. Aborting...");
			}
		}

		// for the average speed method, the traffic situation is already known here:
		if (ecg.getEmissionsComputationMethod() == AverageSpeed) {
			final HbefaTrafficSituation trafficSituation = getTrafficSituation( efkey, averageSpeed_kmh, freeVelocity_ms * 3.6 );
//			logger.warn( "trafficSituation=" + trafficSituation );
			efkey.setHbefaTrafficSituation( trafficSituation );
		}

		double fractionStopGo = 0;

		// for each pollutant, compute and memorize emissions:
		for ( Pollutant warmPollutant : warmPollutants) {
			double generatedEmissions;

			efkey.setHbefaComponent(warmPollutant);

			double ef_gpkm;
			if (ecg.getEmissionsComputationMethod() == StopAndGoFraction) {

				// compute faction.  This cannot be done earlier since efkey.component is needed.
				fractionStopGo = getFractionStopAndGo( vehicleId, freeVelocity_ms * 3.6, averageSpeed_kmh, vehicleInformationTuple, efkey );

				double efStopGo_gpkm = 0. ;
				if ( fractionStopGo>0 ){
					// compute emissions from stop-go fraction:
					efkey.setHbefaTrafficSituation( STOPANDGO );
					efStopGo_gpkm = getEf( vehicleId, vehicleInformationTuple, efkey ).getWarmEmissionFactor();
				}

				double efFreeFlow_gpkm = 0. ;
				if ( fractionStopGo<1.){
					// compute emissions for free-flow fraction:
					efkey.setHbefaTrafficSituation( FREEFLOW );
					efFreeFlow_gpkm = getEf( vehicleId, vehicleInformationTuple, efkey ).getWarmEmissionFactor();
					logger.warn( "pollutant=" + warmPollutant + "; efFreeFlow=" + efFreeFlow_gpkm );
				}

				// sum them up:
				double fractionFreeFlow = 1 - fractionStopGo;
				ef_gpkm = (fractionFreeFlow * efFreeFlow_gpkm) + (fractionStopGo * efStopGo_gpkm);

			} else if (ecg.getEmissionsComputationMethod() == AverageSpeed){
				ef_gpkm = getEf(vehicleId, vehicleInformationTuple, efkey).getWarmEmissionFactor();
			} else {
				throw new RuntimeException( Gbl.NOT_IMPLEMENTED );
			}

			generatedEmissions = (linkLength_m / 1000) * ef_gpkm;
			warmEmissionsOfEvent.put(warmPollutant, generatedEmissions);
		}

		// update counters:
		// yy I don't know what this is good for; I would base downstream analysis rather on events.  kai, jan'20
		if (ecg.getEmissionsComputationMethod() == StopAndGoFraction) {
			incrementCountersFractional( linkLength_m / 1000, fractionStopGo );
		}
		else if (ecg.getEmissionsComputationMethod() == AverageSpeed) {
			incrementCountersAverage(efkey.getHbefaTrafficSituation(), linkLength_m / 1000 );
		} else {
			throw new RuntimeException( Gbl.NOT_IMPLEMENTED );
		}

		return warmEmissionsOfEvent;
	}

	private double getFractionStopAndGo(Id<Vehicle> vehicleId,
										double freeFlowSpeed_kmh, double averageSpeed_kmh,
										Tuple<HbefaVehicleCategory, HbefaVehicleAttributes> vehicleInformationTuple,
										HbefaWarmEmissionFactorKey efkey) {

		efkey.setHbefaTrafficSituation(STOPANDGO);
		double stopGoSpeedFromTable_kmh = getEf(vehicleId, vehicleInformationTuple, efkey).getSpeed();

		double fractionStopGo;

		if((averageSpeed_kmh - freeFlowSpeed_kmh) >= -1.0) { // both speeds are assumed to be not very different > only freeFlow on link
			fractionStopGo = 0.0;
		} else if ((averageSpeed_kmh - stopGoSpeedFromTable_kmh) <= 0.0) { // averageSpeed is less than stopGoSpeed > only stop&go on link
			fractionStopGo = 1.0;
		} else {
			fractionStopGo = stopGoSpeedFromTable_kmh * (freeFlowSpeed_kmh - averageSpeed_kmh) / (averageSpeed_kmh * (freeFlowSpeed_kmh - stopGoSpeedFromTable_kmh));
		}

		return fractionStopGo;
	}

	private HbefaWarmEmissionFactor getEf(Id<Vehicle> vehicleId, Tuple<HbefaVehicleCategory, HbefaVehicleAttributes> vehicleInformationTuple, HbefaWarmEmissionFactorKey efkey) {

		// if no detailed table, get result from average table:
		if ( this.detailedHbefaWarmTable==null ) {
			// set vehicle attributes to "average; average; average":
			efkey.setHbefaVehicleAttributes( new HbefaVehicleAttributes() );

			HbefaWarmEmissionFactor ef = this.avgHbefaWarmTable.get( efkey );
			Gbl.assertNotNull( ef );
			return ef;
		}

		// try lookup in detailed table:
		if ( this.detailedHbefaWarmTable.get( efkey ) != null ) {
			return this.detailedHbefaWarmTable.get( efkey );
		}

		// if vehicle deliberately specified as average;average;average, the lookup needs to be in the average table:
		HbefaVehicleAttributes attributes = vehicleInformationTuple.getSecond();
		if ( "average".equals( attributes.getHbefaEmConcept() ) && "average".equals( attributes.getHbefaSizeClass() ) && "average".equals( attributes.getHbefaTechnology() ) ) {
			HbefaWarmEmissionFactor ef = this.avgHbefaWarmTable.get( efkey );
			Gbl.assertNotNull( ef );
			return ef;
		}

		// try to re-write the key from hbefa3.x to hbefa4.x:
		HbefaVehicleAttributes attribs2 = new HbefaVehicleAttributes();

		// technology is copied:
		attribs2.setHbefaTechnology( vehicleInformationTuple.getSecond().getHbefaTechnology() );

		// size class is "not specified":
		attribs2.setHbefaSizeClass( "not specified" );

		// em concept is re-written with different dashes:
		switch( vehicleInformationTuple.getSecond().getHbefaEmConcept() ) {
			case "PC-P-Euro-1":
				attribs2.setHbefaEmConcept( "PC P Euro-1" );
				break;
			case "PC-P-Euro-2":
				attribs2.setHbefaEmConcept( "PC P Euro-2" );
				break;
			case "PC-P-Euro-3":
				attribs2.setHbefaEmConcept( "PC P Euro-3" );
				break;
			case "PC-P-Euro-4":
				attribs2.setHbefaEmConcept( "PC P Euro-4" );
				break;
			case "PC-P-Euro-5":
				attribs2.setHbefaEmConcept( "PC P Euro-5" );
				break;
			case "PC-P-Euro-6":
				attribs2.setHbefaEmConcept( "PC P Euro-6" );
				break;
			case "PC-D-Euro-1":
				attribs2.setHbefaEmConcept( "PC D Euro-1" );
				break;
			case "PC-D-Euro-2":
				attribs2.setHbefaEmConcept( "PC D Euro-2" );
				break;
			case "PC-D-Euro-3":
				attribs2.setHbefaEmConcept( "PC D Euro-3" );
				break;
			case "PC-D-Euro-4":
				attribs2.setHbefaEmConcept( "PC D Euro-4" );
				break;
			case "PC-D-Euro-5":
				attribs2.setHbefaEmConcept( "PC D Euro-5" );
				break;
			case "PC-D-Euro-6":
				attribs2.setHbefaEmConcept( "PC D Euro-6" );
				break;
		}

		// put this into a new key ...
		HbefaWarmEmissionFactorKey efkey2 = new HbefaWarmEmissionFactorKey( efkey );
		efkey2.setHbefaVehicleAttributes( attribs2 );

		// ... and try to look up:
		if ( this.detailedHbefaWarmTable.get( efkey2 ) != null ) {
			return this.detailedHbefaWarmTable.get( efkey2 );
		}

//		// try "<technology>; average; average":
//		attribs2.setHbefaSizeClass( "average" );
//		attribs2.setHbefaEmConcept( "average" );
//		if ( this.detailedHbefaWarmTable.get( efkey2 ) != null ) {
//			return this.detailedHbefaWarmTable.get( efkey2 );
//		}
		// lookups of type "<technology>; average; average" should, I think, just be entered as such. kai, feb'20

//		logger.warn( "did not find emission factor for efkey=" + efkey );
//		logger.warn( " re-written to " + efkey2 );
//		logger.warn( "" );
//		logger.warn( "full table:" );
//		List<String> list = new ArrayList<>();
//		for( HbefaWarmEmissionFactorKey key : this.detailedHbefaWarmTable.keySet() ){
//			list.add( key.toString() );
//		}
//		list.sort( String::compareTo );
//		for( String str : list ){
//			logger.warn( str );
//		}


		// set vehicle attributes to "average; average; average":
		efkey.setHbefaVehicleAttributes( new HbefaVehicleAttributes() );

		// try average emissions:
		if ( this.avgHbefaWarmTable.get( efkey )!=null ) {
			return this.avgHbefaWarmTable.get( efkey );
		} else {
			logger.warn( "did not find average emission factor for efkey=" + efkey );
			List<HbefaWarmEmissionFactorKey> list = new ArrayList<>( this.avgHbefaWarmTable.keySet() );
			list.sort( Comparator.comparing( HbefaWarmEmissionFactorKey::toString ) );
			for( HbefaWarmEmissionFactorKey key : list ){
				logger.warn( key.toString() );
			}
		}

		throw new RuntimeException( );


		// the above code is replacing the code below.

//		//The logic has changed here, now it will fall back to aggregate factors per traffic scenario, instead of if any scenarios are missing.
//		if(this.detailedHbefaWarmTable != null && this.detailedHbefaWarmTable.get(efkey) != null){
//			ef = this.detailedHbefaWarmTable.get(efkey);
//		} else {
//			vehAttributesNotSpecifiedCnt++;
//			efkey.setHbefaVehicleAttributes(new HbefaVehicleAttributes()); //want to check for average vehicle
//			ef = this.avgHbefaWarmTable.get(efkey);
//
//			int maxWarnCnt = 3;
//			if(this.detailedHbefaWarmTable != null && vehAttributesNotSpecifiedCnt <= maxWarnCnt) {
//				logger.warn("Detailed vehicle attributes are not specified correctly for vehicle " + vehicleId + ": " +
//						"`" + vehicleInformationTuple.getSecond() + "'. Using fleet average values instead.");
//				if(vehAttributesNotSpecifiedCnt == maxWarnCnt) logger.warn(Gbl.FUTURE_SUPPRESSED);
//			}
//
//			if ( ef==null ) {
////				logger.warn( "efkey=" + efkey ); //better throw RuntimeException with meaningful message instead
//				logger.error("Aborting... Did not found neither detailed nor average value for efkey = " + efkey , new RuntimeException());
//			}
////			Gbl.assertNotNull( ef ); //duplicate check
//		}
//		Gbl.assertNotNull( ef );
//		return ef;
	}

	//TODO: this is based on looking at the speeds in the HBEFA files, using an MFP, maybe from A.Loder would be nicer, jm  oct'18
	private HbefaTrafficSituation getTrafficSituation(HbefaWarmEmissionFactorKey efkey, double averageSpeed_kmh, double freeFlowSpeed_kmh) {
		//TODO: should this be generated only once much earlier?
		HbefaRoadVehicleCategoryKey roadTrafficKey = new HbefaRoadVehicleCategoryKey(efkey);
		Map<HbefaTrafficSituation, Double> trafficSpeeds = this.hbefaRoadTrafficSpeeds.get(roadTrafficKey);

		if (trafficSpeeds == null || !trafficSpeeds.containsKey(FREEFLOW)) {
			throw new RuntimeException("At least the FREEFLOW condition must be specifed for all emission factor keys. " +
					"It was not found for " + efkey);
		}

		HbefaTrafficSituation trafficSituation  = FREEFLOW;
		if (trafficSpeeds.containsKey(HEAVY) && averageSpeed_kmh <= trafficSpeeds.get(HEAVY)) {
			trafficSituation = HEAVY;
		}
		if (trafficSpeeds.containsKey(SATURATED) && averageSpeed_kmh <= trafficSpeeds.get(SATURATED)) {
			trafficSituation = SATURATED;
		}
		if (trafficSpeeds.containsKey(STOPANDGO) && averageSpeed_kmh <= trafficSpeeds.get(STOPANDGO)) {
			if (averageSpeed_kmh != trafficSpeeds.get(FREEFLOW)) { //handle case testCheckVehicleInfoAndCalculateWarmEmissions_and_throwWarmEmissionEvent6
				trafficSituation = STOPANDGO;
			}
		}
		return trafficSituation;
	}

	private void incrementCountersFractional(double linkLength_km, double fractionStopGo) {

		kmCounter = kmCounter + linkLength_km;
		emissionEventCounter++;

		freeFlowKmCounter += linkLength_km * (1-fractionStopGo);
		stopGoKmCounter += linkLength_km * fractionStopGo;

		freeFlowCounter += 1-fractionStopGo;
		stopGoCounter += fractionStopGo;
		fractionCounter += (fractionStopGo < 1.0 && fractionStopGo > 0.0) ? 1 : 0;
	}

	private void incrementCountersAverage(HbefaTrafficSituation hbefaTrafficSituation, double linkLength_km) {
		kmCounter = kmCounter + linkLength_km;
		emissionEventCounter++;

		switch (hbefaTrafficSituation) { // both speeds are assumed to be not very different > only freeFlow on link
			case FREEFLOW: {
				freeFlowCounter++;
				freeFlowKmCounter += linkLength_km;
				break;
			}
			case HEAVY: {
				saturatedCounter++;
				saturatedKmCounter += linkLength_km;
				break;
			}
			case SATURATED: {
				heavyFlowCounter++;
				heavyFlowKmCounter += linkLength_km;
				break;
			}
			case STOPANDGO: {
				stopGoCounter++;
				stopGoKmCounter += linkLength_km;
				break;
			}

		}
	}
	/*package-private*/ int getFreeFlowOccurences() {
		return freeFlowCounter;
	}
	/*package-private*/ private int getHeavyOccurences() {
		return heavyFlowCounter;
	}
	/*package-private*/ private int getSaturatedOccurences() {
		return saturatedCounter;
	}
	/*package-private*/ int getStopGoOccurences() {
		return stopGoCounter;
	}
	/*package-private*/ double getKmCounter() {
		return kmCounter;
	}
	/*package-private*/ double getFreeFlowKmCounter() {
		return freeFlowKmCounter;
	}
	/*package-private*/ private double getHeavyFlowKmCounter() {
		return heavyFlowKmCounter;
	}
	/*package-private*/ private double getSaturatedKmCounter() {
		return saturatedKmCounter;
	}
	/*package-private*/ double getStopGoKmCounter() {
		return stopGoKmCounter;
	}
	/*package-private*/ int getWarmEmissionEventCounter() {
		return emissionEventCounter;
	}
	/*package-private*/ int getFractionOccurences() {
		return fractionCounter;
	}
	/*package-private*/ double getFractionKmCounter() {
		return getSaturatedKmCounter() + getHeavyFlowKmCounter();
	}
	/*package-private*/ EmissionsConfigGroup getEcg() {
		return ecg;
	}

}
