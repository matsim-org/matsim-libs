package playground.mzilske.bvg09;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;

public class Hafas2VisumMapper {
	
	public static Map<Id, Id> getMappedLines(){
		Map<Id, Id> visum2HafasLineIds = new HashMap<Id, Id>();
		
		visum2HafasLineIds.put(new IdImpl("B-M44"), new IdImpl("M44  "));
		visum2HafasLineIds.put(new IdImpl("B-344"), new IdImpl("344  "));
		visum2HafasLineIds.put(new IdImpl("U-8"), new IdImpl("U8   "));
		
		return visum2HafasLineIds;
	}
	
	public static Map<Id, Map<Id,Id>> getHafas2VisumMap(){
		
		Map<Id, Map<Id,Id>> hafas2VisumMap = new HashMap<Id, Map<Id,Id>>();
				
		Map<Id, Id> tempMap = new HashMap<Id, Id>();
		tempMap.putAll(getHafas2visumM44());
		hafas2VisumMap.put(new IdImpl("M44  "), tempMap);
		
		tempMap = new HashMap<Id, Id>();
		tempMap.putAll(getHafas2visum344());
		hafas2VisumMap.put(new IdImpl("344  "), tempMap);
				
		tempMap = new HashMap<Id, Id>();
		tempMap.putAll(getHafas2visumU8());
		hafas2VisumMap.put(new IdImpl("U8   "), tempMap);
		
		return hafas2VisumMap;
	}

	private static Map<Id, Id> getHafas2visumM44() {
		Map<Id, Id> hafas2visum = new HashMap<Id, Id>();
		hafas2visum.put(new IdImpl("9081202"), new IdImpl("812020"));
		hafas2visum.put(new IdImpl("9081255"), new IdImpl("812550"));
		hafas2visum.put(new IdImpl("9081203"), new IdImpl("812030"));
		hafas2visum.put(new IdImpl("9081256"), new IdImpl("812560"));
		hafas2visum.put(new IdImpl("9081257"), new IdImpl("812570"));
		hafas2visum.put(new IdImpl("9081201"), new IdImpl("812013"));
		hafas2visum.put(new IdImpl("9080652"), new IdImpl("806520"));
		hafas2visum.put(new IdImpl("9080603"), new IdImpl("806030"));
		hafas2visum.put(new IdImpl("9080601"), new IdImpl("806010"));
		hafas2visum.put(new IdImpl("9080654"), new IdImpl("806540"));
		hafas2visum.put(new IdImpl("9080407"), new IdImpl("804070"));
		hafas2visum.put(new IdImpl("9080406"), new IdImpl("804060"));
		hafas2visum.put(new IdImpl("9080102"), new IdImpl("801020"));
		hafas2visum.put(new IdImpl("9080103"), new IdImpl("801030"));
		hafas2visum.put(new IdImpl("9080153"), new IdImpl("801530"));
		hafas2visum.put(new IdImpl("9080104"), new IdImpl("801040"));
		hafas2visum.put(new IdImpl("9079205"), new IdImpl("792050"));
		hafas2visum.put(new IdImpl("9079221"), new IdImpl("792200"));

		hafas2visum.put(new IdImpl("9080181"), VisumHafasScheduleMerger.REMOVE); // Betriebshof Britz
		return hafas2visum;
	}

	private static Map<Id, Id> getHafas2visum344() {
		Map<Id, Id> hafas2visum = new HashMap<Id, Id>();
		hafas2visum.put(new IdImpl("9079204"), new IdImpl("792040"));
		hafas2visum.put(new IdImpl("9079221"), new IdImpl("792200"));
		hafas2visum.put(new IdImpl("9079201"), new IdImpl("792013"));
		hafas2visum.put(new IdImpl("9079203"), new IdImpl("792030"));
		hafas2visum.put(new IdImpl("9079202"), new IdImpl("792023"));
		hafas2visum.put(new IdImpl("9079291"), new IdImpl("792910"));
		hafas2visum.put(new IdImpl("9078106"), new IdImpl("781060"));
		hafas2visum.put(new IdImpl("9078104"), new IdImpl("781040"));
		hafas2visum.put(new IdImpl("9078101"), new IdImpl("781015"));
		return hafas2visum;
	}

	private static Map<Id, Id> getHafas2visumU8() {
		Map<Id, Id> hafas2visum = new HashMap<Id, Id>();
		hafas2visum.put(new IdImpl("9096101"), new IdImpl("964072"));
		hafas2visum.put(new IdImpl("9096410"), new IdImpl("964100"));
		hafas2visum.put(new IdImpl("9096458"), new IdImpl("964580"));
		hafas2visum.put(new IdImpl("9086160"), new IdImpl("861600"));
		hafas2visum.put(new IdImpl("9085104"), new IdImpl("851040"));
		hafas2visum.put(new IdImpl("9085203"), new IdImpl("852030"));
		hafas2visum.put(new IdImpl("9085202"), new IdImpl("852020"));
		hafas2visum.put(new IdImpl("9009202"), new IdImpl("92025"));
		hafas2visum.put(new IdImpl("9009203"), new IdImpl("92030"));
		hafas2visum.put(new IdImpl("9007102"), new IdImpl("71023"));
		hafas2visum.put(new IdImpl("9007103"), new IdImpl("71030"));
		hafas2visum.put(new IdImpl("9007110"), new IdImpl("71100"));
		hafas2visum.put(new IdImpl("9100023"), new IdImpl("1000230"));
		hafas2visum.put(new IdImpl("9100051"), new IdImpl("1000510"));
		hafas2visum.put(new IdImpl("9100003"), new IdImpl("1000034"));
		hafas2visum.put(new IdImpl("9100004"), new IdImpl("1000043"));
		hafas2visum.put(new IdImpl("9100008"), new IdImpl("1000080"));
		hafas2visum.put(new IdImpl("9013101"), new IdImpl("131010"));
		hafas2visum.put(new IdImpl("9013102"), new IdImpl("131020"));
		hafas2visum.put(new IdImpl("9016201"), new IdImpl("162010"));
		hafas2visum.put(new IdImpl("9078101"), new IdImpl("781013"));
		hafas2visum.put(new IdImpl("9079202"), new IdImpl("792020"));
		hafas2visum.put(new IdImpl("9079201"), new IdImpl("792010"));
		hafas2visum.put(new IdImpl("9079221"), new IdImpl("792215"));
		return hafas2visum;
	}	

}
