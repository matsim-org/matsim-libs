package org.matsim.vehicles;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.matsim.utils.objectattributes.attributable.AttributesXmlReaderDelegate;
import org.xml.sax.Attributes;

import java.util.Stack;

final class VehicleReaderV2 extends MatsimXmlParser{
	private static final Logger log = LogManager.getLogger( VehicleReaderV2.class ) ;

	private final Vehicles vehicles;
	private final VehiclesFactory builder;
	private VehicleType currentVehType = null;

	private final AttributesXmlReaderDelegate attributesDelegate = new AttributesXmlReaderDelegate();
	private org.matsim.utils.objectattributes.attributable.Attributes currAttributes = null ;

	VehicleReaderV2( final Vehicles vehicles ){
		super(ValidationType.XSD_ONLY);
		log.info("Using " + this.getClass().getName());
		this.vehicles = vehicles;
		this.builder = this.vehicles.getFactory();
	}

	@Override
	public void endTag( final String name, final String content, final Stack<String> context ){
		switch( name ){
			case VehicleSchemaV2Names.DESCRIPTION:
				if( content.trim().length() > 0 ){
					this.currentVehType.setDescription( content.trim() );
				}
				break;
			case VehicleSchemaV2Names.VEHICLETYPE:
				this.vehicles.addVehicleType( this.currentVehType );
				this.currentVehType = null;
				break;
			case VehicleSchemaV2Names.ATTRIBUTES:
				/* fall-through */
			case VehicleSchemaV2Names.ATTRIBUTE:
				this.attributesDelegate.endTag( name, content, context );
				break;
			default:
				// do nothing
		}
	}

