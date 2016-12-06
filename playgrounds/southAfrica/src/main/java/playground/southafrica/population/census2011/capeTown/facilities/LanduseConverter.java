/* *********************************************************************** *
 * project: org.matsim.*
 * LanduseConverter.java                                                                        *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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
 * 
 */
package playground.southafrica.population.census2011.capeTown.facilities;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.accessibility.FacilityTypes;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.misc.Counter;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacilitiesFactory;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityOption;
import org.matsim.facilities.FacilitiesUtils;
import org.opengis.feature.simple.SimpleFeature;

import playground.southafrica.population.census2011.capeTown.facilities.Parcel.Landuse;

/**
 * Class to convert a parcel with various land uses into a facility with 
 * (possibly) multiple activity types.
 * 
 * @author jwjoubert
 */
public class LanduseConverter {
	final private Logger log = Logger.getLogger(LanduseConverter.class);
	private ActivityFacilitiesFactory factory;
	private ActivityFacilities facilities;
	private Map<Id<Parcel>, Parcel> parcels = new TreeMap<Id<Parcel>, Parcel>();
//	private final CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation("EPSG:2048", "EPSG:3857");
	private final CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation("SA_Lo19", "SA_Lo19");
	
	public LanduseConverter() {
		this.facilities = null;
		this.factory = null;
	}
	
	public void convertFeature(SimpleFeature feature){
		Parcel parcel = new Parcel(feature);
		
		/* this check was due since some objects in the geodatabase seems to 
		 * have default geometries that are null. */
		if(parcel.getCoord() != null){
			parcels.put(parcel.getId(), parcel);
		}
	}
	
	public ActivityFacilities convertParcelsToFacilities(){
		log.info("Converting parcels to facilities...");
		this.facilities = FacilitiesUtils.createActivityFacilities("Cape Town landuse");
		this.factory = this.facilities.getFactory();
		
		Counter counter = new Counter("  converted # ");
		for(Parcel parcel : this.parcels.values()){
			List<ActivityOption> options = extractActivityOptionsFromLanduse(parcel.getLanduses());
			
			if(options.size() > 0){
				ActivityFacility facility = factory.createActivityFacility(
						Id.create(parcel.getId().toString(), ActivityFacility.class), 
						ct.transform(parcel.getCoord()));
				for(ActivityOption option : options){
					facility.addActivityOption(option);
				}
				facilities.addActivityFacility(facility);
			}
			counter.incCounter();
		}
		counter.printCounter();
		log.info("Done converting parcels. Total number of facilities: " + this.facilities.getFacilities().size());
		return this.facilities;
	}
	
