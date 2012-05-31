package playground.pieter.ipf;

import java.util.HashMap;
import java.util.HashSet;

public class ControlTotal {
	//category - value hashmap for attributes, e.g. sex = f, income = 1000
	private HashMap<Category, String> attributes;
	private HashSet<Record> records;
	
	private void addRecord(Record r){
		records.add(r);
		checkIfRecordQualifies(r);
	}
	private void checkIfRecordQualifies(Record r) {
		// TODO Auto-generated method stub
		
	}
	
	public boolean equals(ControlTotal o) {
		// TODO Auto-generated method stub
		return true;
	}
}
