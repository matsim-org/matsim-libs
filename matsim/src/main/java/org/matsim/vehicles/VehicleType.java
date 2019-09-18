package org.matsim.vehicles;

import org.matsim.api.core.v01.Id;
import org.matsim.utils.objectattributes.attributable.Attributable;
import org.matsim.utils.objectattributes.attributable.Attributes;

public interface VehicleType extends Attributable{
	String getDescription();
	VehicleCapacity getCapacity();
	Id<VehicleType> getId();
	double getPcuEquivalents();
	VehicleType setPcuEquivalents( double pcuEquivalents );
	double getFlowEfficiencyFactor();
	VehicleType setFlowEfficiencyFactor( double flowEfficiencyFactor );
	VehicleType setDescription( String desc );
	VehicleType setLength( double length );
	VehicleType setMaximumVelocity( double meterPerSecond );
	VehicleType setWidth( double width );
	double getWidth();
	double getMaximumVelocity();
	double getLength();
	EngineInformation getEngineInformation();
	CostInformation getCostInformation();
	String getNetworkMode();
	void setNetworkMode( String networkMode );

	@Deprecated double getAccessTime();
	@Deprecated double getEgressTime();
	@Deprecated void setAccessTime( double seconds );
	@Deprecated void setEgressTime( double seconds );
	@Deprecated DoorOperationMode getDoorOperationMode();
	@Deprecated void setDoorOperationMode( DoorOperationMode mode );

	@Deprecated // refactoring device, please inline
	default VehicleType setCapacityWeightInTons( int i ) {
		this.getCapacity().setWeightInTons( i ) ;
		return this ;
	}
	@Deprecated // refactoring device, please inline
	VehicleType setFixCost( double perDay );
	@Deprecated // refactoring device, please inline
	VehicleType setCostPerDistanceUnit( double perMeter );
	@Deprecated // refactoring device, please inline
	VehicleType setCostPerTimeUnit( double perSecond );
	@Deprecated // refactoring device, please inline
	default CostInformation getVehicleCostInformation() {
		return getCostInformation() ;
	}
	@Deprecated // refactoring device, please inline
	default int getCarrierVehicleCapacity() {
		return getCapacity().getOther().intValue() ;
	}

	@Deprecated
	public enum DoorOperationMode{ serial, parallel }

//	@Deprecated // refactoring device, please delete
//	VehicleType build();
	// after CarrierVehicleType.Builder is back, this should no longer be needed

}
