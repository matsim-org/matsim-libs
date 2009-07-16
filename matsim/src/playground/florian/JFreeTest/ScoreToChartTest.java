package playground.florian.JFreeTest;


import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
//import org.dom4j.Document;
//import org.dom4j.DocumentException;
//import org.dom4j.io.XMLWriter;
import org.jfree.ui.RefineryUtilities;



public class ScoreToChartTest {
	public static final String file = "/playground/florian/JFreeTest/scores2.xml"; 
	public static final String stylesheet = "./src/playground/florian/JFreeTest/scores-JFree.xsl"; 


	public static void main(String[] args) {

//		JFreeTest demo = new JFreeTest("JFreeTest");	
//		Dom4jTest dom = new Dom4jTest();
//		try {		
//			Document originalDoc = dom.parse(file);
//			//style the original document using the stylesheet
//			Document resultDoc = dom.styleDocument(originalDoc, stylesheet);
//			//create InputStream from the transformed XML
//			InputStream in = Dom4jTest.serializeToXMLInput(resultDoc);		
//			//create chart
//			demo.createChartFromStream(in);
//			demo.pack();
//			RefineryUtilities.centerFrameOnScreen(demo);
//			demo.setVisible(true);
//		} catch (DocumentException e) {
//			e.printStackTrace();
//		}
	}

}
