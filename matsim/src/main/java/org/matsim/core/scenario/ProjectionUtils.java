package org.matsim.core.scenario;

import org.matsim.core.api.internal.MatsimToplevelContainer;
import org.matsim.utils.objectattributes.attributable.Attributable;

public final class ProjectionUtils {
	/**
	 * Name of the attribute to add to top-level containers to specify the projection the coordinates are in.
	 * When possible, the utility methods should be used instead of directly querying the attributes.
	 */
	public static final String INPUT_CRS_ATT = "coordinateReferenceSystem";

	private ProjectionUtils() {}

	public static <T extends MatsimToplevelContainer & Attributable> String getCRS(T container) {
		return (String) container.getAttributes().getAttribute(INPUT_CRS_ATT);
	}

	public static <T extends MatsimToplevelContainer & Attributable> void putCRS(T container, String CRS) {
		container.getAttributes().putAttribute(INPUT_CRS_ATT, CRS);
	}
}
