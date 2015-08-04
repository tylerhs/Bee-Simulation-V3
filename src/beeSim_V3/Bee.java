package beeSim_V3;

// all java and repast imports
import java.util.List;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.parameter.Parameters;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.SpatialMath;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.continuous.NdPoint;
import repast.simphony.space.grid.Grid;
import repast.simphony.space.grid.GridPoint;

// The Bee Class, representing all things bee
public class Bee {
	
	// VARIABLES
	private ContinuousSpace<Object> space;
	private Grid<Object> grid;
	public double food;
	public double energy;
	public Flower targetFlower;
	private String state;
	private Hive hive;
	private Clock clock;
	private double currentAngle;
	private double endForagingTime;
	private double startForagingTime;
	private double tempTime;
	public double lastFlowerNectar;
	private double followDist;
	public double danceDuration;
	private Parameters p;
	private List<Flower> flowers;
	
	// SIMULATION PARAMETERS
	private double colonySize;
	private double maxFlowerNectar;
	private double lowMetabolicRate;			// metabolic rate during low activity states
	private double highMetabolicRate;			// metabolic rate during high activity states
	
	// OTHER PARAMETERS
	private double foodTransferRate = 1.0;		// how quickly food is transferred to hive
	private double avgStartForagingTime = 9.0;	// hour that foraging starts
	private double avgEndForagingTime = 17.0;	// hour that foraging ends
	private double forageProb = 0.0015;			// probability that bee will forage
	private double scoutProb = 0.7;				// proportion of bees willing to scout
	private double danceProbability = 0.03;		// probability that bee will dance (if good resource)
	private double foragingRate = 2;			// the rate at which bees can extract from flowers
	private double forageNoise = 0.02;			// probability that bees will move between flowers
	private boolean waggle = true;				// toggle waggle dance on or off
	private double maxCrop = 48;				// maximum volume of bee's crop in uL
	private double startingCrop = 23;			// volume of bee's crop before foraging
	private double lowCrop = 9;					// volume of bee's crop before returning to hive
	
	
	// bee constructor
 	public Bee(ContinuousSpace<Object> s, Grid<Object> g, Hive h, Clock c, List<Flower> f) {
 		
 		// initialize properties of bee
		space = s;
		grid = g;
		food = startingCrop;
		targetFlower = null;
		state = "SLEEPING";
		hive = h;
		clock = c;
		flowers = f;
		
		// define semi-random preferred foraging times for bee
		startForagingTime = RandomHelper.nextDoubleFromTo(avgStartForagingTime - 2.0,
														avgStartForagingTime + 2.0);
		endForagingTime = RandomHelper.nextDoubleFromTo(avgEndForagingTime - 2.0,
														avgEndForagingTime + 2.0);
	}
	
	// update bee based on which state it's in, run every tick
	@ScheduledMethod(start = 1, interval = 1)
	public void update() {
		updateParameters();			// update bee coefficients using parameters UI
		killBee();					// (possibly) kill the bee (not currently used)
		lowFoodReturn();
		if(state == "SLEEPING"){
			sleep();
		}
		if(state == "SCOUTING"){
			scout();
		}
		if(state == "TARGETING"){
			target();
		}
		if(state == "HARVESTING"){
			harvest();
		}
		if(state == "RETURNING"){
			returnToHive();
		}
		if(state == "AIMLESSSCOUTING"){
			aimlessScout();
		}
		if(state == "DANCING"){
			dance();
		}
		if(state == "FOLLOWING"){
			follow();
		}
	}
	
	public void killBee(){
		if(food <= 0){
			state = "DEAD";
		}
	}
	
	public void updateParameters(){
		// get parameters from simulation environment and store them in the bee class
		p = RunEnvironment.getInstance().getParameters();
		maxFlowerNectar = (double)p.getValue("maxFlowerNectar");
		lowMetabolicRate = (double)p.getValue("lowMetabolicRate");
		highMetabolicRate = (double)p.getValue("highMetabolicRate");
		colonySize = (int)p.getValue("colonySize");
	}
	
	public void lowFoodReturn(){
		if(food < lowCrop){
			if(state != "RETURNING" && state!= "DANCING" && state != "SLEEPING"){
				state = "RETURNING";
			}
		}
	}
	
	  ///////////////////
	 //  BEE ACTIONS  //
	///////////////////
	
