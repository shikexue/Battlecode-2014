package team073;

import battlecode.common.*;

import java.util.*;

import team073.BugMove;
import team073.VectorFunctions;
import team073.Constants;
import team073.RobotData;

public class RobotPlayer {
	static RobotController rc;
	static Random randall = new Random();
	static Direction allDirections[] = Direction.values();

	static MapLocation pastrLoc;
	static int noisetowerCount;
	
	boolean taskSet = false;
	
	static int pastrsOrderedThisTurn = 0; //TODO: THIS MUST BE RESET AT THE BEGINNING OF EVERY TURN
	
	static int pastrBeingMadeChan = 0;
	static int makePastrChan = 1; //channel will have -1 if no pastures ordered
	static int makePastrLocChan[] = new int[]{2,3,4,20};
	
	static int noisetowersOrderedThisTurn = 0; //TODO: THIS MUST BE RESET AT THE BEGINNING OF EVERY TURN
	
	static int noisetowerBeingMadeChan = 5;
	static int makeNoisetowerChan = 6;
	static int makeNoisetowerLocChan[] = new int[]{7,8,9};	

	static int cowChannel = 4000;
	static int cowLocChannel = 4001;
	static int towerGetPathChan = 4002; //TODO: make this an array so we can have 1+ tower?
	static int towerLocChan = 4003; //TODO: same as above, allow for 1+ tower
	static int towerShootLocChan = 4004;
	static int bestPastrChan = 4005;
	
	static int foundRallyChan = 5000;
	static int rallyLocChan = 5001;
	static int rallySwarmSizeChan = 5002;
	static int rallySwarmMove = 5003;
	static int rallySwarmHasMoved = 5004;
	//next used channel should start with 11
	
	/*
	 * Calls the run functions of the different types of robot
	 */
	public static void run(RobotController rcIn) {
		try{
			rc = rcIn;
			Constants.Task task = Constants.Task.ATTACKING; // default task
			//If the rally point has yet to be set, set it to the default of a third of the way to
			//the enemy HQ.
			if(rc.readBroadcast(foundRallyChan) == 0){
				ArrayList<MapLocation> pathToEnemyHQ = BugMove.generateBugPath(rc.senseEnemyHQLocation(), rc.getLocation(), rc);
				rc.broadcast(rallyLocChan, VectorFunctions.locToInt(pathToEnemyHQ.get(pathToEnemyHQ.size()/3)));
				rc.broadcast(foundRallyChan, 1);
			}
			MapLocation goal = VectorFunctions.intToLoc(rc.readBroadcast(rallyLocChan)); // default goal
			ArrayList<MapLocation> path = BugMove.generateBugPath(goal, rc.getLocation(), rc, goal, 100000);
			boolean taskSet = false;
			RobotData myData = new RobotData(task, goal, path, taskSet);

			// initialize channels and then make first noisetower and pastr
			if (rc.getType()==RobotType.HQ){
				noisetowerCount = 0;
				rc.broadcast(makeNoisetowerChan, 0);
				rc.broadcast(noisetowerBeingMadeChan, 0);
				rc.broadcast(makePastrChan, 0);
				rc.broadcast(pastrBeingMadeChan, 0);
				rc.yield();
				sendMakeNearbyPastrCommand(5);
				sendMakeDefensiveNoisetowerCommand();
				sendMakeNearbyPastrCommand(5);
				sendMakeDefensiveNoisetowerCommand();
				sendMakeNearbyPastrCommand(5);
				sendMakeDefensiveNoisetowerCommand();
				sendMakeNearbyPastrCommand(5);

//				sendMakeDefensiveNoisetowerCommand(4);
			}
			
			// splitting up behavior based on robot type
			while(true){
				if(rc.getType()==RobotType.HQ){
					runHQ();
				}else if(rc.getType()==RobotType.SOLDIER){
					// to store data like task, goal, path between rounds, have runFoo return them for soldiers, pastrs, towers
					myData = runSoldier(myData.getTask(), myData.getPath(), myData.getGoal(), myData.getTaskSet());
				}else if (rc.getType()==RobotType.NOISETOWER){
					//TODO: fix broadcasting
					//TODO: work with path
					if((path == null || rc.readBroadcast(towerGetPathChan) == 0) && rc.sensePastrLocations(rc.getTeam()).length > 0){
						rc.setIndicatorString(0, "pathing");
						myData.path = getTowerPath(rc);
					} else {
						rc.setIndicatorString(0, "no new path");
					}
					runTower(myData.getPath());
				}


				rc.yield();
			}
		}catch(Exception e){
			e.printStackTrace();}
	}

