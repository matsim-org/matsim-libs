/* *********************************************************************** *
 * project: org.matsim.* 												   *
 *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2023 by the members listed in the COPYING,        *
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
package ch.sbb.matsim.analysis.skims;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * A simple m x m matrix storing float values, using arbitrary objects to identify rows and columns. The list of identifying objects must be known before-hand when instantiating a matrix.
 *
 * @param &lt;T&gt; identifier type for matrix entries
 * @author mrieser / SBB
 *         <p>
 *         Design considerations: The matrix stores all cell values in one big array in order to have it as compact as possible. Row/Column identifiers are indexed once at initialization in order to
 *         compute the indices in the array. Thus, each entry/cell uses 4 bytes. A 1000x1000 matrix would thus consume 4 MB of RAM.
 *         <p>
 *         Alternative implementations like Map&lt;T, Map&lt;T, Float&gt;&gt; or Map&lt;Tuple&lt;T, T&gt;, Float&gt; would be far less efficient, even for sparse arrays. Map&lt;T, Map&lt;T,
 *         Float&gt;&gt; requires 48 bytes for each Map.Entry (16 bytes object header, 2 * 8 bytes for Key and Value object reference, next pointer (8 bytes), int hash). Each stored Float consumes 24
 *         Bytes, so a 1000x1000 matrix would consume 1000*48 + 1000*1000*(48+24) = ca 70 MB (factor 16.5). Map&lt;Tuple&lt;T, T&gt;, Float&gt; requires 48 bytes for each Map.Entry, each Tuple would
 *         use 32 bytes (16 object header + 2 * 8 object pointers), Float object 24 bytes, so in total 1000x1000 would consume 1000*1000*(48+32+24) = ca 100 MB (factor 25).
 *         <p>
 *         So, as long as the matrix has entries in at least 1/16.5 = 6% or 1/25 = 4% of all cells, the simple float array should be more efficient.
 *         <p>
 *         For larger matrices the absolute volumes become even more impressive. For an 8000x8000 matrix, the float array will use 250MB, while the alternatives will use 4.5 or 6.5 GB respectively.
 */
public class FloatMatrix<T> {

    final Map<T, Integer> id2index;
    private final int size;
    private final float[] data;

    public FloatMatrix(Set<T> zones, float defaultValue) {
        this.size = zones.size();
        this.id2index = new HashMap<>((int) (this.size * 1.5));
        this.data = new float[this.size * this.size];
        Arrays.fill(this.data, defaultValue);
        int index = 0;
        for (T t : zones) {
            this.id2index.put(t, index);
            index++;
        }
    }

    public float set(T from, T to, float value) {
        int index = getIndex(from, to);
        float oldValue = this.data[index];
        this.data[index] = value;
        return oldValue;
    }

    public float get(T from, T to) {
        int index = getIndex(from, to);
        return this.data[index];
    }

    public float add(T from, T to, float value) {
        int index = getIndex(from, to);
        float oldValue = this.data[index];
        float newValue = oldValue + value;
        this.data[index] = newValue;
        return newValue;
    }

    /**
     * @param from
     * @param to
     * @param factor
     * @return the new value
     */
    public float multiply(T from, T to, float factor) {
        int index = getIndex(from, to);
        float oldValue = this.data[index];
        float newValue = oldValue * factor;
        this.data[index] = newValue;
        return newValue;
    }

    /**
     * Multiplies the values in every cell with the given factor.
     *
     * @param factor the multiplication factor
     */
    public void multiply(float factor) {
        for (int i = 0; i < this.data.length; i++) {
            this.data[i] *= factor;
        }
    }

    private int getIndex(T from, T to) {
        int fromIndex = this.id2index.get(from);
        int toIndex = this.id2index.get(to);
        return fromIndex * this.size + toIndex;
    }
}
