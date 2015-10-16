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


/**
 * <strong>BIOROUTE</strong> interface. This is the super-interface of all other
 * BIOROUTE interfaces. Enables the configuration of reflectively generated
 * classes.
 * 
 * @author Gunnar Flötteröd
 * 
 */
public interface Configurable {

	/**
	 * Configures this instance. Should be called right after instantiation.
	 * 
	 * @param config
	 *            object representation of the XML <em>sub</em>-tree defining
	 *            this object and its configuration
	 */
	public void configure(final Config config);

}
