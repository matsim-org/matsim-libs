package playground.santiago.utils;

import java.io.BufferedReader;

import java.io.IOException;
import java.util.ArrayList;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.PointFeatureFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.core.utils.io.IOUtils;
import org.opengis.feature.simple.SimpleFeature;
import playground.santiago.SantiagoScenarioConstants;

public class SantiagoCounts2ESRIShape {

	private String workingDir = "../../../baseCaseAnalysis/10pct/3_otherThings/locationAndMAPE/";
	private String countsDB = workingDir + "countsLocationAndMAPE10pct.csv";
	private String outputDir = workingDir + "countsShape10pct.shp";
	private String crs = SantiagoScenarioConstants.toCRS;
 	private final Logger log = Logger.getLogger(SantiagoCounts2ESRIShape.class);
 	
 	private PointFeatureFactory countsFactory;
	
	
	
	public static void main(String[] args) {
		SantiagoCounts2ESRIShape sc = new SantiagoCounts2ESRIShape();		
		sc.Run();
	}
		
	private void Run(){

		ArrayList<SimpleFeature> fts = new ArrayList<SimpleFeature>();		
		this.countsFactory = new PointFeatureFactory.Builder().
				setCrs(MGC.getCRS(crs)).
				setName("Counts").
				addAttribute("id", String.class).
				addAttribute("type", String.class).
				addAttribute("eje", String.class).
				addAttribute("desde", String.class).
				addAttribute("hasta", String.class).
				addAttribute("comuna", String.class).
				addAttribute("sentido", String.class).
				addAttribute("selected", String.class).
				addAttribute("link", String.class).
				addAttribute("0x_0", Double.class).		//MAPEs - The steps depend on the scenario
				addAttribute("0x_100", Double.class).
				addAttribute("1_100", Double.class).
				addAttribute("1_400", Double.class).
				addAttribute("1x_300", Double.class).
				addAttribute("1x_600", Double.class).
				addAttribute("1xA_600", Double.class).
				addAttribute("1xA_800", Double.class).
				addAttribute("1xB_600", Double.class).
				addAttribute("1xB_800", Double.class).
				addAttribute("D0x_0", Double.class).		//DELTAs - The steps depend on the scenario
				addAttribute("D0x_100", Double.class).
				addAttribute("D1_100", Double.class).
				addAttribute("D1_400", Double.class).
				addAttribute("D1x_300", Double.class).
				addAttribute("D1x_600", Double.class).
				addAttribute("D1xA_600", Double.class).
				addAttribute("D1xA_800", Double.class).
				addAttribute("D1xB_600", Double.class).
				addAttribute("D1xB_800", Double.class).
				create();
		
		try{

			BufferedReader br = IOUtils.getBufferedReader(countsDB);
			String line=br.readLine(); //skipping the headers.					
			while ((line = br.readLine()) != null){
				String[] entries = line.split(";");
				fts.add(getCountsFeatures(entries));
			}

		}catch(IOException e){
			log.error(new Exception(e));
		}
		
		ShapeFileWriter.writeGeometries(fts, outputDir);
	}
	
	
	
	private SimpleFeature getCountsFeatures(String[] entries){
		String id = entries[0];
		String type = entries [1];
		String eje = entries[2];
		String desde = entries[3];
		String hasta = entries[4];
		String comuna = entries[5];
		String sentido = entries[6];		
		String selected = entries [9];
		String link = entries [10];
		//MAPEs - The steps depend on the scenario
		double step0x_0 = Double.parseDouble(entries [11]);
		double step0x_100 = Double.parseDouble(entries [12]);
		double step1_100 = Double.parseDouble(entries [13]);
		double step1_400 = Double.parseDouble(entries [14]);
		double step1x_300 = Double.parseDouble(entries [15]);
		double step1x_600 = Double.parseDouble(entries [16]);
		double step1xA_600 = Double.parseDouble(entries [17]);
		double step1xA_800 = Double.parseDouble(entries [18]);
		double step1xB_600 = Double.parseDouble(entries [19]);
		double step1xB_800 = Double.parseDouble(entries [20]);
		//DELTAs - The steps depend on the scenario
		double Dstep0x_0 = Double.parseDouble(entries [21]);
		double Dstep0x_100 = Double.parseDouble(entries [22]);
		double Dstep1_100 = Double.parseDouble(entries [23]);
		double Dstep1_400 = Double.parseDouble(entries [24]);
		double Dstep1x_300 = Double.parseDouble(entries [25]);
		double Dstep1x_600 = Double.parseDouble(entries [26]);
		double Dstep1xA_600 = Double.parseDouble(entries [27]);
		double Dstep1xA_800 = Double.parseDouble(entries [28]);
		double Dstep1xB_600 = Double.parseDouble(entries [29]);
		double Dstep1xB_800 = Double.parseDouble(entries [30]);	
		
		Coord c = new Coord(Double.parseDouble(entries[7]),Double.parseDouble(entries[8]));	
		return this.countsFactory.createPoint(c, new Object [] {id,type,eje,desde,hasta,comuna,sentido,selected,link,
				step0x_0,step0x_100,step1_100,step1_400,step1x_300,step1x_600,step1xA_600,step1xA_800,step1xB_600,step1xB_800,
				Dstep0x_0,Dstep0x_100,Dstep1_100,Dstep1_400,Dstep1x_300,Dstep1x_600,Dstep1xA_600,Dstep1xA_800,Dstep1xB_600,Dstep1xB_800},null);
	}
	
}
