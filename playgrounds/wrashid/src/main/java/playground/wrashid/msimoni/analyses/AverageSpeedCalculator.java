package playground.wrashid.msimoni.analyses;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.contrib.parking.lib.obj.TwoKeyHashMapWithDouble;


public class AverageSpeedCalculator implements LinkEnterEventHandler,
		LinkLeaveEventHandler {

	private Map<Id<Link>, ? extends Link> filteredLinks;
	private int binSizeInSeconds;
	private TwoKeyHashMapWithDouble<Id, Id> linkEnterTime=new TwoKeyHashMapWithDouble<Id, Id>();
	// linkId, agentId

	public HashMap<Id, SpeedAccumulator[]> speedAccumulator=new HashMap<Id, SpeedAccumulator[]>();

	public HashMap<Id, double[]> getAverageSpeeds() {
		HashMap<Id, double[]> result = new HashMap<Id, double[]>();

		for (Id linkId:speedAccumulator.keySet()){
			Link link=filteredLinks.get(linkId);
			double[] bins=new double[getNumberOfBins()];
			
			SpeedAccumulator[] sa =			speedAccumulator.get(linkId);
			
			for(int i=0;i<getNumberOfBins();i++){
				if (sa[i]==null){
					if (i==0){
						if (sa[getNumberOfBins()-1]!=null){
							bins[0]=sa[getNumberOfBins()-1].getAverageSpeed();
						} else {
							bins[0]=link.getFreespeed();
						}
					} else {
						if (i<getNumberOfBins()-1){
							if (sa[i+1]!=null){
								bins[i]=bins[i-1];
							} else {
								bins[i]=link.getFreespeed();
							}
						} else {
							bins[i]=bins[i-1];
						}
						
					}
					
				} else {
					bins[i]=sa[i].getAverageSpeed();
				}
			}
			result.put(linkId, bins);
		}
		return result;
	}

	private int getNumberOfBins() {
		return (86400 / binSizeInSeconds) + 1;
	}

	public AverageSpeedCalculator(
			Map<Id<Link>, ? extends Link> filteredEquilNetLinks, int binSizeInSeconds) {
		this.filteredLinks = filteredEquilNetLinks;
		this.binSizeInSeconds = binSizeInSeconds;
		
		for (Id linkId:filteredLinks.keySet()){
			speedAccumulator.put(linkId, new SpeedAccumulator[getNumberOfBins()]);
		}
	}

	@Override
	public void reset(int iteration) {

	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		Id linkId = event.getLinkId();
//		Id personId = event.getDriverId();
		Id personId = event.getVehicleId();
		if (filteredLinks.containsKey(linkId)) {
			if (linkEnterTime.containsKeyTwo(linkId,
					personId)) {
				
				int binIndex = (int) Math.round(Math.floor(GeneralLib.projectTimeWithin24Hours(event.getTime()) / binSizeInSeconds));
				if (speedAccumulator.get(linkId)[binIndex]==null){
					speedAccumulator.get(linkId)[binIndex]=new SpeedAccumulator();
				}
				speedAccumulator.get(linkId)[binIndex].addSpeedSample(filteredLinks.get(linkId).getLength() /  GeneralLib.getIntervalDuration(linkEnterTime.get(linkId, personId), event.getTime()));
				linkEnterTime.removeValue(linkId, personId);
			}
		}
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		if (filteredLinks.containsKey(event.getLinkId())) {
//			linkEnterTime.put(event.getLinkId(), event.getDriverId(),
			linkEnterTime.put(event.getLinkId(), event.getVehicleId(),
						event.getTime());
		}
	}

	private class SpeedAccumulator {
		private double speedSum;
		private int numberOfSamples;

		double getAverageSpeed() {
			return speedSum / numberOfSamples;
		}

		void addSpeedSample(double speed) {
			speedSum += speed;
			numberOfSamples++;
		}
	}

}
