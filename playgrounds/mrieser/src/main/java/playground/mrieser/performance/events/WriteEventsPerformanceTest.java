/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.mrieser.performance.events;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.utils.io.IOUtils;

/**
 * @author mrieser / Senozon AG
 */
public class WriteEventsPerformanceTest {

	public static void main(String[] args) {
		if (args.length == 0) {
			args = new String[] {"/Volumes/Data/vis/zrh/100.events.xml.gz"};
		}
		final List<LinkLeaveEvent> events = new ArrayList<LinkLeaveEvent>();
		EventsManager em = EventsUtils.createEventsManager();
		em.addHandler(new LinkLeaveEventHandler() {
			@Override
			public void reset(int iteration) {
			}
			@Override
			public void handleEvent(LinkLeaveEvent event) {
				events.add(event);
			}
		});
		new MatsimEventsReader(em).readFile(args[0]);
		System.out.println("READ " + events.size() + " link leave events");
		
		
		for (int i = 0; i < 30; i++) {
			System.out.println("RUN " + i);
			run(events, "old.xml.gz", "new.xml.gz");
			System.out.println();
		}
	}
	
	public static void run(List<LinkLeaveEvent> events, String outputFilename1, String outputFilename2) {
		{
			Writer writer = new WriterOld(outputFilename1);
			long start = System.currentTimeMillis();
			for (LinkLeaveEvent e : events) {
				writer.handleEvent(e);
			}
			long end = System.currentTimeMillis();
			writer.close();
			System.out.println("OLD style: duration: " + (end - start));
		}
		{
			Writer writer = new WriterOld2(outputFilename1);
			long start = System.currentTimeMillis();
			for (LinkLeaveEvent e : events) {
				writer.handleEvent(e);
			}
			long end = System.currentTimeMillis();
			writer.close();
			System.out.println("OLD2 style: duration: " + (end - start));
		}
		{
			Writer writer = new WriterOld3(outputFilename1);
			long start = System.currentTimeMillis();
			for (LinkLeaveEvent e : events) {
				writer.handleEvent(e);
			}
			long end = System.currentTimeMillis();
			writer.close();
			System.out.println("OLD3 style: duration: " + (end - start));
		}
		{
			Writer writer = new WriterOld3b(outputFilename1);
			long start = System.currentTimeMillis();
			for (LinkLeaveEvent e : events) {
				writer.handleEvent(e);
			}
			long end = System.currentTimeMillis();
			writer.close();
			System.out.println("OLD3b style: duration: " + (end - start));
		}
		{
			Writer writer = new WriterOld4(outputFilename1);
			long start = System.currentTimeMillis();
			for (LinkLeaveEvent e : events) {
				writer.handleEvent(e);
			}
			long end = System.currentTimeMillis();
			writer.close();
			System.out.println("OLD4 style: duration: " + (end - start));
		}
		{
			Writer writer = new WriterOld5(outputFilename1);
			long start = System.currentTimeMillis();
			for (LinkLeaveEvent e : events) {
				writer.handleEvent(e);
			}
			long end = System.currentTimeMillis();
			writer.close();
			System.out.println("OLD5 style: duration: " + (end - start));
		}
		{
			Writer writer = new WriterOld6(outputFilename1);
			long start = System.currentTimeMillis();
			for (LinkLeaveEvent e : events) {
				writer.handleEvent(e);
			}
			long end = System.currentTimeMillis();
			writer.close();
			System.out.println("OLD6 style: duration: " + (end - start));
		}
		{
			Writer writer = new WriterNew(outputFilename2);
			long start = System.currentTimeMillis();
			for (LinkLeaveEvent e : events) {
				writer.handleEvent(e);
			}
			long end = System.currentTimeMillis();			writer.close();
			System.out.println("NEW style: duration: " + (end - start));
		}
		{
			Writer writer = new WriterNew2(outputFilename2);
			long start = System.currentTimeMillis();
			for (LinkLeaveEvent e : events) {
				writer.handleEvent(e);
			}
			long end = System.currentTimeMillis();			writer.close();
			System.out.println("NEW2 style: duration: " + (end - start));
		}
		{
			Writer writer = new WriterNew3(outputFilename2);
			long start = System.currentTimeMillis();
			for (LinkLeaveEvent e : events) {
				writer.handleEvent(e);
			}
			long end = System.currentTimeMillis();			writer.close();
			System.out.println("NEW3 style: duration: " + (end - start));
		}
		{
			Writer writer = new WriterNew3b(outputFilename2);
			long start = System.currentTimeMillis();
			for (LinkLeaveEvent e : events) {
				writer.handleEvent(e);
			}
			long end = System.currentTimeMillis();			writer.close();
			System.out.println("NEW3b style: duration: " + (end - start));
		}
		{
			Writer writer = new WriterNew4(outputFilename2);
			long start = System.currentTimeMillis();
			for (LinkLeaveEvent e : events) {
				writer.handleEvent(e);
			}
			long end = System.currentTimeMillis();			writer.close();
			System.out.println("NEW4 style: duration: " + (end - start));
		}
		{
			Writer writer = new WriterNew5(outputFilename2);
			long start = System.currentTimeMillis();
			for (LinkLeaveEvent e : events) {
				writer.handleEvent(e);
			}
			long end = System.currentTimeMillis();			writer.close();
			System.out.println("NEW5 style: duration: " + (end - start));
		}
		
	}
	
	
	private static abstract class Writer implements LinkLeaveEventHandler {

