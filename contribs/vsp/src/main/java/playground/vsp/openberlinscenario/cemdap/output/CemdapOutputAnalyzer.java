package playground.vsp.openberlinscenario.cemdap.output;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CemdapOutputAnalyzer {

    public static final Logger LOG = Logger.getLogger(CemdapOutputAnalyzer.class);


    // Cemdap activity file columns
	private static final int HH_ID = 0;
    private static final int P_ID = 1;
    private static final int ACT_TYPE = 2;
    private static final int ACT_START_TIME = 3;
    private static final int ACT_DURATION = 4;
    private static final int ACT_LOCATION_ZONE_ID = 5;

    private List<Id<Person>> personIds = new ArrayList<>();

    private double numberOfAgents = 0;

    private long numberOfActivities = 0;
    private Map<String, Long> activityTypeToCount = new HashMap<>();
    private Map<String, Long> activityTypeToDuration = new HashMap<>();

    void increaseNumberOfAgents() {

        numberOfAgents++;
    }

    void registerTrip(String activityType, int duration) {

        numberOfActivities++;

        Long currentCount = activityTypeToCount.get(activityType);
        Long currentDuration = activityTypeToDuration.get(activityType);
        if (currentCount == null) {
            currentCount = 0L;
            currentDuration = 0L;
        }
        currentCount++;
        currentDuration += duration;
        activityTypeToCount.put(activityType, currentCount);
        activityTypeToDuration.put(activityType, currentDuration);

    }

    public void logOutput() {

        LOG.info("Number of agents: " + numberOfAgents);
        double averageNumberOfActivities = numberOfActivities/numberOfAgents;
        LOG.info("Average number of activities: " + averageNumberOfActivities);
        for (String activityType : activityTypeToCount.keySet()) {

            double averageNumberOfCurrentActivities = activityTypeToCount.get(activityType)/numberOfAgents;
            double averageDurationOfCurrentActivities = activityTypeToDuration.get(activityType)/(double)activityTypeToCount.get(activityType);
            LOG.info("Average number of " + activityType + "-activities: " + averageNumberOfCurrentActivities);
            LOG.info("Average duration of " + activityType + "-activities: " + convertTimeFromSeconds((int)averageDurationOfCurrentActivities));
        }
    }

    private int getTimeInSeconds(String timeString) {

        String[] units = timeString.split(":"); //will break the string up into an array
        int hours = Integer.parseInt(units[0]); //first element
        int minutes = Integer.parseInt(units[1]); //second element
        int seconds = Integer.parseInt(units[2]); //third element
        return 3600 * hours + 60 * minutes + seconds;
    }

    private String convertTimeFromSeconds(int timeInSeconds) {

        int hours = timeInSeconds / 3600; //first element
        while (timeInSeconds > 3600) timeInSeconds -= 3600;
        int minutes = timeInSeconds /60; //second element
        while (timeInSeconds > 60) timeInSeconds -= 60;
        int seconds = timeInSeconds; //third element
        return hours + ":" + minutes + ":" + seconds;
    }

    public void writeOutput(String outputFile) {

        try {
            PrintStream fileStream = new PrintStream(new File(outputFile));

            fileStream.println("Number of agents: " + numberOfAgents);
            double averageNumberOfActivities = numberOfActivities/numberOfAgents;
            fileStream.println("Average number of activities: " + averageNumberOfActivities);
            for (String activityType : activityTypeToCount.keySet()) {

                double averageNumberOfCurrentActivities = activityTypeToCount.get(activityType)/numberOfAgents;
                double averageDurationOfCurrentActivities = activityTypeToDuration.get(activityType)/(double)activityTypeToCount.get(activityType);
                fileStream.println("Average number of " + activityType + "-activities: " + averageNumberOfCurrentActivities);
                fileStream.println("Average duration of " + activityType + "-activities: " + convertTimeFromSeconds((int)averageDurationOfCurrentActivities));
            }
            fileStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
