package beeSim_V3;

import java.util.ArrayList;
import java.util.List;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import com.vividsolutions.jts.algorithm.match.AreaSimilarityMeasure;

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
public class SimBuilder implements ContextBuilder<Object> {
	
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
		double timeRate = (double)p.getValue("secondsPerFrame")/3600.0;
		double hiveInitFood = (double)p.getValue("hiveInitFood");
		double nectarDensity = (double)p.getValue("nectarDensity");
		
		//RunEnvironment.setScheduleTickDelay(20);
		
		  /////////////////////////////////
		 //  READ DENSITY MAP CSV FILE  //
		/////////////////////////////////
		
		// load file and define properties for reading file
		String densityFile = "/Users/Tyler/Google Drive/School/Research/"
				+ "Summer Research 2015/Bee Simulation/processing_analyzeVector/flowers.csv";
		BufferedReader br = null;
		String line = "";
		String cvsSplitBy = ",";
		int rowCount = 0;
		
		// instantiate 2 dimensional arraylist for storing densities
		List<Integer> xCoords = new ArrayList<Integer>();
		List<Integer> yCoords = new ArrayList<Integer>();
		List<Integer> boundingBoxes = new ArrayList<Integer>();
		List<Integer> areas = new ArrayList<Integer>();
		
		try {
			br = new BufferedReader(new FileReader(densityFile));
			line = br.readLine();
			String[] windowSize = line.split(cvsSplitBy);
			width = Integer.parseInt(windowSize[0]);
			height = Integer.parseInt(windowSize[1]);
			//System.out.println("Size: " + width + ", " + height);
			while ((line = br.readLine()) != null) {			// loop across every row (line)
				String[] flowerProperties = line.split(cvsSplitBy);	// split columns into separate strings
				
				xCoords.add(Integer.parseInt(flowerProperties[0]));
				yCoords.add(Integer.parseInt(flowerProperties[1]));
				boundingBoxes.add(Integer.parseInt(flowerProperties[2]));
				areas.add(Integer.parseInt(flowerProperties[3]));
				System.out.println(flowerProperties[0] + ", " + flowerProperties[1]
						+ ", " + flowerProperties[2]  + ", " + flowerProperties[3]);
			}
			
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
		
		// Add flowers from processing sketch
		List<Flower> flowers = new ArrayList<Flower>();
		for(int i=0; i<xCoords.size(); i++){
			double nectar = areas.get(i)*nectarDensity*0.16;
			double density = ((double)areas.get(i))/boundingBoxes.get(i);
			flowers.add(new Flower(space, grid, nectar, (double)boundingBoxes.get(i), density));
			context.add(flowers.get(i));
			space.moveTo(flowers.get(i), xCoords.get(i), yCoords.get(i));
		}
		
		// Add bees randomly to hive
		List<Bee> bees = new ArrayList<Bee>();
		for(int i=0; i<colonySize; i++) {
			bees.add(new Bee(space, grid, hive, clock, flowers));
			context.add(bees.get(i));
			space.moveTo(bees.get(i), (int)width/2+RandomHelper.nextIntFromTo(-1, 1),
									  (int)height/2+RandomHelper.nextIntFromTo(-1, 1));
		}
		
		// update grid coordinates for all objects
		for (Object obj : context) {
			NdPoint pt = space.getLocation(obj);
			grid.moveTo(obj, (int) pt.getX(), (int) pt.getY());
		}
		
		return context;
	}
}
