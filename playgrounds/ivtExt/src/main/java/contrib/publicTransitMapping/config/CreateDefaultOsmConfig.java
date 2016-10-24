/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package contrib.publicTransitMapping.config;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Creates a default osmConverter config file.
 *
 * @author polettif
 */
public class CreateDefaultOsmConfig {

	/**
	 * Creates a default publicTransitMapping config file.
	 * @param args [0] default config filename
	 */
	public static void main(final String[] args) {
		Config config = ConfigUtils.createConfig();

		config.addModule(OsmConverterConfigGroup.createDefaultConfig());

		Set<String> toRemove = config.getModules().keySet().stream().filter(module -> !module.equals(OsmConverterConfigGroup.GROUP_NAME)).collect(Collectors.toSet());
		toRemove.forEach(config::removeModule);

		new ConfigWriter(config).write(args[0]);
	}
}