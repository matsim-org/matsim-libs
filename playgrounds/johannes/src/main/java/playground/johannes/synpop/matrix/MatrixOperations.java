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

package playground.johannes.synpop.matrix;

import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import playground.johannes.studies.matrix2014.matrix.ODPredicate;

import java.util.*;

/**
 * @author johannes
 */
public class MatrixOperations {

    public static <K> Matrix<K, Double> errorMatrix(Matrix<K, Double> reference, Matrix<K, Double> compare, Matrix<K, Double> target) {
        Set<K> keys = new HashSet<>(reference.keys());
        keys.addAll(compare.keys());

        for (K i : keys) {
            for (K j : keys) {
                Double refVal = reference.get(i, j);
                Double compVal = compare.get(i, j);

                if(refVal != null) { // a null ref value is not treated as zero!
//                    if (refVal == null) refVal = new Double(0);
                    if (compVal == null) compVal = new Double(0);

                    if (refVal == 0 && compVal == 0)
                        target.set(i, j, 0.0);
                    else if (refVal == 0) {
                        target.set(i, j, Double.POSITIVE_INFINITY);
                        // TODO: deliberately think about what to do here
                    } else {
                        Double err = (compVal - refVal) / refVal;
                        target.set(i, j, err);
                    }
                }
            }
        }

        return target;
    }


    public static <K> Matrix<K, Double> diffMatrix(Matrix<K, Double> reference, Matrix<K, Double> compare, Matrix<K, Double> target) {
        Set<K> zones = new HashSet<>(reference.keys());
        zones.addAll(compare.keys());

        for (K i : zones) {
            for (K j : zones) {
                Double refVal = reference.get(i, j);
                Double compVal = compare.get(i, j);

                if (refVal == null)
                    refVal = new Double(0);

                if (compVal == null)
                    compVal = new Double(0);

                target.set(i, j, refVal - compVal);
            }
        }

        return target;
    }

    public static <K> void applyFactor(Matrix<K, Double> m, double factor) {
        Set<K> keys = m.keys();
        for (K i : keys) {
            for (K j : keys) {
                Double val = m.get(i, j);
                if (val != null) {
                    m.set(i, j, val * factor);
                }
            }
        }
    }

    public static <K> void applyDiagonalFactor(Matrix<K, Double> m, double factor) {
        Set<K> keys = m.keys();
        for (K i : keys) {
            Double val = m.get(i, i);
            if (val != null) {
                m.set(i, i, val * factor);
            }
        }
    }

    public static <K> double sum(Matrix<K, Double> m) {
        double sum = 0;
        Set<K> keys = m.keys();
        for (K i : keys) {
            for (K j : keys) {
                Double val = m.get(i, j);
                if (val != null) {
                    sum += val;
                }
            }
        }

        return sum;
    }

    public static <K> double diagonalSum(Matrix<K, Double> m) {
        double sum = 0;
        Set<K> keys = m.keys();
        for(K key : keys) {
            Double val = m.get(key, key);
            if(val != null) sum += val;
        }

        return sum;
    }

    public static <K> TObjectDoubleHashMap<K> columnMarginals(Matrix<K, Double> m) {
        TObjectDoubleHashMap<K> marginals = new TObjectDoubleHashMap<>();
        Set<K> keys = m.keys();
        for (K j : keys) {
            double sum = 0;
            for (K i : keys) {
                Double val = m.get(i, j);
                if (val != null) {
                    sum += val;
                }
            }
            marginals.put(j, sum);
        }
        return marginals;
    }

    public static <K> TObjectDoubleMap<K> rowMarginals(Matrix<K, Double> m) {
        TObjectDoubleMap<K> marginals = new TObjectDoubleHashMap<>();
        Set<K> keys = m.keys();
        for(K i : keys) {
            double sum = 0;
            for(K j : keys) {
                Double val = m.get(i, j);
                if(val != null) sum += val;
            }
            marginals.put(i, sum);
        }

        return marginals;
    }

    public static <K> Matrix<K, Double> average(Collection<? extends Matrix<K, Double>> matrices, Matrix<K, Double> target) {
        target = accumulate(matrices, target);
        applyFactor(target, 1 / (double) matrices.size());

        return target;
    }

    public static <K> void symmetrize(Matrix<K, Double> m) {
        List<K> keys = new ArrayList<>(m.keys());

        for (int i = 0; i < keys.size(); i++) {
            K key1 = keys.get(i);
            for (int j = (i + 1); j < keys.size(); j++) {
                K key2 = keys.get(j);
                Double val1 = m.get(key1, key2);
                if (val1 == null)
                    val1 = 0.0;

                Double val2 = m.get(key2, key1);
                if (val2 == null)
                    val2 = 0.0;

                double sum = val1 + val2;
                if (sum > 0) {
                    m.set(key1, key2, sum / 2.0);
                    m.set(key2, key1, sum / 2.0);
                }
            }
        }
    }

    public static <K> Matrix<K, Double> accumulate(Collection<? extends Matrix<K, Double>> matrices, Matrix<K, Double> target) {
        Set<K> keys = new HashSet<>();
        for (Matrix<K, Double> m : matrices) {
            keys.addAll(m.keys());
        }


        for (Matrix<K, Double> m : matrices) {
            for (K i : keys) {
                for (K j : keys) {
                    Double newVal = m.get(i, j);
                    if (newVal == null)
                        newVal = 0.0;

                    Double currentVal = target.get(i, j);
                    if (currentVal == null)
                        currentVal = 0.0;

                    double sum = newVal + currentVal;
                    if (sum > 0) //do not spam the matrix with zero values
                        target.set(i, j, sum);
                }
            }
        }

        return target;
    }

    public static <K> double weightedCellAverage(Matrix<K, Double> m, Matrix<K, Double> weights) {
        double sum = 0;
        double wsum = 0;

        Set<K> keys = m.keys();
        for (K i : keys) {
            for (K j : keys) {
                Double val = m.get(i, j);
                if (val != null) {
                    Double w = weights.get(i, j);
                    if (w == null) w = 1.0;
                    sum += val * w;
                    wsum += w;
                }
            }
        }

        return sum / wsum;
    }

    public static <K> void removeDiagonal(Matrix<K, Double> m) {
        Set<K> keys = m.keys();
        for (K i : keys) {
            m.set(i, i, null);
        }
    }

    public static <K, V> Matrix<K, V> subMatrix(ODPredicate<K, V> predicate, Matrix<K, V> source, Matrix<K, V> target) {
        Set<K> keys = source.keys();
        for(K row : keys) {
            for(K col : keys) {
                V value = source.get(row, col);
                if(value != null && predicate.test(row, col, source)) {
                    target.set(row, col, value);
                }
            }
        }
        return target;
    }
}
