package org.matsim.application.analysis;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.application.CommandSpec;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.CsvOptions;
import org.matsim.application.options.InputOptions;
import org.matsim.application.options.OutputOptions;
import org.matsim.core.utils.io.IOUtils;
import picocli.CommandLine;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@CommandLine.Command(name = "log-file", description = "Analyses MATSim log files to gather run information.")
@CommandSpec(
	requires = "logfile.log",
	produces = {"run_info.csv", "memory_stats.csv", "runtime_stats.csv", "warnings.csv", "status.md"},
	group = "general"
)
public class LogFileAnalysis implements MATSimAppCommand {

	private static final Logger log = LogManager.getLogger(LogFileAnalysis.class);

	@CommandLine.Mixin
	private InputOptions input = InputOptions.ofCommand(LogFileAnalysis.class);
	@CommandLine.Mixin
	private OutputOptions output = OutputOptions.ofCommand(LogFileAnalysis.class);

	@CommandLine.Mixin
	private CsvOptions csv = new CsvOptions(CSVFormat.Predefined.Default);

	public static void main(String[] args) {
		new LogFileAnalysis().execute(args);
	}

	private static LocalDateTime parseDate(String line) {
		// Ignore milliseconds part
		int idx = line.indexOf(',');
		return LocalDateTime.parse(line.substring(0, idx));
	}

	@Override
	public Integer call() throws Exception {

		Pattern gbl = Pattern.compile(".+INFO Gbl:\\d+ (.+?):(.+)");
		Pattern mem = Pattern.compile(".+MemoryObserver:\\d+ used RAM: (\\d+) MB\\s+free: (\\d+) MB\\s+total: (\\d+) MB");
		Pattern warn = Pattern.compile(".+(WARN|ERROR) (\\S+(ConfigGroup|ConsistencyCheck).*):[0-9]+ (.+)");

		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

		Map<String, String> info = new LinkedHashMap<>();

		List<Memory> memory = new ArrayList<>();
		List<Iteration> iterations = new ArrayList<>();
		Set<Warning> warnings = new LinkedHashSet<>();

		String first = null;
		String last = null;

		LocalDateTime itBegin = null;

		try (BufferedReader reader = IOUtils.getBufferedReader(input.getPath())) {
			String line;
			while ((line = reader.readLine()) != null) {

				try {

					Matcher m = gbl.matcher(line);
					if (m.find()) {
						info.put(m.group(1).strip(), m.group(2).strip());
					}
					m = mem.matcher(line);
					if (m.find()) {
						memory.add(new Memory(parseDate(line), Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3))));
					}
					m = warn.matcher(line);
					if (m.find()) {
						warnings.add(new Warning(m.group(2), m.group(4)));
					}

					if (line.contains("### ITERATION")) {
						if (line.contains("BEGINS")) {
							itBegin = parseDate(line);
						} else if (line.contains("ENDS")) {
							iterations.add(new Iteration(itBegin, parseDate(line)));
						}
					}

				} catch (Exception e) {
					log.warn("Error processing line {}", line, e);
					continue;
				}

				if (first == null)
					first = line;

				last = line;
			}
		}

		// Ignored attributes
		info.remove("Thread performance");
		info.remove("used RAM");
		info.remove("### round time");

		if (first != null) {
			LocalDateTime start = parseDate(first);
			LocalDateTime end = parseDate(last);

			info.put("Start", formatter.format(start));
			info.put("End", formatter.format(end));
			info.put("Duration", DurationFormatUtils.formatDurationWords(Duration.between(start, end).toMillis(), true, true));
		}


		try (CSVPrinter printer = csv.createPrinter(output.getPath("run_info.csv"))) {
			printer.printRecord("info", "value");
			for (Map.Entry<String, String> e : info.entrySet()) {
				printer.printRecord(e.getKey(), e.getValue());
			}
		}

		try (CSVPrinter printer = csv.createPrinter(output.getPath("memory_stats.csv"))) {
			printer.printRecord("time", "used", "free");
			for (Memory m : memory) {
				printer.printRecord(formatter.format(m.date), m.used, m.free);
			}
		}

		try (CSVPrinter printer = csv.createPrinter(output.getPath("runtime_stats.csv"))) {
			printer.printRecord("Iteration", "seconds");
			for (int i = 0; i < iterations.size(); i++) {
				Iteration it = iterations.get(i);
				printer.printRecord(i, ChronoUnit.SECONDS.between(it.begin, it.end));
			}
		}

		try (CSVPrinter printer = csv.createPrinter(output.getPath("warnings.csv"))) {
			printer.printRecord("Module", "Message");
			for (Warning warning : warnings) {
				printer.printRecord(warning.module, warning.msg);
			}
		}

		try (BufferedWriter writer = Files.newBufferedWriter(output.getPath("status.md"))) {
			renderWarnings(writer, warnings);
		}

		return 0;
	}

	private void renderWarnings(BufferedWriter writer, Set<Warning> warnings) throws IOException {

		if (warnings.isEmpty()) {
			writer.write("<h3 class=\"no-warnings\">No warnings found ✅</h3>\n\n");
		} else {

			Map<String, List<Warning>> grouped = warnings.stream().collect(Collectors.groupingBy(w -> w.module, Collectors.toList()));

			for (Map.Entry<String, List<Warning>> e : grouped.entrySet()) {

				writer.write("#### " + e.getKey() + "\n\n");
				writer.write("```\n");
				for (Warning w : e.getValue()) {
					writer.write(w.msg + "\n");
				}

				writer.write("```\n");
			}
		}

		writer.write("""
			<style>
			.dash-row.row-warnings .dash-card-frame {
			    background: none;
			}
			.dash-row.row-warnings .no-warnings {
				color: #4BB543;
				font-weight: bold;
			}
			.dash-row.row-warnings .found-warnings {
				color: #ED4337;
				font-weight: bold;
			}
			.dash-row.row-warnings h4 {
				color: white;
				background: #6f5425;
				font-weight: bold;
				padding: 0.75rem 1.5rem;
				margin-top: 1rem;
				border-radius: 10px 10px 0 0;
			}
			.dash-row.row-warnings pre {
				background: #f8f3d6;
				color: #6f5425;
				border-radius: 0 0 10px 10px;
				white-space: pre-wrap;
			}
			</style>""");
	}

	private record Memory(LocalDateTime date, int used, int free, int total) {
	}

	private record Iteration(LocalDateTime begin, LocalDateTime end) {
	}

	private record Warning(String module, String msg) {

	}

}