	public List<ActivityOption> extractActivityOptionsFromLanduse(Map<Landuse, Double> landuse){
		List<ActivityOption> options = new ArrayList<>();
		
		/* Home */
		if(	landuse.containsKey(Landuse.RES_GENERIC) ||
			landuse.containsKey(Landuse.RES_MULTI) ||
			landuse.containsKey(Landuse.RES_SINGLE)){
			/*FIXME Should MIXED_USE be included here? */
			ActivityOption home = factory.createActivityOption(FacilityTypes.HOME);

			/*TODO To determine if a housing unit has one or more households, 
			 * I use an arbitrary threshold of 500m^2, assuming that as soon
			 * as a parcel has an area greater than that, their are space for
			 * multiple (theoretically infinite) households. */
			Double dGeneric = landuse.get(Landuse.RES_GENERIC);
			Double dMulti = landuse.get(Landuse.RES_MULTI);
			Double dSingle = landuse.get(Landuse.RES_SINGLE);
			double area = 0.0;
			area += dGeneric != null ? dGeneric : 0.0;
			area += dMulti != null ? dMulti : 0.0;
			area += dSingle != null ? dSingle : 0.0;
			if(area > 1000.0){
				home.setCapacity(Double.POSITIVE_INFINITY);
			} else{
				home.setCapacity(1.0);
			}
			options.add(home);
		}
		
		/* Work */
		if( landuse.containsKey(Landuse.AGRIC) ||
			landuse.containsKey(Landuse.BUS_GENERIC) ||
			landuse.containsKey(Landuse.BUS_OFFICE) ||
			landuse.containsKey(Landuse.BUS_RETAIL) ||
			landuse.containsKey(Landuse.CIV_GENERIC) ||
			landuse.containsKey(Landuse.CIV_HOSPITAL) ||
			landuse.containsKey(Landuse.CIV_PUBLIC_SERVICE) ||
			landuse.containsKey(Landuse.IND_GENERIC) ||
			landuse.containsKey(Landuse.IND_SRVICE) ||
			landuse.containsKey(Landuse.IND_WAREHOUSE) ||
			landuse.containsKey(Landuse.INST_GENERIC) ||
			landuse.containsKey(Landuse.INST_POI) ||
			landuse.containsKey(Landuse.MINING) ||
			landuse.containsKey(Landuse.MIXED_USE) ||
			landuse.containsKey(Landuse.TRANSPORT) ||
			landuse.containsKey(Landuse.UTILITY)){
			ActivityOption work = factory.createActivityOption(FacilityTypes.WORK);
			options.add(work);
		}
		
		/* Shopping */
		if( landuse.containsKey(Landuse.BUS_RETAIL) ||
			landuse.containsKey(Landuse.MIXED_USE) ||
			landuse.containsKey(Landuse.IND_SRVICE)){
			ActivityOption shopping = factory.createActivityOption(FacilityTypes.SHOPPING);
			options.add(shopping);
		}
		
		/* Leisure */
		if( landuse.containsKey(Landuse.BUS_RETAIL) ||
			landuse.containsKey(Landuse.MIXED_USE)  ||
			landuse.containsKey(Landuse.INST_POI)){
			ActivityOption leisure = factory.createActivityOption(FacilityTypes.LEISURE);
			options.add(leisure);
		}
		
		/* Medical */
		if( landuse.containsKey(Landuse.CIV_HOSPITAL) ){
			ActivityOption medical = factory.createActivityOption(FacilityTypes.MEDICAL);
			options.add(medical);
		}
		
		/* Other */
		if( landuse.containsKey(Landuse.CIV_PUBLIC_SERVICE) ||
			landuse.containsKey(Landuse.IND_GENERIC) ||
			landuse.containsKey(Landuse.IND_SRVICE) ||
			landuse.containsKey(Landuse.INST_GENERIC) ||
			landuse.containsKey(Landuse.INST_POI) ||
			landuse.containsKey(Landuse.MIXED_USE) ||
			landuse.containsKey(Landuse.NR_GENERIC) ||
			landuse.containsKey(Landuse.OS) ||
			landuse.containsKey(Landuse.OTHER) ||
			landuse.containsKey(Landuse.SPECIAL) ||
			landuse.containsKey(Landuse.TRANSPORT) ||
			landuse.containsKey(Landuse.UTILITY)) {
			ActivityOption other = factory.createActivityOption(FacilityTypes.OTHER);
			options.add(other);
		}
		
		return options;
	}
	
	
	public ActivityFacilities getFacilities(){
		if(this.facilities == null){
			log.warn("ActivityFacilities container is empty. First call the convertParcelsToFacilities() method");
			log.warn("Returning null");
		}
		return this.facilities;
	}
	
	
	
	public ActivityFacility createFacilityFromFeature(SimpleFeature feature){
		Object o = feature.getAttribute("OBJECTID");
		
		Id<ActivityFacility> id = null;
		if(o != null && o instanceof String){
			id = Id.create((String)o, ActivityFacility.class);
		}
		
		
		Coord coord = null;
		ActivityFacility facility = factory.createActivityFacility(id, coord);
		
		
		return facility;
	}
	
	
	public void reportLanduseCounts(){
		Map<Integer, Integer> landuseCounts = new TreeMap<>();

		for(Parcel parcel : this.parcels.values()){
			int count = parcel.getNumberOfLanduses();
			if(landuseCounts.containsKey(count)){
				int oldCount = landuseCounts.get(count);
				landuseCounts.put(count, oldCount+1);
			} else{
				landuseCounts.put(count, 1);
			}

		}
		
		log.info("Land use counts (number of land uses: number of observations)...");
		for(int i : landuseCounts.keySet()){
			log.info(String.format("  %d: %d", i, landuseCounts.get(i)));
		}
	}
	
	
}
