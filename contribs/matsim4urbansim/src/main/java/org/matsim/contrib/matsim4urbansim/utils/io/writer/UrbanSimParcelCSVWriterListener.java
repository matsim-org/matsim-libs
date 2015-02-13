package org.matsim.contrib.matsim4urbansim.utils.io.writer;

import org.apache.log4j.Logger;
import org.matsim.contrib.accessibility.Modes4Accessibility;
import org.matsim.contrib.accessibility.gis.SpatialGrid;
import org.matsim.contrib.accessibility.interfaces.SpatialGridDataExchangeInterface;
import org.matsim.contrib.matsim4urbansim.interpolation.Interpolation;
import org.matsim.contrib.matsim4urbansim.utils.io.misc.ProgressBar;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.config.Config;
import org.matsim.facilities.ActivityFacilitiesImpl;

import java.util.HashMap;
import java.util.Map;

public class UrbanSimParcelCSVWriterListener implements SpatialGridDataExchangeInterface {
	
	private static final Logger log = Logger.getLogger(UrbanSimParcelCSVWriterListener.class);
	
	private final ActivityFacilitiesImpl parcels;

	private Config config;
	
	/**
	 * constructor
	 * @param parcels
	 * @param config 
	 */
	public UrbanSimParcelCSVWriterListener(final ActivityFacilitiesImpl parcels, Config config){
		this.parcels = parcels;
		this.config = config;
	}
	
	/**
	 * This is triggered by GridBasedAccessibilityControlerListener when the 
	 * accessibility computation is finished.
	 * The SpatialGrids with the accessibility measures are taken, interpolated 
	 * from the grid for given parcel coordinates and written out in UrbanSim format.
	 */
	@Override
	public void getAndProcessSpatialGrids( Map<Modes4Accessibility,SpatialGrid> spatialGrids ) {
			
		// from here accessibility feedback for each parcel
		UrbanSimParcelCSVWriter.initUrbanSimZoneWriter(config);
		
		Map<Modes4Accessibility,Interpolation> interpolations = new HashMap<Modes4Accessibility,Interpolation>() ;
		
		for ( Modes4Accessibility mode : Modes4Accessibility.values() ) {
			final SpatialGrid spatialGrid = spatialGrids.get(mode);
			if ( spatialGrid != null ) {
				interpolations.put( mode, new Interpolation( spatialGrid, Interpolation.BILINEAR ) ) ;
			}
		}

		if (this.parcels != null) {

			int numberOfParcels = this.parcels.getFacilities().size();
			
			Map<Modes4Accessibility,Double> interpolatedAccessibilities = new HashMap<Modes4Accessibility,Double>() ;

			log.info(numberOfParcels + " parcels are now processing ...");

			ProgressBar bar = new ProgressBar(numberOfParcels);
			
			// this iterates through all parcel coordinates ...
			for ( ActivityFacility parcel : this.parcels.getFacilities().values() ) {
				bar.update();

				for ( Modes4Accessibility mode : Modes4Accessibility.values() ) {
					final SpatialGrid spatialGrid = spatialGrids.get(mode);
					if ( spatialGrid != null ) {
						
					}
				}
				
				// accessibilities are interpolated from the SpatialGrid for the given parcel coordinate
				for ( Modes4Accessibility mode : Modes4Accessibility.values() ) {
					final SpatialGrid spatialGrid = spatialGrids.get(mode);
					if ( spatialGrid != null ) {
						interpolatedAccessibilities.put( mode, interpolations.get(mode).interpolate(parcel.getCoord())) ;
					}
				}
				
				// interpolated accessibility for each parcel is written out in UrbanSim format
				UrbanSimParcelCSVWriter.write(parcel.getId(), interpolatedAccessibilities ) ;
			}
			log.info("... done!");
			UrbanSimParcelCSVWriter.close(config);
		}
	}
}
