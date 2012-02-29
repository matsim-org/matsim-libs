/* *********************************************************************** *
 * project: org.matsim.*
 * JOGLSetup.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package playground.gregor;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;

import org.apache.log4j.Logger;


public abstract class JOGLSetup {
	
	private static final Logger log = Logger.getLogger(JOGLSetup.class);
	
	private static final String JOGL_BASE = "/lib/jogl-1.1.1";
	
	private static  enum OS {Windows,Linux,MacOSX};
	private static boolean is64 = false;
	
	private static OS os;
	
	public static void configureJOGL() {
		log.info("starting JOGL autoconfigurator");
		determineOSandArch();
		String pwd = System.getProperty("user.dir");
		if (os == OS.Windows) {
			if (is64) {
				 try {
						addJNILibDir(pwd + JOGL_BASE + "/jogl-1.1.1-windows-amd64/lib");
					} catch (IOException e) {
						e.printStackTrace();
					}
			} else {
				 try {
						addJNILibDir(pwd + JOGL_BASE + "/jogl-1.1.1-windows-i586/lib");
					} catch (IOException e) {
						e.printStackTrace();
					}
			}
		} else if (os == OS.MacOSX) {
			 try {
				addJNILibDir(pwd + JOGL_BASE + "/jogl-1.1.1-macosx-universal/lib");
			} catch (IOException e) {
				e.printStackTrace();
			}
		
		} else if (os == OS.Linux) {
			if (is64) {
				 try {
						addJNILibDir(pwd + JOGL_BASE + "/jogl-1.1.1-linux-i586/lib");
					} catch (IOException e) {
						e.printStackTrace();
					}
			} else {
				 try {
						addJNILibDir(pwd + JOGL_BASE + "/jogl-1.1.1-linux-i586/lib");
					} catch (IOException e) {
						e.printStackTrace();
					}
			}
		}
		
		try {
			System.loadLibrary("jogl");
		} catch (Throwable e) {
			log.warn("JOGL configuration failed");
			e.printStackTrace();
		}
		log.info("JOGL configurated");
		
	}
	
	
	public static void addJNILibDir(String jNIDir) throws IOException {
		log.info("trying to add " + jNIDir + " to java.library.path");
	    try {
	        Field field = ClassLoader.class.getDeclaredField("usr_paths");
	        field.setAccessible(true);
	        String[] paths = (String[])field.get(null);
	        for (String path : paths) {
	            if (jNIDir.equals(path)) {
	            	log.warn("Directory " + jNIDir + " already in java.library.path");
	                return;
	            }
	        }
	        String[] tmp = new String[paths.length+1];
	        System.arraycopy(paths,0,tmp,0,paths.length);
	        tmp[paths.length] = jNIDir;
	        field.set(null,tmp);
	        System.setProperty("java.library.path", System.getProperty("java.library.path") + File.pathSeparator + jNIDir);
	    } catch (IllegalAccessException e) {
	        throw new IOException("Failed to get permissions to set java.library.path");
	    } catch (NoSuchFieldException e) {
	        throw new IOException("Failed to get field handle to set java.library.path");
	    }
	}

	
	private static void determineOSandArch() {
		String osStr = System.getProperty("os.name");
		if (osStr.toLowerCase().indexOf("linux") >= 0){
			os = OS.Linux;		
		} else if (osStr.toLowerCase().indexOf("mac") >= 0){
			os = OS.MacOSX;
		} else if (osStr.toLowerCase().indexOf("win") >= 0){
			os = OS.Windows;
		} else {
			throw new RuntimeException("Operation system " + osStr +  "not supported by JOGL!");
		}
		String archStr = System.getProperty("os.arch");
		
		if (archStr.indexOf("64") >= 0) {
			is64 = true;
		}
		log.info("Detected operation system:" + os);
		log.info("Detected JVM architecture:" + (is64?"x86_64" : "i386"));
	}
}
