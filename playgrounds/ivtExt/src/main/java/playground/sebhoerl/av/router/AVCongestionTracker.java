package playground.sebhoerl.av.router;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.vehicles.Vehicle;

public class AVCongestionTracker implements LinkEnterEventHandler, LinkLeaveEventHandler {
    final private Map<Id<Link>, Integer> linkIndexMap = new HashMap<>();
    
    final private float[][] congestionMap;
    final private float[][] buildCongestionMap;
    final private int[][] buildCongestionCounts;
    final private float[] freespeedMap;
    
    final private Map<Id<Vehicle>, Double> timeTracker = new HashMap<>();
    
    final private double startTime;
    final private double endTime;
    
    final private int numberOfBins;
    final private int numberOfLinks;
    final private double binSize;
    
    public AVCongestionTracker(double startTime, double endTime, int numberOfBins, Network network) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.numberOfBins = numberOfBins;
        
        binSize = (endTime - startTime) / (double)numberOfBins;
        numberOfLinks = network.getLinks().size();
        
        congestionMap = new float[numberOfBins][numberOfLinks];
        buildCongestionMap = new float[numberOfBins][numberOfLinks];
        buildCongestionCounts = new int[numberOfBins][numberOfLinks];
        
        for (int i = 0; i < numberOfBins; i++) {
            for (int j = 0; j < numberOfLinks; j++) {
                buildCongestionMap[i][j] = 0.0f;
                buildCongestionCounts[i][j] = 0;
            }
        }
     
        freespeedMap = new float[numberOfLinks];
        
        int index = 0;
        float duration = 0.0f;
        
        for (Link link : network.getLinks().values()) {
            linkIndexMap.put(link.getId(), index);
            duration = (float)link.getLength() / (float)link.getFreespeed();
            freespeedMap[index] = duration;
            
            for (int j = 0; j < numberOfBins; j++) {
                congestionMap[j][index] = duration;
            }
            
            index++;
        }
    }
    
    public double getLinkTime(Id<Link> linkId, double time) {
        if (time < startTime || time > endTime) {
            return freespeedMap[linkIndexMap.get(linkId)];
        }
        
        return congestionMap[getBinIndex(time)][linkIndexMap.get(linkId)];
    }
    
    @Override
    public void reset(int iteration) {
        for (int i = 0; i < numberOfBins; i++) {
            for (int j = 0; j < numberOfLinks; j++) {
                if (buildCongestionCounts[i][j] != 0) {
                //    congestionMap[i][j] = freespeedMap[j];
                //} else {
                    congestionMap[i][j] = 0.5f * (congestionMap[i][j] + buildCongestionMap[i][j] / (float)buildCongestionCounts[i][j]);
                }
                
                buildCongestionMap[i][j] = 0.0f;
                buildCongestionCounts[i][j] = 0;
            }
        }
        
        timeTracker.clear();
    }
    
    private int getBinIndex(double time) {
        return (int)Math.floor((time - startTime) / binSize);
    }

    @Override
    public void handleEvent(LinkLeaveEvent event) {
        if (!event.getVehicleId().toString().startsWith("bus")) {
            Double start = timeTracker.remove(event.getVehicleId());
            
            if (start != null) {
                int binIndex = getBinIndex(start);
                int linkIndex = linkIndexMap.get(event.getLinkId());
                
                buildCongestionMap[binIndex][linkIndex] += event.getTime() - start;
                buildCongestionCounts[binIndex][linkIndex]++;
            }
        }
    }

    @Override
    public void handleEvent(LinkEnterEvent event) {
        if (event.getVehicleId().toString().startsWith("av")) {
            timeTracker.put(event.getVehicleId(), event.getTime());
        }
    }
    
}
