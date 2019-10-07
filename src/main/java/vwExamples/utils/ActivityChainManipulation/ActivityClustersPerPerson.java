package vwExamples.utils.ActivityChainManipulation;

import java.util.ArrayList;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

public class ActivityClustersPerPerson {
	public int totalSize;
	public Id<Person> personId;
	public ArrayList<ActivityCluster> ActivityClusters;

	ActivityClustersPerPerson(Id<Person> personId) {
		this.personId = personId;
		this.totalSize = 0;
		this.ActivityClusters = new ArrayList<ActivityCluster>();

	}

	public void addActivityCluster(ActivityCluster activityCluster) {
		this.ActivityClusters.add(activityCluster);
		this.totalSize = this.totalSize + activityCluster.size;

	}

}