	/*
	 * Run function of the HQ - try to spawn as many robots as possible
	 */
	private static void runHQ() throws GameActionException 
	{
		rc.setIndicatorString(0, "" + rc.readBroadcast(makeNoisetowerChan));
		rc.setIndicatorString(1, "" + rc.readBroadcast(makePastrChan));
		rc.broadcast(rallySwarmSizeChan, 0); //
		tryToSpawn();
		Combat.HQAttack(rc);
	}


	/*
	 * Runs the noise towers; currently, just shoot in path
	 */
	private static void runTower(ArrayList<MapLocation >path) throws GameActionException{
		if(rc.isActive()){
			BugMove.shootPath(path, towerGetPathChan, bestPastrChan, towerShootLocChan);
		}
	}
	
	/*
	 * Function for the HQ; if more robots can be spawned and there is a free space near the HQ, spawn
	 */	
	private static void tryToSpawn() throws GameActionException

	{	
		if(rc.isActive()&&rc.senseRobotCount()<GameConstants.MAX_ROBOTS){
			for(int i=0;i<8;i++){
				Direction trialDir = allDirections[i];
				if(rc.canMove(trialDir)){
					rc.spawn(trialDir);
					break;
				}
			}
		}
	}

	/*
	 * Runs soldiers - herding practice
	 */
	private static RobotData runSoldier(Constants.Task task, ArrayList<MapLocation> path, MapLocation goal, boolean taskSet) throws GameActionException{
		//TODO: Select group of cows to herd
		//TODO: herd cows towards pastr
		
		
		rc.setIndicatorString(2, "" + task);

		int pastrsToMake = rc.readBroadcast(makePastrChan);
		int pastrsBeingMade = rc.readBroadcast(pastrBeingMadeChan);

		int noisetowersToMake = rc.readBroadcast(makeNoisetowerChan);
		int noisetowersBeingMade = rc.readBroadcast(noisetowerBeingMadeChan);


		rc.setIndicatorString(1, "" +  pastrsToMake + " " + pastrsBeingMade + " " + noisetowersToMake + " " + noisetowersBeingMade);

		//if there is a noisetower to make that isn't being made, make one		
		//if there is a pastr to make that isn't being made, make one
		if (taskSet == false && pastrsToMake > 0 && pastrsBeingMade < pastrsToMake){
			goal = VectorFunctions.intToLoc(rc.readBroadcast(makePastrLocChan[pastrsToMake-1]));
			path = BugMove.generateBugPath(goal, rc.getLocation(), rc);
			
			task = Constants.Task.PASTRMAKING;
			taskSet = true;
			
			rc.broadcast(pastrBeingMadeChan, pastrsBeingMade + 1);

		}
		
		if(taskSet == false && noisetowersToMake > 0 && noisetowersBeingMade < noisetowersToMake){
			goal = VectorFunctions.intToLoc(rc.readBroadcast(makeNoisetowerLocChan[noisetowersToMake-1]));
			path = BugMove.generateBugPath(goal, rc.getLocation(), rc);
						
			task = Constants.Task.TOWERMAKING;

			taskSet = true;
			rc.broadcast(noisetowerBeingMadeChan, noisetowersBeingMade + 1);
		}

		//try to sense cows before movement
		senseNearbyCows(rc); //TODO: is this too expensive?
		
		//if robot is active, move or perform task
		if (rc.isActive()){
			//if have reached goal, perform designated task
			if (nearEnoughToGoal(task, goal)){
				//rc.setIndicatorString(1, "completing Task");
				switch (task){
				case PASTRMAKING:
					rc.setIndicatorString(1,"making PASTR");
					rc.construct(RobotType.PASTR);
					rc.broadcast(makePastrChan, pastrsToMake-1);
					rc.broadcast(pastrBeingMadeChan, pastrsBeingMade - 1);
					//taskSet = false;
					break;
				case TOWERMAKING:
					rc.setIndicatorString(1,"making tower");
					rc.construct(RobotType.NOISETOWER);

					rc.broadcast(makeNoisetowerChan, noisetowersToMake-1);
					rc.broadcast(noisetowerBeingMadeChan, noisetowersBeingMade - 1);
					rc.broadcast(towerLocChan, VectorFunctions.locToInt(rc.getLocation()));
					//taskSet = false;
					break;
				case ATTACKING:
					rc.broadcast(rallySwarmSizeChan, rc.readBroadcast(rallySwarmSizeChan) + 1);
					if(rc.readBroadcast(rallySwarmSizeChan) >= 3){
						rc.broadcast(rallySwarmMove, 1);
					} if(rc.readBroadcast(rallySwarmMove) == 1){
						MapLocation[] enemyPastrs = rc.sensePastrLocations(rc.getTeam().opponent());
						if (enemyPastrs.length > 0){
						goal = enemyPastrs[0];
						}
						else {
							goal = rc.senseEnemyHQLocation();
						}
						path = BugMove.generateBugPath(goal, rc.getLocation(), rc);
						rc.broadcast(rallySwarmHasMoved, rc.readBroadcast(rallySwarmHasMoved) + 1);
						if(rc.readBroadcast(rallySwarmHasMoved) >= 3){
							rc.broadcast(rallySwarmMove, 0);
							rc.broadcast(rallySwarmSizeChan, 0);
							rc.broadcast(rallySwarmHasMoved, 0);
						}
					}
					rc.setIndicatorString(0, ""+rc.readBroadcast(rallySwarmSizeChan));
					rc.setIndicatorString(1, ""+rc.readBroadcast(rallySwarmMove));
					rc.setIndicatorString(2, ""+rc.readBroadcast(rallySwarmHasMoved));
					Robot[] nearbyEnemies = rc.senseNearbyGameObjects(Robot.class,10,rc.getTeam().opponent());
					for(Robot enemy:nearbyEnemies){
						RobotInfo robotInfo = rc.senseRobotInfo(enemy);
						if(rc.isActive() && rc.canAttackSquare(robotInfo.location)){
							rc.attackSquare(robotInfo.location);
						}
					}
					break;
				default:

				}
			}
			// if not already at goal, move towards goal
			// TODO: allow interruptions
			else{
				rc.setIndicatorString(1, "followingPath");
				Robot[] nearbyEnemies = rc.senseNearbyGameObjects(Robot.class,35,rc.getTeam().opponent());
				if (nearbyEnemies.length > 0) {
					switch(task){
					case PASTRMAKING:
					case TOWERMAKING:
					default:
						for(Robot enemy:nearbyEnemies){
							RobotInfo robotInfo = rc.senseRobotInfo(enemy);
							if(rc.isActive() && rc.canAttackSquare(robotInfo.location)){
								rc.attackSquare(robotInfo.location);
							}
						}
						break;
					case ATTACKING:
						for(Robot enemy:nearbyEnemies){
							RobotInfo robotInfo = rc.senseRobotInfo(enemy);
							if(rc.isActive() && rc.canAttackSquare(robotInfo.location)){
								rc.attackSquare(robotInfo.location);
							}
						}
					}
				}
				BugMove.followPath(path);
			}
		}

		return new RobotData(task, goal, path, taskSet);
	}
	
