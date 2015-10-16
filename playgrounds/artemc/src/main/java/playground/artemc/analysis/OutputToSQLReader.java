package playground.artemc.analysis;

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

	private static String simType = "2min";

	private static String dataPath = "/Volumes/DATA 1 (WD 2 TB)/output_SelectExp1_5p_"+simType+"_1000it/";
	private static String transitSchedule = "/Users/artemc/Dropbox/Work/Input/input17062015_8000_"+simType+"/transitSchedule.xml";
	private static String defaultPropertiesFile = "/Users/artemc/Workspace/playgrounds/artemc/connections/corridorDatabase.properties";
	private static String eventsPath;
	private static String tableSuffix;

	public static void main(String[] args) {

		File directory = new File(dataPath);

		File[] fList = directory.listFiles();

		for (File file : fList) {
			if (file.isFile()) {
				System.out.println(file.getAbsolutePath());

			} else if (file.isDirectory()) {
				System.out.println(file.getAbsolutePath());
				eventsPath = file.getAbsolutePath() + "/it.1000/1000.events.xml.gz";
				tableSuffix = file.getAbsolutePath().split("_1000it/w8-18")[1];
				tableSuffix = tableSuffix.replaceAll("\\.0x", "x");
				tableSuffix = tableSuffix.replaceAll("\\.5", "5");
				tableSuffix = tableSuffix.replaceAll("\\.1", "1");

//				try {
//					EventQueue.invokeAndWait(new Runnable() {
//						public void run() {
//							EventsToTravelSummaryRelationalTablesGUI_V2 frame = new EventsToTravelSummaryRelationalTablesGUI_V2();//
//							frame.setVisible(true);
//							frame.loadDefaultProperties(new File(defaultPropertiesFile));
//							frame.changeDefaultProperties("eventsFile", eventsPath);
//							frame.changeDefaultProperties("tableSuffix", tableSuffix);
//							frame.changeDefaultProperties("transitScheduleFile", transitSchedule);
//							frame.changeDefaultProperties("schemaName", "corridor_" + simType);
//							frame.runEventsProcessing();
//							frame.dispose();
//						}
//					});
//				} catch (InterruptedException e) {
//					e.printStackTrace();
//				} catch (InvocationTargetException e) {
//					e.printStackTrace();
//				}
			}
		}


	}
}
