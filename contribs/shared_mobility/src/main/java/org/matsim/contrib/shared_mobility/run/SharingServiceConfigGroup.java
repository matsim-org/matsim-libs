package org.matsim.contrib.shared_mobility.run;

import java.util.Arrays;
import java.util.Map;

import javax.annotation.Nonnegative;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.shared_mobility.service.SharingService;
import org.matsim.core.config.Config;
import org.matsim.core.config.ReflectiveConfigGroup;
import org.matsim.core.config.consistency.BeanValidationConfigConsistencyChecker;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class SharingServiceConfigGroup extends ReflectiveConfigGroup {

	public static final String GROUP_NAME = "mode";

	public static final String ID = "id";
	public static final String SERVICE_SCHEME = "serviceScheme";
	public static final String ID_COMMENT = "The id of the sharing service";
	public static final String SERVICE_SCHEME_COMMENT = "One of: " + String.join(", ",
			Arrays.asList(ServiceScheme.values()).stream().map(String::valueOf).toList());

	public SharingServiceConfigGroup() {
		super(GROUP_NAME);
	}

	public enum ServiceScheme {
		StationBased, Freefloating
	}

	@Parameter
	@Comment("Input file defining vehicles and stations")
	@NotNull
	private String serviceInputFile;

	@NotNull
	private Id<SharingService> id;

	@Parameter
	@NotNull
	private ServiceScheme serviceScheme;

	@Parameter
	@Comment("Shape file defining the service area")
	private String serviceAreaShapeFile;

	@Parameter
	@Comment("Defines the underlying mode of the service")
	@NotNull
	private String mode;

	@Parameter
	@Comment("Maximum distance to a bike or station")
	@Positive
	private double maximumAccessEgressDistance = 1000;

	@Parameter
	@Comment("Time [second] fare")
	@Nonnegative
	private double timeFare = 0.0;

	@Parameter
	@Comment("Distance [meter] fare")
	@Nonnegative
	private double distanceFare = 0.0;

	@Parameter
	@Comment("Base fare")
	@Nonnegative
	private double baseFare = 0.0;

	@Parameter
	@Comment("Minimum fare per rental")
	@Nonnegative
	private double minimumFare = 0.0;

	@Override
	protected void checkConsistency(Config config) {
		super.checkConsistency(config);
		new BeanValidationConfigConsistencyChecker().checkConsistency(config);
	}

	public String getServiceInputFile() {
		return serviceInputFile;
	}

	public void setServiceInputFile(String serviceInputFile) {
		this.serviceInputFile = serviceInputFile;
	}

	public Id<SharingService> getId() {
		return id;
	}

	public void setId(Id<SharingService> serviceId) {
		id = serviceId;
	}

	public ServiceScheme getServiceScheme() {
		return serviceScheme;
	}

	public void setServiceScheme(ServiceScheme serviceScheme) {
		this.serviceScheme = serviceScheme;
	}

	public String getServiceAreaShapeFile() {
		return serviceAreaShapeFile;
	}

	public void setServiceAreaShapeFile(String serviceAreaShapeFile) {
		this.serviceAreaShapeFile = serviceAreaShapeFile;
	}

	public String getMode() {
		return mode;
	}

	public void setMode(String mode) {
		this.mode = mode;
	}

	public double getMaximumAccessEgressDistance() {
		return maximumAccessEgressDistance;
	}

	public void setMaximumAccessEgressDistance(double maximumAccessEgressDistance) {
		this.maximumAccessEgressDistance = maximumAccessEgressDistance;
	}

	public double getTimeFare() {
		return timeFare;
	}

	public void setTimeFare(double timeFare) {
		this.timeFare = timeFare;
	}

	public double getDistanceFare() {
		return distanceFare;
	}

	public void setDistanceFare(double distanceFare) {
		this.distanceFare = distanceFare;
	}

	public double getBaseFare() {
		return baseFare;
	}

	public void setBaseFare(double baseFare) {
		this.baseFare = baseFare;
	}

	public double getMinimumFare() {
		return minimumFare;
	}

	public void setMinimumFare(double minimumFare) {
		this.minimumFare = minimumFare;
	}

	@StringGetter(ID)
	public String getIdAsString() {
		return id != null ? id.toString() : "";
	}

	@StringSetter(ID)
	public void setIdFromString(String idString) {
		this.id = (idString != null && !idString.isBlank()) ? Id.create(idString, SharingService.class) : null;
	}

	@Override
	public Map<String, String> getComments() {
		Map<String, String> map = super.getComments();
		map.put(ID, ID_COMMENT);
		map.put(SERVICE_SCHEME, SERVICE_SCHEME_COMMENT);
		return map;
	}

}
