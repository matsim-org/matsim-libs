package org.matsim.contrib.freight.carrier;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.matsim.vehicles.CostInformation;
import org.matsim.vehicles.EngineInformation;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.xml.sax.Attributes;

import java.util.Stack;

class CarrierVehicleTypeReaderV1 extends MatsimXmlParser {
	private static final Logger log = Logger.getLogger(CarrierVehicleTypeReaderV1.class) ;

	private static int wrnCnt=10 ;

	private final CarrierVehicleTypes carrierVehicleTypes;
	private VehicleType currentType;

	CarrierVehicleTypeReaderV1( CarrierVehicleTypes carrierVehicleTypes ) {
		super();
		this.carrierVehicleTypes = carrierVehicleTypes;
		this.setValidating(false);
	}


	@Override
	public void startTag( String name, Attributes attributes, Stack<String> context ) {
		if(name.equals("vehicleType")){
			Id<VehicleType> currentTypeId = Id.create( attributes.getValue( "id" ), VehicleType.class );
			this.currentType = VehicleUtils.getFactory().createVehicleType( currentTypeId ) ;
		}
		if(name.equals("allowableWeight")){
//			String weight = atts.getValue("weight");
//			Double.parseDouble( weight );
			// yyyyyy what is this?  kai, sep'19
			if ( wrnCnt>0 ){
				log.warn( "allowableWeight is ignored (and has always been)." );
				wrnCnt--;
				if( wrnCnt == 0 ){
					log.warn( Gbl.FUTURE_SUPPRESSED );
				}
			}
		}
		if(name.equals("engineInformation")){
			EngineInformation engineInfo = this.currentType.getEngineInformation() ;
			VehicleUtils.setFuelConsumption(this.currentType, Double.parseDouble(attributes.getValue( "gasConsumption" )));
			engineInfo.setFuelConsumption( Double.parseDouble( attributes.getValue( "gasConsumption" ) ) );
			engineInfo.setFuelType( EngineInformation.FuelType.valueOf( attributes.getValue( "fuelType" ) ) );
		}

		if(name.equals("costInformation")){
			String fix = attributes.getValue("fix");
			// yyyyyy shouldn't this be "perDay"??? kai, aug'19
			String perMeter = attributes.getValue("perMeter");
			String perSecond = attributes.getValue("perSecond");
			if(fix == null || perMeter == null || perSecond == null) throw new IllegalStateException("cannot read costInformation correctly. probably the paramName was written wrongly");
			CostInformation vehicleCosts = this.currentType.getCostInformation();
			vehicleCosts.setFixedCost( Double.valueOf( fix ) );
			vehicleCosts.setCostsPerMeter( Double.valueOf( perMeter ) );
			vehicleCosts.setCostsPerSecond( Double.valueOf( perSecond ) );
		}
	}

	@Override
	public void endTag(String name, String content, Stack<String> context) {
		if(name.equals("description")){//Ca
			this.currentType.setDescription( content );
		}
		if(name.equals("capacity")){
			this.currentType.getCapacity().setOther( Double.parseDouble( content ) ) ;
		}
		if(name.equals("maxVelocity")){
			this.currentType.setMaximumVelocity( Double.parseDouble( content ) );
		}
		if(name.equals("vehicleType")){
			carrierVehicleTypes.getVehicleTypes().put(this.currentType.getId(), currentType );
			reset();
		}

	}

	private void reset() {
		currentType = null ;
	}

}
