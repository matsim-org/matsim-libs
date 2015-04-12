/**
 * 
 */
package org.matsim.contrib.freightChainsFromTravelDiaries.pwvm;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

/**
 * Keeps a copy of the logbook repository from database in memory on the local machine  
 * 
 * @author schn_se
 *
 */
public class Pwvm_RAMRepository {
	
	private HashMap<Integer, Pwvm_Logbook> lrep = new HashMap<Integer, Pwvm_Logbook>();
	
	//private Vector<Pwvm_Logbook> logbooks = new Vector<Pwvm_Logbook>();
//	private Pwvm_Logbook [] lrep;
//	private int offset;

//	Pwvm_RAMRepository(int minId, int maxId) {
//		int size = maxId - minId + 1;
//		offset = minId;
//		lrep = new Pwvm_Logbook[size];
//		for (int i = 0; i < lrep.length; i++) {
//			lrep[i] = null;
//		}
//	}
	
	
	/**
	 * Returns a logbook with the given logbook Id
	 * @return
	 */
	public Pwvm_Logbook getLogbook(int logbookIdInDB) {
		return lrep.get(logbookIdInDB);
	}
	
//	/**
//	 * Returns a logbook with the given logbook Id
//	 * @return
//	 */
//	public Pwvm_Logbook getLogbook(int logbookIdInDB) {
//		int index = logbookIdInDB - offset;
//		if (lrep[index] != null)
//			return lrep[index];
//		else
//			return null;
//	}
	
	public void addLogbook(Pwvm_Logbook l) {
		lrep.put(l.getLogbookIdInDB(), l);
	}

//	public void addLogbook(Pwvm_Logbook l) {
//		int index = l.getLogbookIdInDB() - offset;
//		lrep[index] = l;
//	}



}
