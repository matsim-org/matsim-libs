package playground.sergioo.weeklySetup2016;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;

import others.sergioo.util.probability.ContinuousRealDistribution;

public class ConfigFileWriter {

	private static final int NUM_DAYS = 7;
	private static final double TIME_GAP = 3600;

	public static void main(String[] args) throws FileNotFoundException, IOException, ClassNotFoundException {
		Config config = ConfigUtils.createConfig(); 
		if(!args[0].equals("no"))
			ConfigUtils.loadConfig(config, args[0]);
		ObjectInputStream ois = new ObjectInputStream(new FileInputStream(args[1]));
		Map<Integer, List<ContinuousRealDistribution>> clusterDistributions = (Map) ois.readObject();
		ois.close();
		for(Entry<Integer, List<ContinuousRealDistribution>> clusterE:clusterDistributions.entrySet())
			for(int d=0; d<NUM_DAYS; d++) {
				double baseTime = d*24*3600;
				ContinuousRealDistribution startTime = clusterE.getValue().get(2*d);
				ContinuousRealDistribution duration = clusterE.getValue().get(2*d+1);
				if(startTime.getValues().size()>=0 && duration.getValues().size()>0) {
					double start = baseTime + startTime.getNumericalMean();
					double end = start+duration.getNumericalMean();
					String actOptText = "work_"+clusterE.getKey()+"_"+d;
					ActivityParams activityParams = new ActivityParams(actOptText);
					activityParams.setOpeningTime(start-1.5*TIME_GAP);
					activityParams.setClosingTime(end+3*TIME_GAP);
					activityParams.setLatestStartTime(start+TIME_GAP);
					activityParams.setEarliestEndTime(end-TIME_GAP);
					activityParams.setMinimalDuration(end-start-1.5*TIME_GAP);
					activityParams.setTypicalDuration(end-start);
					config.planCalcScore().addActivityParams(activityParams);
				}
			}
		ois = new ObjectInputStream(new FileInputStream(args[2]));
		clusterDistributions = (Map) ois.readObject();
		ois.close();
		for(Entry<Integer, List<ContinuousRealDistribution>> clusterE:clusterDistributions.entrySet())
			for(int d=0; d<NUM_DAYS; d++) {
				double baseTime = d*24*3600;
				ContinuousRealDistribution startTime = clusterE.getValue().get(2*d);
				ContinuousRealDistribution duration = clusterE.getValue().get(2*d+1);
				if(startTime.getValues().size()>=0 && duration.getValues().size()>0) {
					double start = baseTime + startTime.getNumericalMean();
					double end = start+duration.getNumericalMean();
					String actOptText = "study_"+clusterE.getKey()+"_"+d;
					ActivityParams activityParams = new ActivityParams(actOptText);
					activityParams.setOpeningTime(start-1.5*TIME_GAP);
					activityParams.setClosingTime(end+1.5*TIME_GAP);
					activityParams.setLatestStartTime(start+TIME_GAP/2);
					activityParams.setEarliestEndTime(end-TIME_GAP/2);
					activityParams.setMinimalDuration(end-start-TIME_GAP);
					activityParams.setTypicalDuration(end-start);
					config.planCalcScore().addActivityParams(activityParams);
				}
			}
		new ConfigWriter(config).write(args[3]);
	}

}
