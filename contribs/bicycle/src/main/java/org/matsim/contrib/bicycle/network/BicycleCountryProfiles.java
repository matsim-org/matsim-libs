/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package org.matsim.contrib.bicycle.network;

import java.util.List;
import java.util.Locale;

/**
 * Factory for {@link BicycleCountryProfile} instances by short code.
 *
 * <p>Used to translate a CLI flag like {@code --country=de} into a profile.
 * Three codes are currently recognised:
 * <ul>
 *   <li>{@code "de"} — {@link BicycleCountryProfileGermany}</li>
 *   <li>{@code "at"} — {@link BicycleCountryProfileAustria}</li>
 *   <li>{@code "generic"} — {@link BicycleCountryProfileGeneric}; tag-only,
 *       no country-specific traffic-sign matching</li>
 * </ul>
 * Unknown codes throw {@link IllegalArgumentException} with a message that
 * lists the supported values and points at {@code generic} as the fallback,
 * so users running on countries without a dedicated profile have a clear
 * path forward instead of a silent surprise.
 *
 * @author smetzler
 */
public final class BicycleCountryProfiles {

	/** All supported codes, in the order shown to users in error messages. */
	public static final List<String> SUPPORTED_CODES = List.of("de", "at", "generic");

	private BicycleCountryProfiles() {
	}

	/**
	 * @param code one of {@link #SUPPORTED_CODES}; case-insensitive
	 * @throws IllegalArgumentException with a human-readable message if the
	 *         code isn't recognised
	 */
	public static BicycleCountryProfile forCode(String code) {
		if (code == null || code.isBlank()) {
			throw new IllegalArgumentException(
				"Country code is null or empty. Supported: " + SUPPORTED_CODES
					+ ". Use 'generic' for tag-only classification when your country isn't listed.");
		}
		switch (code.toLowerCase(Locale.ROOT)) {
			case "de":
				return new BicycleCountryProfileGermany();
			case "at":
				return new BicycleCountryProfileAustria();
			case "generic":
				return new BicycleCountryProfileGeneric();
			default:
				throw new IllegalArgumentException(
					"Unsupported country code '" + code + "'. Supported: " + SUPPORTED_CODES
						+ ". If your country isn't listed, use 'generic' (tag-only classification)"
						+ " — it works reasonably well for countries without detailed traffic_sign"
						+ " tagging in OSM. For better results, implement a new BicycleCountryProfile"
						+ " (see BicycleCountryProfileGermany or BicycleCountryProfileAustria as templates).");
		}
	}
}
