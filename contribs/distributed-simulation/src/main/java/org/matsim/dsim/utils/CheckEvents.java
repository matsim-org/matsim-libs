package org.matsim.dsim.utils;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@CommandLine.Command(name = "check-events", mixinStandardHelpOptions = true, description = "Check events for consistency.")
public class CheckEvents implements Callable<Integer> {

	@CommandLine.Parameters(arity = "1..*", description = "Files to check.")
	private List<Path> files;

	public static void main(String[] args) {
		new CommandLine(new CheckEvents()).execute(args);
	}

	@Override
	public Integer call() throws Exception {

		List<Ev> events = readEvents(files);

		Object2IntMap<String> actCounter = new Object2IntOpenHashMap<>();
		Object2IntMap<String> linkCounter = new Object2IntOpenHashMap<>();

		Set<String> observed = new HashSet<>();

		for (Ev event : events) {

			if ((event.person() != null && event.person().startsWith("29420")) || (event.vehicle() != null && event.vehicle().startsWith("29420"))) {
				System.out.println(event);
			}

			if (event.type().equals("departure")) {
				actCounter.mergeInt(event.person(), 1, Integer::sum);
			} else if (event.type().equals("arrival")) {
				actCounter.mergeInt(event.person(), -1, Integer::sum);
			}

			if ((actCounter.getInt(event.person()) < 0 || actCounter.getInt(event.person()) > 1) && !observed.contains(event.person())) {
				System.err.println("Negative activity counter occurred.");
				System.err.println(event);
				observed.add(event.person());
			}

			if (event.type().equals("entered link")) {
				linkCounter.mergeInt(event.vehicle(), 1, Integer::sum);
			} else if (event.type().equals("left link")) {
				linkCounter.mergeInt(event.vehicle(), -1, Integer::sum);
			}

			if ((linkCounter.getInt(event.vehicle()) < -1 || actCounter.getInt(event.vehicle()) > 0) && !observed.contains(event.vehicle())) {
				System.err.println("Negative link counter occurred.");
				System.err.println(event);
				observed.add(event.vehicle());
			}
		}

		return 0;
	}

	private List<Ev> readEvents(List<Path> files) throws IOException {

		Pattern node = Pattern.compile("node([0-9]+)", Pattern.CASE_INSENSITIVE);
		Pattern time = Pattern.compile("time=\"(\\d+\\.\\d+)\"", Pattern.CASE_INSENSITIVE);
		Pattern type = Pattern.compile("type=\"([0-9a-z ]+)\"", Pattern.CASE_INSENSITIVE);
		Pattern vehicle = Pattern.compile("vehicle=\"([0-9a-z_]+)\"", Pattern.CASE_INSENSITIVE);
		Pattern link = Pattern.compile("link=\"([0-9a-z .#\\-]+)\"", Pattern.CASE_INSENSITIVE);
		Pattern person = Pattern.compile("(?:person|driver)=\"([0-9a-z_\\-]+)\"", Pattern.CASE_INSENSITIVE);

		List<Ev> lines = new ArrayList<>();

		for (Path file : files) {

			String id = extract(file.getFileName().toString(), node, false);

			try (Stream<String> linesStream = Files.lines(file)) {
				linesStream.forEach(line -> {
					String ts = extract(line, time, false);
					if (ts == null) {
						return;
					}

					double t = Double.parseDouble(ts);

					// Hard coded limit
					if (t > 12 * 3600)
						return;

					String ty = extract(line, type, true);
					String ve = extract(line, vehicle, false);
					String li = extract(line, link, false);
					String pe = extract(line, person, false);
					lines.add(new Ev(t, ty, ve, li, pe, id));
				});
			}
		}

		lines.sort(Comparator.comparingDouble(Ev::time));

		return lines;
	}

	private String extract(String line, Pattern pattern, boolean intern) {
		Matcher matcher = pattern.matcher(line);
		if (matcher.find()) {
			return intern ? matcher.group(1).intern() : matcher.group(1);
		}
		return null;
	}

	record Ev(double time, String type, String vehicle, String link, String person, String node) {
	}

}
