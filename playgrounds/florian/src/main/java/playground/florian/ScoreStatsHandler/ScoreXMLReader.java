package playground.florian.ScoreStatsHandler;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.ParserConfigurationException;

import org.matsim.core.utils.io.MatsimJaxbXmlParser;
import org.xml.sax.SAXException;

import playground.florian.jaxb.scores01.XMLScoreType;
import playground.florian.jaxb.scores01.XMLScores;

public class ScoreXMLReader extends MatsimJaxbXmlParser {
	private ArrayList<Double> averageScore = new ArrayList<Double>();
	private ArrayList<Double> bestScore = new ArrayList<Double>();
	private ArrayList<Double> worstScore = new ArrayList<Double>();
	private ArrayList<Double> executedScore = new ArrayList<Double>();
	
	public ScoreXMLReader(String schemaLocation) {
		super(schemaLocation);
	}
	
	public ArrayList<Double> getAverageAverageScores(){
		return averageScore;
	}
	
	public ArrayList<Double> getAverageExecutedScores(){
		return executedScore;
	}
	
	public ArrayList<Double> getAverageBestScores(){
		return bestScore;
	}
	
	public ArrayList<Double> getAverageWorstScores(){
		return worstScore;
	}
	
	
	@Override
	public void readFile(String filename) throws JAXBException, SAXException,ParserConfigurationException, IOException {
		//create jaxb infrastructure
		JAXBContext jc;
		XMLScores xmlScores;
			jc = JAXBContext.newInstance(playground.florian.jaxb.scores01.ObjectFactory.class);
//			ObjectFactory fac = new ObjectFactory();
			Unmarshaller u = jc.createUnmarshaller();
			//validate XML file
			super.validateFile(filename, u);
			System.out.println("starting unmarschalling " + filename);
			xmlScores = (XMLScores) u.unmarshal(new FileInputStream(filename));
			//convert the parsed xml-instances to basic instances
			for (XMLScoreType score: xmlScores.getScore()){
				bestScore.add(score.getIteration(), score.getAverageBest());
				averageScore.add(score.getIteration(), score.getAverageAverage());
				worstScore.add(score.getIteration(), score.getAverageWorst());
				executedScore.add(score.getIteration(), score.getAverageExecuted());
			}
			System.out.println("finish unmarschalling " + filename);
	}
	
	public static void main(String[] args){
		ScoreXMLReader reader = new ScoreXMLReader("schema");
		try {
			reader.readFile("myFilename.xml");
		} catch (JAXBException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
}
