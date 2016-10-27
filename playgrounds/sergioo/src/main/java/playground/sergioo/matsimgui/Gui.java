package playground.sergioo.matsimgui;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.scenario.ScenarioUtils;

import com.google.inject.binder.LinkedBindingBuilder;

public class Gui implements ActionListener, IterationStartsListener {
	
	private enum Type {
		ADD, BIND, NO;
	}
	private static class BindMethod {
		
		Method method;
		String name;
		Type type;
		Class[] params;
		boolean provider;
		JTextField text = new JTextField();
		JComboBox<String> texts = new JComboBox<>();
		Map<Class, JTextField> pTexts = new HashMap<>();
		Map<String, Map<Class, String>> pTextss = new HashMap<>();
		
		public BindMethod(Method method) {
			super();
			this.method = method;
			String[] parts = method.getName().split("(?=\\p{Upper})");
			parts[0] = parts[0].substring(0, 1).toUpperCase() + parts[0].substring(1);
			for(Type type:Type.values())
				if(parts[0].toUpperCase().equals(type.name()))
					this.type = type;
			if(parts.length==1)
				type = Type.NO;
			this.name = Arrays.toString(parts).replaceAll(",", " ").substring(1, Arrays.toString(parts).length()-1);
			params = new Class[method.getParameters().length];
			int i=0;
			for(Parameter param:method.getParameters())
				params[i++] = param.getType();
			System.out.println(this.name+ " " + type+ " " +Arrays.toString(params));
		}
	}
	
	static Map<Type, Set<BindMethod>> methods = new HashMap<>();
	static Set<String> worked = new HashSet<>();
	private static JTextField classTextField = new JTextField();
	static final JFrame frame = new JFrame("MATSim set-up");
	private static JComboBox<String> list = new JComboBox<>();
	private static Map<String, BindMethod> allMethods = new HashMap<>();
	private static JCheckBox singBox;
	private static File configFile;
	private static JLabel configL = new JLabel();
	private static JButton buttonR;
	private static JLabel itLabel;

