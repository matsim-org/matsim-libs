/* *********************************************************************** *
 * project: org.matsim.*
 * DistributionCreator.java
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

/**
 *
 */
package playground.yu.utils.io;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Map.Entry;

import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.utils.charts.ChartUtil;
import org.matsim.core.utils.charts.XYScatterChart;

import playground.yu.utils.charts.TimeScatterChart;
import playground.yu.utils.container.Collection2Array;
import playground.yu.utils.container.CollectionMath;

/**
 * a small tool, which is able to create .txt-file or chart for a distribution.
 * e.g. x-axis - value of a parameter or factor or characteristic, y-axis -
 * number of this parameter with value in a small range
 * 
 * @author yu
 * 
 */
public class DistributionCreator {
	/**
	 * a small test about normal distribution
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		double[] dArray = new double[1000];
		Random r = MatsimRandom.getRandom();
		for (int i = 0; i < dArray.length; i++) {
			dArray[i] = r.nextGaussian();
		}
		// without interval
		DistributionCreator creator = new DistributionCreator(dArray);
		creator.write("D:/tmp/tmp.log");
		creator.createChart("D:/tmp/tmp.png", "title", "tmpValue",
				"size of tmp Value");
		// with interval
		DistributionCreator creator2 = new DistributionCreator(dArray, 0.05);
		creator2.write("D:/tmp/tmp2.log");
		creator2.createChart("D:/tmp/tmp2.png", "title", "tmpValue",
				"size of tmp Value");
	}

	private double[] data;
	private Map<Double, Integer> dataMap = new HashMap<Double, Integer>();

	private double interval;

	private Map<String, double[]> datas = null;
	private Map<String, Map<Double, Integer>> dataMaps = null;

	/**
	 * no interval of the x-axis data means all data records will be used
	 * 
	 * @param data
	 *            an array of data being analyzed
	 * 
	 */
	public DistributionCreator(double[] data) {
		this(data, 0d);
	}

	// --------------CONSTRUCTORS------------------
	/**
	 * @param data
	 *            an array of data being analyzed
	 * @param interval
	 *            interval of the x-axis data,default value == 0d means all data
	 *            records will be used
	 */
	public DistributionCreator(double[] data, double interval) {
		this.data = data;
		this.interval = interval;
		initialize();
	}

	/**
	 * no interval of the x-axis data means all data records will be used
	 * 
	 * @param dataList
	 *            data a {@code List} of data being analyzed, ({@code Set} is
	 *            not supported here, to avoid losing of repetition data record)
	 */
	public DistributionCreator(List<Double> dataList) {
		this(dataList, 0d);
	}

	/**
	 * @param dataList
	 *            data a {@code List} of data being analyzed, ({@code Set} is
	 *            not supported here, to avoid losing of repetition data record)
	 * @param interval
	 *            interval of the x-axis data,default value == 0d means all data
	 *            records will be used
	 */
	public DistributionCreator(List<Double> dataList, double interval) {
		data = Collection2Array.toArrayFromDouble(dataList);
		this.interval = interval;
		initialize();
	}

	/**
	 * version 2, should only call methode xxxx2(...) e.g. write2(...) (more
	 * data series version)
	 * 
	 * @param dataList
	 * @param interval
	 */
	public DistributionCreator(Map<String, List<Double>> dataList,
			double interval) {
		datas = new HashMap<String, double[]>();
		dataMaps = new HashMap<String, Map<Double, Integer>>();
		for (Entry<String, List<Double>> entry : dataList.entrySet()) {
			datas.put(entry.getKey(), Collection2Array.toArrayFromDouble(entry
					.getValue()));
		}
		this.interval = interval;
		initialize2();
	}

	public void createChart(String filename, String title, String xAxisLabel,
			String yAxisLabel) {
		XYScatterChart chart = new XYScatterChart(title, xAxisLabel, yAxisLabel);
		double xs[] = Collection2Array.toArrayFromDouble(dataMap.keySet());
		double ys[] = Collection2Array.toDoubleArray(dataMap.values());
		chart.addSeries(title, xs, ys);
		chart.saveAsPng(filename, 1024, 768);
	}

	public void createChartPercent(String filename, String title,
			String xAxisLabel, String yAxisLabel) {
		XYScatterChart chart = new XYScatterChart(title, xAxisLabel, yAxisLabel);
		double xs[] = Collection2Array.toArrayFromDouble(dataMap.keySet());

		Collection<Integer> values = dataMap.values();
		double sum = CollectionMath.getSum(values);
		Iterator<Integer> it = values.iterator();
		double ys[] = new double[xs.length];
		for (int i = 0; it.hasNext(); i++) {
			ys[i] = (double) it.next() / sum * 100d;
		}

		chart.addSeries(title, xs, ys);
		chart.saveAsPng(filename, 640, 480);
	}

