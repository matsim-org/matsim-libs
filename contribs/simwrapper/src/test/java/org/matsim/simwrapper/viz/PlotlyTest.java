package org.matsim.simwrapper.viz;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import org.junit.Before;
import org.junit.Test;
import tech.tablesaw.plotly.traces.BarTrace;

public class PlotlyTest {

	private ObjectWriter writer;

	@Before
	public void setUp() throws Exception {
		ObjectMapper mapper = new ObjectMapper(new YAMLFactory()
				.disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
				.enable(YAMLGenerator.Feature.MINIMIZE_QUOTES))
				.setSerializationInclusion(JsonInclude.Include.NON_NULL)
				.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);

		writer = mapper.writerFor(Plotly.class);
	}

	@Test
	public void yaml() throws JsonProcessingException {


		Object[] x = {"sheep", "cows", "fish", "tree sloths"};
		double[] y = {1, 4, 9, 16};

		BarTrace trace = BarTrace.builder(x, y).build();

		Plotly plot = new Plotly();

		plot.addTrace(trace, Plotly.data("path to resources"));


		System.out.println(writer.writeValueAsString(plot));

	}
}
