package org.matsim.contrib.carsharing.config;

import java.util.Map;

import org.matsim.core.config.ReflectiveConfigGroup;
/** 
 * @author balac
 */

public class CarsharingConfigGroup extends ReflectiveConfigGroup {
	public static final String GROUP_NAME = "Carsharing";
	
	private static final String PARAM_STATS_WRITER_INTERVAL = "statsWriterInterval";
	private static final String PARAM_VEHICLE_LOCATIONS = "vehiclelocations";
	private static final String PARAM_MEMBERSHIP = "membership";

	private Integer statsWriterInterval = 10;
	private String vehiclelocationsInputFile = null;
	private String membershipInputFile = null;

	
	public CarsharingConfigGroup() {
		super(GROUP_NAME);
	}
	
	@StringGetter( PARAM_STATS_WRITER_INTERVAL )
	public Integer getStatsWriterInterval() {
		return this.statsWriterInterval;
	}

	@StringSetter( PARAM_STATS_WRITER_INTERVAL )
	public void setStatsWriterInterval(final String statsWriterInterval) {
		this.statsWriterInterval = Integer.parseInt( statsWriterInterval );
	}
	
	@StringGetter( PARAM_VEHICLE_LOCATIONS )
	public String getvehiclelocations() {
		return this.vehiclelocationsInputFile;
	}

	@StringSetter( PARAM_VEHICLE_LOCATIONS )
	public void setvehiclelocations(final String vehiclelocationsInputFile) {
		this.vehiclelocationsInputFile = vehiclelocationsInputFile;
	}
	
	@StringGetter( PARAM_MEMBERSHIP )
	public String getmembership() {
		return this.membershipInputFile;
	}

	@StringSetter( PARAM_MEMBERSHIP )
	public void setmembership(final String membershipInputFile) {
		this.membershipInputFile = membershipInputFile;
	}
	
	@Override
    public Map<String, String> getComments() {
        Map<String, String> map = super.getComments();
        map.put(PARAM_STATS_WRITER_INTERVAL, "How often the carsharing demand statistics should be written. Default is every 10th iteration.");
        map.put(PARAM_VEHICLE_LOCATIONS, "Path to the input file containing th elocation of vehicles and stations.");
        map.put(PARAM_MEMBERSHIP, "A path to the file containing carsharing membership information.");
         
        return map;
    }
	
}
