/**
 *
 */
package playground.yu.analysis.MZComparison;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jfree.chart.plot.PlotOrientation;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.roadpricing.RoadPricingReaderXMLv1;

import playground.yu.utils.charts.StackedBarChart;

/**
 * @author yu
 *
 */
public class MZComparisonDataIO implements TabularFileHandler {
	private int lineCount = 0;

	private String[] chartRows = null;

	private final Set<String> chartColumns = new HashSet<String>();

	/**
	 * @param arg0
	 *            - String chart row
	 * @param arg1
	 *            - String chart column
	 */
	private final Map<String, Map<String, Double>> values = new HashMap<String, Map<String, Double>>();

	public void setData2Compare(final MZComparisonData data2compare) {
		String ADDOTA = "average daily distance [km] (MATSim)";
		setValueFromData2Compare(ADDOTA, "car", data2compare
				.getAvgTollDailyDistance_car_m() / 1000.0);
		setValueFromData2Compare(ADDOTA, "pt", data2compare
				.getAvgTollDailyDistance_pt_m() / 1000.0);
		setValueFromData2Compare(ADDOTA, "walk", data2compare
				.getAvgTollDailyDistance_walk_m() / 1000.0);
		setValueFromData2Compare(ADDOTA, "others", data2compare
				.getAvgTollDailyDistance_other_m() / 1000.0);

		String ADERTOTA = "average daily en route time [min] (MATSim)";
		setValueFromData2Compare(ADERTOTA, "car", data2compare
				.getAvgTollDailyEnRouteTime_car_min());
		setValueFromData2Compare(ADERTOTA, "pt", data2compare
				.getAvgTollDailyEnRouteTime_pt_min());
		setValueFromData2Compare(ADERTOTA, "walk", data2compare
				.getAvgTollDailyEnRouteTime_walk_min());
		setValueFromData2Compare(ADERTOTA, "others", data2compare
				.getAvgTollDailyEnRouteTime_other_min());
	}

	private void setValueFromData2Compare(final String chartColumn,
			final String chartRow, final double value) {
		if (!chartColumns.contains(chartColumn))
			chartColumns.add(chartColumn);
		Map<String, Double> m = values.get(chartRow);
		m.put(chartColumn, value);
		values.put(chartRow, m);
	}

	@Override
	public void startRow(final String[] row) {
		// TODO save information from MZ
		if (lineCount > 0) {
			chartColumns.add(row[0]);
			System.out.println("row[0] :\t" + row[0] + "\twas added.");
			for (int i = 1; i < row.length; i++) {
				Map<String, Double> m = values.get(chartRows[i - 1]);
				if (m == null)
					m = new HashMap<String, Double>();
				m.put(row[0], Double.valueOf(row[i]));
				values.put(chartRows[i - 1], m);
				// chartRows[i]->String0
				// row[0]->String1;
			}
		} else {
			chartRows = new String[row.length - 1];
			for (int i = 1; i < row.length; i++)
				chartRows[i - 1] = row[i];
		}
		lineCount++;
	}

	public void write(final String outputBase) {
		try {
			BufferedWriter bw = IOUtils.getBufferedWriter(outputBase + ".txt");
			bw.write("\t");
			for (String chartRow : chartRows)
				bw.write("\t" + chartRow);
			bw.write("\n");
			for (String chartColumn : chartColumns) {
				bw.write(chartColumn);
				for (String chartRow : chartRows)
					bw.write("\t" + values.get(chartRow).get(chartColumn));
				bw.write("\n");
			}
			bw.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		// write chart
		int chartColumnsSize = chartColumns.size();
		double[][] values = new double[chartRows.length][chartColumnsSize];
		List<String> tmpChartColumns = new ArrayList<String>();
		tmpChartColumns.addAll(chartColumns);
		Collections.sort(tmpChartColumns);
		String[] chartColumns = tmpChartColumns
				.toArray(new String[this.chartColumns.size()]);

		for (int i = 0; i < chartRows.length; i++)
			for (int j = 0; j < chartColumnsSize; j++)
				values[i][j] = this.values.get(chartRows[i]).get(
						chartColumns[j]);
		StackedBarChart chart = new StackedBarChart(
				"Mikrozensus 2005 vs MATSim (Kanton Zurich)", "", "values",
				PlotOrientation.HORIZONTAL);
		chart.addSeries(chartRows, chartColumns, values);
		chart.saveAsPng(outputBase + ".png", 900, 600);
	}

	public void readMZData(final String inputFilename) {
		TabularFileParserConfig tfpc = new TabularFileParserConfig();
		// tfpc.setCommentTags(new String[] { "Verkehrsmittel" });
		tfpc.setDelimiterRegex("\t");
		tfpc.setFileName(inputFilename);
		try {
			new TabularFileParser().parse(tfpc, this);
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println(values);
	}

	// -----------only for testing------------------------------
	public static void main(final String[] args) {
		String inputFilename = "../matsimTests/analysis/Vergleichswert.txt";
		String outputBase = "../matsimTests/compare";
		String networkFilename = "../schweiz-ivtch-SVN/baseCase/network/ivtch-osm.xml";
		String plansFilename = "../runs-svn/run684/it.1000/1000.plans.xml.gz";
		String tollFilename = "../matsimTests/toll/KantonZurichToll.xml";

		MZComparisonDataIO mzcdi = new MZComparisonDataIO();
		mzcdi.readMZData(inputFilename);

		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario).readFile(networkFilename);

		scenario.getConfig().scenario().setUseRoadpricing(true);
		RoadPricingReaderXMLv1 tollReader = new RoadPricingReaderXMLv1(scenario.getRoadPricingScheme());
		tollReader.parse(tollFilename);

		Population population = scenario.getPopulation();
		new MatsimPopulationReader(scenario).readFile(plansFilename);

		MZComparisonData mzcd = new MZComparisonData(scenario.getRoadPricingScheme());
		mzcd.run(population);

		mzcdi.setData2Compare(mzcd);
		mzcdi.write(outputBase);
		System.out.println(">>>done.");
	}
}
