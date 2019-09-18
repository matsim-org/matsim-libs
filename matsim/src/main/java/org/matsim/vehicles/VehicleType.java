package org.matsim.vehicles;

import org.matsim.api.core.v01.Id;
import org.matsim.utils.objectattributes.attributable.Attributable;
import org.matsim.utils.objectattributes.attributable.Attributes;

public interface VehicleType extends Attributable{
	String getDescription();
	VehicleCapacity getCapacity();
	Id<VehicleType> getId();
	@Deprecated double getAccessTime();
	@Deprecated double getEgressTime();
	@Deprecated void setAccessTime( double seconds );
	@Deprecated void setEgressTime( double seconds );
	@Deprecated DoorOperationMode getDoorOperationMode();
	@Deprecated void setDoorOperationMode( DoorOperationMode mode );
	double getPcuEquivalents();
	VehicleType setPcuEquivalents( double pcuEquivalents );
	double getFlowEfficiencyFactor();
	VehicleType setFlowEfficiencyFactor( double flowEfficiencyFactor );
	@Override Attributes getAttributes();
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
	@Deprecated // refactoring device, please inline
	VehicleType setCapacityWeightInTons( int i );
	@Deprecated // refactoring device, please inline
	VehicleType setFixCost( double perDay );
	@Deprecated // refactoring device, please inline
	VehicleType setCostPerDistanceUnit( double perMeter );
	@Deprecated // refactoring device, please inline
	VehicleType setCostPerTimeUnit( double perSecond );
	@Deprecated // refactoring device, please delete
	VehicleType build();
	@Deprecated
	public enum DoorOperationMode{ serial, parallel }
}
