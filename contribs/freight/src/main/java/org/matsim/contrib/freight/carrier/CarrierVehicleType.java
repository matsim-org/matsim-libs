package org.matsim.contrib.freight.carrier;

import org.matsim.api.core.v01.Id;
import org.matsim.core.gbl.Gbl;
import org.matsim.utils.objectattributes.attributable.Attributes;
import org.matsim.vehicles.*;

/**
 * The carrier vehicle type.
 * 
 * I decided to put vehicle cost information into the type (which is indeed not a physical attribute of the type). Thus physical and
 * non physical attributes are used. This is likely to be changed in future.
 * 
 * @author sschroeder
 *
 */
public class CarrierVehicleType implements VehicleType {
	private VehicleType delegate ;

	/**
	 * A builder building the type.
	 * 
	 * @author sschroeder
	 *
	 */
	public static class Builder {
		VehicleType delegate ;
		
		/**
		 * Returns a new instance of builder initialized with the typeId.
		 * 
		 * The defaults are [fix=0.0][perDistanceUnit=1.0][perTimeUnit=0.0].
		 * 
		 * @param typeId
		 * @return a type builder
		 */
		public static Builder newInstance(Id<VehicleType> typeId){
			return new Builder(typeId);
		}
		
		/**
		 * Returns a new instance of builder initialized with the typeId and the values the given from existing CarrierVehicleType.
		 * 
		 * Can be used for create a new, modified CarrierVehicleType basing on an existing one. 
		 * Values can be changed within the builder afterwards.
		 * 
		 * @param carrierVehicleType
		 * @param typeId
		 * @return a type builder
		 */
		public static Builder newInstance(Id<VehicleType> typeId, CarrierVehicleType carrierVehicleType){
//			return new Builder(typeId)
//					.setDescription(carrierVehicleType.getDescription())
//					.setEngineInformation(carrierVehicleType.getEngineInformation())
//					.setCapacity(carrierVehicleType.getCarrierVehicleCapacity())
//					.setMaxVelocity(carrierVehicleType.getMaximumVelocity())
//					.setVehicleCostInformation(carrierVehicleType.getVehicleCostInformation());
			throw new RuntimeException("not implemented") ;
		}
		
		private Builder(Id<VehicleType> typeId){
			this.delegate = VehicleUtils.getFactory().createVehicleType( typeId ) ;
		}
		
		/**
		 * Sets fixed costs of vehicle.
		 * 
		 * <p>By default it is 0.
		 * @param fix
		 * @return
		 */
		public Builder setFixCost(double fix){
			this.delegate.getCostInformation().setFixedCost( fix ) ;
			return this;
		}
		
		/**
		 * Sets costs per distance-unit.
		 * 
		 * <p>By default it is 1.
		 * 
		 * @param perDistanceUnit
		 * @return
		 */
		public Builder setCostPerDistanceUnit(double perDistanceUnit){
			this.delegate.getCostInformation().setCostsPerMeter( perDistanceUnit ) ;
			return this;
		}
		
		/**
		 * Sets costs per time-unit.
		 * 
		 * <p>By default it is 0.
		 * 
		 * @param perTimeUnit
		 * @return
		 */
		public Builder setCostPerTimeUnit(double perTimeUnit){
			this.delegate.getCostInformation().setCostsPerSecond( perTimeUnit ) ;
			return this;
		}
		
		/**
		 * Sets description.
		 * 
		 * @param description
		 * @return this builder
		 */
		public Builder setDescription(String description){
			this.delegate.setDescription( description ) ;
			return this;
		}
		
		/**
		 * Sets the capacity of vehicle-type.
		 * 
		 * <p>By defaul the capacity is 0.
		 * 
		 * @param capacity
		 * @return this builder
		 */
		public Builder setCapacity(int capacity){
			this.delegate.getCapacity().setOther( capacity );
			return this;
		}
		
		/**
		 * Builds the type.
		 * 
		 * @return {@link CarrierVehicleType}
		 */
		public CarrierVehicleType build(){
			return new CarrierVehicleType( delegate );
		}

		/**
		 * Sets {@link VehicleCostInformation}
		 * 
		 * <p>The defaults are [fix=0.0][perDistanceUnit=1.0][perTimeUnit=0.0].
		 * 
		 * @param info
		 * @return this builder
		 */
		public Builder setVehicleCostInformation(VehicleCostInformation info) {
			Gbl.assertIf( info.getAttributes().isEmpty() );
			delegate.getCostInformation().setFixedCost( info.getFixedCosts() ) ;
			delegate.getCostInformation().setCostsPerSecond( info.getCostsPerSecond() ) ;
			delegate.getCostInformation().setCostsPerSecond( info.getCostsPerSecond() ) ;
			return this;
		}

