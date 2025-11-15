/**
 * se.vti.emulation
 * 
 * Copyright (C) 2023, 2024, 2025 by Gunnar Flötteröd (VTI, LiU).
 * Partially based on Sebastian Hörl's IER.
 * 
 * VTI = Swedish National Road and Transport Institute
 * LiU = Linköping University, Sweden
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms
 * of the GNU General Public License as published by the Free Software Foundation, either 
 * version 3 of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>. See also COPYING and WARRANTY file.
 */
package se.vti.emulation;

import org.matsim.core.config.ReflectiveConfigGroup;

/**
 *
 * @author shoerl
 * @author Gunnar Flötteröd
 *
 */
public class EmulationConfigGroup extends ReflectiveConfigGroup {

	public static final String GROUP_NAME = "emulation";

	public EmulationConfigGroup() {
		super(GROUP_NAME);
	}

	// -------------------- iterationsPerCycle --------------------

	private int iterationsPerCycle = 10;

	@StringGetter("iterationsPerCycle")
	public int getIterationsPerCycle() {
		return this.iterationsPerCycle;
	}

	@StringSetter("iterationsPerCycle")
	public void setIterationsPerCycle(int iterationsPerCycle) {
		this.iterationsPerCycle = iterationsPerCycle;
	}

	// -------------------- batchSize --------------------

	private int batchSize = 200;

	@StringGetter("batchSize")
	public int getBatchSize() {
		return this.batchSize;
	}

	@StringSetter("batchSize")
	public void setBatchSize(int batchSize) {
		this.batchSize = batchSize;
	}

}
