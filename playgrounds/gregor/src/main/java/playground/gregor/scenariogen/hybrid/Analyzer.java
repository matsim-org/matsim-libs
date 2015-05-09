package playground.gregor.scenariogen.hybrid;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import org.apache.log4j.Logger;


public class Analyzer {
	
	private static final Logger log = Logger.getLogger(Analyzer.class);
	
	public static void main(String [] args) {
		
		
		if (Constants.EXP_90_DEG) {
			new TrajectoryTranslate("/Users/laemmel/svn/jpscore/inputfiles/hybrid/hybrid_trajectories.txt",Constants.RES_DIR + "/90deg_tr.txt",-2.213855890495818,-6.540520005007356).run();
			new TrajectoryCleaner(Constants.RES_DIR + "/90deg_tr.txt", Constants.RES_DIR + "/90deg_tr_cleaned.txt", Constants.INPTU_DIR+"/simpleGeo.shp").run();
		}
		
		
//		//if the referenced external binaries are available the following should work
				String pathToJPSReport = "/Users/laemmel/svn/jpsreport/bin/jpsreport";
				String pathToGnuplot = "/usr/local/bin/gnuplot";
		//
				String gnuplotDataFile = "datafile='"+Constants.RES_DIR+ "/Output/Fundamental_Diagram/Classical_Voronoi/rho_v_Voronoi_agentTrajectoriesFlippedTranslatedCleaned.txt_id_1.dat'";
		//		
				File newPwd = new File(Constants.RES_DIR).getAbsoluteFile();
				if (System.setProperty("user.dir", newPwd.getAbsolutePath()) == null) {
					throw new RuntimeException("could not change working directory");
				}
		//
				try {
					Files.copy(Paths.get(Constants.INPTU_DIR+"/90deg.xml"), 
							Paths.get(Constants.RES_DIR+"/90deg.xml"),
							StandardCopyOption.REPLACE_EXISTING);
					Files.copy(Paths.get(Constants.INPTU_DIR+"/hybrid_hall_geo.xml"), 
							Paths.get(Constants.RES_DIR+"/jpsGeo.xml"),
							StandardCopyOption.REPLACE_EXISTING);
//					Files.copy(Paths.get(Constants.RESOURCE_PATH+"/plotFlowAndSpeed.p"), 
//							Paths.get(Constants.OUTPUT_PATH+"/plotFlowAndSpeed.p"),
//							StandardCopyOption.REPLACE_EXISTING);
				} catch (IOException e1) {
					throw new RuntimeException(e1);
				}
		//
				try {
					Process p1 = new ProcessBuilder(pathToJPSReport, Constants.RES_DIR+"/90deg.xml").start();
					logToLog(p1);
					p1.waitFor();
		//			Process p2 = new ProcessBuilder(pathToGnuplot,"-e",gnuplotDataFile, Constants.OUTPUT_PATH+"/plotFlowAndSpeed.p").start();
		//			logToLog(p2);
				} catch (IOException | InterruptedException e) {
					throw new RuntimeException(e);
				}		
	}
	private static void logToLog(Process p1) throws IOException {
		{
			InputStream is = p1.getInputStream();
			InputStreamReader isr = new InputStreamReader(is);
			BufferedReader br = new BufferedReader(isr);
			String l = br.readLine();
			while (l != null) {
				log.info(l);
				l = br.readLine();
			}
		}
		{
			InputStream is = p1.getErrorStream();
			InputStreamReader isr = new InputStreamReader(is);
			BufferedReader br = new BufferedReader(isr);
			String l = br.readLine();
			while (l != null) {
				log.error(l);
				l = br.readLine();
			}
		}
	}
}
