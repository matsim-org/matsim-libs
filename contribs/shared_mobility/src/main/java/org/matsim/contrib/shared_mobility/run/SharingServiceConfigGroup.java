package org.matsim.contrib.shared_mobility.run;

import java.util.Arrays;
import java.util.Map;

import javax.annotation.Nonnegative;

import org.matsim.core.config.Config;
import org.matsim.core.config.ReflectiveConfigGroup;
import org.matsim.core.config.consistency.BeanValidationConfigConsistencyChecker;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class SharingServiceConfigGroup extends ReflectiveConfigGroup {

	public static final String GROUP_NAME = "mode";

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
	public String serviceInputFile;

	@Parameter
	@Comment("The id of the sharing service")
	@NotNull
	public String id;

	@Parameter
	@NotNull
	public ServiceScheme serviceScheme;

	@Parameter
	@Comment("Shape file defining the service area")
	public String serviceAreaShapeFile;

	@Parameter
	@Comment("Defines the underlying mode of the service")
	@NotNull
	public String mode;

	@Parameter
	@Comment("Maximum distance to a bike or station")
	@Positive
	public double maximumAccessEgressDistance = 1000;

	@Parameter
	@Comment("Time [second] fare")
	@Nonnegative
	public double timeFare = 0.0;

	@Parameter
	@Comment("Distance [meter] fare")
	@Nonnegative
	public double distanceFare = 0.0;

	@Parameter
	@Comment("Base fare")
	@Nonnegative
	public double baseFare = 0.0;

	@Parameter
	@Comment("Minimum fare per rental")
	@Nonnegative
	public double minimumFare = 0.0;

	@Override
	protected void checkConsistency(Config config) {
		super.checkConsistency(config);
		new BeanValidationConfigConsistencyChecker().checkConsistency(config);
	}

	@Override
	public Map<String, String> getComments() {
		Map<String, String> map = super.getComments();
		map.put("serviceScheme", SERVICE_SCHEME_COMMENT);
		return map;
	}

}