	@Override
	public void startTag( final String name, final Attributes atts, final Stack<String> context ){
		switch( name ){
			case VehicleSchemaV2Names.VEHICLETYPE:
				this.currentVehType = this.builder.createVehicleType( Id.create( atts.getValue( VehicleSchemaV2Names.ID ), VehicleType.class ) );
				this.currAttributes = this.currentVehType.getAttributes() ;
				break;
			case VehicleSchemaV2Names.LENGTH:
				this.currentVehType.setLength( Double.parseDouble( atts.getValue( VehicleSchemaV2Names.METER ) ) );
				break;
			case VehicleSchemaV2Names.WIDTH:
				this.currentVehType.setWidth( Double.parseDouble( atts.getValue( VehicleSchemaV2Names.METER ) ) );
				break;
			case VehicleSchemaV2Names.MAXIMUMVELOCITY:
				double val = Double.parseDouble( atts.getValue( VehicleSchemaV2Names.METERPERSECOND ) );
				if( val == 1.0 ) log.warn(
					  "The vehicle type's maximum velocity is set to 1.0 meter per second, is this really intended? vehicletype = " + this.currentVehType.getId().toString() );
				this.currentVehType.setMaximumVelocity( val );
				break;
			case VehicleSchemaV2Names.ENGINEINFORMATION:
				this.currAttributes = this.currentVehType.getEngineInformation().getAttributes() ;
				break;
			case VehicleSchemaV2Names.CAPACITY:
				if (atts.getValue( VehicleSchemaV2Names.SEATS ) != null) {
					this.currentVehType.getCapacity().setSeats( Integer.valueOf( atts.getValue( VehicleSchemaV2Names.SEATS ) ) );
				}
				if (atts.getValue( VehicleSchemaV2Names.STANDINGROOM) != null) {
				this.currentVehType.getCapacity().setStandingRoom( Integer.valueOf( atts.getValue( VehicleSchemaV2Names.STANDINGROOM ) ) );
				}
				if (atts.getValue( VehicleSchemaV2Names.WEIGHT) != null) {
					if(atts.getValue(VehicleSchemaV2Names.WEIGHT).contentEquals("INF")){
						this.currentVehType.getCapacity().setWeightInTons(Double.POSITIVE_INFINITY);
					} else {
						this.currentVehType.getCapacity().setWeightInTons(Double.parseDouble(atts.getValue(VehicleSchemaV2Names.WEIGHT)));
					}
				}
				if (atts.getValue( VehicleSchemaV2Names.VOLUME) != null) {
					if(atts.getValue(VehicleSchemaV2Names.VOLUME).contentEquals("INF")){
						this.currentVehType.getCapacity().setVolumeInCubicMeters(Double.POSITIVE_INFINITY);
					} else {
					log.warn(atts.getValue(VehicleSchemaV2Names.VOLUME));
						this.currentVehType.getCapacity().setVolumeInCubicMeters(Double.parseDouble(atts.getValue(VehicleSchemaV2Names.VOLUME)));
					}
				}
				if (atts.getValue( VehicleSchemaV2Names.OTHER ) != null) {
					this.currentVehType.getCapacity().setOther( Double.valueOf( atts.getValue( VehicleSchemaV2Names.OTHER ) ) );
				}
				this.currAttributes = this.currentVehType.getCapacity().getAttributes() ;
				break;
			case VehicleSchemaV2Names.COSTINFORMATION:
				if (atts.getValue( VehicleSchemaV2Names.FIXEDCOSTSPERDAY ) != null) {
					this.currentVehType.getCostInformation().setFixedCost(Double.valueOf(atts.getValue(VehicleSchemaV2Names.FIXEDCOSTSPERDAY ) ) );
				}
				if (atts.getValue( VehicleSchemaV2Names.COSTSPERMETER) != null) {
				this.currentVehType.getCostInformation().setCostsPerMeter(Double.valueOf( atts.getValue( VehicleSchemaV2Names.COSTSPERMETER ) ) );
				}
				if (atts.getValue( VehicleSchemaV2Names.COSTSPERSECOND) != null) {
					this.currentVehType.getCostInformation().setCostsPerSecond(Double.valueOf(atts.getValue(VehicleSchemaV2Names.COSTSPERSECOND ) ) );
				}
				this.currAttributes = this.currentVehType.getCostInformation().getAttributes();
				break;
			case VehicleSchemaV2Names.VEHICLE:
				String idString = atts.getValue( VehicleSchemaV2Names.ID );
				Id<VehicleType> typeId = Id.create( atts.getValue( VehicleSchemaV2Names.TYPE ), VehicleType.class );
				VehicleType type = this.vehicles.getVehicleTypes().get( typeId );
				if( type == null ){
					log.error( "VehicleType " + typeId + " does not exist." );
				}
				Id<Vehicle> id = Id.create( idString, Vehicle.class );
				Vehicle v = this.builder.createVehicle( id, type );
				currAttributes = v.getAttributes();
				this.vehicles.addVehicle( v );
				break;
			case VehicleSchemaV2Names.PASSENGERCAREQUIVALENTS:
				this.currentVehType.setPcuEquivalents( Double.parseDouble( atts.getValue( VehicleSchemaV2Names.PCE ) ) );
				break;
			case VehicleSchemaV2Names.FLOWEFFICIENCYFACTOR:
				this.currentVehType.setFlowEfficiencyFactor( Double.parseDouble( atts.getValue( VehicleSchemaV2Names.FACTOR ) ) );
				break;
			case VehicleSchemaV2Names.ATTRIBUTES:
				/* fall-through */
			case VehicleSchemaV2Names.ATTRIBUTE:
				attributesDelegate.startTag( name, atts, context, currAttributes );
				break;
			case VehicleSchemaV2Names.NETWORKMODE:
				this.currentVehType.setNetworkMode( atts.getValue( VehicleSchemaV2Names.NETWORKMODE ) );
				break;
			case VehicleSchemaV2Names.DESCRIPTION:
				this.currentVehType.setDescription( atts.getValue( VehicleSchemaV2Names.DESCRIPTION ) );
				break;
			default:
				throw new RuntimeException( "encountered unknown tag=" + name + " in context=" + context );
		}

	}

}
