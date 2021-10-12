package org.matsim.contrib.sharing.run;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Nonnegative;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

import org.matsim.core.config.Config;
import org.matsim.core.config.ReflectiveConfigGroup;
import org.matsim.core.config.consistency.BeanValidationConfigConsistencyChecker;

public class SharingServiceConfigGroup extends ReflectiveConfigGroup {
	public static final String GROUP_NAME = "mode";

	public static final String SERVICE_INPUT_FILE = "serviceInputFile";
	public static final String ID = "id";
	public static final String SERVICE_SCHEME = "serviceScheme";
	public static final String SERVICE_AREA_SHAPE_FILE = "serviceAreaShapeFile";
	public static final String MODE = "mode";
	public static final String MAXIMUM_ACCESS_EGRESS_DISTANCE = "maximumAccesEgressDistance";
	public static final String TIME_FARE = "timeFare";
	public static final String DISTANCE_FARE = "distanceFare";
	public static final String BASE_FARE = "baseFare";
	public static final String MINIMUM_FARE = "minimumFare";
	
	public static final String SERVICE_INPUT_FILE_EXP = "Input file defining vehicles and stations";
	public static final String ID_EXP = "The id of the sharing service";
	public static final String SERVICE_SCHEME_EXP = "One of: " + String.join(", ",
			Arrays.asList(ServiceScheme.values()).stream().map(String::valueOf).collect(Collectors.toList()));
	public static final String SERVICE_AREA_SHAPE_FILE_EXP = "Shape file defining the service area";
	public static final String MODE_EXP = "Defines the underlying mode of the service";
	public static final String MAXIMUM_ACCESS_EGRESS_DISTANCE_EXP = "Maximum distance to a bike or station";
	public static final String TIME_FARE_EXP = "Time [second] fare";
	public static final String DISTANCE_FARE_EXP = "Distance [meter] fare";
	public static final String BASE_FARE_EXP = "Base fare";
	public static final String MINIMUM_FARE_EXP = "Minimum fare per rental";

	public enum ServiceScheme {
		StationBased, Freefloating
	}

	@NotNull
	private String serviceInputFile;

	@NotNull
	private String id;

	@NotNull
	private ServiceScheme serviceScheme;

	private String serviceAreaShapeFile;

	@NotNull
	private String mode;

	@Positive
	private double maximumAccessEgressDistance = 1000;
	
	@Nonnegative
	private double timeFare = 0.0;
	
	@Nonnegative
	private double distanceFare = 0.0;
	
	@Nonnegative
	private double baseFare = 0.0;
	
	@Nonnegative
	private double minimumFare = 0.0;

	public SharingServiceConfigGroup() {
		super(GROUP_NAME);
	}

	@StringGetter(ID)
	public String getId() {
		return id;
	}

	@StringSetter(ID)
	public void setId(String id) {
		this.id = id;
	}

	@StringSetter(SERVICE_INPUT_FILE)
	public void setServiceInputFile(String serviceInputFile) {
		this.serviceInputFile = serviceInputFile;
	}

	@StringGetter(SERVICE_INPUT_FILE)
	public String getServiceInputFile() {
		return serviceInputFile;
	}

	@StringSetter(SERVICE_SCHEME)
	public void setServiceScheme(ServiceScheme serviceScheme) {
		this.serviceScheme = serviceScheme;
	}

	@StringGetter(SERVICE_SCHEME)
	public ServiceScheme getServiceScheme() {
		return serviceScheme;
	}

	@StringSetter(SERVICE_AREA_SHAPE_FILE)
	public void setServiceAreaShapeFile(String serviceAreaShapeFile) {
		this.serviceAreaShapeFile = serviceAreaShapeFile;
	}

	@StringGetter(SERVICE_AREA_SHAPE_FILE)
	public String getServiceAreaShapeFile() {
		return serviceAreaShapeFile;
	}

	@StringSetter(MODE)
	public void setMode(String mode) {
		this.mode = mode;
	}

	@StringGetter(MODE)
	public String getMode() {
		return mode;
	}

	@StringSetter(MAXIMUM_ACCESS_EGRESS_DISTANCE)
	public void setMaximumAccessEgressDistance(double maximumAccessEgressDistance) {
		this.maximumAccessEgressDistance = maximumAccessEgressDistance;
	}

	@StringGetter(MAXIMUM_ACCESS_EGRESS_DISTANCE)
	public double getMaximumAccessEgressDistance() {
		return maximumAccessEgressDistance;
	}
	
	@StringSetter(TIME_FARE)
	public void setTimeFare(double timeFare) {
		this.timeFare = timeFare;
	}

	@StringGetter(TIME_FARE)
	public double getTimeFare() {
		return timeFare;
	}
	
	@StringSetter(DISTANCE_FARE)
	public void setDistanceFare(double distanceFare) {
		this.distanceFare = distanceFare;
	}

	@StringGetter(DISTANCE_FARE)
	public double getDistanceFare() {
		return distanceFare;
	}
	
	@StringSetter(BASE_FARE)
	public void setBaseFare(double baseFare) {
		this.baseFare = baseFare;
	}

	@StringGetter(BASE_FARE)
	public double getBaseFare() {
		return baseFare;
	}
	
	@StringSetter(MINIMUM_FARE)
	public void setMinimumFare(double minimumFare) {
		this.minimumFare = minimumFare;
	}

	@StringGetter(MINIMUM_FARE)
	public double getMinimumFare() {
		return minimumFare;
	}

	@Override
	protected void checkConsistency(Config config) {
		super.checkConsistency(config);
		new BeanValidationConfigConsistencyChecker().checkConsistency(config);
	}

	@Override
	public Map<String, String> getComments() {
		Map<String, String> map = super.getComments();
		map.put(SERVICE_INPUT_FILE, SERVICE_INPUT_FILE_EXP);
		map.put(ID, ID_EXP);
		map.put(SERVICE_SCHEME, SERVICE_SCHEME_EXP);
		map.put(SERVICE_AREA_SHAPE_FILE, SERVICE_AREA_SHAPE_FILE_EXP);
		map.put(MODE, MODE_EXP);
		map.put(MAXIMUM_ACCESS_EGRESS_DISTANCE, MAXIMUM_ACCESS_EGRESS_DISTANCE_EXP);
		map.put(TIME_FARE, TIME_FARE_EXP);
		map.put(DISTANCE_FARE, DISTANCE_FARE_EXP);
		map.put(BASE_FARE, BASE_FARE_EXP);
		map.put(MINIMUM_FARE, MINIMUM_FARE_EXP);
		return map;
	}
	
}