		final BufferedWriter writer;
		
		public Writer(final String filename) {
			writer = IOUtils.getBufferedWriter(filename);
		}
		
		@Override
		public abstract void handleEvent(LinkLeaveEvent event);
		
		@Override
		public void reset(int iteration) {
		}
		
		protected final void write(Map<String, String> attr) {
			StringBuilder eventXML = new StringBuilder(180);
			eventXML.append("\t<event ");
			for (Map.Entry<String, String> entry : attr.entrySet()) {
				eventXML.append(entry.getKey());
				eventXML.append("=\"");
				eventXML.append(encodeAttributeValue(entry.getValue()));
				eventXML.append("\" ");
			}
			eventXML.append(" />\n");
			try {
				this.writer.write(eventXML.toString());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		protected final void newWrite(Map<String, String> attr) {
			try {
				this.writer.append("\t<event ");
				for (Map.Entry<String, String> entry : attr.entrySet()) {
					this.writer.append(entry.getKey());
					this.writer.append("=\"");
					this.writer.append(encodeAttributeValue(entry.getValue()));
					this.writer.append("\" ");
				}
				this.writer.append(" />\n");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		protected final void newWrite2(Map<String, String> attr) {
			try {
				this.writer.append("\t<event ");
				for (Map.Entry<String, String> entry : attr.entrySet()) {
					this.writer.append(entry.getKey());
					this.writer.append("=\"");
					this.writer.append(newEncodeAttributeValue(entry.getValue()));
					this.writer.append("\" ");
				}
				this.writer.append(" />\n");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		protected final void newWrite2b(Map<String, String> attr) {
			try {
				this.writer.append("\t<event ");
				for (Map.Entry<String, String> entry : attr.entrySet()) {
					this.writer.append(entry.getKey());
					this.writer.append("=\"");
					this.writer.append(newEncodeAttributeValue2(entry.getValue()));
					this.writer.append("\" ");
				}
				this.writer.append(" />\n");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		protected final void newWrite3(ArrayBasedStringMap attr) {
			try {
				this.writer.append("\t<event ");
				for (int i = 0, n = attr.size(); i < n; i++) {
					this.writer.append(attr.getKey(i));
					this.writer.append("=\"");
					this.writer.append(newEncodeAttributeValue2(attr.getValue(i)));
					this.writer.append("\" ");
				}
				this.writer.append(" />\n");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		protected final void newWrite3checked(Map<String, String> attr) {
			if (attr instanceof ArrayBasedStringMap) {
				ArrayBasedStringMap attr2 = (ArrayBasedStringMap) attr;
				try {
					this.writer.append("\t<event ");
					for (int i = 0, n = attr2.size(); i < n; i++) {
						this.writer.append(attr2.getKey(i));
						this.writer.append("=\"");
						this.writer.append(newEncodeAttributeValue2(attr2.getValue(i)));
						this.writer.append("\" ");
					}
					this.writer.append(" />\n");
				} catch (IOException e) {
					e.printStackTrace();
				}
			} else {
				newWrite2(attr);
			}
		}

		
		
		public void close() {
			try {
				writer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
	}

	private static class WriterOld extends Writer {
		
		public WriterOld(final String filename) {
			super(filename);
		}
		
		@Override
		public void handleEvent(LinkLeaveEvent event) {
			Map<String, String> attr = new LinkedHashMap<String, String>();
			attr.put("time", Double.toString(event.getTime()));
			attr.put("type", event.getEventType());
			attr.put("person", event.getPersonId().toString());
			attr.put("link", event.getLinkId().toString());
			if (event.getVehicleId() != null) {
				attr.put("vehicle", event.getVehicleId().toString());
			}
			
			write(attr);
		}
		
	}
	
	private static class WriterOld2 extends Writer {
		
		public WriterOld2(final String filename) {
			super(filename);
		}
		
		@Override
		public void handleEvent(LinkLeaveEvent event) {
			Map<String, String> attr = new LinkedHashMap<String, String>();
			attr.put("time", Double.toString(event.getTime()));
			attr.put("type", event.getEventType());
			attr.put("person", event.getPersonId().toString());
			attr.put("link", event.getLinkId().toString());
			if (event.getVehicleId() != null) {
				attr.put("vehicle", event.getVehicleId().toString());
			}
			
			newWrite(attr);
		}
		
	}

	private static class WriterOld3 extends Writer {
		
		public WriterOld3(final String filename) {
			super(filename);
		}
		
		@Override
		public void handleEvent(LinkLeaveEvent event) {
			Map<String, String> attr = new LinkedHashMap<String, String>();
			attr.put("time", Double.toString(event.getTime()));
			attr.put("type", event.getEventType());
			attr.put("person", event.getPersonId().toString());
			attr.put("link", event.getLinkId().toString());
			if (event.getVehicleId() != null) {
				attr.put("vehicle", event.getVehicleId().toString());
			}
			
			newWrite2(attr);
		}
	}

	private static class WriterOld3b extends Writer {
		
		public WriterOld3b(final String filename) {
			super(filename);
		}
		
		@Override
		public void handleEvent(LinkLeaveEvent event) {
			Map<String, String> attr = new LinkedHashMap<String, String>();
			attr.put("time", Double.toString(event.getTime()));
			attr.put("type", event.getEventType());
			attr.put("person", event.getPersonId().toString());
			attr.put("link", event.getLinkId().toString());
			if (event.getVehicleId() != null) {
				attr.put("vehicle", event.getVehicleId().toString());
			}
			
			newWrite2b(attr);
		}
	}

	private static class WriterOld4 extends Writer {
		
		public WriterOld4(final String filename) {
			super(filename);
		}
		
		@Override
		public void handleEvent(LinkLeaveEvent event) {
			Map<String, String> attr = new ArrayBasedStringMap();
			attr.put("time", Double.toString(event.getTime()));
			attr.put("type", event.getEventType());
			attr.put("person", event.getPersonId().toString());
			attr.put("link", event.getLinkId().toString());
			if (event.getVehicleId() != null) {
				attr.put("vehicle", event.getVehicleId().toString());
			}
			
			newWrite2b(attr);
		}
	}

	private static class WriterOld5 extends Writer {
		
		public WriterOld5(final String filename) {
			super(filename);
		}
		
		@Override
		public void handleEvent(LinkLeaveEvent event) {
			Map<String, String> attr = new ArrayBasedStringMap();
			attr.put("time", Double.toString(event.getTime()));
			attr.put("type", event.getEventType());
			attr.put("person", event.getPersonId().toString());
			attr.put("link", event.getLinkId().toString());
			if (event.getVehicleId() != null) {
				attr.put("vehicle", event.getVehicleId().toString());
			}
			
			newWrite3checked(attr);
		}
	}

	private static class WriterOld6 extends Writer {
		
		String[] times = new String[100000];
		
		public WriterOld6(final String filename) {
			super(filename);
			for (int i = 0; i < times.length; i++) {
				times[i] = Double.toString(i);
			}
		}
		
		@Override
		public void handleEvent(LinkLeaveEvent event) {
			Map<String, String> attr = new ArrayBasedStringMap();
			attr.put("time", myDoubleToString(event.getTime()));
			attr.put("type", event.getEventType());
			attr.put("person", event.getPersonId().toString());
			attr.put("link", event.getLinkId().toString());
			if (event.getVehicleId() != null) {
				attr.put("vehicle", event.getVehicleId().toString());
			}
			
			newWrite3checked(attr);
		}
		
		private String myDoubleToString(final double d) {
			int i = (int) d;
			if (i < times.length && d == i) {
				return times[i];
			}
			return Double.toString(d);
		}
	}
	
	private static class WriterNew extends Writer {
		
		Map<String, String> attr = new LinkedHashMap<String, String>();
		
		public WriterNew(final String filename) {
			super(filename);
		}
		
		@Override
		public void handleEvent(LinkLeaveEvent event) {
			attr.clear();
			attr.put("time", Double.toString(event.getTime()));
			attr.put("type", event.getEventType());
			attr.put("person", event.getPersonId().toString());
			attr.put("link", event.getLinkId().toString());
			if (event.getVehicleId() != null) {
				attr.put("vehicle", event.getVehicleId().toString());
			}
			
			write(attr);
		}
		
	}
	
	private static class WriterNew2 extends Writer {
		
		Map<String, String> attr = new LinkedHashMap<String, String>();
		
		public WriterNew2(final String filename) {
			super(filename);
		}
		
		@Override
		public void handleEvent(LinkLeaveEvent event) {
			attr.clear();
			attr.put("time", Double.toString(event.getTime()));
			attr.put("type", event.getEventType());
			attr.put("person", event.getPersonId().toString());
			attr.put("link", event.getLinkId().toString());
			if (event.getVehicleId() != null) {
				attr.put("vehicle", event.getVehicleId().toString());
			}
			
			newWrite(attr);
		}
	}

	private static class WriterNew3 extends Writer {
		
		Map<String, String> attr = new LinkedHashMap<String, String>();
		
		public WriterNew3(final String filename) {
			super(filename);
		}
		
		@Override
		public void handleEvent(LinkLeaveEvent event) {
			attr.clear();
			attr.put("time", Double.toString(event.getTime()));
			attr.put("type", event.getEventType());
			attr.put("person", event.getPersonId().toString());
			attr.put("link", event.getLinkId().toString());
			if (event.getVehicleId() != null) {
				attr.put("vehicle", event.getVehicleId().toString());
			}

			newWrite2(attr);
		}
	}

	private static class WriterNew3b extends Writer {
		
		Map<String, String> attr = new LinkedHashMap<String, String>();
		
		public WriterNew3b(final String filename) {
			super(filename);
		}
		
		@Override
		public void handleEvent(LinkLeaveEvent event) {
			attr.clear();
			attr.put("time", Double.toString(event.getTime()));
			attr.put("type", event.getEventType());
			attr.put("person", event.getPersonId().toString());
			attr.put("link", event.getLinkId().toString());
			if (event.getVehicleId() != null) {
				attr.put("vehicle", event.getVehicleId().toString());
			}
			
			newWrite2b(attr);
		}
	}
	
	private static class WriterNew4 extends Writer {
		
		Map<String, String> attr = new ArrayBasedStringMap();
		
		public WriterNew4(final String filename) {
			super(filename);
		}
		
		@Override
		public void handleEvent(LinkLeaveEvent event) {
			attr.clear();
			attr.put("time", Double.toString(event.getTime()));
			attr.put("type", event.getEventType());
			attr.put("person", event.getPersonId().toString());
			attr.put("link", event.getLinkId().toString());
			if (event.getVehicleId() != null) {
				attr.put("vehicle", event.getVehicleId().toString());
			}
			
			newWrite2b(attr);
		}
		
	}
	
	private static class WriterNew5 extends Writer {
		
		ArrayBasedStringMap attr = new ArrayBasedStringMap();
		
		public WriterNew5(final String filename) {
			super(filename);
		}
		
		@Override
		public void handleEvent(LinkLeaveEvent event) {
			attr.clear();
			attr.put("time", Double.toString(event.getTime()));
			attr.put("type", event.getEventType());
			attr.put("person", event.getPersonId().toString());
			attr.put("link", event.getLinkId().toString());
			if (event.getVehicleId() != null) {
				attr.put("vehicle", event.getVehicleId().toString());
			}
			
			newWrite3(attr);
		}
		
	}
	
	private static String encodeAttributeValue(final String attributeValue) {
		if (attributeValue == null) {
			return null;
		}
		if (attributeValue.contains("&") || attributeValue.contains("\"") || attributeValue.contains("<") || attributeValue.contains(">")) {
			return attributeValue.replace("&", "&amp;").replace("\"", "&quot;").replace("<", "&lt;").replace(">", "&gt;");
		}
		return attributeValue;
	}
	
	protected static String newEncodeAttributeValue(final String attributeValue) {
		if (attributeValue == null) {
			return null;
		}
		if (attributeValue.contains("&") || attributeValue.contains("\"") || attributeValue.contains("<") || attributeValue.contains(">")) {
			
			StringBuffer bf = new StringBuffer();
			int len = attributeValue.length();
			for (int pos = 0; pos < len; pos++) {
				char ch = attributeValue.charAt(pos);
				if (ch == '<') {
					bf.append("&lt;");
				} else if (ch == '\"') {
					bf.append("&quot;");
				} else if (ch == '&') {
					bf.append("&amp;");
				} else {
					bf.append(ch);
				}
			}

			return bf.toString();
		}
		return attributeValue;
	}
	
	protected static String newEncodeAttributeValue2(final String attributeValue) {
		if (attributeValue == null) {
			return null;
		}
		int len = attributeValue.length();
		boolean encode = false;
		for (int pos = 0; pos < len; pos++) {
			char ch = attributeValue.charAt(pos);
			if (ch == '<') {
				encode = true;
				break;
			} else if (ch == '\"') {
				encode = true;
				break;
			} else if (ch == '&') {
				encode = true;
				break;
			}
		}
		if (encode) {
			StringBuffer bf = new StringBuffer();
			for (int pos = 0; pos < len; pos++) {
				char ch = attributeValue.charAt(pos);
				if (ch == '<') {
					bf.append("&lt;");
				} else if (ch == '\"') {
					bf.append("&quot;");
				} else if (ch == '&') {
					bf.append("&amp;");
				} else {
					bf.append(ch);
				}
			}
			
			return bf.toString();
		}
		return attributeValue;
	}
	
	private static class ArrayBasedStringMap implements Map<String, String> {

		ArrayList<String> entries = new ArrayList<String>(32);
		int size = 0;
		
		public String getKey(int index) {
			return this.entries.get(index * 2);
		}
		
		public String getValue(int index) {
			return this.entries.get(index * 2 + 1);
		}
		
		@Override
		public int size() {
			return size;
		}

		@Override
		public boolean isEmpty() {
			return size == 0;
		}

		@Override
		public boolean containsKey(Object key) {
			for (int i = 0; i < entries.size(); i += 2) {
				if (entries.get(i).equals(key)) {
					return true;
				}
			}
			return false;
		}

		@Override
		public boolean containsValue(Object value) {
			for (int i = 1; i < entries.size(); i += 2) {
				String v = entries.get(i);
				if (value==null ? v==null : value.equals(v)) {
					return true;
				}
			}
			return false;
		}

		@Override
		public String get(Object key) {
			for (int i = 0; i < entries.size(); i += 2) {
				if (entries.get(i).equals(key)) {
					return entries.get(i+1);
				}
			}
			return null;
		}

		@Override
		public String put(String key, String value) {
			for (int i = 0; i < entries.size(); i += 2) {
				if (entries.get(i).equals(key)) {
					String oldValue = entries.get(i+1);
					entries.set(i+1, value);
					return oldValue;
				}
			}
			entries.add(key);
			entries.add(value);
			this.size++;
			return null;
		}

		@Override
		public String remove(Object key) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void putAll(Map<? extends String, ? extends String> m) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void clear() {
			this.entries.clear();
			this.size = 0;
		}

		@Override
		public Set<String> keySet() {
			throw new UnsupportedOperationException();
		}

		@Override
		public Collection<String> values() {
			throw new UnsupportedOperationException();
		}

		@Override
		public Set<java.util.Map.Entry<String, String>> entrySet() {
			return new Set<Map.Entry<String, String>>() {
				@Override
				public int size() {
					return ArrayBasedStringMap.this.size;
				}

				@Override
				public boolean isEmpty() {
					return ArrayBasedStringMap.this.isEmpty();
				}

				@Override
				public boolean contains(Object o) {
					return ArrayBasedStringMap.this.containsValue(o);
				}

				@Override
				public Iterator<java.util.Map.Entry<String, String>> iterator() {
					return new Iterator<Map.Entry<String, String>>() {
						int maxIdx = ArrayBasedStringMap.this.size * 2 - 1;
						int nextIdx = 0;
						@Override
						public boolean hasNext() {
							return nextIdx < maxIdx;
						}

						@Override
						public java.util.Map.Entry<String, String> next() {
							final String key = ArrayBasedStringMap.this.entries.get(this.nextIdx);
							final String value = ArrayBasedStringMap.this.entries.get(this.nextIdx + 1);
							this.nextIdx += 2;
							return new Map.Entry<String, String>() {

								@Override
								public String getKey() {
									return key;
								}

								@Override
								public String getValue() {
									return value;
								}

								@Override
								public String setValue(String value) {
									throw new UnsupportedOperationException();
								}
							};
						}

						@Override
						public void remove() {
							throw new UnsupportedOperationException();
						}
						
					};
				}

				@Override
				public Object[] toArray() {
					throw new UnsupportedOperationException();
				}

				@Override
				public <T> T[] toArray(T[] a) {
					throw new UnsupportedOperationException();
				}

				@Override
				public boolean add(java.util.Map.Entry<String, String> e) {
					throw new UnsupportedOperationException();
				}

				@Override
				public boolean remove(Object o) {
					throw new UnsupportedOperationException();
				}

				@Override
				public boolean containsAll(Collection<?> c) {
					throw new UnsupportedOperationException();
				}

				@Override
				public boolean addAll(Collection<? extends java.util.Map.Entry<String, String>> c) {
					throw new UnsupportedOperationException();
				}

				@Override
				public boolean retainAll(Collection<?> c) {
					throw new UnsupportedOperationException();
				}

				@Override
				public boolean removeAll(Collection<?> c) {
					throw new UnsupportedOperationException();
				}

				@Override
				public void clear() {
					throw new UnsupportedOperationException();
				}
				
			};
		}
		
	}
	
}
