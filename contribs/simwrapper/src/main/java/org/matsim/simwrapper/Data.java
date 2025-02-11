package org.matsim.simwrapper;

import org.apache.commons.io.FilenameUtils;
import org.matsim.application.CommandRunner;
import org.matsim.application.MATSimAppCommand;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.utils.io.IOUtils;

import javax.annotation.Nullable;
import java.io.File;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Manages and holds data needed for {@link org.matsim.simwrapper.SimWrapper}.
 */
public final class Data {

	/**
	 * Context of the loaded configuration.
	 */
	private final URL configPath;
	/**
	 * Configured Simwrapper config.
	 */
	private final SimWrapperConfigGroup config;

	/**
	 * Maps context to command runners.
	 */
	private final Map<String, CommandRunner> runners;

	/**
	 * Resources that needs to be copied.
	 */
	private final Map<Path, URL> resources;

	/**
	 * Global args that are added to all commands.
	 */
	private final Map<Class<? extends MATSimAppCommand>, String[]> globalArgs = new LinkedHashMap<>();

	/**
	 * Cache for resolved resources.
	 */
	private final Map<String, URL> resolveCache = new LinkedHashMap<>();

	/**
	 * The output directory.
	 */
	private Path path;
	private CommandRunner currentContext;
	private SimWrapperConfigGroup.ContextParams context;

	Data(URL contex, SimWrapperConfigGroup config) {
		this.configPath = contex;
		// path needed, but not available yet
		this.config = config;
		this.runners = new LinkedHashMap<>();
		this.resources = new LinkedHashMap<>();
		this.currentContext = runners.computeIfAbsent("", CommandRunner::new);
		this.context = config.get("");
	}

	private Data(Data other, String context) {
		this.configPath = other.configPath;
		this.config = other.config;
		this.runners = other.runners;
		this.resources = other.resources;
		this.path = other.path;
		this.setCurrentContext(context);
	}

	/**
	 * Returns the config that was given to {@link SimWrapper}.
	 */
	public SimWrapperConfigGroup config() {
		return config;
	}

	/**
	 * The current active context configuration.
	 */
	public SimWrapperConfigGroup.ContextParams context() {
		return context;
	}

	/**
	 * Set the default args that will be used for a specific command.
	 */
	public Data defaultArgs(Class<? extends MATSimAppCommand> command, String... args) {
		currentContext.add(command, args);
		return this;
	}

