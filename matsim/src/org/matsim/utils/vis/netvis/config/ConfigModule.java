/* *********************************************************************** *
 * project: org.matsim.*
 * ConfigModule.java
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

package org.matsim.utils.vis.netvis.config;

import java.io.File;
import java.util.Map;
import java.util.TreeMap;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

/**
 * @author gunnar
 */
public class ConfigModule extends DefaultHandler implements ConfigModuleI {

    // -------------------- CLASS VARIABLES --------------------

  public static final String MODULE_CLASS_ATTR = "class";

    public static final String PARAM_ELEM = "param";

    public static final String PARAM_NAME_ATTR = "name";

    public static final String PARAM_VALUE_ATTR = "value";

    // -------------------- INSTANCE VARIABLES --------------------

    /**
     * Only write to this via <code>set(String,String)</code> so subclasses
     * can cache the content!
     */
    private final Map<String, String> content = new TreeMap<String, String>();

    private final String moduleName;

    private File rootFile;

    // -------------------- CONSTRUCTION --------------------

    public ConfigModule(String moduleName, String fileName) {
        this.moduleName = moduleName;
        File cfgfile = new File(fileName);
//        this.setRootFile(cfgfile.getParent());
        read(fileName);
        completeCache();
    }

    public ConfigModule(String moduleName) {
        this.moduleName = moduleName;
        this.rootFile = null;
        completeCache();
    }

    // -------------------- SETTERS --------------------

    public void set(String name, String value) {
        content.put(name.toLowerCase(), value);
        cache(name, value);
    }

    public void setRootFile(String path) {
        if (path != null)
            this.rootFile = new File(path);
        else
            this.rootFile = null;
    }

    // -------------------- HOOKS FOR SUBCLASSING --------------------

    /**
     * To be overridden by subclasses that perform more specific checks.
     * Indicates if a subclass configuration module is "complete", depending on
     * its specific purpose.
     *
     * @return <code>true</code> by default
     */
    public boolean isComplete() {
        return true;
    }

    /**
     * To be overriden by subclasses that do caching. Initializes cache with
     * default values. This function is called at last by the constructor of
     * this class. Does nothing by default.
     */
    protected void completeCache() {
    }

    /**
     * To be overridden by subclasses that cache values. Is called by
     * <code>set(String,String)</code>. Does nothing by default.
     *
     * @param name
     *            name of the parameter to be cached
     * @param value
     *            value of the parameter to be cached
     */
    protected void cache(String name, String value) {
    }

    // -------------------- GETTERS --------------------

    public String get(String name) {
        return content.get(name.toLowerCase());
    }

    public String getString(String name) {
        return get(name);
    }

    public long getLong(String name) {
        return Long.parseLong(get(name));
    }

    public int getInt(String name) {
        return Integer.parseInt(get(name));
    }

    public double getDouble(String name) {
        return Double.parseDouble(get(name));
    }

    public boolean getBool(String name) {
        return Boolean.parseBoolean(get(name));
    }

    public double[] getDoubleArray(String name) {
        final String[] values = get(name).trim().split("\\s");
        final double[] result = new double[values.length];
        for (int i = 0; i < values.length; i++)
            result[i] = Double.parseDouble(values[i]);
        return result;
    }

    public String getPath(String name) {
        String value = get(name);
        if (value != null) {
            File file = new File(value);
            if (file.isAbsolute()) {
                return file.getPath();
            } else if (rootFile != null) {
                return rootFile + File.separator + value;
            } else
                return value;
        } else {
            return null;
        }
    }

    public boolean containsKey(String name) {
        return get(name) != null;
    }

    // -------------------- MISC --------------------

    @Override
	public String toString() {
        return asXmlSegment(0);
    }

    public static String indent(int indentCnt) {
        StringBuffer result = new StringBuffer("");
        for (int i = 0; i < indentCnt; i++)
            result.append(" ");
        return result.toString();
    }

    // -------------------- IMPLEMENTATION OF ConfigModuleI --------------------

    public String getName() {
        return moduleName;
    }

    public String asXmlSegment(int indentCnt) {
        final String indent = indent(indentCnt);
        final String newline = System.getProperty("line.separator");
        final String quote = "\"";

        final StringBuffer result = new StringBuffer();

        result.append(indent + "<" + MODULE_ELEM + " " + MODULE_NAME_ATTR + "="
                + quote + getName() + quote + ">" + newline);

        for (Map.Entry<String, String> entry : content.entrySet())
            result.append(indent + "\t<" + PARAM_ELEM + " " + PARAM_NAME_ATTR
                    + "=" + quote + entry.getKey() + quote + " "
                    + PARAM_VALUE_ATTR + "=" + quote + entry.getValue() + quote
                    + "/>" + newline);

        result.append(indent + "</" + MODULE_ELEM + ">" + newline);

        return result.toString();
    }

    // ---------- XML READING, OVERRIDING OF DefaultHandler ----------

    private boolean inConfig = false;

    private boolean inRightModule = false;

    private void read(String fileName) {
        content.clear();
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser parser = factory.newSAXParser();
            parser.parse(new File(fileName), this);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
	public void startElement(String uri, String localName, String qName,
            Attributes attributes) {
        if (CONFIG_ELEM.equals(qName))
            startConfig(attributes);
        else if (MODULE_ELEM.equals(qName))
            startModule(attributes);
        else if (PARAM_ELEM.equals(qName))
            startParam(attributes);
    }

    @Override
	public void endElement(String uri, String localName, String qName) {
        if (CONFIG_ELEM.equals(qName))
            endConfig();
        else if (MODULE_ELEM.equals(qName))
            endModule();
    }

    private void startConfig(Attributes attrs) {
        inConfig = true;
    }

    private void startModule(Attributes attrs) {
        String moduleName = attrs.getValue(MODULE_NAME_ATTR);
        if (getName().equals(moduleName))
            inRightModule = true;
    }

    private void startParam(Attributes attrs) {
        if (inConfig && inRightModule) {
            String paramName = attrs.getValue(PARAM_NAME_ATTR);
            String paramValue = attrs.getValue(PARAM_VALUE_ATTR);
            set(paramName, paramValue);
        }
    }

    private void endConfig() {
        inConfig = false;
    }

    private void endModule() {
        inRightModule = false;
    }

}
