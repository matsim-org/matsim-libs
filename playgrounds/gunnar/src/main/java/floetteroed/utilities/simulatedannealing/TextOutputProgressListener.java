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
package floetteroed.utilities.simulatedannealing;

/**
 * 
 * @author Gunnar Flötteröd
 *
 * @param <S>
 */
public class TextOutputProgressListener<S> implements ProgressListener<S> {

	private int it = 0;

	@Override
	public void notifyCurrentState(final S state,
			final double currentObjectiveFunction,
			final double alternativeObjectiveFunction) {
		System.out.println("it. " + (this.it++) + ": f(x) = "
				+ alternativeObjectiveFunction + ", fOpt(x) = "
				+ currentObjectiveFunction + ", xOpt = " + state);
	}

}