	// method performed if bee is sleeping
	private void sleep() {
		targetFlower = null;
		if(startForaging()) {
			// add food to bee's crop for upcoming foraging run
			if(food < startingCrop){
				hive.food -= (startingCrop - food);
				food = startingCrop;
			}
			if(startScouting() || hive.dancingBees.size() <= 0) {
				state = "SCOUTING";
				currentAngle = RandomHelper.nextDoubleFromTo(0, 2*Math.PI);
			}
			else{ // else follow a dance
				// if bee follows dance, store angle and distance in the bee object
				int index = RandomHelper.nextIntFromTo(0, hive.dancingBees.size() - 1);
				NdPoint flowerLoc = space.getLocation(hive.dancingBees.get(index).targetFlower);
				NdPoint myLoc = space.getLocation(this);
				double ang = SpatialMath.calcAngleFor2DMovement(space, myLoc, flowerLoc);
				double dist = space.getDistance(flowerLoc, myLoc);
				
				// start following dance at correct angle
				currentAngle = ang;
				followDist = dist;
				state = "FOLLOWING";
			}
		}
		else { // continue sleeping
			hive.food -= lowMetabolicRate/2;
			hover(grid.getLocation(hive));
			// update energy and food storage
			if(food > startingCrop){
				food -= foodTransferRate;
				hive.food += foodTransferRate;
			}
		}
	}
	
	// method performed if bee is scouting
	private void scout() {
		targetFlower = null;
		// fly to flower if nearby
		if(foundFlower()) {
			state = "TARGETING";
		}
		// otherwise keep scouting
		else{
			// change angle randomly during flight
			currentAngle += RandomHelper.nextDoubleFromTo(-Math.PI/8, Math.PI/8);
			space.moveByVector(this, 4, currentAngle,0);
			NdPoint myPoint = space.getLocation(this);
			grid.moveTo(this, (int) myPoint.getX(), (int) myPoint.getY());
			food -= highMetabolicRate;
		}
	}
	
	// method performed if bee has just left flower and looking for another
	private void aimlessScout() {
		targetFlower = null;
		// if bee is sufficiently far from flower, look for other flowers
		if((clock.time - tempTime) > 0.15) {
			state = "SCOUTING";
		}
		// otherwise keep flying
		else{
			currentAngle += RandomHelper.nextDoubleFromTo(-Math.PI/8, Math.PI/8);
			space.moveByVector(this, 4, currentAngle,0);
			NdPoint myPoint = space.getLocation(this);
			grid.moveTo(this, (int) myPoint.getX(), (int) myPoint.getY());
			//energy -= exhaustionRate;
			food -= highMetabolicRate;
		}
	}
		
	// method performed if bee is targeting a flower
	private void target() {
		// find distance to flower
		double dist = grid.getDistance(grid.getLocation(this),
									   grid.getLocation(targetFlower));
		// if close to flower start harvesting nectar
		if(dist < 3) {
			state = "HARVESTING";
		}
		// if VERY far from flower, it must have died. start scouting
		else if(dist > 200){
			state = "SCOUTING";
		}
		// else keep moving toward flower
		else{
			moveTowards(grid.getLocation(targetFlower), 3);
			food -= highMetabolicRate;
		}
	}
	
	// method performed if bee is harvesting on a flower
	private void harvest() {
		// if VERY far from flower, it must have died. start scouting
		if(grid.getDistance(grid.getLocation(targetFlower),grid.getLocation(this)) > 200) {
			state = "SCOUTING";
		}
		// if crop storage is full, return to hive with information
		else if(food >= maxCrop) {
			if(targetFlower != null) {
				lastFlowerNectar = targetFlower.food;
			}
			state = "RETURNING";
		}
		// if daylight is diminishing, return to hive
		else if(clock.time > (endForagingTime + 1.0)){
			lastFlowerNectar = 0;
			state = "RETURNING";
		}
		// if flower loses all nectar, start scouting for new flower
		else if(targetFlower.food <= 0){
			state = "AIMLESSSCOUTING";
			tempTime = clock.time;
		}
		// semi-random decision to scout for new flower if current flower has low food
		else if(RandomHelper.nextIntFromTo(0,(int)(maxFlowerNectar/4)) > targetFlower.food &&
				RandomHelper.nextDoubleFromTo(0,1) < forageNoise){
			state = "AIMLESSSCOUTING";
			tempTime = clock.time;
		}
		// otherwise continue harvesting current flower
		else{
			hover(grid.getLocation(targetFlower));
			targetFlower.food -= foragingRate;
			food += foragingRate;
			food -= lowMetabolicRate;
		}
	}
	
	// method performed if bee is returning to the hive
	private void returnToHive() {
		GridPoint hiveLoc = grid.getLocation(hive);
		// if bee is inside hive
		if(grid.getDistance(grid.getLocation(this), hiveLoc) < 10) {
			// decide whether or not to dance (biased by nectar of last resource)
			if((RandomHelper.nextIntFromTo((int)(maxFlowerNectar/3), 
			    (int)(maxFlowerNectar/danceProbability)) < lastFlowerNectar)
			       && waggle && targetFlower != null){
				danceDuration = lastFlowerNectar/5;
				hive.dancingBees.add(this);
				state = "DANCING";
			}
			else{
				state = "SLEEPING";
			}
		}
		else{
			// otherwise continue toward hive
			moveTowards(hiveLoc, 5);
			food -= highMetabolicRate;
		}
	}
	
