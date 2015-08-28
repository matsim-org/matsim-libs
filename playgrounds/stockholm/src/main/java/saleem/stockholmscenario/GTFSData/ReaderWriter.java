package saleem.stockholmscenario.GTFSData;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

public class ReaderWriter {
	public Document readTrasnitSchedule(String path){
		SAXBuilder saxBuilder = new SAXBuilder();
		File xmlFile = new File(path);
		try{
			Document document = (Document) saxBuilder.build(xmlFile);
			Element rootElement = document.getRootElement();
			return document;
		}catch (IOException io) {
	        System.out.println(io.getMessage());
	    }catch (JDOMException jdomex) {
	        System.out.println(jdomex.getMessage());
	    }
		return null;
	}
	public void writeTextFile(String path, String towrite){
		try{
			FileUtils.writeStringToFile(new File(path), towrite);
		}catch(IOException e){
			e.printStackTrace();
		}
		

	}
}