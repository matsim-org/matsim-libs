package patryk.populationgeneration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;

public class StockholmPendlerParser implements TabularFileHandler {

	private final Map<String, County> counties;
	private final Map<String, PendlerRelation> relations;

	private boolean firstRow = false;
	private int noLicenseOrCar;
	private int noWorkingPlace;
	private int linesProcessed;

	public StockholmPendlerParser() {
		this.relations = new HashMap<>();
		this.counties = new HashMap<>();
	}

	public void read(String fileName) {
		firstRow = true;
		TabularFileParserConfig config = new TabularFileParserConfig();
		config.setDelimiterRegex(";");
		config.setFileName(fileName);
		config.setCommentRegex("#");
		new TabularFileParser().parse(config, this);
	}

	@Override
	public void startRow(String[] row) {
		if (firstRow) {
			firstRow = false;
			return;
		}

		County home = getCounty(row[4]);
		County work = getCounty(row[5]);
		
		int birthYear = Integer.parseInt(row[1]);
		int income = Integer.parseInt(row[3]);
		int housingType = Integer.parseInt(row[8]);
		//String mode = row[?];
		
		boolean hasDriversLicence = toBoolean(row[7]);
		boolean carAvailable = toBoolean(row[12]);
		
		if(work == null || home == null){
			noWorkingPlace++;
		}else if(!hasDriversLicence || !carAvailable) {
			noLicenseOrCar++;
		}else {
			String key = new PendlerRelation(home, work).getRelationKey();

			PendlerRelation relation = relations.get(key);
			if (relation == null) {
				relation = new PendlerRelation(home, work);
				relations.put(key, relation);
			}
			relation.addNumber(1);
			relation.addAttributes(birthYear, income, housingType);
		}
		
		linesProcessed++;
	}

	private boolean toBoolean(String string) {
		return string.equals("1");
	}

	private County getCounty(String key) {
		if (key.equals("0")) {
			return null;
		}

		County c = counties.get(key);
		if (c == null) {
			c = new County(key);
			counties.put(key, c);
		}
		return c;
	}

	public List<PendlerRelation> getRelations() {
		return new ArrayList<>(relations.values());
	}

	public List<County> getCounties() {
		return new ArrayList<>(counties.values());
	}

	public int getNumberOfPeopleWithoutLicenseOrCar() {
		return noLicenseOrCar;
	}

	public int getNumberOfPeopleWithoutWorkingPlace() {
		return noWorkingPlace;
	}
	
	public int getLinesProcessed() {
		return linesProcessed;
	}

}