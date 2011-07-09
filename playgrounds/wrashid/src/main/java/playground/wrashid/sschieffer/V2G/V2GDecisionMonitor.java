package playground.wrashid.sschieffer.V2G;

import org.matsim.api.core.v01.Id;

import playground.wrashid.sschieffer.DSC.DecentralizedSmartCharger;
import playground.wrashid.sschieffer.SetUp.IntervalScheduleClasses.LoadDistributionInterval;

public class V2GDecisionMonitor {

	private double costKeeping, costReschedule, compensation;
	Id agentId;
	LoadDistributionInterval l;
	
	public V2GDecisionMonitor(Id agent, double costKeeping, double costReschedule, double compensation, LoadDistributionInterval l){
		this.agentId=agent;
		this.costKeeping=costKeeping;
		this.costReschedule=costReschedule;
		this.compensation=compensation;
		this.l=l;
	}
	
	
	public void writeOut(){
		System.out.println("\n Agent:"+agentId.toString()+ 
				"\t From: "+l.getStartTime()+
				"\t To: "+l.getEndTime()+
				"\t costKeeping: "+costKeeping+
				"\t costReschedule: "+costReschedule+
				"\t compensation: "+compensation+
				"\t reschedule?: "+(costKeeping>=costReschedule)+
				"\t additional Revenue?: "+(costKeeping-costReschedule));
	}
	
	public String writeString(){
		String s=agentId.toString()+ "\t"
				+ DecentralizedSmartCharger.hasAgentEV(agentId)+ "\t"
				+l.getStartTime()+ "\t"
				+l.getEndTime()+ "\t"
				+costKeeping+ "\t"
				+costReschedule+ "\t"
				+compensation+ "\t"
				+(costKeeping>=costReschedule)+ "\t"
				+(costKeeping-costReschedule)+ "\n";
		return s;
	}
	
}
