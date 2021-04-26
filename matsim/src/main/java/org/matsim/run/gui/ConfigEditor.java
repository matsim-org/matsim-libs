
/* *********************************************************************** *
 * project: org.matsim.*
 * ConfigEditor.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

package org.matsim.run.gui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.AbstractAction;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.PlainDocument;
import javax.swing.text.PlainView;
import javax.swing.text.Segment;
import javax.swing.text.StyledEditorKit;
import javax.swing.text.Utilities;
import javax.swing.text.ViewFactory;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;

import org.matsim.core.utils.io.IOUtils;

/**
 * @author mrieser
 */
class ConfigEditor extends JDialog {

	private static final boolean IS_MAC = System.getProperty("os.name").startsWith("Mac");

	private JButton btnSave;
	private File configFile;
	private JTextPane xmlPane;
	private ConfigChangeListener configChangeListener;

	ConfigEditor(JFrame parent, File configFile, ConfigChangeListener configChangeListener) {
		super(parent);
		setTitle("Config Editor");
		this.configChangeListener = configChangeListener;
		this.configFile = configFile;

		this.btnSave = new JButton("Save");
		this.btnSave.addActionListener(e -> this.save());

		JButton btnSaveAs = new JButton("Save as…");
		btnSaveAs.addActionListener(e -> this.saveAs());

		JScrollPane scrollPane = new JScrollPane();

		GroupLayout groupLayout = new GroupLayout(getContentPane());
		groupLayout.setHorizontalGroup(groupLayout.createParallelGroup(Alignment.LEADING)
				.addGroup(groupLayout.createSequentialGroup()
						.addContainerGap()
						.addComponent(this.btnSave)
						.addPreferredGap(ComponentPlacement.RELATED)
						.addComponent(btnSaveAs)
						.addContainerGap(471, Short.MAX_VALUE))
				.addComponent(scrollPane, Alignment.TRAILING, GroupLayout.DEFAULT_SIZE, 661, Short.MAX_VALUE));
		groupLayout.setVerticalGroup(groupLayout.createParallelGroup(Alignment.LEADING)
				.addGroup(groupLayout.createSequentialGroup()
						.addContainerGap()
						.addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
								.addComponent(this.btnSave)
								.addComponent(btnSaveAs))
						.addPreferredGap(ComponentPlacement.RELATED)
						.addComponent(scrollPane, GroupLayout.DEFAULT_SIZE, 456, Short.MAX_VALUE)));

		this.xmlPane = new JTextPane();
		this.xmlPane.setContentType("text/xml");
		this.xmlPane.setEditorKit(new XmlEditorKit());
		scrollPane.setViewportView(this.xmlPane);
		getContentPane().setLayout(groupLayout);

		UndoManager undoManager = this.addUndoFunctionality(this.xmlPane);
		addSaveFunctionality();
		addCloseFunctionality();

		this.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
		this.pack();

		this.loadConfig();
		undoManager.discardAllEdits(); // clear history, otherwise the loading of the initial config could be redone.
		this.btnSave.setEnabled(false);

