/* *********************************************************************** *
 * project: org.matsim.*
 * BoxAndWhiskersChart.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.thibautd.utils.charts;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.DatasetRenderingOrder;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYBoxAndWhiskerRenderer;
import org.jfree.chart.renderer.xy.XYErrorRenderer;
import org.jfree.data.xy.AbstractIntervalXYDataset;
import org.matsim.core.utils.charts.ChartUtil;
import org.matsim.core.utils.collections.Tuple;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Allows to plot easily box and whishers charts against an X axis
 * @author thibautd
 */
public class BoxAndWhiskersChart extends ChartUtil {

	//private final DefaultBoxAndWhiskerCategoryDataset boxes =
	//	new DefaultBoxAndWhiskerCategoryDataset();
	private final BoxAndWhiskerXYNumberDataset boxes =
		new BoxAndWhiskerXYNumberDataset();
	private final StdDevDataset deviationBars =
		new StdDevDataset();
	private boolean datasetsAreCreated = false;

	private final double binWidth;
	private boolean plotStdDev;

	private final List<Tuple<Double,Double>> values = new ArrayList<Tuple<Double,Double>>();
	private double maxX = Double.NEGATIVE_INFINITY;
	private double minX = Double.POSITIVE_INFINITY;

	// /////////////////////////////////////////////////////////////////////////
	// constructors
	// /////////////////////////////////////////////////////////////////////////
	/**
	 * @param title the title of the chart
	 * @param xAxisLabel the name of the X axis
	 * @param yAxisLabel the name of the Y axis
	 * @param binWidth the width on which to agregate data
	 * @param plotStdDev if true, a bar around the mean will display the std. dev.
	 */
	public BoxAndWhiskersChart(
			final String title,
			final String xAxisLabel,
			final String yAxisLabel,
			double binWidth,
			boolean plotStdDev) {
		super(title, xAxisLabel, yAxisLabel);
		this.binWidth = binWidth;
		this.plotStdDev = plotStdDev;
	}

	/**
	 * Creates a chart without standard deviation bar.
	 *
	 * @param title the title of the chart
	 * @param xAxisLabel the name of the X axis
	 * @param yAxisLabel the name of the Y axis
	 * @param binWidth the width on which to agregate data
	 */
	public BoxAndWhiskersChart(
			final String title,
			final String xAxisLabel,
			final String yAxisLabel,
			double binWidth) {
		this( title , xAxisLabel , yAxisLabel , binWidth , false );
	}

	// /////////////////////////////////////////////////////////////////////////
	// ChartUtil interface
	// /////////////////////////////////////////////////////////////////////////
	/**
	 * Creates the chart if it does not exits, and returns it. Once called,
	 * no modifications can be made to the dataset.
	 * @return the chart
	 */
	@Override
	public JFreeChart getChart() {
		if (this.chart == null) createChart();
		return this.chart;
	}

	// /////////////////////////////////////////////////////////////////////////
	// specific methods
	// /////////////////////////////////////////////////////////////////////////
	public void setPlotStandardDeviation(final boolean bool) {
		if (plotStdDev != bool) {
			plotStdDev = bool;
			this.chart = null;
		}
	}

	/**
	 * adds a value to the dataset.
	 *
	 * During chart creation, y values of all point of x values belonging to a
	 * given bin will be used to compute the statistics
	 */
	public void add(final double x, final double y) {
		maxX = Math.max(maxX, x);
		minX = Math.min(minX, x);
		this.values.add(new Tuple<Double, Double>(x,y));
	}

	private void createChart() {
		boolean legend = false;

		this.createDataSet();

		this.chart =  ChartFactory.createBoxAndWhiskerChart(
				this.chartTitle, this.xAxisLabel, this.yAxisLabel,
				this.boxes, legend);

		XYPlot plot = this.chart.getXYPlot();
		plot.setDomainAxis(new NumberAxis(this.xAxisLabel));
		plot.getDomainAxis().configure();

		if (plotStdDev) {
			XYErrorRenderer renderer = new XYErrorRenderer();
			// in black
			renderer.setErrorPaint( Color.BLACK );
			// only plot Y error
			renderer.setDrawXError( false );
			// do not render average (already done by the B&W)
			renderer.setBaseShapesVisible( false );
			plot.setRenderer( 1 , renderer );
			plot.setDataset( 1 , deviationBars );
			plot.setDatasetRenderingOrder( DatasetRenderingOrder.FORWARD );
		}

		//this.addMatsimLogo();

		//try {
		//	this.addDefaultFormatting();
		//} catch (NullPointerException e) {
		//	// occurs if no legend
		//}

		//this.chart.setBackgroundPaint(Color.white);
		plot.setBackgroundPaint(Color.white);
		XYBoxAndWhiskerRenderer renderer = (XYBoxAndWhiskerRenderer) plot.getRenderer();
		//renderer.setFillBox(false);
		//renderer.setSeriesOutlinePaint(0, Color.black);
		//renderer.setSeriesPaint(0, Color.black);
		renderer.setBoxPaint(renderer.getSeriesPaint(0));
		//auto-adjust
		renderer.setBoxWidth(-1);
	}

