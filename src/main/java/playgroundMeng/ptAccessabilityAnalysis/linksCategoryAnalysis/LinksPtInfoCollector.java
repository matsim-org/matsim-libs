package playgroundMeng.ptAccessabilityAnalysis.linksCategoryAnalysis;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.map.HashedMap;
import org.apache.log4j.Logger;
import org.locationtech.jts.io.WKTWriter;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.io.NetworkChangeEventsWriter;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import com.google.inject.Inject;

import playgroundMeng.ptAccessabilityAnalysis.activitiesAnalysis.ActivitiesAnalysisInterface;
import playgroundMeng.ptAccessabilityAnalysis.areaSplit.AreaSplit;
import playgroundMeng.ptAccessabilityAnalysis.areaSplit.GridBasedSplit;
import playgroundMeng.ptAccessabilityAnalysis.run.ConsoleProgressBar;
import playgroundMeng.ptAccessabilityAnalysis.run.PtAccessabilityConfig;
import playgroundMeng.ptAccessabilityAnalysis.stopInfoCellector.RouteStopInfo;
import playgroundMeng.ptAccessabilityAnalysis.stopInfoCellector.TransitStopFacilityExtendImp;



public class LinksPtInfoCollector {
	
	private static final Logger logger = Logger.getLogger(LinksPtInfoCollector.class);
	

	AreaSplit areaSplit;

	PtAccessabilityConfig analysisConfig;
	
	ActivitiesAnalysisInterface activitiesAnalysis;
	

	Map<String,List<NetworkChangeEvent>> district2changeEvents = new HashedMap();
	Map<String,Map<Double,Double>> district2Time2Score = new HashedMap();
	Map<String,Map<Double,Double>> district2Time2KPI = new HashedMap();
	List<NetworkChangeEvent> allNetworkChangeEvents = new LinkedList<NetworkChangeEvent>();
	List<NetworkChangeEvent> networkChangeEventsInArea = new LinkedList<NetworkChangeEvent>();
	
	@Inject
	public LinksPtInfoCollector(AreaSplit areaSplit,PtAccessabilityConfig analysisConfig,ActivitiesAnalysisInterface activitiesAnalysis){
		this.areaSplit = areaSplit;
		this.analysisConfig = analysisConfig;
		this.activitiesAnalysis = activitiesAnalysis;
		
	}
	public void runAndFile () throws Exception {
		int remain = 0;
		int total = areaSplit.getLinksClassification().keySet().size();
		String linksInfoCollectProcess = "LinksInfoCollectProcess";
		
		List<Id<Link>> emptyLinkList = new ArrayList<Id<Link>>();

		for(String string : areaSplit.getLinksClassification().keySet()) {
			for(LinkExtendImp linkExtendImp: areaSplit.getLinksClassification().get(string)) {
				if(areaSplit.getStopsClassification().keySet().contains(string)) {
					for(TransitStopFacilityExtendImp transitStopFacilityExtendImp: areaSplit.getStopsClassification().get(string)) {
						linkExtendImp.addStopsInfo(transitStopFacilityExtendImp);
					}
				}
			}
			if(analysisConfig.isWriteLinksInfo()) {
				logger.info("beginn to write" + string +" area");
				LinkInfoPrinter linkInfoPrinter = new LinkInfoPrinter( areaSplit.getLinksClassification().get(string));
				linkInfoPrinter.print(analysisConfig.getOutputDirectory()+"LinksPtInfo_"+string+"_area.xml");	
			}
			this.district2Time2Score.put(string, new HashedMap());
			this.scoreCaculate(string, areaSplit.getLinksClassification().get(string));
			
			if(!(this.district2changeEvents.get(string) == null)) {
				this.allNetworkChangeEvents.addAll(this.district2changeEvents.get(string));
				if(analysisConfig.isWriteNetworkChangeEventForEachArea()) {
					new NetworkChangeEventsWriter().write(analysisConfig.getOutputDirectory()+"networkChangeEvent_"+string+"_area.xml", this.district2changeEvents.get(string));
				}
			}
			remain++;
			if(remain % (total/10) == 0) {
				ConsoleProgressBar.progressPercentage(remain, total, linksInfoCollectProcess, logger);
			}  else if (remain == total) {
				ConsoleProgressBar.progressPercentage(remain, total, linksInfoCollectProcess,logger);
			}
			
		}
		new NetworkChangeEventsWriter().write(analysisConfig.getOutputDirectory()+"allNetworkChangeEvent.xml", this.allNetworkChangeEvents);
		
		if(this.analysisConfig.isWriteArea2Time2Score()) {
			this.printDistrict2Time2Score();
		}
		if(this.analysisConfig.isConsiderActivities()) {
			if(this.analysisConfig.isWriteArea2Time2Activities()) {
				this.printDistrict2Time2Activities();
			}
			this.caculateKPI();
			this.printDistrict2KPI();
		}
		logger.info("finish");
	}
	
