package playground.florian.ScoreStatsHandler;

import java.io.FileNotFoundException;
import java.io.IOException;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.MatsimJaxbXmlWriter;
import playground.florian.jaxb.scores01.ObjectFactory;
import playground.florian.jaxb.scores01.XMLScoreType;
import playground.florian.jaxb.scores01.XMLScores;

public class ScoreXMLWriter extends MatsimJaxbXmlWriter {
	private ObjectFactory fac = null;
	private XMLScores xmlScores = null;
	private static final  String SCHEMALOCATION = "http://www.matsim.org/files/dtd/scores_v0.1.xsd";
	
	public ScoreXMLWriter(){
		fac = new ObjectFactory();
		xmlScores = fac.createXMLScores();
	}
	

	@Override
	public void write(String filename) {
		JAXBContext jc;
		try {
			jc = JAXBContext.newInstance(playground.florian.jaxb.scores01.ObjectFactory.class);
			Marshaller m = jc.createMarshaller();
			super.setMarshallerProperties(SCHEMALOCATION, m);
			m.marshal(this.xmlScores,IOUtils.getBufferedWriter(filename));
		} catch (JAXBException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	public void addScore(int iteration, double averageAverageScore, double averageBestScore, double averageWorstScore, double averageExecutedScore){
		XMLScoreType xmlScoreType = fac.createXMLScoreType();
		xmlScoreType.setIteration(iteration);
		xmlScoreType.setAverageAverage(averageAverageScore);
		xmlScoreType.setAverageBest(averageBestScore);
		xmlScoreType.setAverageWorst(averageWorstScore);
		xmlScoreType.setAverageExecuted(averageExecutedScore);
		this.xmlScores.getScore().add(xmlScoreType);	
	}
	
}
