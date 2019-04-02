package org.matsim.vehicles;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.matsim.utils.objectattributes.attributable.AttributesXmlReaderDelegate;
import org.xml.sax.Attributes;

import java.util.Stack;

class VehicleReaderV2 extends MatsimXmlParser{
	private static final Logger log = Logger.getLogger( VehicleReaderV2.class ) ;

	private final Vehicles vehicles;
	private final VehiclesFactory builder;
	private VehicleType currentVehType = null;
	private VehicleCapacity currentCapacity = null;
	private FreightCapacity currentFreightCapacity = null;
	private EngineInformation.FuelType currentFuelType = null;
	private double fixedCostsPerDay = Double.NaN;
	private double costsPerMeter = Double.NaN;
	private double costsPerSecond = Double.NaN;

	private final AttributesXmlReaderDelegate attributesReader = new AttributesXmlReaderDelegate();
	private org.matsim.utils.objectattributes.attributable.Attributes currAttributes =
		  new org.matsim.utils.objectattributes.attributable.Attributes();

	public VehicleReaderV2( final Vehicles vehicles ){
		log.info("Using " + this.getClass().getName());
		this.vehicles = vehicles;
		this.builder = this.vehicles.getFactory();
	}

	@Override
	public void endTag( final String name, final String content, final Stack<String> context ){
		if( VehicleSchemaV2Names.DESCRIPTION.equalsIgnoreCase( name ) && (content.trim().length() > 0) ){
			this.currentVehType.setDescription( content.trim() );
		} else if (name.equalsIgnoreCase( VehicleSchemaV2Names.ATTRIBUTES )) {
			this.currAttributes = null;
		} else if (name.equalsIgnoreCase(VehicleSchemaV2Names.ATTRIBUTE)) {
			this.attributesReader.endTag( name , content , context );}
		else if( VehicleSchemaV2Names.ENGINEINFORMATION.equalsIgnoreCase( name ) ){
			VehicleUtils.setEngineInformation(this.currentVehType, this.currentFuelType, VehicleUtils.getFuelConsumption(this.currentVehType));
			this.currentFuelType = null;
		} else if( VehicleSchemaV2Names.COSTSINFORMATION.equalsIgnoreCase( name ) ){
			CostInformation currentCostInformation = this.builder.createCostInformation(this.fixedCostsPerDay, this.costsPerMeter, this.costsPerSecond);
			this.currentVehType.setCostInformation(currentCostInformation);
			this.fixedCostsPerDay = Double.NaN;
			this.costsPerMeter = Double.NaN;
			this.costsPerSecond = Double.NaN;
		} else if( VehicleSchemaV2Names.FUELTYPE.equalsIgnoreCase( name ) ){
			this.currentFuelType = this.parseFuelType( content.trim() );
		} else if( VehicleSchemaV2Names.FREIGHTCAPACITY.equalsIgnoreCase( name ) ){
			this.currentCapacity.setFreightCapacity( this.currentFreightCapacity);
			this.currentFreightCapacity = null;
		} else if( VehicleSchemaV2Names.CAPACITY.equalsIgnoreCase( name ) ){
			this.currentVehType.setCapacity( this.currentCapacity );
			this.currentCapacity = null;
		} else if( VehicleSchemaV2Names.VEHICLETYPE.equalsIgnoreCase( name ) ){
			this.vehicles.addVehicleType( this.currentVehType );
			this.currentVehType = null;

		}
	}

	private EngineInformation.FuelType parseFuelType( final String content ){
		if( EngineInformation.FuelType.gasoline.toString().equalsIgnoreCase( content ) ){
			return EngineInformation.FuelType.gasoline;
		} else if( EngineInformation.FuelType.diesel.toString().equalsIgnoreCase( content ) ){
			return EngineInformation.FuelType.diesel;
		} else if( EngineInformation.FuelType.electricity.toString().equalsIgnoreCase( content ) ){
			return EngineInformation.FuelType.electricity;
		} else if( EngineInformation.FuelType.biodiesel.toString().equalsIgnoreCase( content ) ){
			return EngineInformation.FuelType.biodiesel;
		} else{
			throw new IllegalArgumentException( "Fuel type: " + content + " is not supported!" );
		}
	}

	private VehicleType.DoorOperationMode parseDoorOperationMode( final String modeString ){
		if( VehicleType.DoorOperationMode.serial.toString().equalsIgnoreCase( modeString ) ){
			return VehicleType.DoorOperationMode.serial;
		} else if( VehicleType.DoorOperationMode.parallel.toString().equalsIgnoreCase( modeString ) ){
			return VehicleType.DoorOperationMode.parallel;
		} else{
			throw new IllegalArgumentException( "Door operation mode " + modeString + " is not supported" );
		}
	}

