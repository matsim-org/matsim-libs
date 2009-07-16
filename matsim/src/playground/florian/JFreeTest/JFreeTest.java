package playground.florian.JFreeTest;

import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.xml.DatasetReader;
import org.jfree.ui.ApplicationFrame;

public class JFreeTest extends ApplicationFrame{
	
	public JFreeTest(String title){
		super(title);
	}
	
	public void createChartFromFile(String file){
		//Read the File
		URL url = getClass().getResource(file);
		try {
			InputStream in = url.openStream();
			createChart(in);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
	
	public void createChartFromStream(InputStream in){
		createChart(in);
	}
	
//	public void createChartFromStream(OutputStream out){
//		
//	}
	
	private void createChart(InputStream in){
		CategoryDataset dataset = null;
		try{
			dataset = DatasetReader.readCategoryDatasetFromXML(in);
		}
			catch (IOException ioe) {
				System.out.println(ioe.getMessage());
		}
			
		//Create the Graph
		JFreeChart chart = ChartFactory.createLineChart("TEST", "Iteration", "Score", dataset, PlotOrientation.VERTICAL, true, true, false);
		chart.setBackgroundPaint(Color.white);
		
		//Create the Chart Panel
		ChartPanel chartPanel = new ChartPanel(chart);
		chartPanel.setPreferredSize(new java.awt.Dimension(500,270));
		setContentPane(chartPanel);
		
		//Create the Output to File		
	}
}
