package org.matsim.contrib.freight.carrier;

import org.matsim.api.core.v01.Id;
import org.matsim.vehicles.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CarrierUtils{
	public static Carrier createCarrier( Id<Carrier> id ){
		return new CarrierImpl(id);
	}

	public static CarrierPlan copyPlan( CarrierPlan plan2copy ) {
		List<ScheduledTour> tours = new ArrayList<ScheduledTour>();
		for (ScheduledTour sTour : plan2copy.getScheduledTours()) {
			double depTime = sTour.getDeparture();
			CarrierVehicle vehicle = sTour.getVehicle();
			Tour tour = sTour.getTour().duplicate();
			tours.add(ScheduledTour.newInstance(tour, vehicle, depTime));
		}
		CarrierPlan copiedPlan = new CarrierPlan(plan2copy.getCarrier(), tours);
		double initialScoreOfCopiedPlan = plan2copy.getScore();
		copiedPlan.setScore(initialScoreOfCopiedPlan);
		return copiedPlan;

	}

	/**
	 * A builder building the type.
	 *
	 * @author sschroeder
	 *
	 */
	public static class CarrierVehicleTypeBuilder{

		/**
		 * Returns a new instance of builder initialized with the typeId.
		 *
		 * The defaults are [fix=0.0][perDistanceUnit=1.0][perTimeUnit=0.0].
		 *
		 * @param typeId
		 * @return a type builder
		 */
		public static CarrierVehicleTypeBuilder newInstance( Id<VehicleType> typeId ){
			return new CarrierVehicleTypeBuilder(typeId);
		}

//		/**
//		 * Returns a new instance of builder initialized with the typeId and the values the given from existing CarrierVehicleType.
//		 *
//		 * Can be used for create a new, modified CarrierVehicleType basing on an existing one.
//		 * Values can be changed within the builder afterwards.
//		 *
//		 * @param carrierVehicleType
//		 * @param typeId
//		 * @return a type builder
//		 */
//		public static CarrierVehicleTypeBuilder newInstance( Id<VehicleType> typeId, VehicleType carrierVehicleType ){
//			return new CarrierVehicleTypeBuilder(typeId)
//					.setDescription(carrierVehicleType.getDescription())
//					.setEngineInformation(carrierVehicleType.getEngineInformation())
//					.setCapacityWeightInTons( carrierVehicleType.getCapacity().getWeightInTons() )
//					.setMaxVelocity(carrierVehicleType.getMaximumVelocity())
//					.setVehicleCostInformation(carrierVehicleType.getCostInformation());
//		}

		private final VehicleType vehicleType;
		// since the implementation is no longer immutable, we can as well have the final object directly. kai, sep'19

		private CarrierVehicleTypeBuilder( Id<VehicleType> typeId ){
			vehicleType = VehicleUtils.getFactory().createVehicleType( typeId ) ;
		}

		/**
		 * Sets fixed costs of vehicle.
		 *
		 * <p>By default it is 0.
		 * @param perDay
		 * @return
		 */
		public CarrierVehicleTypeBuilder setFixedCost( double perDay ){
			this.vehicleType.getCostInformation().setFixedCosts( perDay ) ;
			return this;
		}

		/**
		 * Sets costs per distance-unit.
		 *
		 * <p>By default it is 1.
		 *
		 * @param perMeter
		 * @return
		 */
		public CarrierVehicleTypeBuilder setCostPerMeter( double perMeter ){
			this.vehicleType.getCostInformation().setCostsPerMeter( perMeter ) ;
			return this;
		}

		/**
		 * Sets costs per time-unit.
		 *
		 * <p>By default it is 0.
		 *
		 * @param perSecond
		 * @return
		 */
		public CarrierVehicleTypeBuilder setCostPerSecond( double perSecond ){
			this.vehicleType.getCostInformation().setCostsPerSecond( perSecond ) ;
			return this;
		}

		/**
		 * Sets description.
		 *
		 * @param description
		 * @return this builder
		 */
		public CarrierVehicleTypeBuilder setDescription( String description ){
			this.vehicleType.setDescription( description );
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
		public CarrierVehicleTypeBuilder setCapacityWeightInTons( double capacity ){
			this.vehicleType.getCapacity().setWeightInTons( capacity ) ;
			return this;
		}

		/**
		 * Builds the type.
		 *
		 * @return {@link VehicleType}
		 */
		public VehicleType build(){
//			VehicleType vehicleType = new VehicleType( this.typeId );
////			vehicleType.setCostInformation(new CostInformation(this.fix, this.perDistanceUnit, this.perTimeUnit) );
//			vehicleType.getCostInformation().setFixedCosts( this.fix );
//			vehicleType.getCostInformation().setCostsPerMeter( this.perDistanceUnit );
//			vehicleType.getCostInformation().setCostsPerSecond( this.perTimeUnit );
////			if(this.engineInfo != null) vehicleType.setEngineInformation(this.engineInfo);
//			if ( this.engineInfo != null ) {
//				for( Map.Entry<String, Object> entry : this.engineInfo.getAttributes().getAsMap().entrySet() ){
//					vehicleType.getEngineInformation().getAttributes().putAttribute( entry.getKey(), entry.getValue() ) ;
//				}
//			}
//			if(this.description != null) vehicleType.setDescription(this.description);
//
////		capacity = builder.capacity;
////			VehicleCapacity aCapacity = new VehicleCapacity() ;
//			vehicleType.getCapacity().setWeightInTons( this.weightInTons );
////			vehicleType.setCapacity( aCapacity );
//
//			vehicleType.setMaximumVelocity(this.maxVeloInMeterPerSeconds);
//			return vehicleType ;
			return this.vehicleType ;
		}

//		/**
//		 * Sets {@link CostInformation}
//		 *
//		 * <p>The defaults are [fix=0.0][perDistanceUnit=1.0][perTimeUnit=0.0].
//		 *
//		 * @param info
//		 * @return this builder
//		 */
//		public CarrierVehicleTypeBuilder setVehicleCostInformation( CostInformation info ) {
//			fix = info.getFixedCosts();
//			perDistanceUnit = info.getCostsPerMeter();
//			perTimeUnit = info.getCostsPerSecond();
//			return this;
//		}

//		/**
//		 * Sets {@link EngineInformation}
//		 *
//		 * @param engineInfo
//		 * @return this builder
//		 */
//		public CarrierVehicleTypeBuilder setEngineInformation( EngineInformation engineInfo ) {
//			this.engineInfo = engineInfo;
//			return this;
//		}
		public EngineInformation getEngineInformation() {
			return this.vehicleType.getEngineInformation() ;
		}

		public CarrierVehicleTypeBuilder setMaxVelocity( double veloInMeterPerSeconds ) {
			this.vehicleType.setMaximumVelocity( veloInMeterPerSeconds );
			return this;
		}
	}
}