	@Override
	public void startTag( final String name, final Attributes atts, final Stack<String> context ){
		if( VehicleSchemaV2Names.VEHICLETYPE.equalsIgnoreCase( name ) ){
			this.currentVehType = this.builder.createVehicleType( Id.create( atts.getValue( VehicleSchemaV2Names.ID ), VehicleType.class ) );
		} else if( VehicleSchemaV2Names.LENGTH.equalsIgnoreCase( name ) ){
			this.currentVehType.setLength( Double.parseDouble( atts.getValue( VehicleSchemaV2Names.METER ) ) );
		} else if( VehicleSchemaV2Names.WIDTH.equalsIgnoreCase( name ) ){
			this.currentVehType.setWidth( Double.parseDouble( atts.getValue( VehicleSchemaV2Names.METER ) ) );
		} else if( VehicleSchemaV2Names.MAXIMUMVELOCITY.equalsIgnoreCase( name ) ){
			double val = Double.parseDouble( atts.getValue( VehicleSchemaV2Names.METERPERSECOND ) );
			if( val == 1.0 ){
				log.warn(
					  "The vehicle type's maximum velocity is set to 1.0 meter per second, is this really intended? vehicletype = " + this.currentVehType.getId().toString() );
			}
			this.currentVehType.setMaximumVelocity( val );
		} else if( VehicleSchemaV2Names.CAPACITY.equalsIgnoreCase( name ) ){
			this.currentCapacity = this.builder.createVehicleCapacity();
		} else if( VehicleSchemaV2Names.SEATS.equalsIgnoreCase( name ) ){
			this.currentCapacity.setSeats( Integer.valueOf( atts.getValue( VehicleSchemaV2Names.PERSONS ) ) );
		} else if( VehicleSchemaV2Names.STANDINGROOM.equalsIgnoreCase( name ) ){
			this.currentCapacity.setStandingRoom( Integer.valueOf( atts.getValue( VehicleSchemaV2Names.PERSONS ) ) );
		} else if( VehicleSchemaV2Names.FREIGHTCAPACITY.equalsIgnoreCase( name ) ){
			this.currentFreightCapacity = this.builder.createFreigthCapacity();
		} else if( VehicleSchemaV2Names.VOLUME.equalsIgnoreCase( name ) ){
			this.currentFreightCapacity.setVolume( Double.parseDouble( atts.getValue( VehicleSchemaV2Names.CUBICMETERS ) ) );
		} else if( VehicleSchemaV2Names.WEIGHT.equalsIgnoreCase( name ) ){
			this.currentFreightCapacity.setWeight( Double.parseDouble( atts.getValue( VehicleSchemaV2Names.TONS) ) );
		} else if( VehicleSchemaV2Names.COSTINFORMATION.equalsIgnoreCase( name ) ){
			this.fixedCostsPerDay = Double.parseDouble( atts.getValue( VehicleSchemaV2Names.FIXEDCOSTSPERDAY) );
			this.costsPerMeter = Double.parseDouble( atts.getValue( VehicleSchemaV2Names.COSTSPERMETER ) );
			this.costsPerSecond = Double.parseDouble( atts.getValue( VehicleSchemaV2Names.COSTSPERSECOND ) );
		} else if( VehicleSchemaV2Names.VEHICLE.equalsIgnoreCase( name ) ){
			Id<VehicleType> typeId = Id.create( atts.getValue( VehicleSchemaV2Names.TYPE ), VehicleType.class );
			VehicleType type = this.vehicles.getVehicleTypes().get( typeId );
			if( type == null ){
				log.error( "VehicleType " + typeId + " does not exist." );
			}
			String idString = atts.getValue( VehicleSchemaV2Names.ID );
			Id<Vehicle> id = Id.create( idString, Vehicle.class );
			Vehicle v = this.builder.createVehicle( id, type );
			this.vehicles.addVehicle( v );
		} else if( VehicleSchemaV2Names.PASSENGERCAREQUIVALENTS.equalsIgnoreCase( name ) ){
			this.currentVehType.setPcuEquivalents( Double.parseDouble( atts.getValue( VehicleSchemaV2Names.PCE ) ) );
		} else if( VehicleSchemaV2Names.FLOWEFFICIENCYFACTOR.equalsIgnoreCase( name ) ){
			this.currentVehType.setFlowEfficiencyFactor( Double.parseDouble( atts.getValue( VehicleSchemaV2Names.FEF ) ) );
		} else if (name.equalsIgnoreCase(VehicleSchemaV2Names.ATTRIBUTES)) {
			if (context.peek().equalsIgnoreCase(VehicleSchemaV2Names.VEHICLETYPE)) {
				currAttributes = this.currentVehType.getAttributes();
				attributesReader.startTag( name , atts , context, currAttributes );
			}
		}
		else if (name.equalsIgnoreCase(VehicleSchemaV2Names.ATTRIBUTE)) {
			attributesReader.startTag( name , atts , context, currAttributes );
		}

	}

}
