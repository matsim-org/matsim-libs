package playground.florian.JFreeTest;


import java.io.InputStream;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.jfree.chart.JFreeChart;



public class ScoreToChartTest{
	 

	public static final String stylesheet = "./test/input/playground/florian/JFreeTest/scores-JFree.xsl"; 


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
