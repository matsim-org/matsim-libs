package playground.dziemke.ikea;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYDotRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.ApplicationFrame;

public class FeathersOutputAnalysis_ActivityDurations {private static String dataFile = "./input/prdToAscii.csv";


public static void main(String[] args) {
	// TODO Auto-generated method stub
	// set indices

		int index_activityDuration=16;
		int index_activityType=14;
		int[] activityType = new int[10];
		int[] n_activities_per_type={0,0,0,0,0,0,0,0,0,0,0};
		int[] activity_durations_MAX={1440, 1122,0, 391, 407,0, 609, 623, 633, 873, 571};
		String[] activityTypeStrings = {"home","work","n.a.","brinGet","dailyShopping","nonDailyShopping","services","socialVisit","leisure","touring","other"} ;


for(int j=0;j<=10;j++){	
	
	BufferedWriter writer = null;
	
	//create a temporary file
    String timeLog = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());
    File logFile = new File("FeathersOutputAnalysis_ActivityDurations "+activityTypeStrings[j]+""+timeLog);

    // This will output the full path where the file will be written to...
    try {
		System.out.println(logFile.getCanonicalPath());
        writer = new BufferedWriter(new FileWriter(logFile));

    } catch (IOException e1) {
		// TODO Auto-generated catch block
		e1.printStackTrace();
	}
    
    
	try{
		// load data-file
	BufferedReader bufferedReader = new BufferedReader(new FileReader(dataFile));
	// skip header
	String line = bufferedReader.readLine();
	
	
	
	int activity_duration;
	XYSeries series1 = new XYSeries("Activity durations for activity type " + activityTypeStrings[j]);
	int[] activity_durations_array=new int[activity_durations_MAX[j]+1];
	int n=0;
	
	
	while((line=bufferedReader.readLine()) != null){
		String parts[] = line.split(";");

if(Integer.parseInt(parts[index_activityType])==j){
activity_duration=Integer.parseInt(parts[index_activityDuration]);
n++;
	activity_durations_array[activity_duration]++;

System.out.println("Zeile " + n+ ": Dauer: " + activity_duration +" Anzahl: "+ activity_durations_array[activity_duration]);
}
	}
	//create series for chart
	for(int i=0;i<activity_durations_MAX[j];i++){
	series1.add(i,activity_durations_array[i]);}
//write data to file
	writer.write("duration; number of journeys");
	writer.newLine();
	for(int i=0;i<activity_durations_MAX[j]+1;i++){
writer.write(i+";"+activity_durations_array[i]);
		writer.newLine();
	System.out.println(activity_durations_array[i]+" activities have a duration of "+i+" minutes");
	}
	XYSeriesCollection dataset2 = new XYSeriesCollection();
	dataset2.addSeries(series1);
	
	XYDotRenderer dot = new XYDotRenderer();
	dot.setDotHeight(3);
	dot.setDotWidth(3);
	NumberAxis xax = new NumberAxis("x");
	NumberAxis yax = new NumberAxis("y");
	XYPlot plot = new XYPlot(dataset2,xax,yax, dot);
	JFreeChart chart2 = new JFreeChart(plot);
	// Create output window
	ApplicationFrame punkteframe = new ApplicationFrame("Activity type " + activityTypeStrings[j]); //"Punkte" entspricht der Ueberschrift des Fensters

	ChartPanel chartPanel2 = new ChartPanel(chart2);
	punkteframe.setContentPane(chartPanel2);
	punkteframe.pack();
	punkteframe.setVisible(true);
	
	System.out.println("finito " + j);
	
}
	catch (IOException e) {
		e.printStackTrace();
	}
	
	 try {
		writer.close();
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
}
}
}
