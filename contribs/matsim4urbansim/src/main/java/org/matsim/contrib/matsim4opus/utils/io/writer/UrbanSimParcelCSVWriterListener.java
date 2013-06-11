package org.matsim.contrib.matsim4opus.utils.io.writer;

import java.util.Iterator;

import org.apache.log4j.Logger;
import org.matsim.contrib.accessibility.gis.SpatialGrid;
import org.matsim.contrib.accessibility.interfaces.SpatialGridDataExchangeInterface;
import org.matsim.contrib.matsim4opus.interpolation.Interpolation;
import org.matsim.contrib.matsim4opus.utils.misc.ProgressBar;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.facilities.ActivityFacilitiesImpl;

public class UrbanSimParcelCSVWriterListener implements SpatialGridDataExchangeInterface {
	
	private static final Logger log = Logger.getLogger(UrbanSimParcelCSVWriterListener.class);
	
	private final ActivityFacilitiesImpl parcels;
	
	/**
	 * constructor
	 * @param parcels
	 */
	public UrbanSimParcelCSVWriterListener(final ActivityFacilitiesImpl parcels){
		this.parcels = parcels;
	}
	
	/**
	 * This is triggered by GridBasedAccessibilityControlerListener when the 
	 * accessibility computation is finished.
	 * The SpatialGrids with the accessibility measures are taken, interpolated 
	 * from the grid for given parcel coordinates and written out in UrbanSim format.
	 */
	public void getAndProcessSpatialGrids(SpatialGrid freeSpeedGrid, SpatialGrid carGrid,
			SpatialGrid bikeGrid, SpatialGrid walkGrid, SpatialGrid ptGrid) {

		// from here accessibility feedback for each parcel
		UrbanSimParcelCSVWriter.initUrbanSimZoneWriter();
		
		Interpolation freeSpeedGridInterpolation = null;
		Interpolation carGridInterpolation = null;
		Interpolation bikeGridInterpolation = null;
		Interpolation walkGridInterpolation = null;
		Interpolation ptGridInterpolation = null;
		
		if(freeSpeedGrid != null)
			freeSpeedGridInterpolation = new Interpolation(freeSpeedGrid, Interpolation.BILINEAR);
		if(carGrid != null)
			carGridInterpolation  = new Interpolation(carGrid,	Interpolation.BILINEAR);
		if(bikeGrid != null)
			bikeGridInterpolation = new Interpolation(bikeGrid, Interpolation.BILINEAR);
		if(walkGrid != null)
			walkGridInterpolation = new Interpolation(walkGrid, Interpolation.BILINEAR);
		if(ptGrid != null)
			ptGridInterpolation   = new Interpolation(ptGrid, Interpolation.BILINEAR);

		if (this.parcels != null) {

			int numberOfParcels = this.parcels.getFacilities().size();
			double freeSpeedAccessibility = Double.NaN;
			double carAccessibility = Double.NaN;
			double bikeAccessibility = Double.NaN;
			double walkAccessibility = Double.NaN;
			double ptAccessibility = Double.NaN;

			log.info(numberOfParcels + " parcels are now processing ...");

			Iterator<ActivityFacility> parcelIterator = this.parcels.getFacilities().values().iterator();
			ProgressBar bar = new ProgressBar(numberOfParcels);
			
			// this iterates through all parcel coordinates ...
			while (parcelIterator.hasNext()) {

				bar.update();

				ActivityFacility parcel = parcelIterator.next();
				
				// accessibilities are interpolated from the SpatialGrid for the given parcel coordinate
				if(freeSpeedGrid != null)
					freeSpeedAccessibility = freeSpeedGridInterpolation.interpolate(parcel.getCoord());
				if(carGrid != null)
					carAccessibility = carGridInterpolation.interpolate(parcel.getCoord());
				if(bikeGrid != null)
					bikeAccessibility = bikeGridInterpolation.interpolate(parcel.getCoord());
				if(walkGrid != null)
					walkAccessibility = walkGridInterpolation.interpolate(parcel.getCoord());
				if(ptGrid != null)
					ptAccessibility = ptGridInterpolation.interpolate(parcel.getCoord());
				
				// interpolated accessibility for each parcel is written out in UrbanSim format
				UrbanSimParcelCSVWriter.write(parcel.getId(), freeSpeedAccessibility, carAccessibility,
											  bikeAccessibility, walkAccessibility, ptAccessibility);
			}
			log.info("... done!");
			UrbanSimParcelCSVWriter.close();
		}
	}
}
