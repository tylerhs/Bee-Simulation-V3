package beeSim_V3;

import java.util.ArrayList;

import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.parameter.Parameters;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.grid.Grid;

// class representing the bee hive, its properties, and abilities
public class Hive {
	
	// instantiate all changing variables of the hive
	private ContinuousSpace<Object> space;
	private Grid<Object> grid;
	public double food;
	public ArrayList<Bee> dancingBees;
	private Parameters p;
	private int numFlowers;
	private double hiveInitFood;
	
	// GLOBALS
	public double foodCapacity = 35000.0;
	
	// Hive constructor
	public Hive(ContinuousSpace<Object> space, Grid<Object> grid, double f) {
		// define initial hive properties
		this.space = space;
		this.grid = grid;
		food = f;
		dancingBees = new ArrayList<Bee>();
		// get parameters from simulation environment
		p = RunEnvironment.getInstance().getParameters();
		hiveInitFood = (double)p.getValue("hiveInitFood");
	}
	
	public double getFood() {
		return food - hiveInitFood;
	}
	
	public double getDanceAmount() {
		return dancingBees.size();
	}
	
	/*
	// remove dances if too many are stored in the hive
	@ScheduledMethod(start = 2, interval = 5)
	public void trimDances() {
		if(danceDistances.size() > 30){
			for(int i=0; i<danceDistances.size()-30; i++){
				danceDistances.remove(0);
				danceAngles.remove(0);
			}
		}
		//System.out.println("Number of Dances: " + danceDistances.size());
	}
	
	// remove outdated dances
	@ScheduledMethod(start = 1, interval = 125)
	public void regulateDances() {
		if(danceDistances.size() > 0){
			danceDistances.remove(0);
			danceAngles.remove(0);
		}
		
	}*/
}
