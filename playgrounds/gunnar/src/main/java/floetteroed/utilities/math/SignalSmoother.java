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
package floetteroed.utilities.math;

import java.io.Serializable;

/**
 * 
 * @author Gunnar Flötteröd
 * 
 */
public class SignalSmoother implements Serializable {

	// -------------------- CONSTANTS --------------------

	private static final long serialVersionUID = 1L;

	// -------------------- MEMBER VARIABLES --------------------

	private final SignalSmoothingWeight smoothingWeight;

	private double smoothedValue = 0;

	// -------------------- CONSTRUCTION --------------------

	public SignalSmoother(final double innovationWeight) {
		this.smoothingWeight = new SignalSmoothingWeight(innovationWeight);
	}

	// -------------------- IMPLEMENTATION --------------------

	public void setInnovationWeight(final double innovationWeight) {
		this.smoothingWeight.setInnovationWeight(innovationWeight);
	}

	public void freeze() {
		this.smoothingWeight.freeze();
	}

	public boolean isFrozen() {
		return this.smoothingWeight.isFrozen();
	}

	public double getLastInnovationWeight() {
		return this.smoothingWeight.getLastInnovationWeight();
	}

	public double addValue(final double value) {
		final double w = this.smoothingWeight.getNextInnovationWeight();
		this.smoothedValue = (1.0 - w) * this.smoothedValue + w * value;
		return this.getSmoothedValue();
	}

	public double getSmoothedValue() {
		return this.smoothedValue;
	}
}
