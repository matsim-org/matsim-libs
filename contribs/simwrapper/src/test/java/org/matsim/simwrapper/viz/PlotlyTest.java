package org.matsim.simwrapper.viz;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import tech.tablesaw.plotly.components.*;
import tech.tablesaw.plotly.components.Line;
import tech.tablesaw.plotly.components.change.ChangeLine;
import tech.tablesaw.plotly.components.change.Decreasing;
import tech.tablesaw.plotly.components.threeD.Camera;
import tech.tablesaw.plotly.components.threeD.Scene;
import tech.tablesaw.plotly.traces.BarTrace;
import tech.tablesaw.plotly.traces.HistogramTrace;
import tech.tablesaw.plotly.traces.ScatterTrace;

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
	public void inline() throws JsonProcessingException {

		Object[] x = {"sheep", "cows", "fish", "tree sloths"};
		double[] y = {1, 4, 9, 16};

		BarTrace trace = BarTrace.builder(x, y).build();

		Plotly plot = new Plotly();

		plot.addTrace(trace, Plotly.inline());

		writer.writeValueAsString(plot);

	}

	@Test
	public void data() throws JsonProcessingException {


		BarTrace trace = BarTrace.builder(Plotly.OBJ_INPUT, Plotly.INPUT).build();

		Plotly plot = new Plotly();

		plot.addTrace(trace, Plotly.data("ref.csv").x("values").y("names"));

		writer.writeValueAsString(plot);

	}

	@Test
	public void multiple() throws JsonProcessingException {


		ScatterTrace scatter = ScatterTrace.builder(Plotly.INPUT, Plotly.INPUT)
				.line(Line.builder().dash(Line.Dash.LONG_DASH).build())
				.marker(Marker.builder().showScale(true).symbol(Symbol.BOWTIE).build())
				.yAxis(ScatterTrace.YAxis.Y)
				.build();

		ScatterTrace line = ScatterTrace.builder(Plotly.INPUT, Plotly.INPUT)
				.line(Line.builder().width(1).smoothing(0.5).build())
				.decreasing(Decreasing.builder().changeLine(ChangeLine.builder().width(10).build()).build())
				.hoverLabel(HoverLabel.builder().font(Font.builder().family(Font.Family.OPEN_SANS).build()).build())
				.build();

		HistogramTrace hist = HistogramTrace.builder(Plotly.INPUT)
				.autoBinX(true)
				.histFunc(HistogramTrace.HistFunc.COUNT)
				.histNorm(HistogramTrace.HistNorm.DENSITY)
				.build();

		Plotly plot = new Plotly();

		plot.addTrace(scatter, Plotly.inline());
		plot.addTrace(line, Plotly.inline());
		plot.addTrace(hist, Plotly.inline());

		plot.layout = Layout.builder()
				.hoverDistance(5)
				.grid(Grid.builder().pattern(Grid.Pattern.COUPLED).build())
				.paperBgColor("black")
				.margin(Margin.builder().autoExpand(true).left(5).build())
				.build();

		plot.config = Config.builder()
				.displayLogo(false)
				.responsive(true)
				.build();

		String content = writer.writeValueAsString(plot);

		System.out.println(content);


		Assertions.fail("Implement");

	}

}