		this.xmlPane.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void insertUpdate(DocumentEvent e) {
				ConfigEditor.this.btnSave.setEnabled(true);
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				ConfigEditor.this.btnSave.setEnabled(true);
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				ConfigEditor.this.btnSave.setEnabled(true);
			}
		});
	}

	void showEditor() {
		setVisible(true);
		this.xmlPane.requestFocus();
	}

	void closeEditor() {
		setVisible(false);
	}

	private void saveAs() {
		SaveFileSaver chooser = new SaveFileSaver();
		chooser.setSelectedFile(this.configFile);
		int saveResult = chooser.showSaveDialog(null);
		if (saveResult == JFileChooser.APPROVE_OPTION) {
			this.configFile = chooser.getSelectedFile();
			save();
		}
	}

	private void save() {
		String fullXml = this.xmlPane.getText();
		try (BufferedWriter writer = IOUtils.getBufferedWriter(this.configFile.getAbsolutePath())) {
			writer.write(fullXml);
		} catch (IOException e) {
			e.printStackTrace();
		}
		this.configChangeListener.configChanged(this.configFile);
		this.btnSave.setEnabled(false);
	}

	private UndoManager addUndoFunctionality(JTextPane textPane) {
		String UNDO_ACTION = "__UNDO__";
		String REDO_ACTION = "__REDO__";

		final UndoManager undoManager = new UndoManager();

		// Add listener for undoable events
		textPane.getDocument().addUndoableEditListener(pEvt -> undoManager.addEdit(pEvt.getEdit()));

		// Add undo/redo actions
		textPane.getActionMap().put(UNDO_ACTION, new AbstractAction(UNDO_ACTION) {
			public void actionPerformed(ActionEvent pEvt) {
				try {
					if (undoManager.canUndo()) {
						undoManager.undo();
					}
				} catch (CannotUndoException e) {
					e.printStackTrace();
				}
			}
		});
		textPane.getActionMap().put(REDO_ACTION, new AbstractAction(REDO_ACTION) {
			public void actionPerformed(ActionEvent pEvt) {
				try {
					if (undoManager.canRedo()) {
						undoManager.redo();
					}
				} catch (CannotRedoException e) {
					e.printStackTrace();
				}
			}
		});

		// Create keyboard accelerators for undo/redo actions (Ctrl+Z/Ctrl+Y)
		textPane.getInputMap()
				.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z,
						IS_MAC ? InputEvent.META_DOWN_MASK : InputEvent.CTRL_DOWN_MASK), UNDO_ACTION);
		textPane.getInputMap()
				.put(KeyStroke.getKeyStroke(KeyEvent.VK_Y,
						IS_MAC ? InputEvent.META_DOWN_MASK : InputEvent.CTRL_DOWN_MASK), REDO_ACTION);

		return undoManager;
	}

	private void addSaveFunctionality() {
		String SAVE_ACTION = "__SAVE__";

		this.xmlPane.getActionMap().put(SAVE_ACTION, new AbstractAction(SAVE_ACTION) {
			public void actionPerformed(ActionEvent pEvt) {
				save();
			}
		});
		this.xmlPane.getInputMap()
				.put(KeyStroke.getKeyStroke(KeyEvent.VK_S,
						IS_MAC ? InputEvent.META_DOWN_MASK : InputEvent.CTRL_DOWN_MASK), SAVE_ACTION);
	}

	private void addCloseFunctionality() {
		String CLOSE_ACTION = "__CLOSE__";

		this.xmlPane.getActionMap().put(CLOSE_ACTION, new AbstractAction(CLOSE_ACTION) {
			public void actionPerformed(ActionEvent pEvt) {
				ConfigEditor.this.setVisible(false);
			}
		});
		this.xmlPane.getInputMap()
				.put(KeyStroke.getKeyStroke(KeyEvent.VK_W,
						IS_MAC ? InputEvent.META_DOWN_MASK : InputEvent.CTRL_DOWN_MASK), CLOSE_ACTION);
	}

	private void loadConfig() {
		StringBuilder buffer = new StringBuilder(1024);
		try (BufferedReader in = IOUtils.getBufferedReader(this.configFile.getAbsolutePath())) {
			String line;
			while ((line = in.readLine()) != null) {
				buffer.append(line);
				buffer.append(IOUtils.NATIVE_NEWLINE);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		this.xmlPane.setText(buffer.toString());
		this.xmlPane.setCaretPosition(0);
	}

	interface ConfigChangeListener {
		void configChanged(File newConfigFile);
	}

	/**
	 * XmlEditorKit based on https://boplicity.nl/knowledgebase/Java/Xml+syntax+highlighting+in+Swing+JTextPane.html
	 * <p>
	 * Copyright 2006-2008 Kees de Kooter
	 * Licensed under the Apache License, Version 2.0
	 */

	private static class XmlEditorKit extends StyledEditorKit {

		private static final long serialVersionUID = 2969169649596107757L;
		private ViewFactory xmlViewFactory;

		public XmlEditorKit() {
			xmlViewFactory = XmlView::new;
		}

		@Override
		public ViewFactory getViewFactory() {
			return xmlViewFactory;
		}

		@Override
		public String getContentType() {
			return "text/xml";
		}
	}

	/**
	 * Thanks: http://groups.google.com/group/de.comp.lang.java/msg/2bbeb016abad270
	 * <p>
	 * IMPORTANT NOTE: regex should contain 1 group.
	 * <p>
	 * Using PlainView here because we don't want line wrapping to occur.
	 *
	 * @author kees
	 * date 13-jan-2006
	 */
	private static class XmlView extends PlainView {

		private static HashMap<Pattern, Color> patternColors;
		private static String TAG_PATTERN = "(</?[a-z\\-]*)\\s?>?";
		private static String TAG_END_PATTERN = "(/>)";
		private static String TAG_ATTRIBUTE_PATTERN = "\\s(\\w*)\\=";
		private static String TAG_ATTRIBUTE_VALUE = "[a-z-]*\\=(\"[^\"]*\")";
		private static String TAG_COMMENT = "(<!--.*-->)";
		private static String TAG_CDATA_START = "(\\<!\\[CDATA\\[).*";
		private static String TAG_CDATA_END = ".*(]]>)";

		static {
			// NOTE: the order is important!
			patternColors = new HashMap<>();
			patternColors.put(Pattern.compile(TAG_CDATA_START), new Color(128, 128, 128));
			patternColors.put(Pattern.compile(TAG_CDATA_END), new Color(128, 128, 128));
			patternColors.put(Pattern.compile(TAG_PATTERN), new Color(63, 127, 127));
			patternColors.put(Pattern.compile(TAG_ATTRIBUTE_PATTERN), new Color(127, 0, 127));
			patternColors.put(Pattern.compile(TAG_END_PATTERN), new Color(63, 127, 127));
			patternColors.put(Pattern.compile(TAG_ATTRIBUTE_VALUE), new Color(42, 0, 255));
			patternColors.put(Pattern.compile(TAG_COMMENT), new Color(63, 95, 191));
		}

		public XmlView(Element element) {

			super(element);

			// Set tabsize to 4 (instead of the default 8)
			getDocument().putProperty(PlainDocument.tabSizeAttribute, 4);
		}

		@Override
		protected int drawUnselectedText(Graphics graphics, int x, int y, int p0, int p1) throws BadLocationException {

			Document doc = getDocument();
			String text = doc.getText(p0, p1 - p0);

			Segment segment = getLineBuffer();

			SortedMap<Integer, Integer> startMap = new TreeMap<>();
			SortedMap<Integer, Color> colorMap = new TreeMap<>();

			// Match all regexes on this snippet, store positions
			for (Map.Entry<Pattern, Color> entry : patternColors.entrySet()) {

				Matcher matcher = entry.getKey().matcher(text);

				while (matcher.find()) {
					startMap.put(matcher.start(1), matcher.end());
					colorMap.put(matcher.start(1), entry.getValue());
				}
			}

			int i = 0;

			// Colour the parts
			for (Map.Entry<Integer, Integer> entry : startMap.entrySet()) {
				int start = entry.getKey();
				int end = entry.getValue();

				if (i < start) {
					graphics.setColor(Color.black);
					doc.getText(p0 + i, start - i, segment);
					x = Utilities.drawTabbedText(segment, x, y, graphics, this, i);
				}

				graphics.setColor(colorMap.get(start));
				i = end;
				doc.getText(p0 + start, i - start, segment);
				x = Utilities.drawTabbedText(segment, x, y, graphics, this, start);
			}

			// Paint possible remaining text black
			if (i < text.length()) {
				graphics.setColor(Color.black);
				doc.getText(p0 + i, text.length() - i, segment);
				x = Utilities.drawTabbedText(segment, x, y, graphics, this, i);
			}

			return x;
		}

	}
}
