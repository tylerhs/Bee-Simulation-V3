package beeSim_V3;

import java.util.ArrayList;
import java.util.List;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import repast.simphony.parameter.Parameters;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.grid.Grid;
import repast.simphony.context.Context;
import repast.simphony.context.space.continuous.ContinuousSpaceFactory;
import repast.simphony.context.space.continuous.ContinuousSpaceFactoryFinder;
import repast.simphony.context.space.grid.GridFactory;
import repast.simphony.context.space.grid.GridFactoryFinder;
import repast.simphony.dataLoader.ContextBuilder;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.continuous.NdPoint;
import repast.simphony.space.continuous.RandomCartesianAdder;
import repast.simphony.space.grid.GridBuilderParameters;
import repast.simphony.space.grid.SimpleGridAdder;
import repast.simphony.space.grid.WrapAroundBorders;
import repast.simphony.ui.RunOptionsModel;

// setup simulation environment
public class SimBuilder_V1 implements ContextBuilder<Object> {
	
	public int width;
	public int height;
	private double maxDensity = 0;
	
	// SIMULATION PARAMETERS
	private double flowerThreshold = 0.95;	// the density threshold (0-1) to make a flower
	private int flowerResolution = 20;		// the resolution used when creating flowers
	
	@Override
	public Context build(Context<Object> context) {
		context.setId("beeSim_V3");
		
		// Get important parameters from simulation environment
		Parameters p = RunEnvironment.getInstance().getParameters();
		//int width = (Integer)p.getValue("width");
		//int height = (Integer)p.getValue("height");
		int colonySize = (Integer)p.getValue("colonySize");
		double timeRate = (double)p.getValue("timeRate");
		double hiveInitFood = (double)p.getValue("hiveInitFood");
		double maxFlowerNectar = (double)p.getValue("maxFlowerNectar");
		
		//RunEnvironment.setScheduleTickDelay(20);
		
		  /////////////////////////////////
		 //  READ DENSITY MAP CSV FILE  //
		/////////////////////////////////
		
		// load file and define properties for reading file
		String densityFile = "/Users/Tyler/Google Drive/School/Research/"
				+ "Summer Research 2015/Bee Simulation/densityMap.csv";
		BufferedReader br = null;
		String line = "";
		String cvsSplitBy = ",";
		int rowCount = 0;
		
		// instantiate 2 dimensional arraylist for storing densities
		ArrayList<ArrayList<Double>> densityTable = new ArrayList<ArrayList<Double>>();
		try {
			br = new BufferedReader(new FileReader(densityFile));
			while ((line = br.readLine()) != null) {			// loop across every row (line)
				String[] densityRow = line.split(cvsSplitBy);	// split columns into separate strings
				densityTable.add(new ArrayList<Double>());	
				for(int i=0; i<densityRow.length; i++){			// loop across every column
					if(densityRow[i].equals("nan")){			// if nan, set to 0.0
						densityTable.get(rowCount).add(0.0);
					}
					else{										// else convert string to double
						double tempDensity = Double.parseDouble(densityRow[i]);
						if(tempDensity > maxDensity){
							maxDensity = tempDensity;
						}
						densityTable.get(rowCount).add(tempDensity);
					}
					//System.out.print(densityTable.get(rowCount).get(i) + "\t");
				}
				//System.out.println("");
				rowCount++;
				width = densityRow.length;
			}
			height = rowCount;
		// catch errors if they are present
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
		  //////////////////////////////////////
		 //  SETUP SIMULATION & ADD OBJECTS  //
		//////////////////////////////////////
		
		// Setup continuous space 
		ContinuousSpaceFactory spaceFactory = 
			ContinuousSpaceFactoryFinder.createContinuousSpaceFactory(null);
		ContinuousSpace<Object> space = 
			spaceFactory.createContinuousSpace("space", context,
			new RandomCartesianAdder<Object>(),
			new repast.simphony.space.continuous.WrapAroundBorders(), width, height);
		
		// Setup discrete grid
		GridFactory gridFactory = GridFactoryFinder.createGridFactory(null);
		Grid<Object> grid = gridFactory.createGrid("grid",
			context, new GridBuilderParameters<Object>(new WrapAroundBorders(),
			new SimpleGridAdder<Object>(), true, width, height));
		
		// Add clock and start time
		Clock clock = new Clock(timeRate, grid, space, width, height);
		context.add(clock);
		space.moveTo(clock, (int)width/2,(int)height/2);
		
		// Add hive and move to center of window
		Hive hive = new Hive(space, grid, hiveInitFood);
		context.add(hive);
		space.moveTo(hive, (int)width/2, (int)height/2);
		
		// Add bees randomly to hive
		List<Bee> bees = new ArrayList<Bee>();
		for(int i=0; i<colonySize; i++) {
			bees.add(new Bee(space, grid, hive, clock));
			context.add(bees.get(i));
			space.moveTo(bees.get(i), (int)width/2+RandomHelper.nextIntFromTo(-1, 1),
									  (int)height/2+RandomHelper.nextIntFromTo(-1, 1));
		}
		
		
		// Add flowers from density map
		for(int j=0; j<height; j+= flowerResolution){
			for(int i=0; i<width; i += flowerResolution){
				double tempDensity = densityTable.get(j).get(i);
				if(tempDensity > flowerThreshold*maxDensity){
					Flower flower = new Flower(space, grid, 
						maxFlowerNectar*Math.pow(tempDensity/maxDensity, 8));
					context.add(flower);
					space.moveTo(flower, i, j);
				}
			}
		}
		
		// Add flowers randomly to screen
		/*for(int i=0; i<numFlowers; i++) {
			context.add(new Flower(space, grid, RandomHelper.nextDoubleFromTo(0, 200)));
		}*/
		
		// update grid coordinates for all objects
		for (Object obj : context) {
			NdPoint pt = space.getLocation(obj);
			grid.moveTo(obj, (int) pt.getX(), (int) pt.getY());
		}
		
		return context;
	}
}
