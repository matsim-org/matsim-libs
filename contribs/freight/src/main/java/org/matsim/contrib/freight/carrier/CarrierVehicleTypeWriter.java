package org.matsim.contrib.freight.carrier;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.internal.MatsimWriter;
import org.matsim.core.utils.io.MatsimXmlWriter;
import org.matsim.vehicles.*;

import java.io.IOException;
import java.util.Map;

/**
 * A writer that writes carriers and their plans in an xml-file.
 * 
 * @author sschroeder
 *
 */
public class CarrierVehicleTypeWriter implements MatsimWriter {

	private MatsimVehicleWriter delegate ;

	public CarrierVehicleTypeWriter( CarrierVehicleTypes types ) {
		// note: for reading, we do the automatic version handling.  for writing, we just always write the newest version; the older writer handlers are
		// left around if someone insists on writing the old version.  Since the carrier vehicle type format is just a subset of the vehicle definitions,
		// we can just use the normal vehicle writer.  kai, sep'19

		Vehicles vehicles = VehicleUtils.createVehiclesContainer() ;
		for( Map.Entry<Id<VehicleType>, VehicleType> entry : types.getVehicleTypes().entrySet() ){
			vehicles.addVehicleType( entry.getValue() );
		}
		delegate = new MatsimVehicleWriter( vehicles ) ;
	}

	@Override public void write( String filename ){
		delegate.writeFile( filename );
	}

}
