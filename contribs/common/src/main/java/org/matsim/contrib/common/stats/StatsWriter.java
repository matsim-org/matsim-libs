/* *********************************************************************** *
 * project: org.matsim.*
 * TXTWriter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package org.matsim.contrib.common.stats;

import gnu.trove.*;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.core.utils.collections.Tuple;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

/**
 * Utility functions for writing distributions and histograms to plain text files (for further plotting or processing in
 * statistics software).
 *
 * @author illenberger
 */
public class StatsWriter {

    private static final String TAB = "\t";

    private static final String NA = "NA";

    /**
     * Writes a plain text file with two columns where the first column contains the map keys and second the map values.
     * Rows are sorted according to the natural order of the map keys.
     *
     * @param map    a map (histogram)
     * @param keyCol the header for the first column
     * @param valCol the header for the second column
     * @param file   a filename
     * @throws IOException
     */
    public static void writeHistogram(TDoubleDoubleHashMap map, String keyCol, String valCol, String file) throws IOException {
        writeHistogram(map, keyCol, valCol, file, false);
    }

    /**
     * Writes a plain text file with two columns where the first column contains the map keys and second the map
     * values.
     *
     * @param map        a map (histogram)
     * @param keyCol     the header for the first column
     * @param valCol     the header for the second column
     * @param file       a filename
     * @param descending if <tt>true</tt> rows are sorted descending according to the map keys, otherwise ascending.
     * @throws IOException
     */
    public static void writeHistogram(TDoubleDoubleHashMap map, String keyCol, String valCol, String file, boolean descending) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(file));

        writer.write(keyCol);
        writer.write(TAB);
        writer.write(valCol);
        writer.newLine();

        double[] keys = map.keys();
        Arrays.sort(keys);
        if (descending)
            ArrayUtils.reverse(keys);

        for (double key : keys) {
            writer.write(String.valueOf(key));
            writer.write(TAB);
            writer.write(String.valueOf(map.get(key)));
            writer.newLine();
        }

        writer.close();
    }

    /**
     * Writes a plain text file with two columns where the first column contains the map keys and second the map values.
     * If {@code sortByValues} is <tt>true</tt>, row are sorted according to the natural order of the map values,
     * otherwise rows are sorted by the natural order of the map keys.
     *
     * @param map          a map (labeled histogram)
     * @param keyCol       the header for the first column
     * @param valCol       the header for the second column
     * @param file         a filename
     * @param sortByValues if <tt>true</tt> the rows are sorted by map values, otherwise by key values
     * @throws IOException
     */
    public static void writeLabeledHistogram(TObjectDoubleHashMap<String> map, String keyCol, String valCol, String file, boolean sortByValues) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(file));

        writer.write(keyCol);
        writer.write(TAB);
        writer.write(valCol);
        writer.newLine();

        String[] keys = map.keys(new String[map.size()]);
        if (sortByValues) {
            List<Tuple<String, Double>> list = new LinkedList<>();
            TObjectDoubleIterator<String> it = map.iterator();
            for (int i = 0; i < map.size(); i++) {
                it.advance();
                list.add(new Tuple<>(it.key(), it.value()));
            }

            Collections.sort(list, new Comparator<Tuple<String, Double>>() {

                @Override
                public int compare(Tuple<String, Double> o1, Tuple<String, Double> o2) {
                    double result = o1.getSecond() - o2.getSecond();
                    if (result == 0) {
                        if (o1.getFirst().equals(o2.getFirst()))
                            return 0;
                        else
                            return o1.hashCode() - o2.hashCode();
                    } else if (result < 0)
                        return 1;
                    else
                        return -1;
                }
            });

            for (int i = 0; i < list.size(); i++) {
                keys[i] = list.get(i).getFirst();
            }
        } else {
            Arrays.sort(keys);
        }

        for (String key : keys) {
            writer.write(key);
            writer.write(TAB);
            writer.write(String.valueOf(map.get(key)));
            writer.newLine();
        }

        writer.close();
    }

    /**
     * Writes a plain text file with two columns where the first column contains the map keys and second the map values.
     * Rows are sorted by the natural order of the map keys.
     *
     * @param map    a map (labeled histogram)
     * @param keyCol the header for the first column
     * @param valCol the header for the second column
     * @param file   a filename
     * @throws IOException
     */
    public static void writeLabeledHistogram(TObjectDoubleHashMap<String> map, String keyCol, String valCol, String file) throws IOException {
        writeLabeledHistogram(map, keyCol, valCol, file, false);
    }

    /**
     * Extracts the values out of the {@code DescriptiveStatistics} object and than cals {@link
     * #writeBoxplot(TDoubleObjectHashMap, String)}.
     *
     * @param table a map with a {@code DescriptiveStatistics} object as value
     * @param file  the filename
     * @throws IOException
     */
    public static void writeBoxplotStats(TDoubleObjectHashMap<DescriptiveStatistics> table, String file) throws IOException {
        TDoubleObjectIterator<DescriptiveStatistics> it = table.iterator();
        TDoubleObjectHashMap<double[]> newTable = new TDoubleObjectHashMap<double[]>();
        for (int i = 0; i < table.size(); i++) {
            it.advance();
            newTable.put(it.key(), it.value().getValues());
        }

        writeBoxplot(newTable, file);

    }

    /**
     * Writes a table with one column for each map entry. Columns are filled with the list of values in each map entry.
     * Column headers are the map keys. For instance, use this function to create boxplots.
     *
     * @param table a map with a list of samples as value
     * @param file  the filename
     * @throws IOException
     */
    public static void writeBoxplot(TDoubleObjectHashMap<double[]> table, String file) throws IOException {
        int maxSize = 0;
        TDoubleObjectIterator<double[]> it = table.iterator();
        for (int i = 0; i < table.size(); i++) {
            it.advance();
            maxSize = Math.max(maxSize, it.value().length);
        }

        double keys[] = table.keys();
        Arrays.sort(keys);

        BufferedWriter writer = new BufferedWriter(new FileWriter(file));
        for (int k = 0; k < keys.length; k++) {
            writer.write(String.valueOf(keys[k]));
            if (k + 1 < keys.length)
                writer.write(TAB);
        }
        writer.newLine();

        for (int i = 0; i < maxSize; i++) {
            for (int k = 0; k < keys.length; k++) {
                double[] list = table.get(keys[k]);
                if (i < list.length) {
                    writer.write(String.valueOf(list[i]));
                } else {
                    writer.write(NA);
                }
                if (k + 1 < keys.length)
                    writer.write(TAB);
            }
            writer.newLine();
        }
        writer.close();
    }

    /**
     * Writes a plain text file with two columns. The first columns contains the map keys, the second the values in the
     * {@code DescriptiveStatistics} object value. For each key, rows are repeated for all values in the {@code
     * DescriptiveStatistics} object.
     *
     * @param table a map with samples stored in a {@code DescriptiveStatistics} object
     * @param file  the filename
     * @throws IOException
     */
    public static void writeScatterPlot(TDoubleObjectHashMap<DescriptiveStatistics> table, String file) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(file));

        TDoubleObjectIterator<DescriptiveStatistics> it = table.iterator();
        for (int i = 0; i < table.size(); i++) {
            it.advance();
            double[] vals = it.value().getValues();
            for (int j = 0; j < vals.length; j++) {
                writer.write(String.valueOf(it.key()));
                writer.write(TAB);
                writer.write(String.valueOf(vals[j]));
                writer.newLine();
            }
        }
        writer.close();
    }

    /**
     * Writes a table with columns map-key and statistical indicators mean, median, min, max and number of samples. Rows
     * are sorted according to the natural order of the map keys.
     *
     * @param statsMap a map with {@code DescriptiveStatistics} objects
     * @param keyLabel the header for the first column (containing the map keys)
     * @param file     the filename
     * @throws IOException
     */
    public static void writeStatistics(TDoubleObjectHashMap<DescriptiveStatistics> statsMap, String keyLabel, String file) throws IOException {
        double[] keys = statsMap.keys();
        Arrays.sort(keys);

        BufferedWriter writer = new BufferedWriter(new FileWriter(file));

        writer.write(keyLabel);
        writer.write(TAB);
        writer.write("mean");
        writer.write(TAB);
        writer.write("median");
        writer.write(TAB);
        writer.write("min");
        writer.write(TAB);
        writer.write("max");
        writer.write(TAB);
        writer.write("n");
        writer.newLine();

        for (double key : keys) {
            DescriptiveStatistics stats = statsMap.get(key);

            writer.write(String.valueOf(key));
            writer.write(TAB);
            writer.write(String.valueOf(stats.getMean()));
            writer.write(TAB);
            writer.write(String.valueOf(stats.getPercentile(50)));
            writer.write(TAB);
            writer.write(String.valueOf(stats.getMin()));
            writer.write(TAB);
            writer.write(String.valueOf(stats.getMax()));
            writer.write(TAB);
            writer.write(String.valueOf(stats.getN()));
            writer.newLine();
        }

        writer.close();
    }

    /**
     * Writes a table with columns map-key and statistical indicators mean, median, min, max and number of samples. Rows
     * are sorted according to the natural order of the map key.
     *
     * @param statsMap a map with {@code DescriptiveStatistics} objects
     * @param filename the filename
     * @throws IOException
     */
    public static void writeStatistics(Map<String, DescriptiveStatistics> statsMap, String filename) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(filename));

        writer.write("property");
        writer.write(TAB);
        writer.write("mean");
        writer.write(TAB);
        writer.write("median");
        writer.write(TAB);
        writer.write("min");
        writer.write(TAB);
        writer.write("max");
        writer.write(TAB);
        writer.write("n");
        writer.newLine();
        writer.newLine();

        SortedMap<String, DescriptiveStatistics> sortedMap = new TreeMap<>(statsMap);
        for (Entry<String, DescriptiveStatistics> entry : sortedMap.entrySet()) {
            writer.write(entry.getKey());
            writer.write("\t");
            writer.write(String.valueOf(entry.getValue().getMean()));
            writer.write("\t");
            writer.write(String.valueOf(entry.getValue().getPercentile(50)));
            writer.write("\t");
            writer.write(String.valueOf(entry.getValue().getMin()));
            writer.write("\t");
            writer.write(String.valueOf(entry.getValue().getMax()));
            writer.write("\t");
            writer.write(String.valueOf(entry.getValue().getN()));
            writer.write("\t");
            writer.write(String.valueOf(entry.getValue().getVariance()));
            writer.newLine();
        }

        writer.close();
    }

    /**
     * Writes a plain text file with two columns where the first column contains the values from {@code col1} and the
     * second from {@code col2}. Both lists must have the same length.
     *
     * @param col1     a list of values
     * @param col2     a list of values
     * @param name1    the header of the first column
     * @param name2    the header of the second column
     * @param filename the filename
     * @throws IOException
     */
    public static void writeScatterPlot(TDoubleArrayList col1, TDoubleArrayList col2, String name1, String name2, String filename) throws IOException {
        if (col1.size() != col2.size()) {
            throw new RuntimeException(String.format("Unequal number of rows (col1:%s, col2:%s)", col1.size(), col2.size()));
        }

        BufferedWriter writer = new BufferedWriter(new FileWriter(filename));

        writer.write(name1);
        writer.write(TAB);
        writer.write(name2);
        writer.newLine();

        for (int i = 0; i < col1.size(); i++) {
            writer.write(String.valueOf(col1.get(i)));
            writer.write("\t");
            writer.write(String.valueOf(col2.get(i)));
            writer.newLine();
        }

        writer.close();
    }
}
