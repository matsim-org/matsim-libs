/**
 * 
 */
package playground.tschlenther.Math;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.jfree.util.Log;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.CountsReaderMatsimV1;
import org.matsim.counts.CountsWriter;

import playground.vsp.demandde.counts.TSBASt2Count;

import java.lang.Number.*;
import java.net.URL;

/**
 * @author tschlenther
 */
public class GaussDist {
	private final static double SIGMA = 60;
	//---------------------------------------------//
	private final static int BEGIN_MORNING = 6;
	private final static int END_MORNING = 12;
	private static double my_morning = 7.25;
	private final static double TRAFFICVOLUME_MORNING = 226.0;
	
	private final static int BEGIN_AFTERNOON= 12;
	private final static int END_AFTERNOON = 18;
	private static double my_afternoon= 15.75;
	private final static double TRAFFICVOLUME_AFTERNOON= 218.0;
	
	private final static Id<Link> linkID = Id.createLinkId("3276");
	private final static String stationName= "KP4.1.West/West/out";
	
	//--------------------------------------------------//
	private static double begin_m_morning = 0.0;
	private static double end_m_morning = 0.0;
	private static double begin_m_afternoon = 0.0;
	private static double end_m_afternoon= 0.0;
	private static double gaussHeight = 0.0;
	//private static final URL COUNTS_PATH = "C:/Users/Work/svn/shared-svn/projects/cottbus/data/scenarios/cottbus_scenario/CottbusCounts/counts_matsim/counts_onlyPeaks.xml"; 
	private static final String COUNTS_PATH2 = "C:/Users/Work/svn/shared-svn/projects/cottbus/data/scenarios/cottbus_scenario/CottbusCounts/counts_matsim/counts_final.xml"; 

	private static final Logger logger = Logger.getLogger(GaussDist.class);
	

	
	//--------------------------------------------------//
	
	private static double gFunction (double dd, double my){
		return Math.exp(-0.5*(Math.pow(((dd-my)/SIGMA), 2))) / (SIGMA*(Math.sqrt(2*Math.PI)));
	}
	
	private static double fFunction(double dd, double my){
		return gaussHeight * (gFunction(dd,my));
	}
	
	private static void calcMinutes(){
		begin_m_morning = BEGIN_MORNING*60;
		end_m_morning = END_MORNING*60;
		begin_m_afternoon= BEGIN_AFTERNOON*60;
		end_m_afternoon= END_AFTERNOON*60;
		my_morning = my_morning*60;
		my_afternoon = my_afternoon*60;
	}
	
	private static Map<Integer,Integer> calcData(double begin, double end, double my){
		double hour_sum = 0.0;
		int minCount = 0;
		Map<Integer, Integer> data = new HashMap<Integer,Integer>();
		for(double i = begin; i < end; i++){
			minCount += 1;
			if(minCount <60){
				hour_sum += fFunction(i,my);
			}
			else{
				Integer hour = (int) ((i-59.0)/60); 
				System.out.println("" + hour + ":\t" + hour_sum);
				data.put(hour, (int) Math.round(hour_sum));
				hour_sum = 0.0;
				minCount = 0;
			}
		}
		if(data.isEmpty()) throw new RuntimeException();
		return data;
	}
	
	public static void main(String[] args){
		Map<Integer, Integer> countData = new HashMap<Integer,Integer>();
		
		calcMinutes();
		double g_sum_morning = 0.0;
		for (double i = begin_m_morning; i < end_m_morning; i++){
			g_sum_morning += gFunction(i,my_morning);
		}
		gaussHeight = TRAFFICVOLUME_MORNING/g_sum_morning;
		countData = calcData(begin_m_morning,end_m_morning,my_morning);
		
		double g_sum_afternoon = 0.0;
		for (double i = begin_m_afternoon; i < end_m_afternoon; i++){
			g_sum_afternoon+= gFunction(i,my_afternoon);
		}
		gaussHeight = TRAFFICVOLUME_AFTERNOON/g_sum_afternoon;
		countData.putAll(calcData(begin_m_afternoon,end_m_afternoon,my_afternoon));
		
		Counts counts2 = new Counts();
		//new CountsReaderMatsimV1(counts2).parse(COUNTS_PATH);
//		Count current = counts2.createAndAddCount(linkID, stationName);
//		if (current ==  null) logger.error("count = null");
//		if (countData == null) logger.error("countData = null");
//		for(int hh : countData.keySet()){
//			current.createVolume(hh, countData.get(hh));
//		}
		CountsWriter writer = new CountsWriter(counts2);
		writer.write(COUNTS_PATH2);
		logger.info("FINISHED");
		
	}
	
	
	
	
	
}
