package playground.anhorni.locationchoice.preprocess.analyzeMZ;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.matsim.core.utils.io.IOUtils;

public class ActWriter {
	
	
	/*
	B015211A = 408;	Verbrauchermaerkte (> 2500 m2) 
	B015211B = 409; Grosse Supermaerkte (1000-2499 m2)
	B015211C = 410;	Kleine Supermaerkte (400-999 m2)
	B015211D = 411;	Grosse Geschaefte (100-399 m2)
	B015211E = 412;	Kleine Geschaefte (< 100 m2)
	B015212A = 413; Warenhaeuser
	*
	B015221A = 415; Detailhandel mit Obst und Gemuese
	B015222A = 416; Detailhandel mit Fleisch und Fleischwaren	
	B015223A = 417; Detailhandel mit Fisch und Meeresfruechten	
	B015224A = 418; Detailhandel mit Brot, Back- und Suesswaren	
	B015225A = 419;	Detailhandel mit Getraenken
	*
	B015227A = 421; Detailhandel mit Milcherzeugnissen und Eiern	
	B015227B = 422; Sonstiger Fachdetailhandel mit Nahrungsmitteln, Getraenken und Tabak a.n.g. (in Verkaufsraeumen)
	*/
	
	
	public void write(List<MZTripHectare> relations, String outpath) {
		
		double[] activitiesCount = {0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0};
		double[] activitiesCountSingle = {0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0};
		
		double totalActCount = 0.0;
		double totalActCountSingle = 0.0;
		
		String[] NOGA = {
				"B015211A = 408;	Verbrauchermaerkte (> 2500 m2)",
				"B015211B = 409; 	Grosse Supermaerkte (1000-2499 m2)",
				"B015211C = 410;	Kleine Supermaerkte (400-999 m2)",
				"B015211D = 411;	Grosse Geschaefte (100-399 m2)",
				"B015211E = 412;	Kleine Geschaefte (< 100 m2)",
				"B015212A = 413; 	Warenhaeuser",
				"B015221A = 415; 	Detailhandel mit Obst und Gemuese",
				"B015222A = 416; 	Detailhandel mit Fleisch und Fleischwaren",
				"B015223A = 417; 	Detailhandel mit Fisch und Meeresfruechten",	
				"B015224A = 418; 	Detailhandel mit Brot, Back- und Suesswaren",	
				"B015225A = 419;	Detailhandel mit Getraenken",
				"B015227A = 421; 	Detailhandel mit Milcherzeugnissen und Eiern",
				"B015227B = 422; 	Sonstiger Fachdetailhandel mit Nahrungsmitteln, Getraenken und Tabak a.n.g. " +
				"	(in Verkaufsraeumen)"
		};
		
		
		try {
			BufferedWriter out = IOUtils.getBufferedWriter(outpath +"acts.txt");
			
			Iterator<MZTripHectare> relations_it = relations.iterator();
			while (relations_it.hasNext()) {
				MZTripHectare relation = relations_it.next();
				
				//out.write(relation.getMzTrip().getId().toString() + ": ");
				for (int i = 0; i < relation.getHectare().getShops().size(); i++) {
					//out.write(relation.getHectare().getShops().get(i) + "\t");
					
					int index = transform(relation.getHectare().getShops().get(i));
					if (relation.getHectare().getShops().size() == 1) {
						activitiesCountSingle[index]++;
						totalActCountSingle++;
					}
					activitiesCount[index] += (1.0 / relation.getHectare().getShops().size());	
					totalActCount += (1.0 / relation.getHectare().getShops().size());	
				}
				//out.newLine();
			}
			out.flush();
			
			for (int i = 0; i < 13; i++) {
				out.write(+ activitiesCount[i] + "\t" +  
						100.0 * activitiesCount[i]/totalActCount + " per cent" + "\t" +  NOGA[i] + "\n");
			}
			out.newLine();
			out.write("Only one store per hectare: \n");
			for (int i = 0; i < 13; i++) {
				out.write(+ activitiesCountSingle[i] + "\t " +  
						100.0 * activitiesCountSingle[i]/totalActCountSingle + " per cent" + "\t" + NOGA[i] + "\n");
			}
			out.flush();
			out.close();			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private int transform(int i) {
		if (i < 415) return i - 408;
		else if (i < 421 && i > 413) return i - 409;
		else return i -410;
	}
}
