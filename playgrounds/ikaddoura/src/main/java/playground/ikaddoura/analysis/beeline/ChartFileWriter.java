package playground.ikaddoura.analysis.beeline;

import java.util.SortedMap;

import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.LogarithmicAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.category.BarRenderer;
import org.matsim.core.utils.charts.BarChart;
import org.matsim.core.utils.charts.LineChart;
import org.matsim.core.utils.charts.XYLineChart;

public class ChartFileWriter {

	public void writeXYLineChartFile(String title, SortedMap<String, Modus> modiMap, String outputFile) {
		// TODO Auto-generated method stub
		String xAxesLabel = "Reiseweite [m]";
		String yAxesLabel = "Anzahl an Legs";
		XYLineChart chart = new XYLineChart(title, xAxesLabel, yAxesLabel, false);
	    
		for (String modusName : modiMap.keySet()){
			Modus modus = modiMap.get(modusName);
			double[] xWerte = new double[modus.getLegsPerLuftlinienGroups().size()];
			double[] yWerte = new double[modus.getLegsPerLuftlinienGroups().size()];
			int counter = 0;
			for (Double distanz : modus.getLegsPerLuftlinienGroups().keySet()){
				xWerte[counter] = distanz;
				yWerte[counter] = modus.getLegsPerLuftlinienGroups().get(distanz);
				counter++;
			}
			chart.addSeries(modus.getModeName(), xWerte, yWerte);
		}
		XYPlot p = (XYPlot) chart.getChart().getPlot();
		LogarithmicAxis axis_x = new LogarithmicAxis(xAxesLabel);
		axis_x.setAllowNegativesFlag(false);
		p.setDomainAxis(axis_x);
		
		chart.saveAsPng(outputFile, 1000, 800); //File Export
		System.out.println(outputFile+" geschrieben.");
	}

//-------------------------------------------------------------------------------------------------------------
	
	public void writeBarChartFile(String title, SortedMap<String, Modus> modiMap, String outputFile) {
		String xAxesLabel = "Reiseweite [m]";
		String yAxesLabel = "Anzahl an Legs";
		
		Modus carModus = modiMap.get("car");
		String[] distanceGroups = new String[carModus.getLegsPerLuftlinienGroups().size()];
		int counter1 = 0;
		for (Double distanz : carModus.getLegsPerLuftlinienGroups().keySet()){
			distanceGroups[counter1] = distanz.toString();
			counter1++;
		}
		
		BarChart chart = new BarChart(title, xAxesLabel, yAxesLabel, distanceGroups);

		for (String modusName : modiMap.keySet()){
			Modus modus = modiMap.get(modusName);
			double[] yWerte = new double[modus.getLegsPerLuftlinienGroups().size()];
			int counter2 = 0;
			for (Double distanz : modus.getLegsPerLuftlinienGroups().keySet()){
				distanceGroups[counter2] = distanz.toString();
				yWerte[counter2] = modus.getLegsPerLuftlinienGroups().get(distanz);
				counter2++;
			}
			chart.addSeries(modus.getModeName(), yWerte);
		}
		Plot plot = chart.getChart().getCategoryPlot();
		CategoryAxis axis1 = ((CategoryPlot) plot).getDomainAxis();
		axis1.setCategoryMargin(0.25);
		
		BarRenderer renderer = (BarRenderer)((CategoryPlot) plot).getRenderer();
		renderer.setItemMargin(0.10);
		
		chart.saveAsPng(outputFile, 2000, 800); //File Export
		System.out.println(outputFile+" geschrieben.");
	}
//-------------------------------------------------------------------------------------------------------------
	
	public void writeLineChartFile(String title, SortedMap<String, Modus> modiMap, String outputFile) {
		String xAxesLabel = "Reiseweite [m]";
		String yAxesLabel = "Anzahl an Legs";
		
		Modus carModus = modiMap.get("car");
		String[] distanceGroups = new String[carModus.getLegsPerLuftlinienGroups().size()];
		int counter1 = 0;
		for (Double distanz : carModus.getLegsPerLuftlinienGroups().keySet()){
			distanceGroups[counter1] = distanz.toString();
			counter1++;
		}
		
		LineChart chart = new LineChart(title, xAxesLabel, yAxesLabel, distanceGroups);

		for (String modusName : modiMap.keySet()){
			Modus modus = modiMap.get(modusName);
			double[] yWerte = new double[modus.getLegsPerLuftlinienGroups().size()];
			int counter2 = 0;
			for (Double distanz : modus.getLegsPerLuftlinienGroups().keySet()){
				distanceGroups[counter2] = distanz.toString();
				yWerte[counter2] = modus.getLegsPerLuftlinienGroups().get(distanz);
				counter2++;
			}
			chart.addSeries(modus.getModeName(), yWerte);
		}
		chart.saveAsPng(outputFile, 2000, 800); //File Export
		System.out.println(outputFile+" geschrieben.");
	}

}