	public static void run() {
        Gui gui = new Gui();
		frame.setLayout(new BorderLayout());
		JPanel extra = new JPanel(new BorderLayout());
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		JPanel configPanel = new JPanel(new GridLayout(1, 2));
		configPanel.setBorder(new TitledBorder("CONFIG"));
		JButton button = new JButton("Select file");
		button.addActionListener(gui);
		button.setActionCommand("file");
		configPanel.add(button);
		configPanel.add(configL );
		extra.add(configPanel, BorderLayout.NORTH);
		JPanel ownPanel = new JPanel(new GridLayout(3, 2));
		ownPanel.setBorder(new TitledBorder("OWN CLASSES"));
		ownPanel.add(new JLabel("Name of the class"));
		classTextField.getDocument().addDocumentListener(new DocumentListener() {
			
			@Override
			public void changedUpdate(DocumentEvent arg0) {
				warn(arg0);
			}

			@Override
			public void insertUpdate(DocumentEvent arg0) {
				warn(arg0);
			}

			@Override
			public void removeUpdate(DocumentEvent arg0) {
				warn(arg0);
			}
			
			private void warn(DocumentEvent arg0) {
				try {
					Class.forName(classTextField.getText());
				} catch (ClassNotFoundException e1) {
					classTextField.setForeground(new Color(1f,0f,0f));
					return;
				}
				classTextField.setForeground(new Color(0f,0f,0f));
			}
		});
		ownPanel.add(classTextField);
		singBox = new JCheckBox("Is Singleton?");
		ownPanel.add(singBox);
		button = new JButton("Add");
		button.addActionListener(gui);
		button.setActionCommand("add");
		ownPanel.add(button);
		ownPanel.add(list);
		extra.add(ownPanel, BorderLayout.SOUTH);
		frame.add(extra, BorderLayout.NORTH);
		JPanel conPanel = new JPanel(new GridLayout(Type.values().length-1, 1));
		for(Method method:AbstractModule.class.getDeclaredMethods())
			if(method.getDeclaringClass().equals(AbstractModule.class) && method.getReturnType().isAssignableFrom(LinkedBindingBuilder.class)) {
				BindMethod bm = new BindMethod(method);
				allMethods.put(bm.name, bm);
				Set<BindMethod> set = methods.get(bm.type);
				if(set==null) {
					set = new HashSet<>();
					methods.put(bm.type, set);
				}
				set.add(bm);
			}
		for(Type type:Type.values())
			if(type != Type.NO) {
				Set<BindMethod> bSet = methods.get(type);
				JPanel conTypePanel = new JPanel(new GridLayout(bSet.size()+1, type==Type.ADD?6:4));
				conTypePanel.add(new JLabel("Binding"));
				conTypePanel.add(new JLabel("Provider"));
				conTypePanel.add(new JLabel("Parameters"));
				conTypePanel.add(new JLabel("Name"));
				if(type==Type.ADD) {
					conTypePanel.add(new JLabel("Button"));
					conTypePanel.add(new JLabel("List"));
				}
				conTypePanel.setBorder(new TitledBorder(type.name()));
				for(BindMethod bm:bSet) {
					conTypePanel.add(new JLabel(bm.name));
					JCheckBox box = new JCheckBox("Is Provider?");
					box.addActionListener(gui);
					box.setActionCommand("prov,"+bm.name);
					conTypePanel.add(box);
					JPanel pPanel = new JPanel(new GridLayout(2, bm.params.length));
					for(Class par:bm.params)
						pPanel.add(new JLabel(par.getSimpleName()));
					for(Class par:bm.params) {
						JTextField text = new JTextField();
						bm.pTexts.put(par, text);
						pPanel.add(text);
					}
					conTypePanel.add(pPanel);
					bm.text.setForeground(new Color(1f,0f,0f));
					bm.text.getDocument().addDocumentListener(new DocumentListener() {
						
						@Override
						public void changedUpdate(DocumentEvent arg0) {
							warn(arg0);
						}

						@Override
						public void insertUpdate(DocumentEvent arg0) {
							warn(arg0);
						}

						@Override
						public void removeUpdate(DocumentEvent arg0) {
							warn(arg0);
						}
						
						private void warn(DocumentEvent arg0) {
							try {
								Class.forName(bm.text.getText());
							} catch (ClassNotFoundException e1) {
								bm.text.setForeground(new Color(1f,0f,0f));
								return;
							}
							bm.text.setForeground(new Color(0f,0f,0f));
						}
					});
					conTypePanel.add(bm.text);
					if(type==Type.ADD) {
						JButton buttonA = new JButton("Add");
						buttonA.addActionListener(gui);
						buttonA.setActionCommand("add,"+bm.name);
						conTypePanel.add(buttonA);
						conTypePanel.add(bm.texts);
					}
				}
				conPanel.add(conTypePanel);
			}
		frame.add(conPanel, BorderLayout.CENTER);
		buttonR = new JButton("Run");
		buttonR.addActionListener(gui);
		buttonR.setActionCommand("run");
		buttonR.setEnabled(false);
		frame.add(buttonR, BorderLayout.SOUTH);
		frame.pack();
		frame.setVisible(true);
		
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if(e.getActionCommand().equals("file")) {
			JFileChooser jfc = new JFileChooser("./");
			jfc.showOpenDialog(frame);
			configFile = jfc.getSelectedFile();
			configL.setText(configFile.getAbsolutePath());
			buttonR.setEnabled(true);
		}
		else if(e.getActionCommand().equals("run")) {
			Config config = ConfigUtils.loadConfig(configFile.getAbsolutePath());
			config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
			//Modify config
			Scenario scenario = ScenarioUtils.createScenario(config);
			//Modify scenario
			Controler controler = new Controler(config);
			controler.addControlerListener(this);
			//Modify controler
			controler.addOverridingModule(new AbstractModule() {
				@Override
				public void install() {
					for(int i=0; i<list.getItemCount(); i++)
						try {
							String[] parts = list.getItemAt(i).split(",");
							if(parts.length>1)
								bind(Class.forName(parts[0])).asEagerSingleton();
							else
								bind(Class.forName(parts[0]));
						} catch (ClassNotFoundException e) {
							if(!Gui.worked.contains(e.getMessage())) {
								JOptionPane.showMessageDialog(frame, e.getClass().getSimpleName()+": "+e.getMessage());
								Gui.worked.add(e.getMessage());
							}
						}
					for(Entry<Type, Set<BindMethod>> set:methods.entrySet())
						for(BindMethod method:set.getValue())
							if(!method.text.getText().equals("") || method.texts.getItemCount()>0) {
								if(set.getKey() == Type.BIND) {
									Object[] params = new Object[method.pTexts.size()];
									int i=0;
									for(Entry<Class, JTextField> text:method.pTexts.entrySet())
										try {
											params[i++] = text.getKey().equals(String.class)?text.getValue().getText():text.getKey().equals(Class.class)?Class.forName(text.getValue().getText()):null;
										} catch (ClassNotFoundException e) {
											if(!Gui.worked.contains(e.getMessage())) {
												JOptionPane.showMessageDialog(frame, e.getClass().getSimpleName()+": "+e.getMessage());
												Gui.worked.add(e.getMessage());
											}
										}
									try {
										if(!method.provider)
											((LinkedBindingBuilder)method.method.invoke(this, params)).to(Class.forName(method.text.getText()));
										else
											((LinkedBindingBuilder)method.method.invoke(this, params)).toProvider(Class.forName(method.text.getText()));
									} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException
											| ClassNotFoundException e) {
										if(!Gui.worked.contains(e.getMessage())) {
											JOptionPane.showMessageDialog(frame, e.getClass().getSimpleName()+": "+e.getMessage());
											Gui.worked.add(e.getMessage());
										}
									}
								}
								else {
									for(Entry<String, Map<Class, String>> textx:method.pTextss.entrySet()) {
										Object[] params = new Object[textx.getValue().size()];
										int i=0;
										for(Entry<Class, String> text:textx.getValue().entrySet())
											try {
												params[i++] = text.getKey().equals(String.class)?text.getValue():text.getKey().equals(Class.class)?Class.forName(text.getValue()):null;
											} catch (ClassNotFoundException e) {
												if(!Gui.worked.contains(e.getMessage())) {
													JOptionPane.showMessageDialog(frame, e.getClass().getSimpleName()+": "+e.getMessage());
													Gui.worked.add(e.getMessage());
												}
											}
										try {
											if(!method.provider)
												((LinkedBindingBuilder)method.method.invoke(this, params)).to(Class.forName(textx.getKey()));
											else
												((LinkedBindingBuilder)method.method.invoke(this, params)).toProvider(Class.forName(textx.getKey()));
										} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException
												| ClassNotFoundException e) {
											if(!Gui.worked.contains(e.getMessage())) {
												JOptionPane.showMessageDialog(frame, e.getClass().getSimpleName()+": "+e.getMessage());
												Gui.worked.add(e.getMessage());
											}
										}
									}
									
								}
							}
					if(!Gui.worked.isEmpty())
						frame.setVisible(false);
				}
			});
			JDialog itFrame = new JDialog(frame, "Iteration", true);
		        itFrame.setLayout(new BorderLayout());
				itLabel = new JLabel("-");
				itLabel.setHorizontalAlignment(SwingConstants.CENTER);
				itLabel.setFont(new Font(itLabel.getFont().getName(), Font.BOLD, 200));
				itFrame.add(itLabel, BorderLayout.CENTER);
				itFrame.pack();
				itFrame.setSize(itFrame.getWidth()*3,itFrame.getHeight());
				new Thread(new Runnable() {
					@Override
					public void run() {
						try {
							controler.run();
						} catch (Exception e2) {
							if(!Gui.worked.contains(e2.getMessage())) {
								JOptionPane.showMessageDialog(frame, e2.getClass().getSimpleName()+": "+e2.getMessage());
								Gui.worked.add(e2.getMessage());
							}
						}
						itFrame.setVisible(false);
					}
				}).start();
				itFrame.setVisible(true);
		}
		else if(e.getActionCommand().equals("add")) {
			list.addItem(classTextField.getText()+(singBox.isSelected()?", Singleton":""));
			classTextField.setText("");
			singBox.setSelected(false);
		}
		else if(e.getActionCommand().startsWith("prov,"))
			allMethods.get(e.getActionCommand().split(",")[1]).provider = !allMethods.get(e.getActionCommand().split(",")[1]).provider;
		else if(e.getActionCommand().startsWith("add,")) {
			BindMethod method = allMethods.get(e.getActionCommand().split(",")[1]);
			String text = allMethods.get(e.getActionCommand().split(",")[1]).text.getText();
			method.texts.addItem(text);
			method.text.setText("");
			Map map = new HashMap<>();
			method.pTextss.put(text, map);
			for(Class clas:method.params) {
				map.put(clas, method.pTexts.get(clas).getText());
				method.pTexts.get(clas).setText("");
			}
		}
		else if(e.getActionCommand().startsWith("text,")) {
			
		}
	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		itLabel.setText(event.getIteration()+"");
	}
	
}
