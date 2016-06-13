package playground.artemc.analysis;

import playground.artemc.events.EventsToTravelSummaryRelationalTablesGUI_V2;

import java.awt.*;
import java.io.File;
import java.lang.reflect.InvocationTargetException;

import java.awt.*;
import java.io.File;
import java.lang.reflect.InvocationTargetException;

/**
 * Created by artemc on 7/3/15.
 */
public class OutputToSQLReader {

	private static String simType = "5min";
	private static String dataPathSuffix =  "_homo";
	//private static String dataPath = "/Volumes/DATA 1 (WD 2 TB)/output_SelectExp1_5p_"+simType+"_1000it"+dataPathSuffix;
	//private static String transitSchedule = "/Users/artemc/Dropbox/Work/Input/input22092015_8000_"+simType+"/transitSchedule.xml";
	//private static String defaultPropertiesFile = "/Users/artemc/Workspace/matsim-git/playgrounds/artemc/connections/corridorDatabase.properties";
	//private static String schema = "corridor_" + simType;

	//private static String schema = "corridor_" + simType + dataPathSuffix;
	private static String eventsPath;
	private static String tableSuffix;

	private static String dataPath = "/Volumes/DATA 1 (WD 2 TB)/sfOutput_se1_5p_5min_1000it_2fare_intCrowd_intComfort";
	private static String transitSchedule = "/Users/artemc/Dropbox/Work/Input/sfInput281015/transitSchedule.xml";
	private static String defaultPropertiesFile = "/Users/artemc/Workspace/matsim-git/playgrounds/artemc/connections/sfDatabase.properties";
	private static String schema = "sf_comfort_2fare_case1_intcrowd_intcomfort";

	public static void main(String[] args) {

		File directory = new File(dataPath);

		File[] fList = directory.listFiles();

		for (File file : fList) {
			if (file.isFile()) {
				System.out.println(file.getAbsolutePath());

			} else if (file.isDirectory()) {
				System.out.println(file.getAbsolutePath());
				eventsPath = file.getAbsolutePath() + "/it.1000/1000.events.xml.gz";
				//tableSuffix = file.getAbsolutePath().split("_1000it" + dataPathSuffix+"/w8-18")[1];
				tableSuffix = "_"+file.getAbsolutePath().split("_intComfort/")[1];
				tableSuffix = tableSuffix.replaceAll("\\.0x", "x");
				tableSuffix = tableSuffix.replaceAll("\\.5", "5");
				tableSuffix = tableSuffix.replaceAll("\\.1", "1");
				tableSuffix = tableSuffix.replaceAll("\\.0", "0");

				try {
					EventQueue.invokeAndWait(new Runnable() {
						public void run() {
							EventsToTravelSummaryRelationalTablesGUI_V2 frame = new EventsToTravelSummaryRelationalTablesGUI_V2();//
							frame.setVisible(true);
							frame.loadDefaultProperties(new File(defaultPropertiesFile));
							frame.changeDefaultProperties("eventsFile", eventsPath);
							frame.changeDefaultProperties("tableSuffix", tableSuffix);
							frame.changeDefaultProperties("transitScheduleFile", transitSchedule);
							frame.changeDefaultProperties("schemaName", schema);
							try {
								frame.runEventsProcessing();
							} catch (playground.artemc.utils.NoConnectionException e) {
								e.printStackTrace();
							}
							frame.dispose();
						}
					});
				} catch (InterruptedException e) {
					e.printStackTrace();
				} catch (InvocationTargetException e) {
					e.printStackTrace();
				}
			}
		}


	}
}
