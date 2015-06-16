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

public class FeathersOutputAnalysis_TravelDurations {private static String dataFile = "./input/prdToAscii.csv";


public static void main(String[] args) {
	// TODO Auto-generated method stub

	BufferedWriter writer = null;
	
	//create a temporary file
    String timeLog = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());
    File logFile = new File("FeathersOutputAnalysis_TravelDurations"+timeLog);

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
	
	// set indices

	int index_travelDuration=18;

	int travel_duration;
	XYSeries series1 = new XYSeries("Travel durations");
	int[] travel_durations_array=new int[570];
	int n=0;
	while((line=bufferedReader.readLine()) != null){
		String parts[] = line.split(";");
n++;
travel_duration=Integer.parseInt(parts[index_travelDuration]);
if (travel_duration!=0){
	travel_durations_array[travel_duration]++;

System.out.println("Zeile " + n+ ": Dauer: " + travel_duration +" Anzahl: "+ travel_durations_array[travel_duration]);
}
	}
	//create series for chart
	for(int i=0;i<180;i++){
	series1.add(i,travel_durations_array[i]);}
//write data to file
	writer.write("duration; number of journeys");
	writer.newLine();
	for(int i=0;i<568;i++){
writer.write(i+";"+travel_durations_array[i]);
		writer.newLine();
	System.out.println(travel_durations_array[i]+" journeys have a duration of "+i+" minutes");
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
	// Erstellen eines Ausgabefensters
	ApplicationFrame punkteframe = new ApplicationFrame("title");

	ChartPanel chartPanel2 = new ChartPanel(chart2);
	punkteframe.setContentPane(chartPanel2);
	punkteframe.pack();
	punkteframe.setVisible(true);
		
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
