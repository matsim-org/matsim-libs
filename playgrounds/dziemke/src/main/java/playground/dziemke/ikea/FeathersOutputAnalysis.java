package playground.dziemke.ikea;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;


public class FeathersOutputAnalysis {private static String dataFile = "./input/prdToAscii.csv";


public static void main(String[] args) {
	// TODO Auto-generated method stub

	BufferedWriter writer = null;
	
	//create a temporary file
    String timeLog = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());
    File logFile = new File("FeathersOutputAnalysis_"+timeLog);

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
	int index_homezone = 1;
	int index_activityZone = 17;
	int index_travelDuration=18;
	int index_activityDuration=16;
	int index_activityType=14;
	int index_personID=7;
	
	int homezoneMAX=0;
	int activityzoneMAX=0;
	int n_lines=0;
	int sum_travel_duration=0;
	long[] sum_activity_duration={0,0,0,0,0,0,0,0,0,0,0};
	int n_journeys=0;
	int n_activities=0;
	int[] n_activities_per_type={0,0,0,0,0,0,0,0,0,0,0};
	int n_agents=0;
	int travel_duration_MAX=0;
	int travel_duration_MIN=99;
	int[] activity_duration_MAX={0,0,0,0,0,0,0,0,0,0,0};
	int[] activity_duration_MIN={99,99,99,99,99,99,99,99,99,99,99};
	
	
	int travel_duration;
	int activity_duration;
	int[] activityType = new int[10];
	String[] activityTypeStrings = {"home","work","n.a.","brinGet","dailyShopping","nonDailyShopping","services","socialVisit","leisure","touring","other"} ;

	
	while((line=bufferedReader.readLine()) != null){
		String parts[] = line.split(";");
n_lines++;
//read file
travel_duration=Integer.parseInt(parts[index_travelDuration]);
activity_duration=Integer.parseInt(parts[index_activityDuration]);
//--
sum_travel_duration=sum_travel_duration + travel_duration;
sum_activity_duration[Integer.parseInt(parts[index_activityType])]=sum_activity_duration[Integer.parseInt(parts[index_activityType])] + activity_duration;
// journeys
//find travel duration MAX and MIN
		
if(Integer.parseInt(parts[index_travelDuration])!=0){
	n_journeys++;
	if(travel_duration<travel_duration_MIN){
		travel_duration_MIN=travel_duration;
	}
	if(travel_duration>travel_duration_MAX){
		travel_duration_MAX=travel_duration;
	}
}

// activities
n_activities++;
n_activities_per_type[Integer.parseInt(parts[index_activityType])]++;
if(activity_duration<activity_duration_MIN[Integer.parseInt(parts[index_activityType])]){
	activity_duration_MIN[Integer.parseInt(parts[index_activityType])]=activity_duration;
}
if(activity_duration>activity_duration_MAX[Integer.parseInt(parts[index_activityType])]){
	activity_duration_MAX[Integer.parseInt(parts[index_activityType])]=activity_duration;
}

// find homezone MAX
		if(Integer.parseInt(parts[index_homezone])>homezoneMAX){
			homezoneMAX=Integer.parseInt(parts[index_homezone]);}
		
		// find Activity Zone MAX
		if(Integer.parseInt(parts[index_activityZone])>activityzoneMAX){
			activityzoneMAX=Integer.parseInt(parts[index_activityZone]);	
		}
			

n_agents=Integer.parseInt(parts[index_personID]);		
	}
		bufferedReader.close();
	int travel_duration_AVG=sum_travel_duration/n_journeys;
	long[] activity_duration_AVG= new long[11];
	for (int i=0;i<=10;i++){
		if(n_activities_per_type[i]!=0){
	activity_duration_AVG[i]=sum_activity_duration[i]/n_activities_per_type[i];
		}}
	//sample variances:
	double sum_Variance_journeys = 0;
	double[] sum_Variance_activities = {0,0,0,0,0,0,0,0,0,0,0};

	bufferedReader = new BufferedReader(new FileReader(dataFile));
	line = bufferedReader.readLine();
	while((line=bufferedReader.readLine()) != null){
		String parts[] = line.split(";");

		sum_Variance_journeys=sum_Variance_journeys+
((Integer.parseInt(parts[index_travelDuration])
		-travel_duration_AVG)
		*(Integer.parseInt(parts[index_travelDuration])-travel_duration_AVG));

sum_Variance_activities[Integer.parseInt(parts[index_activityType])]=sum_Variance_activities[Integer.parseInt(parts[index_activityType])]+
((Integer.parseInt(parts[index_activityDuration])
		-activity_duration_AVG[Integer.parseInt(parts[index_activityType])])
		*(Integer.parseInt(parts[index_activityDuration])-activity_duration_AVG[Integer.parseInt(parts[index_activityType])]));
	}
	
	writer.write("number of lines: " + n_lines);
	writer.newLine();
	writer.write("number of agents: " + n_agents);
	writer.newLine();
	writer.write("number of journeys: " + n_journeys);
	writer.newLine();
	writer.write("travel duration MAX: " + travel_duration_MAX);
	writer.newLine();
	writer.write("travel duration MIN: " + travel_duration_MIN);
	writer.newLine();
	writer.write("average journey duration: " + travel_duration_AVG + " minutes");
	writer.newLine();
	writer.write("sample variance of journey durations: " + sum_Variance_journeys/(n_journeys-1));
	writer.newLine();
	writer.write("standard deviation journey durations: " + Math.sqrt(sum_Variance_journeys/(n_journeys-1)));
	writer.newLine();
	writer.write("total number of activities: " + n_activities);
	writer.newLine();
	writer.write("activities per type");
	writer.newLine();
	
	for (int i=0;i<=10;i++){
		if(n_activities_per_type[i]!=0){
		writer.write("activity Type: " +activityTypeStrings[i] + 
				";\tnumber of act.: "+ n_activities_per_type[i]+
				";\tAVG: "+ activity_duration_AVG[i]+
				";\tMAX: "+ activity_duration_MAX[i]+
				";\tMIN: "+ activity_duration_MIN[i]+
				";\tVAR: "+ (sum_Variance_activities[i]/(n_activities_per_type[i]-1)) +
				";\tSTD: "+ Math.sqrt(sum_Variance_activities[i]/(n_activities_per_type[i]-1)));
		writer.newLine();
		}	}
	writer.write("homezone MAX: " + homezoneMAX);
	writer.newLine();
	writer.write("activity Zone MAX: " + activityzoneMAX);
		
	System.out.println("finito");
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
