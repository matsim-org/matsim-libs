package org.matsim.simwrapper.DbViewer;

import com.google.inject.Inject;
import org.agrona.concurrent.Agent;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.parquet.example.data.Group;
import org.apache.parquet.example.data.simple.SimpleGroupFactory;
import org.apache.parquet.hadoop.ParquetWriter;
import org.apache.parquet.hadoop.example.ExampleParquetWriter;
import org.apache.parquet.hadoop.metadata.CompressionCodecName;
import org.apache.parquet.hadoop.util.HadoopOutputFile;
import org.apache.parquet.schema.MessageType;
import org.apache.parquet.schema.MessageTypeParser;
import org.apache.parquet.schema.PrimitiveType;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.population.PopulationUtils;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AgentTable {

	ParquetWriter<Group> writerAgents;
	private Integer agentCount = 0;
	private final Population population;
	private final static Logger log = LogManager.getLogger(AgentTable.class);
	private String outputDirectory;

	public AgentTable(Population population, String outputDirectory)  {
		this.population = population;
		this.outputDirectory = outputDirectory;
	}
	public void run() throws IOException, SQLException {

		Map<String, Object> attributes = this.population.getPersons().values().iterator().next().getAttributes().getAsMap();
		String parquetSchemaString = generateParquetSchema("persons", attributes);

		MessageType schemaAgents = MessageTypeParser.parseMessageType(parquetSchemaString);
		SimpleGroupFactory factoryAgents = new SimpleGroupFactory(schemaAgents);

		Configuration conf = new Configuration();
		Path pathAgents = new Path(this.outputDirectory + "/agents.parquet");
		FileSystem fs = FileSystem.get(new Configuration());

		if (fs.exists(pathAgents)) {
			fs.delete(pathAgents, true);
		}

		try {
			this.writerAgents = ExampleParquetWriter
				.builder(HadoopOutputFile.fromPath(pathAgents, conf))
				.withType(schemaAgents)
				.withConf(conf)
				.withCompressionCodec(CompressionCodecName.ZSTD)
				.withDictionaryEncoding(true)
				.build();
		} catch (IOException e) {
			log.error("Failed to open parquet file", e);
		}

		for (Person person : this.population.getPersons().values()) {
			try {

				Group group = factoryAgents.newGroup();
				group.append("agent_id", person.getId().toString()); // ← add this


				for (Map.Entry<String, Object> entry : person.getAttributes().getAsMap().entrySet()) {
					if (person.getAttributes().getAsMap().size() != attributes.size()) {
						log.warn("Person {} has {} attributes, expected {}",
							person.getId(), person.getAttributes().getAsMap().size(), attributes.size());
					}
					String col = entry.getKey().toLowerCase().replace(" ", "_");
					Object value = entry.getValue();

					if (value instanceof Boolean) {
						group.append(col, (Boolean) value);
					} else if (value instanceof Integer) {
						group.append(col, (Integer) value);
					} else if (value instanceof Double) {
						group.append(col, (Double) value);
					} else if (value instanceof Float) {
						group.append(col, (Float) value);
					} else {
						group.append(col, value.toString());
					}
				}

				this.writerAgents.write(group);
				this.agentCount++;
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		finish();
	}


	private static String toSqlType(Object value) {
		if (value instanceof Integer)        return "INT";
		if (value instanceof String)           return "BIGINT";
		if (value instanceof Double
			|| value instanceof Float)          return "DOUBLE";
		if (value instanceof Boolean)        return "BOOLEAN";
		return "VARCHAR(255)";
	}

	private static PrimitiveType.PrimitiveTypeName toParquetType(Object value) {
		if (value instanceof Integer) return PrimitiveType.PrimitiveTypeName.INT32;
		if (value instanceof String)  return PrimitiveType.PrimitiveTypeName.BINARY;
		if (value instanceof Double)  return PrimitiveType.PrimitiveTypeName.DOUBLE;
		if (value instanceof Boolean) return PrimitiveType.PrimitiveTypeName.BOOLEAN;
		if (value instanceof Float)   return PrimitiveType.PrimitiveTypeName.FLOAT;
		return PrimitiveType.PrimitiveTypeName.BINARY; // fallback
	}

	public String generateParquetSchema(String tableName, Map<String, Object> attributes) {
		StringBuilder sql = new StringBuilder();
		sql.append("message ").append(tableName).append(" {\n");
		sql.append("required binary agent_id (UTF8);\n"); // optional surrogate key

		List<String> columns = new ArrayList<>();
		for (Map.Entry<String, Object> entry : attributes.entrySet()) {
			String colName = entry.getKey().toLowerCase().replace(" ", "_");
			PrimitiveType.PrimitiveTypeName parquetType = toParquetType(entry.getValue());
			if (entry.getValue() instanceof String) {
				columns.add("required " + parquetType + " " + colName + " (UTF8);");
			} else {
				columns.add("required "  + parquetType + " "  + colName + ";");
			}

		}

		sql.append(String.join("\n", columns));
		sql.append("\n}");
		return sql.toString();
	}

	public void finish() throws SQLException {
		log.info("finish() called — agentCount={}", this.agentCount);
		try {
			this.writerAgents.close();
		}  catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}


}
