/**
 * 
 */
package playground.yu.analysis;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;

import playground.yu.utils.charts.StackedBarChart;

/**
 * @author yu
 * 
 */
public class MZComparisonDataIO implements TabularFileHandler {
	private int lineCount = 0;

	private String[] chartRows;

	private Set<String> chartColumns = new HashSet<String>();

	/**
	 * @param arg0 -
	 *            String chart row
	 * @param arg1 -
	 *            String chart column
	 */
	private Map<String, Map<String, Double>> values = new HashMap<String, Map<String, Double>>();

	public void setData2Compare(MZComparisonData data2compare) {
		setValueFromData2Compare(
				"average daily distance of drivers [m] (MATSim)", "car",
				data2compare.getAvgDailyDistance_car_m());
		setValueFromData2Compare(
				"average daily distance of public transit passengers [m] (MATSim)",
				"pt", data2compare.getAvgDailyDistance_pt_m());
		setValueFromData2Compare(
				"average daily distance of pedestrians [m] (MATSim)", "walk",
				data2compare.getAvgDailyDistance_walk_m());
		setValueFromData2Compare(
				"average daily distance of persons with other trip modes [m] (MATSim)",
				"others", data2compare.getAvgDailyDistance_other_m());

		setValueFromData2Compare(
				"average daily en route time of drivers [min] (MATSim)", "car",
				data2compare.getAvgDailyEnRouteTime_car_min());
		setValueFromData2Compare(
				"average daily en route time of public transit passengers [min] (MATSim)",
				"pt", data2compare.getAvgDailyEnRouteTime_pt_min());
		setValueFromData2Compare(
				"average daily en route time of pedestrians [min] (MATSim)",
				"walk", data2compare.getAvgDailyEnRouteTime_walk_min());
		setValueFromData2Compare(
				"average daily en route time of persons with other trip modes [min] (MATSim)",
				"others", data2compare.getAvgDailyEnRouteTime_other_min());

		setValueFromData2Compare(
				"average daily distance of drivers from toll area [m] (MATSim)",
				"car", data2compare.getAvgTollDailyDistance_car_m());
		setValueFromData2Compare(
				"average daily distance of public transit passengers from toll area [m] (MATSim)",
				"pt", data2compare.getAvgTollDailyDistance_pt_m());
		setValueFromData2Compare(
				"average daily distance of pedestrians from toll area [m] (MATSim)",
				"walk", data2compare.getAvgTollDailyDistance_walk_m());
		setValueFromData2Compare(
				"average daily distance of persons with other trip modes from toll area [m] (MATSim)",
				"others", data2compare.getAvgTollDailyDistance_other_m());

		setValueFromData2Compare(
				"average daily en route time of drivers from toll area [min] (MATSim)",
				"car", data2compare.getAvgTollDailyEnRouteTime_car_min());
		setValueFromData2Compare(
				"average daily en route time of public transit passengers from toll area [min] (MATSim)",
				"pt", data2compare.getAvgTollDailyEnRouteTime_pt_min());
		setValueFromData2Compare(
				"average daily en route time of pedestrians from toll area [min] (MATSim)",
				"walk", data2compare.getAvgTollDailyEnRouteTime_walk_min());
		setValueFromData2Compare(
				"average daily en route time of persons with other trip modes from toll area [min] (MATSim)",
				"others", data2compare.getAvgTollDailyEnRouteTime_other_min());
	}

	private void setValueFromData2Compare(String chartColumn, String chartRow,
			double value) {
		chartColumns.add(chartColumn);
		Map<String, Double> m = values.get(chartRow);
		m.put(chartColumn, value);
		values.put(chartRow, m);
	}

	public void startRow(String[] row) {
		// TODO save information from MZ
		if (lineCount > 0) {
			chartColumns.add(row[0]);
			for (int i = 1; i < row.length; i++) {
				Map<String, Double> m = new HashMap<String, Double>();
				m.put(row[0], Double.valueOf(row[i]));
				values.put(chartRows[i - 1], m);
				// chartRows[i]->String0
				// row[0]->String1;
			}
		} else {
			chartRows = new String[row.length - 1];
			for (int i = 1; i < row.length; i++) {
				chartRows[i - 1] = row[i];
			}
		}
		lineCount++;
	}

	public void write(String outputBase) {
		try {
			BufferedWriter bw = IOUtils.getBufferedWriter(outputBase + ".txt");
			bw.write("\t");
			for (int i = 0; i < chartRows.length; i++) {
				bw.write("\t" + chartRows[i]);
			}
			bw.write("\n");
			for (String chartColumn : chartColumns) {
				bw.write(chartColumn);
				for (int i = 0; i < chartRows.length; i++) {
					bw.write("\t" + values.get(chartRows[i]).get(chartColumn));
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		// write chart
		int chartColumnsSize = chartColumns.size();
		double[][] values = new double[chartRows.length][chartColumnsSize];
		String[] chartColumns = (String[]) this.chartColumns.toArray();
		for (int i = 0; i < chartRows.length; i++) {
			for (int j = 0; j < chartColumnsSize; j++) {
				values[i][j] = this.values.get(chartRows[i]).get(
						chartColumns[j]);
			}
		}
		StackedBarChart chart = new StackedBarChart(
				"Mikrozensus 2005 vs MATSim", "category", "values");
		chart.addSeries(chartRows, chartColumns, values);
		chart.saveAsPng(outputBase + ".png", 800, 600);
	}

	public void readMZData(String inputFilename) {
		TabularFileParserConfig tfpc = new TabularFileParserConfig();
		// tfpc.setCommentTags(new String[] { "Verkehrsmittel" });
		tfpc.setDelimiterRegex("\t");
		tfpc.setFileName(inputFilename);
		try {
			new TabularFileParser().parse(tfpc, this);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// -----------only for testing------------------------------
	public static void main(String[] args) {
		String inputFilename = "../matsimTests/analysis/Vergleichswert.txt";
		String outputBase = "../matsimTests/??????";
		MZComparisonDataIO mzcdi = new MZComparisonDataIO();
		mzcdi.readMZData(inputFilename);
		mzcdi.write(outputBase);
	}
}