	private static boolean nearEnoughToGoal(Constants.Task task, MapLocation goal){
		switch(task){
		case PASTRMAKING:
		case TOWERMAKING:
			return rc.getLocation().equals(goal);
		case ATTACKING:
			return rc.getLocation().distanceSquaredTo(goal) < 9;
		default:
			return rc.getLocation().equals(goal);
		}
	}

	/*
	 * HQ sends out broadcast that a PASTR should be made
	 * @Location: place where PASTR should be made
	 */
	private static void sendMakeNearbyPastrCommand(int pastrDistance) throws GameActionException{
		int currentMakeCount = rc.readBroadcast(makePastrChan) + pastrsOrderedThisTurn;
		int hqX = rc.senseHQLocation().x;
		int hqY = rc.senseHQLocation().y;
	
		pastrLoc = new MapLocation(VectorFunctions.abs(randall.nextInt(2*pastrDistance)-pastrDistance + hqX),VectorFunctions.abs(randall.nextInt(2*pastrDistance)-pastrDistance + hqY));

		// make sure that is not an obstacle 
		//TODO: try a few times to make sure is a field?
		while(rc.senseTerrainTile(pastrLoc).ordinal() > 1){ //0 NORMAL, 1 ROAD, 2 VOID, 3 OFF_MAP
			pastrLoc = new MapLocation(VectorFunctions.abs(randall.nextInt(2*pastrDistance)-pastrDistance + hqX),VectorFunctions.abs(randall.nextInt(2*pastrDistance)-pastrDistance + hqY));		
		}

		//broadcast that a channel should be made, and in desired position
		// make sure that position is broadcast before soldiers are told to read it
		rc.broadcast(makePastrLocChan[currentMakeCount], VectorFunctions.locToInt(pastrLoc));
		rc.broadcast(makePastrChan, currentMakeCount + 1);	
		
		pastrsOrderedThisTurn++;
		
	}
	
