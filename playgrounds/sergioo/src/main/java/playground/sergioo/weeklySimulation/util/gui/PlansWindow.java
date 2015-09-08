package playground.sergioo.weeklySimulation.util.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.Collection;

import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioUtils;

import playground.sergioo.passivePlanning2012.api.population.BasePerson;
import playground.sergioo.passivePlanning2012.core.population.BasePersonImpl;
import playground.sergioo.visualizer2D2012.LayersPanel;

public class PlansWindow extends JFrame implements ActionListener, KeyListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public enum Mode {
		SELECTED_PLAN, BASE_PLAN, ALL_PLANS, FULL_PLANS;
	}
	
	public enum ModeLayout {
		H_H, H_V, V_H, V_V;
	}
	
	private BasePerson person;
	private Mode mode = Mode.SELECTED_PLAN;
	private ModeLayout modeLayout = ModeLayout.V_V;
	private WeeklyPlanPanel[] panels;
	private Population population;
	private JComboBox<String> comboBox;
	private JPanel jPanel;
	
	public PlansWindow(String filename) {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimPopulationReader(scenario).readFile(filename);
		this.population = scenario.getPopulation();
		Collection<BasePerson> toBeAdded = new ArrayList<BasePerson>();
		for(Person person: population.getPersons().values())
			toBeAdded.add(BasePersonImpl.convertToBasePerson(person));
		for(Person person:toBeAdded) {
			population.getPersons().remove(person.getId());
			population.addPerson(person);
		}
		jPanel = new JPanel();
		comboBox = new JComboBox<String>();
		for(Person person:toBeAdded)
			comboBox.addItem(person.getId().toString());
		comboBox.addActionListener(this);
		this.setLayout(new BorderLayout());
		add(comboBox, BorderLayout.NORTH);
		init();
		add(jPanel, BorderLayout.CENTER);
		pack();
		jPanel.addKeyListener(this);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}
	public void init() {
		person = (BasePerson) population.getPersons().get(Id.createPersonId((String) comboBox.getSelectedItem()));
		switch(mode) {
		case SELECTED_PLAN:
			paintSelectedPlan();
			break;
		case BASE_PLAN:
			paintBasePlan();
			break;
		case ALL_PLANS:
			paintAllPlans();
			break;
		case FULL_PLANS:
			paintFullPlans();
			break;
		}
		pack();
		jPanel.requestFocus();
	}
	@Override
	public void actionPerformed(ActionEvent e) {
		init();
	}
	private void paintFullPlans() {
		int n = person.getPlans().size();
		panels = new WeeklyPlanPanel[n];
		int i=0;
		for(Plan plan:person.getPlans()) {
			panels[i] = new WeeklyPlanPanel(plan, modeLayout.toString().startsWith("H"));
			panels[i++].setPreferredSize(modeLayout.toString().startsWith("H"), n);
		}
		setupLayout(n);
	}
	private void paintAllPlans() {
		int n = person.getPlans().size()+1;
		panels = new WeeklyPlanPanel[n];
		panels[0] = new WeeklyPlanPanel(person.getBasePlan(), modeLayout.toString().startsWith("H"));
		panels[0].setPreferredSize(modeLayout.toString().endsWith("H"), n);
		int i=1;
		for(Plan plan:person.getPlans()) {
			panels[i] = new WeeklyPlanPanel(plan, modeLayout.toString().startsWith("H"));
			panels[i++].setPreferredSize(modeLayout.toString().endsWith("H"), n);
		}
		setupLayout(n);
	}
	private void paintBasePlan() {
		panels = new WeeklyPlanPanel[1];
		panels[0] = new WeeklyPlanPanel(person.getBasePlan(), modeLayout.toString().startsWith("H"));
		panels[0].setPreferredSize(modeLayout.toString().endsWith("H"), 1);
		setupLayout(1);
	}
	private void paintSelectedPlan() {
		panels = new WeeklyPlanPanel[1];
		panels[0] = new WeeklyPlanPanel(person.getSelectedPlan(), modeLayout.toString().startsWith("H"));
		panels[0].setPreferredSize(modeLayout.toString().endsWith("H"), 1);
		setupLayout(1);
	}
	public void setupLayout(int n) {
		if(jPanel!=null)
			for(Component component:jPanel.getComponents())
				jPanel.remove(component);
		int rows = 1, cols = 1;
		if(modeLayout.toString().endsWith("H"))
			rows = n;
		else
			cols = n;
		jPanel.setLayout(new GridLayout(rows, cols));
		for(LayersPanel panel:panels)
			jPanel.add(panel);
	}
	@Override
	public void keyTyped(KeyEvent e) {
		switch(e.getKeyChar()) {
		case KeyEvent.VK_1:
			mode = Mode.SELECTED_PLAN;
			break;
		case KeyEvent.VK_2:
			mode = Mode.BASE_PLAN;
			break;
		case KeyEvent.VK_3:
			mode = Mode.ALL_PLANS;
			break;
		case KeyEvent.VK_4:
			mode = Mode.FULL_PLANS;
			break;
		case 'h':
			modeLayout = ModeLayout.H_H;
			break;
		case 'v':
			modeLayout = ModeLayout.V_V;
			break;
		case 'g':
			modeLayout = ModeLayout.H_V;
			break;
		case 'b':
			modeLayout = ModeLayout.V_H;
			break;
		}
		init();
	}
	@Override
	public void keyPressed(KeyEvent e) {
		
	}
	@Override
	public void keyReleased(KeyEvent e) {
		
	}
	public static void main(String[] args) {
		new PlansWindow(args[0]).setVisible(true);
	}
}
