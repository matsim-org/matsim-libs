package org.matsim.vehicles;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;

import java.util.Stack;

final class VehicleReaderV1 extends MatsimXmlParser{
	private static final Logger log = Logger.getLogger(VehicleReaderV1.class) ;

	private final Vehicles vehicles;
	private final VehiclesFactory builder;
	private VehicleType currentVehType = null;
	private VehicleCapacity currentCapacity = null;
//	private FreightCapacity currentFreightCap = null;
	private EngineInformation.FuelType currentFuelType = null;

	VehicleReaderV1( final Vehicles vehicles ){
		log.info("Using " + this.getClass().getName());
		this.vehicles = vehicles;
		this.builder = this.vehicles.getFactory();
	}

	@Override
	public void endTag( final String name, final String content, final Stack<String> context ){
		if( VehicleSchemaV1Names.DESCRIPTION.equalsIgnoreCase( name ) && (content.trim().length() > 0) ){
			this.currentVehType.setDescription( content.trim() );
		} else if( VehicleSchemaV1Names.ENGINEINFORMATION.equalsIgnoreCase( name ) ){
//			VehicleUtils.setEngineInformation(this.currentVehType, this.currentFuelType, VehicleUtils.getFuelConsumption(this.currentVehType));
			this.currentFuelType = null;
		} else if( VehicleSchemaV1Names.FUELTYPE.equalsIgnoreCase( name ) ){
			this.currentFuelType = this.parseFuelType( content.trim() );
		} else if( VehicleSchemaV1Names.FREIGHTCAPACITY.equalsIgnoreCase( name ) ){
//			this.currentCapacity.setFreightCapacity( this.currentFreightCap );
//			this.currentFreightCap = null;
		} else if( VehicleSchemaV1Names.CAPACITY.equalsIgnoreCase( name ) ){
			this.currentVehType.setCapacity( this.currentCapacity );
			this.currentCapacity = null;
		} else if( VehicleSchemaV1Names.VEHICLETYPE.equalsIgnoreCase( name ) ){
			this.vehicles.addVehicleType( this.currentVehType );
			this.currentVehType = null;
		}
	}

	private EngineInformation.FuelType parseFuelType( final String content ){
		return EngineInformation.FuelType.valueOf(content) ;
//		if( EngineInformation.FuelType.gasoline.toString().equalsIgnoreCase( content ) ){
//			return EngineInformation.FuelType.gasoline;
//		} else if( EngineInformation.FuelType.diesel.toString().equalsIgnoreCase( content ) ){
//			return EngineInformation.FuelType.diesel;
//		} else if( EngineInformation.FuelType.electricity.toString().equalsIgnoreCase( content ) ){
//			return EngineInformation.FuelType.electricity;
//		} else if( EngineInformation.FuelType.biodiesel.toString().equalsIgnoreCase( content ) ){
//			return EngineInformation.FuelType.biodiesel;
//		} else{
//			throw new IllegalArgumentException( "Fuel type: " + content + " is not supported!" );
//		}
	}

	private VehicleUtils.DoorOperationMode parseDoorOperationMode( final String modeString ){
		return VehicleUtils.DoorOperationMode.valueOf(modeString) ;
//		if( VehicleUtils.DoorOperationMode.serial.toString().equalsIgnoreCase( modeString ) ){
//			return VehicleUtils.DoorOperationMode.serial;
//		} else if( VehicleUtils.DoorOperationMode.parallel.toString().equalsIgnoreCase( modeString ) ){
//			return VehicleUtils.DoorOperationMode.parallel;
//		} else{
//			throw new IllegalArgumentException( "Door operation mode " + modeString + " is not supported" );
//		}
	}

	@Override
	public void startTag( final String name, final Attributes atts, final Stack<String> context ){
		if( VehicleSchemaV1Names.VEHICLETYPE.equalsIgnoreCase( name ) ){
			this.currentVehType = this.builder.createVehicleType( Id.create( atts.getValue( VehicleSchemaV1Names.ID ), VehicleType.class ) );
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
		} else if( VehicleSchemaV1Names.CAPACITY.equalsIgnoreCase( name ) ){
			this.currentCapacity = this.builder.createVehicleCapacity();
		} else if( VehicleSchemaV1Names.SEATS.equalsIgnoreCase( name ) ){
			this.currentCapacity.setSeats( Integer.valueOf( atts.getValue( VehicleSchemaV1Names.PERSONS ) ) );
		} else if( VehicleSchemaV1Names.STANDINGROOM.equalsIgnoreCase( name ) ){
			this.currentCapacity.setStandingRoom( Integer.valueOf( atts.getValue( VehicleSchemaV1Names.PERSONS ) ) );
		} else if( VehicleSchemaV1Names.FREIGHTCAPACITY.equalsIgnoreCase( name ) ){
//			this.currentFreightCap = this.builder.createFreigthCapacity();
		} else if( VehicleSchemaV1Names.VOLUME.equalsIgnoreCase( name ) ){
//			this.currentFreightCap.setVolume(Double.parseDouble( atts.getValue( VehicleSchemaV1Names.CUBICMETERS ) ));
			this.currentCapacity.setVolumeInCubicMeters( Double.parseDouble( atts.getValue( VehicleSchemaV1Names.CUBICMETERS ) ) );
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
		    VehicleUtils.setDoorOperationMode(this.currentVehType, this.parseDoorOperationMode((atts.getValue(VehicleSchemaV1Names.MODE))));
		} else if( VehicleSchemaV1Names.PASSENGERCAREQUIVALENTS.equalsIgnoreCase( name ) ){
			this.currentVehType.setPcuEquivalents( Double.parseDouble( atts.getValue( VehicleSchemaV1Names.PCE ) ) );
		}
	}

}
