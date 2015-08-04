package beeSim_V3;

import repast.simphony.context.Context;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.parameter.Parameters;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.continuous.NdPoint;
import repast.simphony.space.grid.Grid;
import repast.simphony.util.ContextUtils;

// the Flower class
public class Flower {
	public double avgProductionRate = 0.25;
	public double spawnRate = 0.00001;
	
	private ContinuousSpace<Object> space;
	private Grid<Object> grid;
	public double food;
	public double productionRate;
	private Context<Object> context;
	private int width;
	private int height;
	private boolean alive = true;
	private double maxNectar;
	public double size;
	public double density;
	
	// The flower constructor
	public Flower(ContinuousSpace<Object> spc, Grid<Object> grd, double f, double s, double d) {
		// define initial properties of flowers
		space = spc;
		grid = grd;
		food = f;
		maxNectar = f;
		size = s;
		density = d;
		// get parameters from simulation environment
		Parameters p = RunEnvironment.getInstance().getParameters();
		//int width = (Integer)p.getValue("width");
		//int height = (Integer)p.getValue("height");
		productionRate = avgProductionRate*RandomHelper.nextDoubleFromTo(0.5, 2);
	}
	
	@ScheduledMethod(start=1, interval=1)
	public void update(){
		// add and remove flowers randomly
		/*if(RandomHelper.nextDoubleFromTo(0, 1) < spawnRate){
			context = ContextUtils.getContext(this);
			Flower flower = new Flower(space, grid, RandomHelper.nextDoubleFromTo(0,200));
			context.add(flower);
			NdPoint pt = space.getLocation(flower);
			grid.moveTo(flower, (int) pt.getX(), (int) pt.getY());
		}
		if(RandomHelper.nextDoubleFromTo(0, 1) < spawnRate){
			space.moveTo(this, (int)width/2, (int)height/2);
			NdPoint pt = space.getLocation(this);
			grid.moveTo(this, (int) pt.getX(), (int) pt.getY());
			alive = false;
			food = 0;
		}*/
		// increment nectar of flower patch
		if(alive){
			if(food > maxNectar/2){
				// asymptote at 200 nectar units
				double increment = productionRate*((maxNectar-food)/(maxNectar/2));
				food += increment;
			}
			else{
			food += productionRate;
			}
		}
	}
	
	public int isAlive(){
		if(alive){
			return 1;
		}
		else{
			return 0;
		}
	}
	
	@ScheduledMethod(start=1, interval=50)
	public void println(){
		//System.out.println(food);
	}
	
	public double getIconSize(){
		return size*3;
	}
}