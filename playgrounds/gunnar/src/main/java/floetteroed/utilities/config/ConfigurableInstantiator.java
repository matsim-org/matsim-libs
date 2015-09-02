/*
 * Copyright 2015 Gunnar Flötteröd
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * contact: gunnar.floetteroed@abe.kth.se
 *
 */ 
package floetteroed.utilities.config;

import java.util.Arrays;

/**
 * Instantiates and configures a <code>Configurable</code> class.
 * 
 * @author Gunnar Flötteröd
 * 
 */
public class ConfigurableInstantiator {

	// -------------------- CONSTRUCTION --------------------

	private ConfigurableInstantiator() {
		// not to be instantiated
	}

	// -------------------- IMPLEMENTATION --------------------

	/**
	 * Instantiates and configures a class indicated within an element of the
	 * XML tree represented by <code>config</code>.
	 * <p>
	 * All configuration used here is assumed to be contained in the XML
	 * sub-tree having the <em>second last</em> entry of <code>elements</code>
	 * as its root. The class to be instantiated is identified by the the
	 * <code>value</code> attribute of the <em>last</em> entry of
	 * <code>elements</code>.
	 * 
	 * @param config
	 *            object representation of the XML configuration file
	 * @param elements
	 *            the element sequence until (and including) the element that
	 *            defines the class to be instantiated
	 * @return a configured instance of Configurable (needs to be cast to a more
	 *         specific type)
	 * @throws RuntimeException
	 *             encapsulates whatever can go wrong in the process
	 */
	public static Configurable newConfiguredInstance(final Config config,
			final String... elements) {
		/*
		 * (1) instantiate class
		 */
		final Class<?> clazz;
		final Configurable result;
		try {
			clazz = Class.forName(config.get(elements));
			result = (Configurable) clazz.newInstance();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		/*
		 * (2) configure class
		 */
		result.configure(config.newSubConfig(Arrays.asList(elements).subList(0,
				elements.length - 1)));
		return result;
	}
}
