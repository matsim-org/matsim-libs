package playground.jjoubert.roadpricing.analysis;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nl.knaw.dans.common.dbflib.CorruptedTableException;
import nl.knaw.dans.common.dbflib.DbfLibException;
import nl.knaw.dans.common.dbflib.Field;
import nl.knaw.dans.common.dbflib.IfNonExistent;
import nl.knaw.dans.common.dbflib.InvalidFieldLengthException;
import nl.knaw.dans.common.dbflib.InvalidFieldTypeException;
import nl.knaw.dans.common.dbflib.NumberValue;
import nl.knaw.dans.common.dbflib.Record;
import nl.knaw.dans.common.dbflib.Table;
import nl.knaw.dans.common.dbflib.Type;
import nl.knaw.dans.common.dbflib.Value;
import nl.knaw.dans.common.dbflib.Version;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.io.IOUtils;

public class MyGantryComparator {
	private static Logger log = Logger.getLogger(MyGantryComparator.class);
	
	private File baseFile;
	private File compareFile;
	private File linkIdFile;
	
	private Map<Id, Integer> baseMap;
	private Map<Id, Integer> compareMap;
	private List<Id> linkList;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		MyGantryComparator mgc = null;
		if(args.length != 4){
			throw new RuntimeException("Need three arguments: base linkstats file; comparative linkstats file; and file containing link Ids to compare.");
		} else{
			try {
				mgc = new MyGantryComparator(args[0], args[1], args[2]);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
		
		log.info("===========================================================================");
		log.info(" Comparing the pre- and post-roadpricing traffic counts on selected links.");
		log.info("---------------------------------------------------------------------------");
		log.info(" Baseline file : " + mgc.baseFile.getAbsolutePath());
		log.info(" Comparing file: " + mgc.compareFile.getAbsolutePath());
		log.info(" Links Ids from: " + mgc.linkIdFile.getAbsolutePath());
		
		mgc.compareTotalCount();
		mgc.writeComparisonToFile(args[3]);
//		mgc.writeComparisonToDbf(args[3]);
		
		log.info("---------------------------------------------------------------------------");
		log.info("                                Completed");
		log.info("===========================================================================");

	}
	
	
	
	
	
	public void compareTotalCount() {
		this.readLinks();
		baseMap = this.readLinkStatsOptionOne(baseFile);
		compareMap = this.readLinkStatsOptionOne(compareFile);
	}
	
	public void writeComparisonToFile(String filename){
		if(baseMap.isEmpty() || compareMap.isEmpty()){
			throw new RuntimeException("No comparison can be written. Maps are empty.");
		}
		/* Prepare for writing: calculate the difference and sort. */
		try {
			BufferedWriter bw = IOUtils.getBufferedWriter(filename);
			try{
				bw.write("linkId,baseCount,compareCount,Diff");
				bw.newLine();
				for(Id id : linkList){
					if(!baseMap.containsKey(id) || !compareMap.containsKey(id)){
						log.warn("Could not find link Id " + id.toString() + " in both maps.");
					} else{
						bw.write(id.toString());
						bw.write(",");
						bw.write(String.valueOf(baseMap.get(id)));
						bw.write(",");
						bw.write(String.valueOf(compareMap.get(id)));
						bw.write(",");
						double d = ((double) compareMap.get(id) - (double) baseMap.get(id)) / (double) baseMap.get(id);
						bw.write(String.format("%3.4f", d));
						bw.newLine();
					}
				}
			}finally{
				bw.close();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void writeComparisonToDbf(String filename){
		log.info("Writing comparison to " + filename);
		File f = new File(filename);
		if(f.exists()){
			log.warn("Will delete " + filename);
			boolean b = f.delete();
			if(!b){
				log.warn("Could delete " + filename);
			}
		}
		Field id = new Field("linkId", Type.NUMBER, 10, 0);
		Field base = new Field("baseCount", Type.NUMBER, 10, 0);
		Field comp = new Field("compCount", Type.NUMBER, 10, 0);
		Field change = new Field("change", Type.NUMBER, 6, 4);
		
		List<Field> fields = new ArrayList<Field>();
		fields.add(id);
		fields.add(base);
		fields.add(comp);
		fields.add(change);
		
		Map<String, Value> map = new HashMap<String, Value>();
		try {
			Table t = new Table(new File(filename), Version.DBASE_5, fields);
			t.open(IfNonExistent.CREATE);
			try{
				for(Id i : linkList){
					Value vId = new NumberValue(Integer.parseInt(i.toString()));
					Value vBase = new NumberValue(baseMap.get(i));
					Value vComp = new NumberValue(compareMap.get(i));
					double d = ((double) compareMap.get(i) - (double) baseMap.get(i)) / (double) baseMap.get(i);
					Value vChange= new NumberValue(d);
					log.info(Integer.parseInt(i.toString()) + ";" + baseMap.get(i) + ";" + compareMap.get(i) + ";" + d);
					map.put("linkId", vId);
					map.put("baseCount", vBase);
					map.put("compCount", vComp);
//					map.put("change", vChange);
					t.addRecord(new Record(map));
				}
			} catch (DbfLibException e) {
				e.printStackTrace();
			} finally{
				t.close();
			}
		} catch (InvalidFieldTypeException e) {
			e.printStackTrace();
		} catch (InvalidFieldLengthException e) {
			e.printStackTrace();
		} catch (CorruptedTableException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		log.info("Wrote comparison to " + filename);
	}
	
	/**
	 * Reading the {@link Link}{@link Id}s of those links that must be compared.
	 * <b><i>Note:</i></b> It is assumed that the file contains a single {@link Id}
	 * per line, and no header.
	 */
	private void readLinks(){
		linkList = new ArrayList<Id>();
		int counter = 0;
		try {
			BufferedReader br = IOUtils.getBufferedReader(this.linkIdFile.getAbsolutePath());
			try{
				String line = null;
				while((line = br.readLine()) != null){
					linkList.add(new IdImpl(line));
					counter++;
				}
			}finally{
				br.close();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}	
		log.info("Read " + counter + " link Ids from " + linkIdFile.getName());
	}
	
	
	private Map<Id, Integer> readLinkStatsOptionOne(File file){
		Map<Id, Integer> map = new HashMap<Id, Integer>();
		
		try {
			BufferedReader br = IOUtils.getBufferedReader(file.getAbsolutePath());
			try{
				/* Read the header. */
				String line = br.readLine();
				while((line = br.readLine()) != null){
					String[] entry = line.split("\t");
					Id id = new IdImpl(entry[0]);
					if(linkList.contains(id)){
						map.put(id, Integer.parseInt(entry[80]));						
					}
				}
			}finally{
				br.close();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return map;
	}





	/**
	 * Instantiates the class with three necessary arguments. All three files 
	 * are check to ensure they exist and are readable.
	 * @param baseFilename the absolute path of the <code>linkstats.txt</code> or 
	 * 		<code>linkstats.txt.gz</code> file that must be used as baseline. 
	 * @param compareFilename the absolute path of the <code>linkstats.txt</code> or 
	 * 		<code>linkstats.txt.gz</code> file that will be compared against the
	 * 		baseline file.
	 * @param linkIdFilename the absolute path of the text file that contains
	 * 		the <code>LinkId</code>s that will be compared.
	 * @throws FileNotFoundException
	 */
	public MyGantryComparator(String baseFilename, String compareFilename, String linkIdFilename) throws FileNotFoundException {
		File baseFile = new File(baseFilename);
		if(!baseFile.exists() || !baseFile.canRead()){
			throw new FileNotFoundException("Could not find " + baseFilename);
		}else{
			this.baseFile = baseFile;
		}
		
		File compareFile = new File(compareFilename);
		if(!compareFile.exists() || !compareFile.canRead()){
			throw new FileNotFoundException("Could not find " + compareFilename);
		}else{
			this.compareFile = compareFile;
		}
		
		File linkIdFile = new File(linkIdFilename);
		if(!linkIdFile.exists() || !linkIdFile.canRead()){
			throw new FileNotFoundException("Could not find " + linkIdFilename);
		}else{
			this.linkIdFile = linkIdFile;
		}
	}
	
	
	public Map<Id, Integer> getBaseMap() {
		return baseMap;
	}


	public Map<Id, Integer> getCompareMap() {
		return compareMap;
	}


	public List<Id> getLinkList() {
		return linkList;
	}






	
	

}
