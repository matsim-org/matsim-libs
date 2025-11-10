/**
 * se.vti.utils
 * 
 * Copyright (C) 2015-2025 by Gunnar Flötteröd (VTI, LiU).
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
package se.vti.utils.misc.math;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 *
 * @author Gunnar Flötteröd
 *
 *         TODO Move this to the utilities-project.
 * 
 */
public class SetUtils {

	public static <T> Set<T> union(final Set<? extends T> arg0, final Set<? extends T> arg1) {
		final Set<T> result = new LinkedHashSet<>(arg0);
		result.addAll(arg1);
		return result;
	}

	public static <T> Set<T> intersect(final Set<? extends T> arg0, final Set<? extends T> arg1) {
		final Set<T> result = new LinkedHashSet<>(arg0);
		result.retainAll(arg1);
		return result;
	}

	public static <T> Set<T> difference(final Collection<? extends T> minuend,
			final Collection<? extends T> subtrahend) {
		final Set<T> result = new LinkedHashSet<>(minuend);
		result.removeAll(subtrahend);
		return result;
	}

}
