package org.matsim.utils;

/**
 * A helper class that provides a central place to read out rarely used options provided as System properties.
 *
 * @author mrieser / Simunto GmbH
 */
public abstract class FeatureFlags {

	private FeatureFlags() {
	}

	public static boolean preferLocalDTDs() {
		String property = System.getProperty("matsim.preferLocalDtds");
		if (property != null) {
			return Boolean.parseBoolean(property);
		}
		return false; // default fallback
	}

	public static boolean useParallelIO() {
		String property = System.getProperty("matsim.useParallelIO");
		if (property != null) {
			return Boolean.parseBoolean(property);
		}
		return true; // default fallback
	}

}
