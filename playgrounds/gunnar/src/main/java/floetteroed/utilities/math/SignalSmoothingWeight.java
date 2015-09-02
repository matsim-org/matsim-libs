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
import java.util.logging.Logger;

/**
 * 
 * @author Gunnar Flötteröd
 * 
 */
public class SignalSmoothingWeight implements Serializable {

	// -------------------- CONSTANTS --------------------

	private static final long serialVersionUID = 1L;

	// -------------------- MEMBER VARIABLES --------------------

	private double innovationWeight = 1.0;

	private double freezeIteration = Double.MAX_VALUE;

	private int iteration = 0;

	// -------------------- CONSTRUCTION --------------------

	public SignalSmoothingWeight(final double innovationWeight) {
		this.setInnovationWeight(innovationWeight);
	}

	// -------------------- IMPLEMENTATION --------------------

	public void setInnovationWeight(final double innovationWeight) {
		if (Double.isNaN(innovationWeight) || innovationWeight < 0
				|| innovationWeight > 1) {
			throw new IllegalArgumentException("innovation weight "
					+ innovationWeight + " is not in [0,1]");
		}

		if (innovationWeight == 0.0) {
			Logger.getLogger(this.getClass().getName()).warning(
					"innovation is 0.0 (before freezing)");
		}

		if (this.isFrozen()) {
			Logger.getLogger(this.getClass().getName()).warning(
					"smoothing was already frozen");
		}
		this.innovationWeight = innovationWeight;
		this.freezeIteration = Double.MAX_VALUE;
	}

	public void freeze() {
		if (this.innovationWeight > 0) {
			this.freezeIteration = this.iteration - 1.0 / this.innovationWeight;
		} else {
			this.freezeIteration = 0;
			Logger.getLogger(this.getClass().getName()).warning(
					"freezing from zero innovation weight");
		}
	}

	public boolean isFrozen() {
		return this.iteration >= this.freezeIteration;
	}

	public double getNextInnovationWeight() {
		if (this.iteration++ == 0) {
			return 1.0;
		} else {
			if (this.isFrozen()) {
				this.innovationWeight = 1.0 / (this.iteration - this.freezeIteration);
			}
			return this.innovationWeight;
		}
	}

	public double getLastInnovationWeight() {
		if (this.iteration == 0) {
			return 1.0;
		} else {
			return this.innovationWeight;
		}
	}
}
