package org.matsim.simwrapper.viz;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.simwrapper.ComponentMixin;
import org.matsim.testcases.MatsimTestUtils;
import tech.tablesaw.plotly.components.Line;
import tech.tablesaw.plotly.components.*;
import tech.tablesaw.plotly.components.change.ChangeLine;
import tech.tablesaw.plotly.components.change.Decreasing;
import tech.tablesaw.plotly.traces.BarTrace;
import tech.tablesaw.plotly.traces.HistogramTrace;
import tech.tablesaw.plotly.traces.ScatterTrace;

import java.io.File;
import java.io.IOException;

public class PlotlyTest {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	private ObjectWriter writer;

	public static ObjectWriter createWriter() {
		ObjectMapper mapper = new ObjectMapper(new YAMLFactory()
				.disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
				.enable(YAMLGenerator.Feature.MINIMIZE_QUOTES))
				.setSerializationInclusion(JsonInclude.Include.NON_NULL)
				.enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING)
				.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
				.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);

		mapper.registerModule(new JavaTimeModule());
		mapper.addMixIn(Component.class, ComponentMixin.class);

		return mapper.writerFor(Plotly.class);
	}

	@BeforeEach
	public void setUp() throws Exception {
		writer = createWriter();
	}

	@Test
	void inline() throws IOException {

		Object[] x = {"sheep", "cows", "fish", "tree sloths"};
		double[] y = {1, 4, 9, 16};

		BarTrace trace = BarTrace.builder(x, y).build();

		Plotly plot = new Plotly();

		plot.addTrace(trace);

		String value = writer.writeValueAsString(plot);

		Assertions.assertThat(new File(utils.getClassInputDirectory(), "inline.yaml"))
				.hasContent(value);

	}

	@Test
	void data() throws IOException {

		ScatterTrace.ScatterBuilder trace = ScatterTrace.builder(Plotly.INPUT, Plotly.INPUT)
				.text(Plotly.TEXT_INPUT)
				.marker(Marker.builder()
						.color(Plotly.TEXT_INPUT)
						.size(Plotly.INPUT)
						.build());

		Plotly plotly = new Plotly();

		plotly.addTrace(trace.build(), plotly.addDataset("test.csv").mapping()
				.x("xColumn")
				.y("yColumn")
				.text("labelColumn")
				.size("sizeColumn")
				.opacity("opacityColumn")
				.color("colorColumn")
		);

		Assertions.assertThat(new File(utils.getClassInputDirectory(), "data.yaml"))
				.hasContent(writer.writeValueAsString(plotly));
	}

	@Test
	void multiple() throws IOException {


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

		plot.addTrace(scatter);
		plot.addTrace(line);
		plot.addTrace(hist);

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

		String value = writer.writeValueAsString(plot);

		Assertions.assertThat(new File(utils.getClassInputDirectory(), "multiple.yaml"))
				.hasContent(value);

	}

}
