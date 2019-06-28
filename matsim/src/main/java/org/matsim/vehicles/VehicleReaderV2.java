package org.matsim.vehicles;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.matsim.utils.objectattributes.attributable.AttributesXmlReaderDelegate;
import org.xml.sax.Attributes;

import java.util.Map;
import java.util.Stack;
import java.util.stream.Collectors;

class VehicleReaderV2 extends MatsimXmlParser{
	private static final Logger log = Logger.getLogger( VehicleReaderV2.class ) ;

	private final Vehicles vehicles;
	private final VehiclesFactory builder;
	private VehicleType currentVehType = null;
	private VehicleCapacity currentCapacity = null;
	private FreightCapacity currentFreightCapacity = null;
	private EngineInformation currentEngineInformation = null;
//	private EngineInformation.FuelType currentFuelType = null;
//	private double fixedCostsPerDay = Double.NaN;
//	private double costsPerMeter = Double.NaN;
//	private double costsPerSecond = Double.NaN;

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
		} else if( VehicleSchemaV2Names.ENGINEINFORMATION.equalsIgnoreCase( name ) ){
			this.currentVehType.setEngineInformation( this.currentEngineInformation );
			this.currentEngineInformation = null ;
//				VehicleUtils.setEngineInformation(this.currentVehType, this.currentEngineInformation.getAttributes());
//				this.currentEngineInformation = null;
				//TODO new settings from engineInformationAttributes
			//VehicleUtils.setEngineInformation(this.currentVehType, this.currentFuelType, VehicleUtils.getFuelConsumption(this.currentVehType));
			//this.currentFuelType = null;
//		} else if( VehicleSchemaV2Names.COSTSINFORMATION.equalsIgnoreCase( name ) ){
//			CostInformation currentCostInformation = this.builder.createCostInformation(this.fixedCostsPerDay, this.costsPerMeter, this.costsPerSecond);
//			this.currentVehType.setCostInformation(currentCostInformation);
//			this.fixedCostsPerDay = Double.NaN;
//			this.costsPerMeter = Double.NaN;
//			this.costsPerSecond = Double.NaN;
		} else if( VehicleSchemaV2Names.FREIGHTCAPACITY.equalsIgnoreCase( name ) ){
			this.currentCapacity.setFreightCapacity( this.currentFreightCapacity);
			this.currentFreightCapacity = null;
		} else if( VehicleSchemaV2Names.CAPACITY.equalsIgnoreCase( name ) ){
			this.currentVehType.setCapacity( this.currentCapacity );
			this.currentCapacity = null;
		} else if( VehicleSchemaV2Names.VEHICLETYPE.equalsIgnoreCase( name ) ){
			this.vehicles.addVehicleType( this.currentVehType );
			this.currentVehType = null;
		} else if (name.equalsIgnoreCase( VehicleSchemaV2Names.ATTRIBUTES )) {
			this.currAttributes = null;
		} else if (name.equalsIgnoreCase(VehicleSchemaV2Names.ATTRIBUTE)) {
			this.attributesReader.endTag( name , content , context );
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
		} else if( VehicleSchemaV2Names.MAXIMUMVELOCITY.equalsIgnoreCase( name ) ) {
			double val = Double.parseDouble(atts.getValue(VehicleSchemaV2Names.METERPERSECOND));
			if (val == 1.0) {
				log.warn(
						"The vehicle type's maximum velocity is set to 1.0 meter per second, is this really intended? vehicletype = " + this.currentVehType.getId().toString());
			}
			this.currentVehType.setMaximumVelocity(val);
		} else if( VehicleSchemaV2Names.ENGINEINFORMATION.equalsIgnoreCase( name ) ){
			this.currentEngineInformation = new EngineInformationImpl();
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
			double fixedCostsPerDay = Double.parseDouble( atts.getValue( VehicleSchemaV2Names.FIXEDCOSTSPERDAY ) );
			double costsPerMeter = Double.parseDouble( atts.getValue( VehicleSchemaV2Names.COSTSPERMETER ) );
			double costsPerSecond = Double.parseDouble( atts.getValue( VehicleSchemaV2Names.COSTSPERSECOND ) );
			CostInformation currentCostInformation = this.builder.createCostInformation(fixedCostsPerDay, costsPerMeter, costsPerSecond);
			this.currentVehType.setCostInformation(currentCostInformation);
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
			this.currentVehType.setFlowEfficiencyFactor( Double.parseDouble( atts.getValue( VehicleSchemaV2Names.FACTOR) ) );
		} else if (name.equalsIgnoreCase(VehicleSchemaV2Names.ATTRIBUTES)) {
			log.warn( "attributes encountered; context.peek()=" + context.peek() ) ;
			if (context.peek().equalsIgnoreCase(VehicleSchemaV2Names.VEHICLETYPE)){
				currAttributes = this.currentVehType.getAttributes();
				attributesReader.startTag( name, atts, context, currAttributes );
			} else if (context.peek().equalsIgnoreCase(VehicleSchemaV2Names.ENGINEINFORMATION)) {
				currAttributes = this.currentEngineInformation.getAttributes();
				attributesReader.startTag( name , atts , context, currAttributes );
			} else {
				throw new RuntimeException("encountered attributes for context in which they are not registered; context=" + context ) ;
			}
		} else if (name.equalsIgnoreCase(VehicleSchemaV2Names.ATTRIBUTE)) {
			attributesReader.startTag( name , atts , context, currAttributes );
			log.warn("---") ;
			for( Map.Entry<String, Object> entry : currAttributes.getAsMap().entrySet() ){
				log.warn( "key=" + entry.getKey() + "; value=" + entry.getValue()) ;
			}
			log.warn("---") ;
		} else if( VehicleSchemaV2Names.NETWORKMODE.equalsIgnoreCase( name ) ){
			this.currentVehType.setNetworkMode( atts.getValue( VehicleSchemaV2Names.NETWORKMODE ) );
			//		}
			//		else if (name.equalsIgnoreCase(VehicleSchemaV2Names.EIATTRIBUTES)) {
			////			if (context.peek().equalsIgnoreCase(VehicleSchemaV2Names.VEHICLETYPE)) {
			//				currEiAttributes = this.currentEngineInformation.getAttributes();
			//				attributesReader.startTag( name , atts , context, currEiAttributes );
			////			}
			//		}
			//		else if (name.equalsIgnoreCase(VehicleSchemaV2Names.EIATTRIBUTE)) {
			//			attributesReader.startTag( name , atts , context, currEiAttributes );
		} else if ( name.equalsIgnoreCase( VehicleSchemaV2Names.DESCRIPTION ) ) {
			this.currentVehType.setDescription( atts.getValue( VehicleSchemaV2Names.DESCRIPTION ) );
		} else {
			throw new RuntimeException("encountered unknown tag=" + name + " in context=" + context  ) ;
		}

	}

}