	private void scoreCaculate(String string, double beginnTime, double EndTime, List<LinkExtendImp> linkExtendImps) throws Exception {
		
		this.district2Time2Score.get(string).put(beginnTime, 0.);
		
		for(LinkExtendImp linkExtendImp: linkExtendImps) {
			linkExtendImp.setTime2Score(beginnTime, 0.);
			
			for(Map<Id<TransitStopFacility>, RouteStopInfo> key: linkExtendImp.getPtInfos().keySet()) {
				for(RouteStopInfo routeStopInfo: key.values()) {
					if(routeStopInfo.getDepatureTime()>=beginnTime && routeStopInfo.getDepatureTime()<EndTime) {
						linkExtendImp.setTime2Score(beginnTime, (linkExtendImp.getTime2Score().get(beginnTime)+ScoreCaculateFromMode(routeStopInfo)));
					}
				}
			}
			
			double a = this.district2Time2Score.get(string).get(beginnTime) + linkExtendImp.getTime2Score().get(beginnTime);
			this.district2Time2Score.get(string).put(beginnTime, a);
			addToNetworkChangeEvent(string, beginnTime, linkExtendImp);	
		}
	}
	private void addToNetworkChangeEvent(String string, double beginnTime, LinkExtendImp linkExtendImp) {
		NetworkChangeEvent networkChangeEvent = new NetworkChangeEvent(beginnTime);
		networkChangeEvent.setFlowCapacityChange(new NetworkChangeEvent.ChangeValue(NetworkChangeEvent.ChangeType.ABSOLUTE_IN_SI_UNITS, linkExtendImp.getTime2Score().get(beginnTime)/(3600)));
		networkChangeEvent.addLink(linkExtendImp);
		if(!this.district2changeEvents.containsKey(string)) {
			this.district2changeEvents.put(string,new LinkedList<NetworkChangeEvent>());
			this.district2changeEvents.get(string).add(networkChangeEvent);
		} else {
			this.district2changeEvents.get(string).add(networkChangeEvent);
		}
	}
	private void addNetworkChangeEvent2Area() {
		for(String string: this.district2Time2KPI.keySet()) {
			for(LinkExtendImp linkExtendImp : this.areaSplit.getLinksClassification().get(string)) {
				for(double time:this.district2Time2KPI.get(string).keySet()) {
					NetworkChangeEvent networkChangeEvent = new NetworkChangeEvent(time);
					networkChangeEvent.setFlowCapacityChange(new NetworkChangeEvent.ChangeValue(NetworkChangeEvent.ChangeType.ABSOLUTE_IN_SI_UNITS, this.district2Time2KPI.get(string).get(time)/(3600)));
					networkChangeEvent.addLink(linkExtendImp);
					this.networkChangeEventsInArea.add(networkChangeEvent);
				}
			}
		}
		
	}
	private void scoreCaculate(String string, List<LinkExtendImp> linkExtendImps) throws Exception {
		for(double x=analysisConfig.getBeginnTime(); x<analysisConfig.getEndTime(); x+=analysisConfig.getAnalysisTimeSlice()) {
			this.scoreCaculate(string, x, x+analysisConfig.getAnalysisTimeSlice(), linkExtendImps);
		}
	}
	private double ScoreCaculateFromMode(RouteStopInfo routeStopInfo) throws Exception {
		if(!analysisConfig.getModeScore().containsKey(routeStopInfo.getTransportMode())) {
			throw new Exception(routeStopInfo.getTransportMode()+" is not defined in config");
		} else {
			return analysisConfig.getModeScore().get(routeStopInfo.getTransportMode());
		}
	}	
	
