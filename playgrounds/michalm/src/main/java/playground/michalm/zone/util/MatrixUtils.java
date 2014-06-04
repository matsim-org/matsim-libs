/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.michalm.zone.util;

import org.matsim.api.core.v01.Id;
import org.matsim.matrices.*;

import com.google.common.collect.Iterables;


public class MatrixUtils
{
    public static Matrix createDenseMatrix(String id, Iterable<Id> ids, double[][] values)
    {
        return createMatrix(id, ids, values, true);
    }


    public static Matrix createSparseMatrix(String id, Iterable<Id> ids, double[][] values)
    {
        return createMatrix(id, ids, values, false);
    }


    public static Matrix createMatrix(String id, Iterable<Id> ids, double[][] values,
            boolean denseMatrix)
    {
        Matrix matrix = new Matrix(id, null);
        Id[] idArray = Iterables.toArray(ids, Id.class);

        for (int i = 0; i < idArray.length; i++) {
            for (int j = 0; j < idArray.length; j++) {
                if (denseMatrix || values[i][j] != 0) {
                    matrix.createEntry(idArray[i], idArray[j], values[i][j]);
                }
            }
        }

        return matrix;
    }


    public static Iterable<Entry> createEntryIterable(Matrix matrix)
    {
        return Iterables.concat(matrix.getFromLocations().values());
    }
}
