/*
 * Copyright 2015 Gunnar Flötteröd
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * contact: gunnar.floetteroed@abe.kth.se
 *
 */ 
package floetteroed.utilities.config;

import java.io.File;

/**
 * Completes paths given in a configuration file by
 * <ul>
 * <li>maintaining absolute paths;
 * <li>evaluating relative paths relative to the configuration file and
 * returning the resulting absolute path.
 * </ul>
 * 
 * @author Flötteröd
 * 
 */
public class ConfigPaths {

	// -------------------- MEMBERS --------------------

	private final File pathToConfig;

	// -------------------- CONSTRUCTION --------------------

	public ConfigPaths(final String pathToConfig) {
		if (pathToConfig == null) {
			throw new IllegalArgumentException("path to config is null");
		}
		final File absoluteFile = new File(pathToConfig).getAbsoluteFile();
		if (absoluteFile.isDirectory()) {
			this.pathToConfig = absoluteFile;
		} else if (absoluteFile.isFile()) {
			this.pathToConfig = absoluteFile.getParentFile();
		} else {
			throw new IllegalArgumentException(
					"path to config neither is neither an existing file nor an existing directory");
		}
	}

	// -------------------- IMPLEMENTATION --------------------

	public String getAbsolutePathToConfig() {
		return this.pathToConfig.getAbsolutePath();
	}

	public String getAbsoluteFileName(final String fileName) {
		final File originalFile = new File(fileName);
		if (originalFile.isAbsolute()) {
			return fileName;
		} else {
			return new File(this.pathToConfig, fileName).getAbsolutePath();
		}
	}
}
