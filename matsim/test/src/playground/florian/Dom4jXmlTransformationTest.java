package playground.florian;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.matsim.core.utils.misc.CRCChecksum;
import org.matsim.testcases.MatsimTestCase;

import playground.florian.JFreeTest.Dom4jTest;

public class Dom4jXmlTransformationTest extends MatsimTestCase{

	private static final String TESTXML  = "scorestats.xml";
	private static final String TEST_STYLE_SHEET = "scores-JFree.xsl";
	private static final String CHECKEDXML = "scorestats_checked.xml";
	
	public void testXMLTransformation() throws DocumentException, IOException{
		Dom4jTest dom = new Dom4jTest();
		
		//Get the source XMLFile
		String file = getPackageInputDirectory() + TESTXML;
		String stylesheet = getPackageInputDirectory() + TEST_STYLE_SHEET;
		String compfile = getPackageInputDirectory() + CHECKEDXML;
		String outputFile = getOutputDirectory() + TESTXML; 
		//Transform the XMLFile
		Document originalDoc = dom.parse(file);
		Document resultDoc = dom.styleDocument(originalDoc, stylesheet);
		//Is there an output?
		assertNotNull(resultDoc);
		assertNotNull(resultDoc.asXML());
		//compare the transformed output to the checked XMLFile
		String output = resultDoc.asXML();
		
		BufferedWriter fileWriter = new BufferedWriter(new FileWriter(new File(outputFile)));
		fileWriter.write(output);
		fileWriter.close();

		assertEquals(CRCChecksum.getCRCFromFile(compfile), CRCChecksum.getCRCFromFile(outputFile));
	}
}
