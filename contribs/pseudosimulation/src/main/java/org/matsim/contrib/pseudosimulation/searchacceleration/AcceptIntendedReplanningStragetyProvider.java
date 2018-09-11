/*
 * Copyright 2018 Gunnar Flötteröd
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
 * contact: gunnar.flotterod@gmail.com
 *
 */
package org.matsim.contrib.pseudosimulation.searchacceleration;

import javax.inject.Provider;

import org.matsim.core.replanning.PlanStrategy;

import com.google.inject.Inject;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class AcceptIntendedReplanningStragetyProvider implements Provider<PlanStrategy> {

	// -------------------- MEMBERS --------------------

	@Inject
	private SearchAccelerator searchAccelerator;

	// -------------------- IMPLEMENTATION OF Provider --------------------

	@Override
	public PlanStrategy get() {
		return new AcceptIntendedReplanningStrategy(this.searchAccelerator);
	}

}
