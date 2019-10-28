
/* *********************************************************************** *
 * project: org.matsim.*
 * package-info.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

 /**
 * The configuration can be used to change the behavior of MATSim in some well-defined places.
 * Users may know best the configuration-files for MATSim, XML-files in which several parameters and 
 * corresponding values can be entered. The config-package is responsible to store these parameters
 * in memory during runs.<br>
 * 
 * Each parameter is part of exactly one {@linkplain org.matsim.core.config.groups config-group}. Such a group
 * collects similar parameters, or different parameters for one specific part of MATSim. The config-groups
 * defined in the subpackage {@link org.matsim.core.config.groups} are considered "core" config-groups, which
 * should change only if really needed. Often it is better to add additional config-paramters for new
 * functionality as a new config-group (see <a href="#custom-params">Custom Config-Parameters</a> below). 
 * 
 * <a name="custom-params"><h3>Custom Config-Parameters</h3></a>
 * One can add custom parameters any time to a configuration file, but they have to be in an additional
 * "group" (in the XML representation, a "group" is specified as <code>module</code>. This may change in
 * the future to have consistent names). As an example, you could add the following code to your 
 * configuration file:
 * <pre>
 *   &lt;module name="my-settings">
 *     &lt;param name="my-value1" value="1" />
 *     &lt;param name="my-value2" value="2.3" />
 *     &lt;param name="my-value3" value="foo-bar" />
 *   &lt;/module>
 * </pre>
 * These settings are then stored in an instance of {@link org.matsim.core.config.ConfigGroup}, which just stores
 * all the parameters in a map. You could access such parameters with:
 * <pre>
 * Config config;
 * // ... init/read config
 * String val1 = config.getModule("my-settings).getParam("my-value1");
 * </pre>
 * This works out-of-the-box, but it has the downside of only providing String-values. In some cases, when
 * this string value must be converted into an <code>int</code> or <code>double</code> many times, this
 * can be very inefficient. In such cases, it may be of advantage to offer a custom config-group to store
 * the parameters with their native types.<br>
 * 
 * To write a custom config-group, extend {@link org.matsim.core.config.ConfigGroup}. This allows you to convert
 * the parsed values from the configuration file only once and then store the parameters in their native
 * type. Just provide the corresponding getters and setters for your parameters, and overwrite the following
 * methods:
 * <ul>
 * <li>{@link org.matsim.core.config.ConfigGroup#addParam(String, String)} to store the values read from a 
 * 		configuration file.</li>
 * <li>{@link org.matsim.core.config.ConfigGroup#getParams()} to return a map containing all the parameter names and 
 * 		their values to be stored in a configuration file.</li>
 * <li>{@link org.matsim.core.config.ConfigGroup#getValue(String)} should be implemented as well for compatibility
 * 		reasons. You can implement it with an <code>UnsupportedOperationException</code> if you are sure that
 *    you will never call this method.</li>
 * <li>{@link org.matsim.core.config.ConfigGroup#getName()} should return the name of your config-group, as it
 *    appears in the configuration file.</li>
 * <li>Optionally: {@link org.matsim.core.config.ConfigGroup#checkConsistency(Config)} to check that all the settings make
 *    some sense together after being read from a file.</li> 
 * </ul> 
 * The custom config-group must be 
 * {@linkplain org.matsim.core.config.Config#addModule(ConfigGroup) added} to the 
 * {@link org.matsim.core.config.Config}-object before the configuration is read from file, because
 * otherwise a generic {@link org.matsim.core.config.ConfigGroup} will be created for these settings. The 
 * {@linkplain org.matsim.core.controler controler-documenation} has an example of how to use custom
 * config-groups with additional functionality provided by 
 * {@link org.matsim.core.controler.listener.ControlerListener}s in the "Best Practices" section.
 * 
 * @author mrieser
 */
package org.matsim.core.config;
