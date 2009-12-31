package playground.florian;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;

import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.utils.misc.CRCChecksum;
import org.matsim.testcases.MatsimTestCase;
import org.xml.sax.SAXException;

import playground.florian.ScoreStatsHandler.ScoreStatsOutput;
import playground.florian.ScoreStatsHandler.ScoreXMLReader;


public class XmlPngOutputTest extends MatsimTestCase {
	
	private static final String CONFIG_FILE = "config.xml";
	private static final String EXPECTED_SCORE = "scores.xml";
	private static final String SCHEMALOCATION = "http://www.matsim.org/files/dtd/scores_v0.1.xsd";
	
	public void testXmlPngOutput() throws IOException, JAXBException, SAXException, ParserConfigurationException{
		
		//get Filenames
		String configFile = getPackageInputDirectory() + CONFIG_FILE;
		String expectedScore = getPackageInputDirectory() + EXPECTED_SCORE;
		String outputFile = getOutputDirectory() + "scores.xml";
		
		//put up the controler and add the ScoreStatsOutputListener
		Config config = super.loadConfig(configFile);
		Controler con = new Controler(config);
		con.setCreateGraphs(false);
		ScoreStatsOutput scoreOut = new ScoreStatsOutput(outputFile, true);
		con.addControlerListener(scoreOut);
		con.run();
		
		//check the output
		BufferedReader scoreXML = new BufferedReader(new FileReader(new File(outputFile)));
		BufferedReader scorePNG = new BufferedReader(new FileReader(new File(getOutputDirectory() + "scores.xml.png")));
		assertNotNull(scoreXML);
		assertNotNull(scorePNG);
		scorePNG.close();
		scoreXML.close();
		//The Files exist, now check if the created PNG equals the InputPNG
		assertEquals(CRCChecksum.getCRCFromFile(getOutputDirectory() + "scores.xml.png"), CRCChecksum.getCRCFromFile(getPackageInputDirectory() + "scores.xml.png"));
		//now check, whether the Scores are the expected SCores
		ScoreXMLReader actualScore = new ScoreXMLReader(SCHEMALOCATION);
		ScoreXMLReader exScore = new ScoreXMLReader(SCHEMALOCATION);
		actualScore.readFile(outputFile);
		exScore.readFile(expectedScore);
		//Check Average Scores
		ArrayList<Double> aScores = actualScore.getAverageAverageScores();
		ArrayList<Double> eScores = exScore.getAverageAverageScores();
		assertEquals(eScores.size(),aScores.size());
		for(int i=0;i<eScores.size();i++){
			assertEquals(eScores.get(i), aScores.get(i));
		}
		//Check best Scores
		aScores = actualScore.getAverageBestScores();
		eScores = exScore.getAverageBestScores();
		assertEquals(eScores.size(),aScores.size());
		for(int i=0;i<eScores.size();i++){
			assertEquals(eScores.get(i), aScores.get(i));
		}
		//Check Worst Scores
		aScores = actualScore.getAverageWorstScores();
		eScores = exScore.getAverageWorstScores();
		assertEquals(eScores.size(),aScores.size());
		for(int i=0;i<eScores.size();i++){
			assertEquals(eScores.get(i), aScores.get(i));
		}
		//Check Worst Scores
		aScores = actualScore.getAverageWorstScores();
		eScores = exScore.getAverageWorstScores();
		assertEquals(eScores.size(),aScores.size());
		for(int i=0;i<eScores.size();i++){
			assertEquals(eScores.get(i), aScores.get(i));
		}
		//Check Executed Scores
		aScores = actualScore.getAverageExecutedScores();
		eScores = exScore.getAverageExecutedScores();
		assertEquals(eScores.size(),aScores.size());
		for(int i=0;i<eScores.size();i++){
			assertEquals(eScores.get(i), aScores.get(i));
		}		
	}
}
