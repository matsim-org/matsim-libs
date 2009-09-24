package playground.florian.JFreeTest;


import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentException;
import org.dom4j.io.XMLWriter;
import org.jfree.chart.JFreeChart;
import org.jfree.ui.RefineryUtilities;
import org.matsim.core.utils.charts.ChartUtil;



public class ScoreToChartTest{
	 

	public static final String stylesheet = "./src/playground/florian/JFreeTest/scores-JFree.xsl"; 


	public static JFreeChart createChartFromXMLScore(String filename) {
		JFreeChart chart = null;
		JFreeTest converter = new JFreeTest("Score Statistic");	
		Dom4jTest dom = new Dom4jTest();
		
		try {
			Document originalDoc = dom.parse(filename);
			//style the original document using the stylesheet
			Document resultDoc = dom.styleDocument(originalDoc, stylesheet);
			//create InputStream from the transformed XML
			InputStream in = Dom4jTest.serializeToXMLInput(resultDoc);		
			//create chart
			chart = converter.createChartFromStream(in);
			
		} catch (DocumentException e) {
			e.printStackTrace();
		}
		
		return chart;
	}
}