	/**
	 * Set shp file for specific command, otherwise default shp will be used.
	 */
	public Data shp(Class<? extends MATSimAppCommand> command, String path) {

		URL resolved = resolve(path);
		if (resolved == null)
			throw new IllegalArgumentException("Resource '" + path + "' not found!");

		// Use absolute path if a file was resolved
		try {
			URI uri = resolved.toURI();
			if (uri.getScheme().equals("file")) {
				path = new File(uri).getAbsolutePath();
			} else {
				path = uri.toString();
			}

			currentContext.setShp(command, path);
			return this;

		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Reference to a file within the runs output directory.
	 *
	 * @param first name of the file or first part of the path
	 * @param path  don't use path separators, but multiple arguments
	 */
	public String output(String first, String... path) {

		// Must use unix separators and not use Path because * might be used as part of the path
		StringBuilder p = new StringBuilder(first);
		for (String s : path) {
			p.append("/");
			p.append(s);
		}

		return p.toString();
	}

	/**
	 * Uses a command to compute the required output.
	 *
	 * @param command the command to be executed
	 * @param file    name of the produced output file
	 */
	public String compute(Class<? extends MATSimAppCommand> command, String file, String... args) {
		currentContext.add(command, args);
		Path p = currentContext.getRequiredPath(command, file);

		if (file.contains("%s"))
			throw new IllegalArgumentException("Placeholder in file name not supported. Use computeWithPlaceholder instead.");

		// Relative path from the simulation output
		return this.getUnixPath(this.path.getParent().relativize(p));
	}

	/**
	 * Uses a command to compute the required output. This can be used for commands that produce multiple outputs with a placeholder in the name.
	 *
	 * @param placeholder placeholder to be replaced in the file name
	 * @see #compute(Class, String, String...)
	 */
	public String computeWithPlaceholder(Class<? extends MATSimAppCommand> command, String file, String placeholder, String... args) {
		currentContext.add(command, args);
		Path p = currentContext.getRequiredPath(command, file, placeholder);

		// Relative path from the simulation output
		return this.getUnixPath(this.path.getParent().relativize(p));

	}

	public String subcommand(String command, String file) {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	/**
	 * Copies and references file bundled in the classpath.
	 *
	 * @param name name of the resource
	 */
	public String resource(String name) {

		String path = resolveResource(name, true);

		// Handle shape files separately, copy additional files that are known to belong to shp files
		if (name.endsWith(".shp")) {
			resolveResource(name.replace(".shp", ".cpg"), false);
			resolveResource(name.replace(".shp", ".dbf"), false);
			resolveResource(name.replace(".shp", ".qix"), false);
			resolveResource(name.replace(".shp", ".qmd"), false);
			resolveResource(name.replace(".shp", ".prj"), false);
			resolveResource(name.replace(".shp", ".shx"), false);
		}

		return path;
	}

	private URL resolve(String name) {
		if (resolveCache.containsKey(name))
			return resolveCache.get(name);

		URL resource = null;
		try {
			resource = IOUtils.resolveFileOrResource(name);
		} catch (UncheckedIOException e) {
			// Nothing to do
		}

		// Try to prefix / automatically
		if (resource == null) {
			resource = this.getClass().getResource("/" + name);
		}

		// Load configs relative to the config file
		if (resource == null) {
			resource = ConfigGroup.getInputFileURL(configPath, name);
		}

		// If a file is expected, immediately check if it exists
		if (resource != null && resource.getProtocol().equals("file")) {

			try {
				File f = new File(resource.toURI());
				if (!f.exists())
					resource = null;

			} catch (URISyntaxException e) {
				throw new RuntimeException(e);
			}
		}

		resolveCache.put(name, resource);
		return resource;
	}

	private String resolveResource(String name, boolean required) {
		URL resource = resolve(name);
		if (resource == null) {
			if (required)
				throw new IllegalArgumentException("Resource '" + name + "' not found!");
			else
				return null;
		}

		String baseName = FilenameUtils.getName(resource.getPath());

		Path baseDir;
		if (currentContext.getName().isBlank())
			baseDir = this.path.resolve("resources");
		else
			baseDir = this.path.resolve("resources-" + currentContext.getName());

		// Final path where resource should be copied
		Path resolved = baseDir.resolve(baseName);

		try {
			if (resources.containsKey(resolved) && !resources.get(resolved).toURI().equals(resource.toURI()))
				throw new IllegalArgumentException(String.format("Resource '%s' was already mapped to resource '%s'. ", name, resources.get(resolved)));

		} catch (URISyntaxException e) {
			throw new RuntimeException("Illegal URL", e);
		}

		resources.put(resolved, resource);
		return this.getUnixPath(this.path.getParent().relativize(resolved));
	}

	/**
	 * Copies an input file (which can be local or an url) to the output directory. This method is intended to copy input for analysis classes.
	 */
	public String copyInput(URL file, String baseFolder) {

		String baseName = FilenameUtils.getName(file.getPath());

		Path baseDir;
		if (currentContext.getName().isBlank())
			baseDir = this.path.resolve(baseFolder);
		else
			baseDir = this.path.resolve(baseFolder + "-" + currentContext.getName());

		Path resolved = baseDir.resolve(baseName);

		try {
			if (resources.containsKey(resolved) && !resources.get(resolved).toURI().equals(file.toURI()))
				throw new IllegalArgumentException(String.format("File '%s' was already mapped to url '%s'. ", file, resources.get(resolved)));

		} catch (URISyntaxException e) {
			throw new RuntimeException("Illegal URL", e);
		}

		resources.put(resolved, file);
		return Paths.get("").relativize(resolved).toString();
	}

	/**
	 * Returns the unix path. Otherwise, paths might be Windows paths, which are not compatible to simwrapper.
	 */
	private String getUnixPath(Path p) {
		return FilenameUtils.separatorsToUnix(p.toString());
	}

	/**
	 * Switch to a different context, which can hold different arguments and shp options.
	 * This changes the state of the underlying object. The pubic API will return a new object and leave the original unchanged.
	 *
	 * @see #withContext(String)
	 */
	void setCurrentContext(String name) {
		currentContext = runners.computeIfAbsent(name, CommandRunner::new);
		currentContext.setOutput(path);
		context = config.get(name);
	}

	/**
	 * Use the default context for this data object.
	 */
	public Data withDefaultContext() {
		return withContext(null);
	}

	/**
	 * Change the context of this data object.
	 */
	public Data withContext(@Nullable String name) {
		if (name == null)
			name = "";

		return new Data(this, name);
	}


	/**
	 * Adds arguments to the given command in all contexts. This can be used to globally modify the behaviour of a command.
	 */
	public Data addGlobalArgs(Class<? extends MATSimAppCommand> command, String... args) {

		if (globalArgs.containsKey(command)) {
			String[] oldArgs = globalArgs.get(command);
			String[] newArgs = new String[oldArgs.length + args.length];
			System.arraycopy(oldArgs, 0, newArgs, 0, oldArgs.length);
			System.arraycopy(args, 0, newArgs, oldArgs.length, args.length);
			globalArgs.put(command, newArgs);
		} else {
			globalArgs.put(command, Arrays.copyOf(args, args.length));
		}

		return this;
	}


	void setPath(Path path) {
		this.path = path;
	}

	Map<String, CommandRunner> getRunners() {
		return runners;
	}

	Map<Path, URL> getResources() {
		return resources;
	}

	Map<Class<? extends MATSimAppCommand>, String[]> getGlobalArgs() {
		return globalArgs;
	}

}
