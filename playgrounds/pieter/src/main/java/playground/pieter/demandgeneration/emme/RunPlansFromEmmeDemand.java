package playground.pieter.demandgeneration.emme;

import java.io.File;

import org.apache.log4j.Logger;
import org.geotools.referencing.CRS;

import playground.pieter.demandgeneration.utilities.EmmeStringBuilder;

public class RunPlansFromEmmeDemand {
	private static Logger log = Logger.getLogger(RunPlansFromEmmeDemand.class);
	private static int studyArea;
	private static String studyAreaName;
	private static String root;
	private static String date;
	private static String proj;

	/**
	 * Implementation of the <code>playground.pieter.demandgeneration.emme.PlansFromEmmeDemand</code>. 
	 * Allowed values for the <code>studyArea</code> are:
	 * <ul>
	 * 		<li> 1 - Gauteng<br>
	 * 		<li> 2 - eThekwini<br>
	 * </ul>
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		if(args.length == 3){
			studyArea = Integer.parseInt(args[0]);
			root = args[1];
			date = args[2];
			
			String matrix;
			String coords;
			String departure;
			String outputPath;
			
			EmmeStringBuilder esb = new EmmeStringBuilder();
			
			switch (studyArea) {
			case 1:
				studyAreaName = "Gauteng";
				proj = esb.getUTM35S();
				break;
				
			case 2:
				studyAreaName = "eThekwini";
				proj = esb.getUTM36S();
				break;

			default:
				break;
			}
			log.info("======================================================");
			log.info(" Creating demand from Emme data for " + studyAreaName);
			log.info("------------------------------------------------------");

			matrix = root + studyAreaName + "/" + date + "/privdemand.csv";
			coords = root + studyAreaName + "/" + date + "/nodes.csv";
			departure = "07:00:00";
			outputPath = "./Output/";
			File locationFolder = new File(outputPath);
			boolean folderCreated = locationFolder.mkdirs();
			log.info("Output folder created (" + folderCreated + ") at " + locationFolder.getAbsolutePath());

			PlansFromEmmeDemand pfed = new PlansFromEmmeDemand(matrix, coords, departure, outputPath, CRS.parseWKT(proj));
			pfed.processInput();
			pfed.createPlansXML();
		} else{
			throw new RuntimeException("Must have TWO arguments: a) Study area (number); b) Root; c) Date.");
		}
		log.info("======================================================");
		log.info("                   PROCESS COMPLETED");
		log.info("======================================================");
	}

}