	/**
	 * (more data series version)
	 * 
	 * @param filename
	 * @param title
	 * @param xAxisLabel
	 * @param yAxisLabel
	 */
	public void createChart2percent(String filename, String title,
			String xAxisLabel, String yAxisLabel, final boolean timeXAxis) {
		ChartUtil chart = timeXAxis ? new TimeScatterChart(title, xAxisLabel,
				yAxisLabel) : new XYScatterChart(title, xAxisLabel, yAxisLabel);

		for (String series : dataMaps.keySet()) {
			Map<Double, Integer> aDataMap = dataMaps.get(series);
			double xs[] = Collection2Array.toArrayFromDouble(aDataMap.keySet());
			Collection<Integer> values = aDataMap.values();
			double sum = CollectionMath.getSum(values);
			Iterator<Integer> it = values.iterator();
			double ys[] = new double[xs.length];
			for (int i = 0; it.hasNext(); i++) {
				ys[i] = (double) it.next() / sum * 100d;
			}
			if (timeXAxis) {
				((TimeScatterChart) chart).addSeries(series, xs, ys);
			} else {
				((XYScatterChart) chart).addSeries(series, xs, ys);
			}
		}

		chart.saveAsPng(filename, 1024, 768);
	}

	public void createChart2(String filename, String title, String xAxisLabel,
			String yAxisLabel, final boolean timeXAxis) {
		ChartUtil chart = timeXAxis ? new TimeScatterChart(title, xAxisLabel,
				yAxisLabel) : new XYScatterChart(title, xAxisLabel, yAxisLabel);

		for (String series : dataMaps.keySet()) {
			Map<Double, Integer> aDataMap = dataMaps.get(series);
			double xs[] = Collection2Array.toArrayFromDouble(aDataMap.keySet());

			double ys[] = Collection2Array.toDoubleArray(aDataMap.values());
			if (timeXAxis) {
				((TimeScatterChart) chart).addSeries(series, xs, ys);
			} else {
				((XYScatterChart) chart).addSeries(series, xs, ys);
			}
		}

		chart.saveAsPng(filename, 1024, 768);
	}

	protected void gatherInformation() {
		double key;

		for (int i = 0; i < data.length; i++) {
			key = interval > 0d ? getXAxisValue(data[i]) : data[i];
			Integer cnt = dataMap.get(key);
			if (cnt == null) {
				dataMap.put(key, 1);
			} else {
				dataMap.put(key, cnt + 1);
			}
		}

	}

	protected void gatherInformation2() {
		for (String mapKey : datas.keySet()) {
			double[] aData = datas.get(mapKey);

			double key;
			for (int i = 0; i < aData.length; i++) {
				key = interval > 0d ? getXAxisValue(aData[i]) : aData[i];

				Map<Double, Integer> aDataMap = dataMaps.get(mapKey);
				if (aDataMap == null) {
					aDataMap = new HashMap<Double, Integer>();
					dataMaps.put(mapKey, aDataMap);
				}

				Integer cnt = aDataMap.get(key);
				if (cnt == null) {
					aDataMap.put(key, 1);
				} else {
					aDataMap.put(key, cnt + 1);
				}
			}
		}
	}

	/**
	 * use it only if interval > 0
	 * 
	 * @param record
	 *            data record
	 * @return the value on X-axis corresponding to the data record
	 */
	protected double getXAxisValue(double record) {
		return (int) (record / interval) * interval
				- (record < 0 ? interval : 0d);
	}

	/**
	 * this method should be called right after contructor
	 */
	private void initialize() {
		gatherInformation();
	}

	/**
	 * this method should be called right after contructor (more data series
	 * version)
	 */
	private void initialize2() {
		gatherInformation2();
	}

	public void write(String filename) {
		SimpleWriter writer = new SimpleWriter(filename);
		writer.writeln("value+\tsize");
		for (Entry<Double, Integer> entry : dataMap.entrySet()) {
			writer.writeln(entry.getKey() + "\t" + entry.getValue());
		}
		writer.close();
	}

	public void writePercent(String filename) {
		SimpleWriter writer = new SimpleWriter(filename);
		writer.writeln("value+\tfraction");
		double sum = CollectionMath.getSum(dataMap.values());
		for (Entry<Double, Integer> entry : dataMap.entrySet()) {
			writer.writeln(entry.getKey() + "\t" + (double) entry.getValue()
					/ sum * 100d);
		}
		writer.close();
	}

	public void write2(String filename) {
		SimpleWriter writer = new SimpleWriter(filename);

		for (String key : dataMaps.keySet()) {

			writer.writeln(key + "\nvalue+\tsize");
			for (Entry<Double, Integer> entry : dataMaps.get(key).entrySet()) {
				writer.writeln(entry.getKey() + "\t" + entry.getValue());
			}
			writer.writeln("-------------------------");
		}

		writer.close();
	}

	public void write2percent(String filename) {
		SimpleWriter writer = new SimpleWriter(filename);

		for (String key : dataMaps.keySet()) {

			writer.writeln(key + "\nvalue+\tfraction");

			Map<Double, Integer> localDataMap = dataMaps.get(key);
			double sum = CollectionMath.getSum(localDataMap.values());
			// TODO sum

			for (Entry<Double, Integer> entry : localDataMap.entrySet()) {
				writer.writeln(entry.getKey() + "\t" + entry.getValue() / sum
						* 100d);
			}
		}

		writer.close();
	}
}
