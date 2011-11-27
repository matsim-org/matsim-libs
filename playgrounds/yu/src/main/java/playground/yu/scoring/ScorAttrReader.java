/**
 * 
 */
package playground.yu.scoring;

import java.util.Map;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;

/**
 * reads scoring function attributes from scorAttr file, and saves them in
 * custom attributes of {@code Plan}
 * 
 * @author yu
 * 
 */
public class ScorAttrReader implements TabularFileHandler {
	private final TabularFileParserConfig parserConfig;
	private Population population=null;
	private String[] attrNames = null;

	public ScorAttrReader(String scorAttrFilename, Population population) {
		parserConfig = new TabularFileParserConfig();
		parserConfig.setFileName(scorAttrFilename);
		parserConfig.setDelimiterRegex("\t");
		//		parserConfig.setStartTag("PersonId");
		this.population = population;
	}

	public void parser() {
		TabularFileParser parser = new TabularFileParser();
		parser.parse(parserConfig, this);
	}

	@Override
	public void startRow(String[] row) {
		if (!row[0].equals("PersonId")) {
			if (attrNames == null) {
				throw new RuntimeException(
				"There is not yet attributes name collection, was Filehead not read?");
			}

			Person person = population.getPersons().get(new IdImpl(row[0]));
			Plan plan = person.getPlans().get(Integer.parseInt(row[1]));
			Map<String, Object> attrs = plan.getCustomAttributes();
			for (int i = 2; i < row.length; i++) {
				attrs.put(attrNames[i], Double.parseDouble(row[i]));
			}
		} else/* is started */{
			attrNames = row;// first 2 element don't belong to
			// attrNames
		}
	}
}
