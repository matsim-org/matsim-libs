/*
 * Cadyts - Calibration of dynamic traffic simulations
 *
 * Copyright 2009, 2010, 2011 Gunnar Fl�tter�d
 * 
 *
 * This file is part of Cadyts.
 *
 * Cadyts is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Cadyts is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Cadyts.  If not, see <http://www.gnu.org/licenses/>.
 *
 * contact: gunnar.floetteroed@epfl.ch
 *
 */
package floetteroed.utilities.latex;

import static floetteroed.utilities.math.MathHelpers.round;
import static java.lang.Double.NEGATIVE_INFINITY;
import static java.lang.Double.POSITIVE_INFINITY;
import static java.lang.Math.ceil;
import static java.lang.Math.floor;
import static java.lang.Math.max;
import static java.lang.Math.min;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import floetteroed.utilities.Tuple;

/**
 * An attempt to deal with the (at least to me incomprehensible) coordinate
 * system specification for data plotting in PSTricks.
 * 
 * @author Gunnar Flötteröd
 * 
 */
public class PSTricksDiagramWriter {

	// -------------------- CONSTANTS --------------------

	private final double sizeX_cm;

	private final double sizeY_cm;

	// -------------------- MEMBERS --------------------

	private String endLine = "";//"\n";

	private Double xMin = null;

	private Double yMin = null;

	private Double xMax = null;

	private Double yMax = null;

	private Double xDelta = null;

	private Double yDelta = null;

	private String xLabel = "";

	private String yLabel = "";

	@Deprecated
	private String plotAttrs = "";

	// TODO NEW
	private List<String> commandList = new ArrayList<String>();

	// TODO NEW
	private Map<String, String> key2plotAttrs = new HashMap<String, String>();

	private Map<String, List<Tuple<Double, Double>>> key2xyList = new LinkedHashMap<String, List<Tuple<Double, Double>>>();

	// -------------------- CONSTRUCTION --------------------

	public PSTricksDiagramWriter(final double sizeX_cm, final double sizeY_cm) {
		this.sizeX_cm = sizeX_cm;
		this.sizeY_cm = sizeY_cm;
	}

	// -------------------- IMPLEMENTATION --------------------

	public void setEndLine(final String endLine) {
		this.endLine = endLine;
	}

	public void setLabelX(final String xLabel) {
		this.xLabel = (xLabel != null ? xLabel : "");
	}

	public void setLabelY(final String yLabel) {
		this.yLabel = (yLabel != null ? yLabel : "");
	}

	@Deprecated
	public void setPlotAttrs(final String attrs) {
		this.plotAttrs = (attrs != null ? attrs : "");
	}

	// TODO NEW
	public void setPlotAttrs(final String key, final String attrs) {
		this.key2plotAttrs.put(key, attrs);
	}

	// TODO NEW
	public void addCommand(final String command) {
		this.commandList.add(command);
	}

	public void setXMin(final Double xMin) {
		this.xMin = xMin;
	}

	public void setXMax(final Double xMax) {
		this.xMax = xMax;
	}

	public void setYMin(final Double yMin) {
		this.yMin = yMin;
	}

	public void setYMax(final Double yMax) {
		this.yMax = yMax;
	}

	public void setXDelta(final Double xDelta) {
		this.xDelta = xDelta;
	}

	public void setYDelta(final Double yDelta) {
		this.yDelta = yDelta;
	}

	public void add(final String key, final double x, final double y) {
		List<Tuple<Double, Double>> xyList = this.key2xyList.get(key);
		if (xyList == null) {
			xyList = new ArrayList<Tuple<Double, Double>>();
			this.key2xyList.put(key, xyList);
		}
		xyList.add(new Tuple<Double, Double>(x, y));
	}