	//make a defensive noisetower close to the nearest constructed pasture
	private static void sendMakeDefensiveNoisetowerCommand() throws GameActionException{
		int currentMakeCount = rc.readBroadcast(makeNoisetowerChan) + noisetowersOrderedThisTurn;
		
		//choose a free location adjacent to where your pasture was created	
		boolean foundLocation = false;
		//TODO: This should never happen, but maybe map location shouldn't default to this anyway?
		MapLocation noisetowerLoc = new MapLocation(0, 0);
		for(int i = -1; i <= 1; i++){
			for(int j = -1; j <= 1; j++){
			noisetowerLoc = VectorFunctions.mladd(pastrLoc, new MapLocation(i, j));
			//make sure noisetower can be placed
			if((i != 0 || j != 0) && rc.senseTerrainTile(noisetowerLoc).ordinal() <= 1) {
				foundLocation = true;
				break;
				}
			}
			if(foundLocation) break;

		}
	
		//broadcast that a channel should be made, and in desired position
		// make sure that position is broadcast before soldiers are told to read it
		rc.broadcast(makeNoisetowerLocChan[currentMakeCount], VectorFunctions.locToInt(noisetowerLoc));
		rc.broadcast(makeNoisetowerChan, currentMakeCount + 1);
		
		noisetowersOrderedThisTurn++;
	}
	
	private static void senseNearbyCows(RobotController rc) throws GameActionException{
		for(MapLocation nearby:MapLocation.getAllMapLocationsWithinRadiusSq(rc.getLocation(), 35)){
			int cows = (int)rc.senseCowsAtLocation(nearby);
			if (cows > rc.readBroadcast(cowChannel) && nearby.distanceSquaredTo(VectorFunctions.intToLoc(rc.readBroadcast(towerLocChan))) <= 361){
				boolean farPasture = true;
				for(MapLocation allyPasture:rc.sensePastrLocations(rc.getTeam())){
					if (nearby.distanceSquaredTo(allyPasture) < GameConstants.PASTR_RANGE)
						farPasture = false;
				}
				for(MapLocation enemyPasture:rc.sensePastrLocations(rc.getTeam().opponent())){
					if (nearby.distanceSquaredTo(enemyPasture) < GameConstants.PASTR_RANGE)
						farPasture = false;
				}
				if (farPasture){
					rc.broadcast(cowChannel, cows);
					rc.broadcast(cowLocChannel, VectorFunctions.locToInt(nearby));
				}
				//TODO vector function that shit
			}
		}
		rc.setIndicatorString(0, ""+rc.readBroadcast(cowLocChannel));
	}
	
	private static ArrayList<MapLocation> getTowerPath(RobotController rc) throws GameActionException{
		MapLocation[] allyPastrLocs = rc.sensePastrLocations(rc.getTeam());
		MapLocation bestTarget = VectorFunctions.intToLoc(rc.readBroadcast(4001));
		rc.broadcast(cowChannel, 0); //reset cow channel for next sensing
		rc.broadcast(towerGetPathChan, 1); //set channel for having found a path
		MapLocation bestAllyPastr = allyPastrLocs[0];
		int bestDistance = bestAllyPastr.distanceSquaredTo(bestTarget);
		for(int i = 1; i < allyPastrLocs.length; i++){
			if(allyPastrLocs[i].distanceSquaredTo(bestTarget) < bestDistance){
				bestAllyPastr = allyPastrLocs[i];
				bestDistance = allyPastrLocs[i].distanceSquaredTo(bestTarget);
			}
		}
		rc.broadcast(bestPastrChan, VectorFunctions.locToInt(bestAllyPastr));
		rc.setIndicatorString(1, bestAllyPastr + " " + bestTarget);
		return BugMove.mergePath(BugMove.generateBugPath(bestAllyPastr, bestTarget, rc, VectorFunctions.intToLoc(rc.readBroadcast(towerLocChan)), 361));
		//for(int i = 0; i < 10; i++){
		//	path = BugMove.simplefyPath(path);
		//}
	}
}
	