	private void createDataSet() {
		if (datasetsAreCreated) return;
		Collections.sort(this.values, new TupleComparator());
		List<Double> currentBox = new ArrayList<Double>();;
		StdDevDataset.Element stdDevElement = new StdDevDataset.Element();
		double currentUpperBound = minX + binWidth;

		for (Tuple<Double, Double> tuple : this.values) {
			if (tuple.getFirst().doubleValue() < currentUpperBound) {
				currentBox.add(tuple.getSecond());
				if (plotStdDev) stdDevElement.addValue( tuple.getSecond() );
			}
			else {
				//this.boxes.add(currentBox, "", currentUpperBound - (binWidth/2d));
				double x = currentUpperBound - (binWidth/2d);
				this.boxes.add(x, currentBox);

				//if (plotStdDev) {
					this.deviationBars.addElement( x , stdDevElement );
					stdDevElement = new StdDevDataset.Element();
				//}
				currentBox = new ArrayList<Double>();
				currentUpperBound += binWidth;
			}
		}
		//this.boxes.add(currentBox, "", currentUpperBound - (binWidth/2d));

		double x = currentUpperBound - (binWidth/2d);
		this.boxes.add(x , currentBox);
		/*if (plotStdDev)*/ this.deviationBars.addElement( x , stdDevElement );

		datasetsAreCreated = true;
	}

	private static class TupleComparator implements Comparator<Tuple<Double, ? extends Object>> {

		@Override
		public int compare(final Tuple<Double, ? extends Object> arg0,
				final Tuple<Double, ? extends Object> arg1) {
			return arg0.getFirst().compareTo(arg1.getFirst());
		}
	}
}

class StdDevDataset extends AbstractIntervalXYDataset {
	private static final long serialVersionUID = 1L;

	private final List< Tuple<Double, Element> > elements =
		 	new ArrayList< Tuple<Double, Element> >();
	private final Comparable<? extends Object> key;

	public StdDevDataset()  {
		this( "standard deviation" );
	}

	public StdDevDataset( final Comparable<? extends Object> seriesKey ) {
		this.key = seriesKey;
	}
	// /////////////////////////////////////////////////////////////////////////
	// specific methods
	// /////////////////////////////////////////////////////////////////////////
	public void addElement( final double x, final Element element ) {
		elements.add( new Tuple<Double, Element>(x, element) );
	}

	// /////////////////////////////////////////////////////////////////////////
	// Dataset interface
	// /////////////////////////////////////////////////////////////////////////
	@Override
	public Number getEndX(final int series, final int item) {
		return getX( series , item );
	}

	@Override
	public Number getEndY(final int series, final int item) {
		Element elem = elements.get( item ).getSecond();
		return elem.getAverage() + elem.getStdDev() ;
	}

	@Override
	public Number getStartX(final int series, final int item) {
		return getX( series , item );
	}

	@Override
	public Number getStartY(final int series, final int item) {
		Element elem = elements.get( item ).getSecond();
		return elem.getAverage() - elem.getStdDev() ;
	}

	@Override
	public int getItemCount(final int series) {
		return elements.size();
	}

	@Override
	public Number getX(final int series, final int item) {
		return elements.get( item ).getFirst();
	}

	@Override
	public Number getY(final int series, final int item) {
		Element elem = elements.get( item ).getSecond();
		return elem.getAverage();
	}

	@Override
	public int getSeriesCount() {
		return 1;
	}

	@Override
	public Comparable<? extends Object> getSeriesKey(final int series) {
		return key;
	}

	// /////////////////////////////////////////////////////////////////////////
	// helpers
	// /////////////////////////////////////////////////////////////////////////
	public static class Element {
		private final List<Double> values = new ArrayList<Double>();
		private boolean locked = false;
		private double average = Double.NaN;
		private double stdDev = Double.NaN;

		public void addValue( final double value ) {
			if (locked) throw new IllegalStateException( "cannot modify dataset once accessed" );
			values.add( value );
		}

		public double getAverage() {
			process();
			return average;
		}

		public double getStdDev() {
			process();
			return stdDev;
		}

		private void process() {
			if (!locked) {
				locked = true;

				int count = 0;
				average = 0;
				for (double value : values) {
					count++;
					average += value;
				}
				average /= count;

				stdDev = 0;
				double current;
				for (double value : values) {
					current = value - average;
					current *= current;
					stdDev += current;
				}
				stdDev /= count;
				stdDev = Math.sqrt( stdDev );
			}
		}
	}
}
