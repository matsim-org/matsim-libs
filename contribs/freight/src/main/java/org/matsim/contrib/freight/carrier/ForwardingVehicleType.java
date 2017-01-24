package org.matsim.contrib.freight.carrier;

import org.matsim.api.core.v01.Id;
import org.matsim.vehicles.EngineInformation;
import org.matsim.vehicles.VehicleCapacity;
import org.matsim.vehicles.VehicleType;

public class ForwardingVehicleType implements VehicleType{

	private VehicleType vType;
	
	public ForwardingVehicleType(VehicleType vType) {
		super();
		this.vType = vType;
	}

	@Override
	public void setDescription(String desc) { vType.setDescription(desc); }
		
	@Override
	public void setLength(double length) { vType.setLength(length); }

	@Override
	public void setWidth(double width) { vType.setWidth(width); }

	@Override
	public void setMaximumVelocity(double meterPerSecond) { vType.setMaximumVelocity(meterPerSecond); }

	@Override
	public void setEngineInformation(EngineInformation currentEngineInfo) { vType.setEngineInformation(currentEngineInfo); }

	@Override
	public void setCapacity(VehicleCapacity capacity) { vType.setCapacity(capacity); }

	@Override
	public double getWidth() { return vType.getWidth(); }

	@Override
	public double getMaximumVelocity() { return vType.getMaximumVelocity(); }
	
	@Override
	public double getLength() { return vType.getLength(); }

	@Override
	public EngineInformation getEngineInformation() { return vType.getEngineInformation(); }

	@Override
	public String getDescription() { return vType.getDescription(); }

	@Override
	public VehicleCapacity getCapacity() { return vType.getCapacity(); }

	@Override
	public Id<VehicleType> getId() { return vType.getId(); }

	@Override
	@Deprecated
	public double getAccessTime() { throw new UnsupportedOperationException(); }

	@Override
	@Deprecated
	public void setAccessTime(double seconds) { throw new UnsupportedOperationException(); }

	@Override
	@Deprecated
	public double getEgressTime() { throw new UnsupportedOperationException(); }

	@Override
	@Deprecated
	public void setEgressTime(double seconds) { throw new UnsupportedOperationException(); }

	@Override
	public DoorOperationMode getDoorOperationMode() { return vType.getDoorOperationMode(); }

	@Override
	public void setDoorOperationMode(DoorOperationMode mode) { vType.setDoorOperationMode(mode); }

	@Override
	public double getPcuEquivalents() { return vType.getPcuEquivalents(); } 

	@Override
	public void setPcuEquivalents(double pcuEquivalents) { vType.setPcuEquivalents(pcuEquivalents); }
	
    @Override
    public double getFlowEfficiencyFactor() { return vType.getFlowEfficiencyFactor(); } 

    @Override
    public void setFlowEfficiencyFactor(double flowEfficiencyFactor) { vType.setFlowEfficiencyFactor(flowEfficiencyFactor); }
}
