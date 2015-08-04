package beeSim_V3;

import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.parameter.Parameters;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.continuous.NdPoint;
import repast.simphony.space.grid.Grid;

// The Clock class
public class Clock {
	
	private double timeRate;
	public double time = 7.0;
	private Grid<Object> grid;
	private ContinuousSpace<Object> space;
	private int width;
	private int height;
	
	// The Clock constructor
	public Clock(double tr, Grid<Object> grid, ContinuousSpace<Object> space, int w, int h) {
		// define initial properties of the clock
		timeRate = tr;
		this.space = space;
		this.grid = grid;
		width = w;
		height = h;
	}
	
	// update time and move clock across screen
	@ScheduledMethod(start = 1, interval = 1)
	public void update() {
		if(time >= 24.0) {
			time = 0;
		}
		time += timeRate;
		space.moveTo(this, time*(width/24.0),height-10);
		NdPoint myPoint = space.getLocation(this);
		grid.moveTo(this, (int) myPoint.getX(), (int) myPoint.getY());
	}
}