	// they can dance if they want to, they can leave their friends behind
	private void dance() {
		// if they are done dancing, start sleeping
		if(danceDuration <= 0){
			hive.dancingBees.remove(this);
			state = "SLEEPING";
		}
		else{ // otherwise keep dancing and decrease duration of dance
			danceDuration--;
		}
	}
	
	// method performed if bee is following a dance
	private void follow() {
		// calculate distance back to hive
		double distToHive = grid.getDistance(grid.getLocation(this),grid.getLocation(hive));
		// if close to flower from dance information, start scouting
		if(distToHive > followDist - 5) {
			state = "SCOUTING";
		}
		// otherwise keep flying in direction of dance
		else{
			// deviate slightly from correct angle because bee's aren't perfect (I don't think)
			currentAngle += RandomHelper.nextDoubleFromTo(-Math.PI/50, Math.PI/50);
			space.moveByVector(this, 4, currentAngle,0);
			NdPoint myPoint = space.getLocation(this);
			grid.moveTo(this, (int) myPoint.getX(), (int) myPoint.getY());
			food -= highMetabolicRate;
		}
	}
	
	  ////////////////
	 //  BOOLEANS  //
	////////////////
	
	// decide whether or not to start foraging while in hive
	private boolean startForaging() {
		// check if it is an acceptable time to forage
		if(clock.time > startForagingTime && clock.time < endForagingTime) {
			if(RandomHelper.nextDoubleFromTo(0, 1) < forageProb) {
				return true;
			}
		}
		return false;
	}
	
	// decide whether to start scouting or to follow a dance
	private boolean startScouting(){
		// calculate motivation to forage based on amount of food in hive
		double motivation = 1 - (hive.food/hive.foodCapacity);
		
		// calculate motivation to follow dance based on amount of dances
		double danceFactor = (1-Math.exp((-1/colonySize)*hive.dancingBees.size()));
		
		// calculate whether or not bee should scout
		if(RandomHelper.nextDoubleFromTo(0,1) < scoutProb*motivation + 
				(1-scoutProb)*motivation*(1-danceFactor)*scoutProb){
			return true;
		}
		
		// otherwise follow a dance
		return false;
	}
	
	// return true if the bee has found a flower
	private boolean foundFlower() {
		// get the grid location of this Bee
		GridPoint myPoint = grid.getLocation(this);
		GridPoint theirPoint;
		
		// if the bee is within range of a flower's size, target that flower
		for(int i=0; i<flowers.size(); i++){
			theirPoint = grid.getLocation(flowers.get(i));
			if(grid.getDistance(myPoint, theirPoint) < (flowers.get(i).size/100.0)){
				targetFlower = flowers.get(i);
				return true;
			}
		}
		return false;
	}
	
	  //////////////////////
	 //  MOVING METHODS  //
	//////////////////////
	
	// allow the bee to hover around a certain point
	private void hover(GridPoint pt){
		// if too far away, move back to point
		if(grid.getDistance(grid.getLocation(this), pt) > 10) {
			moveTowards(pt, 2);
		}
		// otherwise hover randomly
		else{
			moveAmount(RandomHelper.nextIntFromTo(-1, 1),
					   RandomHelper.nextIntFromTo(-1,1));
		}
	}
	
	// move toward point by certain amount
	public void moveTowards(GridPoint pt, int amount) {
		NdPoint myPoint = space.getLocation(this);
		NdPoint otherPoint = new NdPoint(pt.getX(), pt.getY());
		double angle = SpatialMath.calcAngleFor2DMovement(space, myPoint,
				otherPoint);
		space.moveByVector(this, amount, angle, 0);
		myPoint = space.getLocation(this);
		grid.moveTo(this, (int) myPoint.getX(), (int) myPoint.getY());
	}
	
	// move an exact amount in x and y direction
	public void moveAmount(int x, int y) {
		space.moveByDisplacement(this, x, y);
		NdPoint myPoint = space.getLocation(this);
		grid.moveTo(this, (int) myPoint.getX(), (int) myPoint.getY());
	}
	
	  ///////////////////////////////
	 //  BEE STATE COUNT METHODS  //
	///////////////////////////////
	public int isSleeping(){
		if(state == "SLEEPING")  { return 1; }
		else					 { return 0; }
	}
	public int isWandering(){
		if(state == "SCOUTING" || state == "AIMLESSSCOUTING") { return 1; }
		else 					 { return 0; }
	}
	public int isTargeting(){
		if(state == "TARGETING") { return 1; }
		else					 { return 0; }
	}
	public int isHarvesting(){
		if(state == "HARVESTING"){ return 1; }
		else					 { return 0; }
	}
	public int isReturning(){
		if(state == "RETURNING") { return 1; }
		else					 { return 0; }
	}
	public int isFollowing(){
		if(state == "FOLLOWING") { return 1; }
		else					 { return 0; }
	}
	public int isDancing(){
		if(state == "DANCING")   { return 1; }
		else					 { return 0; }
	}
	public Flower flowerTarget(){
		return targetFlower;
	}
}
