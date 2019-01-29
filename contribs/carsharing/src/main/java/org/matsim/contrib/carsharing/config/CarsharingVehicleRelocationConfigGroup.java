package org.matsim.contrib.carsharing.config;

import org.matsim.core.config.ReflectiveConfigGroup;

public class CarsharingVehicleRelocationConfigGroup extends ReflectiveConfigGroup{

	public static final String GROUP_NAME = "CarsharingVehicleRelocation";

	private String relocationZonesInputFile = null;

	private String relocationTimesInputFile = null;

	private String relocationAgentsInputFile = null;

	private Integer moduleEnableAfterIteration = null;

	private Integer demandEstimateIterations = null;

	public CarsharingVehicleRelocationConfigGroup() {
		super(GROUP_NAME);
	}

	@StringGetter( "relocationZones" )
	public String getRelocationZones() {
		return this.relocationZonesInputFile;
	}

	@StringSetter( "relocationZones" )
	public void setRelocationZones(final String relocationZonesInputFile) {
		this.relocationZonesInputFile = relocationZonesInputFile;
	}

	@StringGetter( "relocationTimes" )
	public String getRelocationTimes() {
		return this.relocationTimesInputFile;
	}

	@StringSetter( "relocationTimes" )
	public void setRelocationTimes(final String relocationTimesInputFile) {
		this.relocationTimesInputFile = relocationTimesInputFile;
	}

	@StringGetter( "relocationAgents" )
	public String getRelocationAgents() {
		return this.relocationAgentsInputFile;
	}

	@StringSetter( "relocationAgents" )
	public void setRelocationAgents(final String relocationAgentsInputFile) {
		this.relocationAgentsInputFile = relocationAgentsInputFile;
	}

	@StringGetter( "moduleEnableAfterIteration" )
	public Integer moduleEnableAfterIteration() {
		return this.moduleEnableAfterIteration;
	}

	@StringSetter( "moduleEnableAfterIteration" )
	public void setModuleEnableAfterIteration(final String moduleEnableAfterIteration) {
		this.moduleEnableAfterIteration = Integer.parseInt( moduleEnableAfterIteration );
	}

	@StringGetter( "demandEstimateIterations" )
	public Integer demandEstimateIterations() {
		return this.demandEstimateIterations;
	}

	@StringSetter( "demandEstimateIterations" )
	public void setDemandEstimateIterations(final String demandEstimateIterations) {
		this.demandEstimateIterations = Integer.parseInt( demandEstimateIterations );
	}
}