		/**
		 * Sets {@link EngineInformation}
		 * 
		 * @param engineInfo
		 * @return this builder
		 */
		public Builder setEngineInformation(EngineInformation engineInfo) {
			Gbl.assertIf( engineInfo.getAttributes().isEmpty() );
			this.delegate.getEngineInformation().setFuelConsumption( engineInfo.getFuelConsumption() );
			this.delegate.getEngineInformation().setFuelType( engineInfo.getFuelType() );
			return this;
		}

		public Builder setMaxVelocity(double veloInMeterPerSeconds) {
			this.delegate.setMaximumVelocity( veloInMeterPerSeconds ) ;
			return this;
		}
	}

	@Deprecated // refactoring device; please inline to CostInformation
	public static class VehicleCostInformation extends CostInformation {

	}

	private CarrierVehicleType( VehicleType delegate ){
		this.delegate = delegate ;
	}


	@Override public String getDescription(){
		return delegate.getDescription();
	}
	@Override public VehicleCapacity getCapacity(){
		return delegate.getCapacity();
	}
	@Override public Id<VehicleType> getId(){
		return delegate.getId();
	}
	@Override @Deprecated public double getAccessTime(){
		return delegate.getAccessTime();
	}
	@Override @Deprecated public double getEgressTime(){
		return delegate.getEgressTime();
	}
	@Override @Deprecated public void setAccessTime( double seconds ){
		delegate.setAccessTime( seconds );
	}
	@Override @Deprecated public void setEgressTime( double seconds ){
		delegate.setEgressTime( seconds );
	}
	@Override @Deprecated public DoorOperationMode getDoorOperationMode(){
		return delegate.getDoorOperationMode();
	}
	@Override @Deprecated public void setDoorOperationMode( DoorOperationMode mode ){
		delegate.setDoorOperationMode( mode );
	}
	@Override public double getPcuEquivalents(){
		return delegate.getPcuEquivalents();
	}
	@Override public VehicleType setPcuEquivalents( double pcuEquivalents ){
		return delegate.setPcuEquivalents( pcuEquivalents );
	}
	@Override public double getFlowEfficiencyFactor(){
		return delegate.getFlowEfficiencyFactor();
	}
	@Override public VehicleType setFlowEfficiencyFactor( double flowEfficiencyFactor ){
		return delegate.setFlowEfficiencyFactor( flowEfficiencyFactor );
	}
	@Override public Attributes getAttributes(){
		return delegate.getAttributes();
	}
	@Override public VehicleType setDescription( String desc ){
		return delegate.setDescription( desc );
	}
	@Override public VehicleType setLength( double length ){
		return delegate.setLength( length );
	}
	@Override public VehicleType setMaximumVelocity( double meterPerSecond ){
		return delegate.setMaximumVelocity( meterPerSecond );
	}
	@Override public VehicleType setWidth( double width ){
		return delegate.setWidth( width );
	}
	@Override public double getWidth(){
		return delegate.getWidth();
	}
	@Override public double getMaximumVelocity(){
		return delegate.getMaximumVelocity();
	}
	@Override public double getLength(){
		return delegate.getLength();
	}
	@Override public EngineInformation getEngineInformation(){
		return delegate.getEngineInformation();
	}
	@Override public CostInformation getCostInformation(){
		return delegate.getCostInformation();
	}
	@Override public String getNetworkMode(){
		return delegate.getNetworkMode();
	}
	@Override public void setNetworkMode( String networkMode ){
		delegate.setNetworkMode( networkMode );
	}
	@Override @Deprecated public VehicleType setCapacityWeightInTons( int i ){
		return delegate.setCapacityWeightInTons( i );
	}
	@Override @Deprecated public VehicleType setFixCost( double perDay ){
		return delegate.setFixCost( perDay );
	}
	@Override @Deprecated public VehicleType setCostPerDistanceUnit( double perMeter ){
		return delegate.setCostPerDistanceUnit( perMeter );
	}
	@Override @Deprecated public VehicleType setCostPerTimeUnit( double perSecond ){
		return delegate.setCostPerTimeUnit( perSecond );
	}

}