	private void printDistrict2Time2Score() {
		File file = new File(this.analysisConfig.getOutputDirectory()+"District2Time2Score.csv");
		try {
			logger.info("beginn to write District2Time2Scores' info");
			FileWriter fileWriter = new FileWriter(file);
			BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
			bufferedWriter.write("District");
			for(double x=analysisConfig.getBeginnTime(); x<analysisConfig.getEndTime(); x+=analysisConfig.getAnalysisTimeSlice()) {
				bufferedWriter.write(","+x);
			}
			int remain = 0;
			int total = this.district2Time2Score.keySet().size();
			String printDistrict2ScoreProcess = "PrintDistrict2ScoreProcess";
			
			for(String string: this.district2Time2Score.keySet()){
				bufferedWriter.newLine();
				bufferedWriter.write(string);
				for(double x=analysisConfig.getBeginnTime(); x<analysisConfig.getEndTime(); x+=analysisConfig.getAnalysisTimeSlice()) {
					bufferedWriter.write(","+this.district2Time2Score.get(string).get(x));
				}
				remain++;
				if(remain % (total/10) == 0) {
					ConsoleProgressBar.progressPercentage(remain, total, printDistrict2ScoreProcess, logger);
				}  else if (remain == total) {
					ConsoleProgressBar.progressPercentage(remain, total, printDistrict2ScoreProcess,logger);
				}
			
			}
			bufferedWriter.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	private void printDistrict2Time2Activities() {
		File file = new File(this.analysisConfig.getOutputDirectory()+"District2Time2Activities.csv");
		try {
			logger.info("beginn to write District2Time2Activities' info");
			FileWriter fileWriter = new FileWriter(file);
			BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
			bufferedWriter.write("District");
			for(double x=analysisConfig.getBeginnTime(); x<analysisConfig.getEndTime(); x+=analysisConfig.getAnalysisTimeSlice()) {
				bufferedWriter.write(","+x);
			}
			for(String string: this.activitiesAnalysis.getArea2time2activities().keySet()){
				bufferedWriter.newLine();
				bufferedWriter.write(string);
				for(double x=analysisConfig.getBeginnTime(); x<analysisConfig.getEndTime(); x+=analysisConfig.getAnalysisTimeSlice()) {
					bufferedWriter.write(","+this.activitiesAnalysis.getArea2time2activities().get(string).get(x).size());
				}
			
			}
			bufferedWriter.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	private void caculateKPI() {
		for(String string : this.district2Time2Score.keySet()) {
			this.district2Time2KPI.put(string, new HashedMap());
			for(Double time: this.district2Time2Score.get(string).keySet()) {
				double score = this.district2Time2Score.get(string).get(time);
				double activities = this.activitiesAnalysis.getArea2time2activities().get(string).get(time).size();
				if(activities == 0) {
					this.district2Time2KPI.get(string).put(time, -1.);
				} else {
					this.district2Time2KPI.get(string).put(time, score/ activities);
				}
			}
		}
	}
	private void printDistrict2KPI() {
		File file = new File(this.analysisConfig.getOutputDirectory()+"Time2District2KPI.csv");
		try {
			logger.info("beginn to write Time2District2KPI' info");
			FileWriter fileWriter = new FileWriter(file);
			BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
			bufferedWriter.write("District");
			for(double x=analysisConfig.getBeginnTime(); x<analysisConfig.getEndTime(); x+=analysisConfig.getAnalysisTimeSlice()) {
				int h = (int)(x/3600);
				int m = (int)((x-h*3600)/60);
				int s = (int) (x-h*3600-m*60);
				bufferedWriter.write(","+timeConvert(h)+":"+timeConvert(m)+":"+timeConvert(s));
			}
			for(String string: this.district2Time2KPI.keySet()){
				bufferedWriter.newLine();
				bufferedWriter.write(string);
				for(double x=analysisConfig.getBeginnTime(); x<analysisConfig.getEndTime(); x+=analysisConfig.getAnalysisTimeSlice()) {
					bufferedWriter.write(","+this.district2Time2KPI.get(string).get(x));
				}
			
			}
			bufferedWriter.close();
			this.printTime2District2KPIForTimeManager();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}
	private void printTime2District2KPIForTimeManager() {
		File file2 = new File(this.analysisConfig.getOutputDirectory()+"Time2District2KPIForTimeManager.csv");
		try {
			logger.info("beginn to write Time2District2KPIForTimeManager's info");
			FileWriter fileWriter = new FileWriter(file2);
			BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
			bufferedWriter.write("BeginnTime");bufferedWriter.write("/EndTime");bufferedWriter.write("/Area");bufferedWriter.write("/KPI");
			bufferedWriter.newLine();
			for(String string: this.district2Time2KPI.keySet()){
				
				for(double x: this.district2Time2KPI.get(string).keySet()) {
					if(x< 24*3600 && this.district2Time2KPI.get(string).get(x) > -1) {
						int h = (int)(x/3600);
						int m = (int)((x-h*3600)/60);
						int s = (int) (x-h*3600-m*60);
						
						double y = x+this.analysisConfig.getAnalysisTimeSlice()-1;
						int h2 = (int)(y/3600);
						int m2 = (int)((y-h2*3600)/60);
						int s2 = (int) (y-h2*3600-m2*60);
						
						
						bufferedWriter.write(timeConvert(h)+":"+timeConvert(m)+":"+timeConvert(s));
						bufferedWriter.write("/"+timeConvert(h2)+":"+timeConvert(m2)+":"+timeConvert(s2));
						GridBasedSplit gridBasedSplit = (GridBasedSplit) areaSplit;
						WKTWriter wktWriter = new WKTWriter();
						bufferedWriter.write("/"+wktWriter.write(gridBasedSplit.getNum2Polygon().get(string)));
						bufferedWriter.write("/"+this.district2Time2KPI.get(string).get(x));
						bufferedWriter.newLine();
					}
				}
			}
			bufferedWriter.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	private String timeConvert(int a) {
		if (a<10) {
			return "0"+a;
		} else {
			return String.valueOf(a);
		}
		
		
	}
}

