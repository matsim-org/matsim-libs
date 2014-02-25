package playground.jbischoff.taxi.berlin.supply;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.bouncycastle.jce.provider.JDKGOST3410Signer.gost3410;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;

public class TaxiIdReader {
	
	private static final Logger log = Logger.getLogger(TaxiIdReader.class);
	private Map<Integer,Integer> addToSystem;
	private Map<Integer,Integer> removeFromSystem;
	private Map<Integer,Integer> inSystem;
	private List<TaxiIdData> taxiIdData;
	
	public static void main(String[] args) {
		for (int i = 15; i<22; i++){
		TaxiIdReader tir = new TaxiIdReader();
		tir.go(i);
		}
	}
	private void go(int i){
		
		TaxiIdParser tip = new TaxiIdParser();
		this.read("/Users/jb/shared-svn/projects/sustainability-w-michal-and-dlr/data/OD/vehicles/rawFCD_201304"+i+".csv", tip);
		this.taxiIdData = tip.getTaxiIds();
		this.analyse();
		this.write("/Users/jb/shared-svn/projects/sustainability-w-michal-and-dlr/data/OD/vehicles/201304"+i+"_taxisOverTime.csv");
		
	}
	
	
	private void write(String string) {
		BufferedWriter bw = IOUtils.getBufferedWriter(string);
		
		try {
			for (Entry<Integer,Integer> sec : this.inSystem.entrySet()){
				bw.append(sec.getKey()+"\t"+sec.getValue()+"\n");
			}
			bw.flush();
			bw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	private void analyse() {
		for (TaxiIdData td: this.taxiIdData){
			this.addTaxi(td.getStartTime());
			this.removeTaxi(td.getEndTime());
		}
	
		for (int i=1; i<30*3600;i++ ){
			this.incTaxi(i, this.inSystem.get(i-1));
			int inc = this.addToSystem.get(i);
			this.incTaxi(i,inc );

			this.decTaxi(i, this.removeFromSystem.get(i));

		}
	}
	
	public TaxiIdReader() {
		this.addToSystem = new HashMap<Integer, Integer>();
		this.removeFromSystem = new HashMap<Integer, Integer>();
		this.inSystem = new TreeMap<Integer, Integer>();
		for (int d = 0 ; d<30*3600 ; d++){
			this.addToSystem.put(d, 0);
			this.removeFromSystem.put(d, 0);
			this.inSystem.put(d,0);
		}
	}

	private void read(String file, TabularFileHandler handler) {
		TabularFileParserConfig config = new TabularFileParserConfig();
		log.info("parsing " + file);
		config.setDelimiterTags(new String[]{"\t"," "});
		config.setFileName(file);
		config.setCommentTags(new String[]{"#"});
		new TabularFileParser().parse(config, handler);
		log.info("done. (parsing " + file + ")");
	}
	
	private void addTaxi (int time){
		int amountbefore = this.addToSystem.get(time);
		amountbefore++;
		this.addToSystem.put(time, amountbefore);
	}
	private void removeTaxi (int time){
		int amountbefore = this.removeFromSystem.get(time);
		amountbefore++;
		this.removeFromSystem.put(time, amountbefore);
	}
	private void incTaxi (int time, int amount){
		int amountbefore = this.inSystem .get(time);
		amountbefore+=amount;
		this.inSystem.put(time, amountbefore);
	}
	private void decTaxi (int time, int amount){
		int amountbefore = this.inSystem .get(time);
		amountbefore-=amount;
		this.inSystem.put(time, amountbefore);
	}
	

}


class TaxiIdData{
	private Id taxiId;
	private String startDate;
	private String endDate;
	private int startTime;
	private int endTime;
	
	TaxiIdData(Id taxiId, String startDate, String endDate, int startTime, int endTime){
		this.taxiId = taxiId;
		this.startDate = startDate;
		this.endDate = endDate;
		this.startTime = startTime;
		this.endTime = endTime;
	}

	public Id getTaxiId() {
		return taxiId;
	}

	public String getStartDate() {
		return startDate;
	}

	public String getEndDate() {
		return endDate;
	}

	public int getStartTime() {
		return startTime;
	}

	public int getEndTime() {
		return endTime;
	}
	
		
}

class TaxiIdParser implements TabularFileHandler{

	private List<TaxiIdData> taxiIds = new ArrayList<TaxiIdData>();
	
	@Override
	public void startRow(String[] row) {
		Id taxiId = new IdImpl(row[0]);
		String startDate = row[1];
		int startTime = parseTime(row[2]);
		String endDate = row[3];
		int endTime = parseTime(row[4]);
		
		if (startTime>endTime) endTime += 24*3600;
	taxiIds.add(new TaxiIdData(taxiId, startDate, endDate, startTime, endTime))	;
	}

	private int parseTime(String timeString) {
		String[] time = timeString.split(":");
		int timesec = Integer.parseInt(time[0])*3600 + Integer.parseInt(time[1])*60 + Integer.parseInt(time[2]);
//		System.out.println(timeString +"-->"+timesec);
		return timesec;
	}

	public List<TaxiIdData> getTaxiIds() {
		return taxiIds;
	}
	
	
	}