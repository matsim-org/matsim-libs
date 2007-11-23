/* *********************************************************************** *
 * project: org.matsim.*
 * Sort.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.lnicolas.convexhull;

import java.util.Vector;
public class Sort
{
public static void quick(Vector objs, Comparator c)
{
	quickSort(objs, 0, objs.size() - 1, c);
}
/**
 * This method was created in VisualAge.
 * @param o java.lang.Object[]
 * @param lo int
 * @param hi int
 * @param c Comparator
 */
private static void quickSort(Vector objs, int lo0, int hi0, Comparator c)
{
	int lo = lo0;
	int hi = hi0;
	Object mid;
	if (hi0 > lo0)
	{
		// pick the medium value
		mid = objs.elementAt((lo0 + hi0) / 2);

		// loop through the array until indices cross
		while (lo <= hi)
		{
			// find the first element that is greater than or equal to
			// the partition element starting from the left Index.
			//
			// Nasty to have to cast here. Would it be quicker
			// to copy the vectors into arrays and sort the arrays?
			while ((lo < hi0) && (c.compare(objs.elementAt(lo), mid) == 1))
			{
				++lo;
			}

			// find an element that is smaller than or equal to
			// the partition element starting from the right Index.
			while ((hi > lo0) && (c.compare(mid, objs.elementAt(hi)) == 1))
			{
				--hi;
			}

			// if the indexes have not crossed, swap
			if (lo <= hi)
			{
				Object tmp = objs.elementAt(lo);
				objs.setElementAt(objs.elementAt(hi), lo);
				objs.setElementAt(tmp, hi);
				++lo;
				--hi;
			}
		}

		// If the right index has not reached the left side of array
		// must now sort the left partition.
		if (lo0 < hi)
			quickSort(objs, lo0, hi, c);

		// If the left index has not reached the right side of array
		// must now sort the right partition.
		if (lo < hi0)
			quickSort(objs, lo, hi0, c);
	}
}
}