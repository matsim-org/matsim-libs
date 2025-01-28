package org.matsim.vehicles;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;

import java.util.Stack;

final class VehicleReaderV1 extends MatsimXmlParser{
	private static final Logger log = LogManager.getLogger(VehicleReaderV1.class) ;

	private final Vehicles vehicles;
	private final VehiclesFactory builder;
	private VehicleType currentVehType = null;

	VehicleReaderV1( final Vehicles vehicles ){
		super(ValidationType.XSD_ONLY);
		log.info("Using " + this.getClass().getName());
		this.vehicles = vehicles;
		this.builder = this.vehicles.getFactory();
	}

	@Override
	public void endTag( final String name, final String content, final Stack<String> context ){
		if( VehicleSchemaV1Names.DESCRIPTION.equalsIgnoreCase( name ) && (content.trim().length() > 0) ){
			this.currentVehType.setDescription( content.trim() );
		} else if( VehicleSchemaV1Names.FUELTYPE.equalsIgnoreCase( name ) ){
			VehicleUtils.setFuelType(this.currentVehType.getEngineInformation(), EngineInformation.FuelType.valueOf(content.trim()));
		} else if( VehicleSchemaV1Names.VEHICLETYPE.equalsIgnoreCase( name ) ){
			this.vehicles.addVehicleType( this.currentVehType );
			this.currentVehType = null;
		}
	}

	@Override
	public void startTag( final String name, final Attributes atts, final Stack<String> context ){
		if( VehicleSchemaV1Names.VEHICLETYPE.equalsIgnoreCase( name ) ){
			this.currentVehType = this.builder.createVehicleType( Id.create( atts.getValue( VehicleSchemaV1Names.ID ), VehicleType.class ) );
			// In the old format there is no network mode, and everything was basically a car.
			// Vehicle type does not contain a default network mode anymore, therefore we need to set it here.
			this.currentVehType.setNetworkMode( TransportMode.car );

		} else if( VehicleSchemaV1Names.LENGTH.equalsIgnoreCase( name ) ){
			this.currentVehType.setLength( Double.parseDouble( atts.getValue( VehicleSchemaV1Names.METER ) ) );
		} else if( VehicleSchemaV1Names.WIDTH.equalsIgnoreCase( name ) ){
			this.currentVehType.setWidth( Double.parseDouble( atts.getValue( VehicleSchemaV1Names.METER ) ) );
		} else if( VehicleSchemaV1Names.MAXIMUMVELOCITY.equalsIgnoreCase( name ) ){
			double val = Double.parseDouble( atts.getValue( VehicleSchemaV1Names.METERPERSECOND ) );
			if( val == 1.0 ){
				log.warn(
					  "The vehicle type's maximum velocity is set to 1.0 meter per second, is this really intended? vehicletype = " + this.currentVehType.getId().toString() );
			}
			this.currentVehType.setMaximumVelocity( val );
		} else if( VehicleSchemaV1Names.SEATS.equalsIgnoreCase( name ) ){
			this.currentVehType.getCapacity().setSeats( Integer.valueOf( atts.getValue( VehicleSchemaV1Names.PERSONS ) ) );
		} else if( VehicleSchemaV1Names.STANDINGROOM.equalsIgnoreCase( name ) ){
			this.currentVehType.getCapacity().setStandingRoom( Integer.valueOf( atts.getValue( VehicleSchemaV1Names.PERSONS ) ) );
		} else if( VehicleSchemaV1Names.VOLUME.equalsIgnoreCase( name ) ) {
			if(atts.getValue(VehicleSchemaV1Names.CUBICMETERS).contentEquals("INF")){
				this.currentVehType.getCapacity().setVolumeInCubicMeters(Double.POSITIVE_INFINITY);
			} else {
				this.currentVehType.getCapacity().setVolumeInCubicMeters(Double.parseDouble(atts.getValue(VehicleSchemaV1Names.CUBICMETERS)));
			}
		} else if( VehicleSchemaV1Names.GASCONSUMPTION.equalsIgnoreCase( name ) ){
			VehicleUtils.setFuelConsumption(this.currentVehType, Double.parseDouble( atts.getValue( VehicleSchemaV1Names.LITERPERMETER )) );
		} else if( VehicleSchemaV1Names.VEHICLE.equalsIgnoreCase( name ) ){
			Id<VehicleType> typeId = Id.create( atts.getValue( VehicleSchemaV1Names.TYPE ), VehicleType.class );
			VehicleType type = this.vehicles.getVehicleTypes().get( typeId );
			if( type == null ){
				log.error( "VehicleType " + typeId + " does not exist." );
			}
			String idString = atts.getValue( VehicleSchemaV1Names.ID );
			Id<Vehicle> id = Id.create( idString, Vehicle.class );
			Vehicle v = this.builder.createVehicle( id, type );
			this.vehicles.addVehicle( v );
		} else if( VehicleSchemaV1Names.ACCESSTIME.equalsIgnoreCase( name ) ){
			VehicleUtils.setAccessTime(this.currentVehType, Double.parseDouble( atts.getValue( VehicleSchemaV1Names.SECONDSPERPERSON ) ));
		} else if( VehicleSchemaV1Names.EGRESSTIME.equalsIgnoreCase( name ) ){
		    VehicleUtils.setEgressTime(this.currentVehType, Double.parseDouble( atts.getValue( VehicleSchemaV1Names.SECONDSPERPERSON ) ));
		} else if( VehicleSchemaV1Names.DOOROPERATION.equalsIgnoreCase( name ) ){
			VehicleUtils.setDoorOperationMode(this.currentVehType, VehicleType.DoorOperationMode.valueOf((atts.getValue(VehicleSchemaV1Names.MODE )) ) );
		} else if( VehicleSchemaV1Names.PASSENGERCAREQUIVALENTS.equalsIgnoreCase( name ) ){
			this.currentVehType.setPcuEquivalents( Double.parseDouble( atts.getValue( VehicleSchemaV1Names.PCE ) ) );
		}
	}

}
