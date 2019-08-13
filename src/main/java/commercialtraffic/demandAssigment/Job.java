package commercialtraffic.demandAssigment;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;

public class Job {
	String jobId;
	Id<Person> personid;
	String serviceType;
	String customerRelation;
	String zone;
	Double serviceDuration;
	int planIdx;
	Activity regularAgentActivity;
	String carrierId;
	Double startTime;
	Double endTime;
	
	
	public Job(String jobId,String carrierId, Id<Person> personid, String serviceType, String customerRelation, String zone,
			Double serviceDuration, int planIdx, Activity regularAgentActivity, Double startTime, Double endTime) {
		this.jobId = jobId;
		this.carrierId = carrierId;
		this.personid = personid;
		this.serviceType = serviceType;
		this.customerRelation = customerRelation;
		this.zone = zone;
		this.serviceDuration = serviceDuration;
		this.planIdx = planIdx;
		this.regularAgentActivity = regularAgentActivity;
		this.startTime=startTime;
		this.endTime = endTime;
		
	}



}
