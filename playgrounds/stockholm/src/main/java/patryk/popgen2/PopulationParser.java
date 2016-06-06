package patryk.popgen2;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;

import gunnar.ihop2.regent.demandreading.Zone;

public class PopulationParser implements TabularFileHandler {
	
	private final HashMap<String, Zone> zones;
	private final ArrayList<ParsedPerson> persons;
	private boolean firstRow;
	
	public PopulationParser() {
		this.zones = new HashMap<>();
		this.persons = new ArrayList<>();
	}
	
	public void read(String filename) {
		TabularFileParserConfig config = new TabularFileParserConfig();
		config.setDelimiterRegex(";");
		config.setFileName(filename);
		config.setCommentRegex("#");
		
		firstRow = true; 
		
		TabularFileParser parser = new TabularFileParser();
		parser.parse(config, this);
	}

	@Override
	public void startRow(String[] row) {

		if (firstRow == true) {
			firstRow = false;
			return;
		}
		
		String id = row[0];
		int birthYear = Integer.parseInt(row[1]);
		int currentYear = Calendar.getInstance().get(Calendar.YEAR);
		int age = currentYear - birthYear;
		int sex = Integer.parseInt(row[2]);
		int income = Integer.parseInt(row[3]);
		
		String homeZone = row[4];
		String workZone = row[5];
		putInHashMap(homeZone);
		putInHashMap(workZone);
		
		String mode = row[6];
		int housingType = Integer.parseInt(row[8]);
		
		ParsedPerson person = new ParsedPerson(id, homeZone, workZone, mode, 
				age, income, sex, housingType);
		
		persons.add(person);
	}
	
	public HashMap<String, Zone> getZones() {
		return zones;
	}
	
	public ArrayList<ParsedPerson> getParsedPersons() {
		return persons;
	}
	
	public void putInHashMap(String zoneId) {
		if(!zones.containsKey(zoneId)) {
			zones.put(zoneId, new Zone(zoneId));
		}
	}

}
