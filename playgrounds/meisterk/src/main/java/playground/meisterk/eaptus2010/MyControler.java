package playground.meisterk.eaptus2010;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import org.matsim.core.controler.Controler;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.StrategyManager;
import org.matsim.core.replanning.selectors.PlanSelector;


public class MyControler extends Controler {

	public static final String FILENAME_ChangeProbStats = "changeProbStats.txt";
	
	private ChangeProbStats changeProbStats = null;
	protected int movingAverageSkipAndPeriod = 50;
	
	public MyControler(String[] args) {
		super(args);
		// TODO Auto-generated constructor stub
	}
	
	@Override
	protected void loadControlerListeners() {
		// TODO Auto-generated method stub
		super.loadControlerListeners();
		
		// erzeuge ChangeProbStats objekt
		List<PlanStrategyImpl> strategies = this.strategyManager.getStrategies();
		
		ExpBetaPlanChanger2 expBetaChanger2 = null;
		for (PlanStrategyImpl planStrategy : strategies) {
			
			PlanSelector planSelector = planStrategy.getPlanSelector();
			
			if (planSelector.getClass().equals(ExpBetaPlanChanger2.class)) {
				expBetaChanger2 = (ExpBetaPlanChanger2) planSelector;
			}
			
		}
		
		try {
			this.changeProbStats = new ChangeProbStats(
					this.population, 
					this.getControlerIO().getOutputFilename(FILENAME_ChangeProbStats), 
					expBetaChanger2, 
					this.movingAverageSkipAndPeriod);
			this.addControlerListener(this.changeProbStats);
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
	}

	@Override
	protected StrategyManager loadStrategyManager() {
		StrategyManager manager = new StrategyManager();
		MyStrategyManagerConfigLoader.load(this, manager);
		return manager;
	}
	
	/* ===================================================================
	 * main
	 * =================================================================== */

	public static void main(final String[] args) {
		if ((args == null) || (args.length == 0)) {
			System.out.println("No argument given!");
			System.out.println("Usage: Controler config-file [dtd-file]");
			System.out.println();
		} else {
			final MyControler controler = new MyControler(args);
			controler.run();
		}
		System.exit(0);
	}

	public int getMovingAverageSkipAndPeriod() {
		return movingAverageSkipAndPeriod;
	}

	public void setMovingAverageSkipAndPeriod(int movingAverageSkipAndPeriod) {
		this.movingAverageSkipAndPeriod = movingAverageSkipAndPeriod;
	}
	
}