	public void printAll(final PrintStream out) {

		/*
		 * (0) DEFINE SOME MAGIC CONSTANTS
		 */
		final double fact = 0.0;
		final int digitsShort = 2;
		final int digits = 6;
		final int dataColumns = 4;
		final double left_cm = 1.0;
		final double lower_cm = 0.5;
		final double right_cm = 0.0;
		final double top_cm = 0.1;

		/*
		 * (1) IDENTIFY DATA RANGE & ROUND TO INTEGER
		 */

		double xMin;
		if (this.xMin == null) {
			xMin = POSITIVE_INFINITY;
		} else {
			xMin = this.xMin;
		}

		double yMin;
		if (this.yMin == null) {
			yMin = POSITIVE_INFINITY;
		} else {
			yMin = this.yMin;
		}

		double xMax;
		if (this.xMax == null) {
			xMax = NEGATIVE_INFINITY;
		} else {
			xMax = this.xMax;
		}

		double yMax;
		if (this.yMax == null) {
			yMax = NEGATIVE_INFINITY;
		} else {
			yMax = this.yMax;
		}

		for (List<Tuple<Double, Double>> xyList : this.key2xyList.values()) {
			for (Tuple<Double, Double> xy : xyList) {
				final double x = xy.getA();
				final double y = xy.getB();
				if (this.xMin == null) {
					xMin = min(xMin, x);
				}
				if (this.xMax == null) {
					xMax = max(xMax, x);
				}
				if (this.yMin == null) {
					yMin = min(yMin, y);
				}
				if (this.yMax == null) {
					yMax = max(yMax, y);
				}
			}
		}

		xMin = floor(xMin);
		yMin = floor(yMin);
		xMax = ceil(xMax);
		yMax = ceil(yMax);

		final double xSpan = xMax - xMin;
		final double ySpan = yMax - yMin;

		/*
		 * (2) DEFINE COORDINATE UNITS
		 */
		final double xUnit = this.sizeX_cm / xSpan;
		final double yUnit = this.sizeY_cm / ySpan;

		/*
		 * (3) WRITE OUT PSPICTURE DIMENSIONS (IN CM)
		 */
		final double xMin_cm = round(xMin * xUnit - left_cm, digits);
		final double yMin_cm = round(yMin * yUnit - lower_cm, digits);
		final double xMax_cm = round((xMax + fact * xSpan) * xUnit + right_cm,
				digits);
		final double yMax_cm = round((yMax + fact * ySpan) * yUnit + top_cm,
				digits);
		out.print("\\begin{pspicture}(");
		out.print(xMin_cm);
		out.print("cm,");
		out.print(yMin_cm);
		out.print("cm)(");
		out.print(xMax_cm);
		out.print("cm,");
		out.print(yMax_cm);
		out.print("cm)");
		out.println(endLine);

		/*
		 * (4) WRITE OUT LABELS (IN CM COORDINATES)
		 */
		out.print("  \\rput[rb](");
		out.print(round(xMax_cm - right_cm, digits));
		out.print("cm,");
		out.print(round(yMin_cm + lower_cm + 0.25, digits));
		out.print("cm){");
		out.print(this.xLabel);
		out.print("}");
		out.println(endLine);

		out.print("  \\rput[lt](");
		out.print(round(xMin_cm + left_cm + 0.25, digits));
		out.print("cm,");
		out.print(round(yMax_cm - top_cm, digits));
		out.print("cm){");
		out.print(this.yLabel);
		out.print("}");
		out.println(endLine);

		/*
		 * (5) SWITCH TO DATA COORDINATES
		 */
		out.print("  \\psset{xunit=");
		out.print(round(xUnit, digits));
		out.print("cm,yunit=");
		out.print(round(yUnit, digits));
		out.print("cm}");
		out.println(endLine);

		/*
		 * (6) WRITE OUT AXES
		 */
		out.print("  \\psaxes[arrowsize=6pt 0,Ox=");
		out.print(xMin);
		out.print(",Oy=");
		out.print(yMin);
		out.print(",Dx=");
		if (this.xDelta == null) {
			out.print(round(Math.ceil(xSpan / 6), digitsShort));
		} else {
			out.print(round(this.xDelta, digitsShort));
		}
		out.print(",Dy=");
		if (this.yDelta == null) {
			out.print(round(Math.ceil(ySpan / 6), digitsShort));
		} else {
			out.print(round(this.yDelta, digitsShort));
		}
		out.print("]{->}(");
		out.print(xMin);
		out.print(",");
		out.print(yMin);
		out.print(")(");
		out.print(round(xMax + fact * xSpan, digits));
		out.print(",");
		out.print(round(yMax + fact * ySpan, digits));
		out.print(")");
		out.println(endLine);

		/*
		 * (7) WRITE OUT DATA
		 */
		for (final Map.Entry<String, List<Tuple<Double, Double>>> entry : this.key2xyList
				.entrySet()) {
			out.print("  \\savedata{\\");
			out.print(entry.getKey());
			out.print("}[{");
			int i = 0;
			for (Tuple<Double, Double> xy : entry.getValue()) {
				if (i++ % dataColumns == 0) {
					out.println(endLine);
					out.print("    ");
				}

				out.print("{");
				out.print(round(xy.getA(), digits));
				out.print(", ");
				out.print(round(xy.getB(), digits));
				out.print("}");

				if (i < entry.getValue().size()) {
					out.print(", ");
				}

				// out.print("D ");
				// out.print(round(xy.getA(), digits));
				// out.print(" D ");
				// out.print(round(xy.getB(), digits));
				// out.print(" ");

			}
			out.println(endLine);
			// out.println("  }]\n");
			out.println("}]\n");
			out.print("  \\dataplot[");
			{
				// TODO NEW
				if (this.key2plotAttrs.containsKey(entry.getKey())) {
					out.print(this.key2plotAttrs.get(entry.getKey()));
				}
				// out.print(this.plotAttrs);
			}
			out.print("]{\\");
			out.print(entry.getKey());
			out.print("}");
			out.println(endLine);
		}

		/*
		 * (8) WRITE OUT FURTHER COMMANDS
		 */
		for (String cmd : this.commandList) {
			out.print(cmd);
			out.println(endLine);
		}

		out.println("\\end{pspicture}\n");
	}

	@Override
	public String toString() {
		final ByteArrayOutputStream os = new ByteArrayOutputStream();
		final PrintStream ps = new PrintStream(os);
		this.printAll(ps);
		try {
			return os.toString("UTF8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	// MAIN FUNCTION, ONLY FOR TESTING

	public static void main(String[] args) {
		final Random rnd = new Random();
		final PSTricksDiagramWriter w = new PSTricksDiagramWriter(8, 5);
		w.setLabelX("X-Achse");
		w.setLabelY("Y-Achse");
		w.setPlotAttrs("plotstyle=dots");

		for (int i = 0; i < 100; i++) {
			w.add("mydata", rnd.nextGaussian(), rnd.nextGaussian());
		}

		w.printAll(System.out);
	}

}
