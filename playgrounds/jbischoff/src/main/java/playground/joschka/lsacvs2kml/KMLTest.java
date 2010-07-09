package playground.joschka.lsacvs2kml;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.network.KmlNetworkWriter;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.vis.kml.KMZWriter;

public class KMLTest  {
	/**
	 * @param args
	 * @throws IOException 
	 */
   

	
	
	public static void main(String[] args) throws IOException {
		
		List<LSA> lsaliste = filetoSAList("/Users/JB/Documents/Work/lsa.txt");
		
		CoordinateTransformation ct = 
			TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84_UTM33N,TransformationFactory.WGS84);
		for (LSA ampel:lsaliste){
			Coord current = new CoordImpl(ampel.getXcord(),ampel.getYcord());
			current=ct.transform(current);
			ampel.setXcord(current.getX());
			ampel.setYcord(current.getY());
		}
		
	
		

	writeListtoXML(lsaliste,"/Users/JB/Documents/Work/lsa-berlin.kml");
		
		
	}
	
	public static List<LSA> filetoSAList(String fn){
		 String  filename;
		FileReader fr;
		BufferedReader br;
		filename=fn;
		List<LSA> lsalist = new ArrayList<LSA>();

		try {
			fr = new FileReader(new File (filename));
			br = new BufferedReader(fr);
			
			String line = null;
			while ((line = br.readLine()) != null) {
				
		         String[] result = line.split(";");
		         LSA current = new LSA(result[0],result[1],Double.parseDouble(result[2]),Double.parseDouble(result[3]));
		         lsalist.add(current);
		
			
	 }}
		
			
		 catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			System.err.println("File not Found...");
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		 return lsalist;
		      }

	public static void writeListtoXML(List<LSA> lsaliste, String outputfile) throws IOException{
		String out = outputfile;
		List<LSA> lsalist = lsaliste;
		Element rE = new Element("kml");
		rE.setAttribute("xmlnsns2", "http://www.w3.org/2005/Atom");
		Element Dok = new Element("Document");
		for (LSA ampeln:lsalist){
			Element currentPM = new Element("Placemark");
			Element shortname = new Element("name");
			shortname.setText(ampeln.getShortName());
			currentPM.addContent(shortname);
			
			Element longname = new Element("description");
			longname.setText(ampeln.getLongName());
			currentPM.addContent(longname);
			
			Element currentPoint = new Element("Point");
			Element cord = new Element("coordinates");
			cord.setText(ampeln.getXcord()+","+ampeln.getYcord()+",0.0");
			currentPoint.addContent(cord);
			
			
			currentPM.addContent(currentPoint);
			Dok.addContent(currentPM);
			
		}
		rE.addContent(Dok);
		
		
		
		Document doc = new Document(rE);
		XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
		FileOutputStream output = new FileOutputStream(out);
		outputter.output(doc, output);

		
		
		
			}
		
		
	
}
