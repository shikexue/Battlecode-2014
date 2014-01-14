package herdingPlayer;

import battlecode.common.*;

import java.util.*;
import herdingPlayer.BugMove;
import herdingPlayer.VectorFunctions;
import herdingPlayer.Constants;
import herdingPlayer.RobotData;

public class RobotPlayer {


	static RobotController rc;
	static Random randall = new Random();
	static Direction allDirections[] = Direction.values();

	static MapLocation pastrLoc;

	// defining channels
	static int pastrsOrderedThisTurn = 0; //TODO: THIS MUST BE RESET AT THE BEGINNING OF EVERY TURN.
	
	static int pastrBeingMadeChan = 0;
	static int makePastrChan = 1; //channel will have -1 if no pastures ordered
	static int makePastrLocChan[] = new int[]{2,3,4};
	
	static int noisetowersOrderedThisTurn = 0; //TODO: THIS MUST BE RESET AT THE BEGINNING OF EVERY TURN
	
	static int noisetowerBeingMadeChan = 5;
	static int makeNoisetowerChan = 6;
	static int makeNoisetowerLocChan[] = new int[]{7,8,9};
	
	
	//next used channel should start with 11
	
	/*
	 * Calls the run functions of the different types of robot
	 */
	public static void run(RobotController rcIn) {
		try{
			rc = rcIn;
			Constants.Task task = Constants.Task.ATTACKING; // default task
			MapLocation goal = rc.senseEnemyHQLocation(); // default goal
			ArrayList<MapLocation> path = BugMove.generateBugPath(goal, rc.getLocation(), rc);
	
			RobotData myData = new RobotData(task, goal, path);

			// initialize channels and then make first noisetower and pastr
			if (rc.getType()==RobotType.HQ){
				rc.broadcast(makeNoisetowerChan, -1);
				rc.broadcast(noisetowerBeingMadeChan, -1);
				rc.broadcast(makePastrChan, -1);
				rc.broadcast(pastrBeingMadeChan, -1);
				rc.yield();
				sendMakeNearbyPastrCommand(5);
				sendMakeDefensiveNoisetowerCommand();
				sendMakeNearbyPastrCommand(5);
				sendMakeDefensiveNoisetowerCommand();
				sendMakeNearbyPastrCommand(7);
			}
			
			// splitting up behavior based on robot type
			while(true){
				if(rc.getType()==RobotType.HQ){
					runHQ();
				}else if(rc.getType()==RobotType.SOLDIER){
					// to store data like task, goal, path between rounds, have runFoo return them for soldiers, pastrs, towers
					myData = runSoldier(myData.getTask(), myData.getPath(), myData.getGoal());
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
		
		tryToSpawn();
		
		
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
	private static RobotData runSoldier(Constants.Task task, ArrayList<MapLocation> path, MapLocation goal) throws GameActionException{
		//TODO: Select group of cows to herd
		//TODO: herd cows towards pastr
		
		
		rc.setIndicatorString(2, "" + task);

		int pastrsToMake = rc.readBroadcast(makePastrChan);
		int pastrsBeingMade = rc.readBroadcast(pastrBeingMadeChan);

		int noisetowersToMake = rc.readBroadcast(makeNoisetowerChan);
		int noisetowersBeingMade = rc.readBroadcast(noisetowerBeingMadeChan);

		rc.setIndicatorString(1, "" +  pastrsToMake + " " + pastrsBeingMade + " " + noisetowersToMake + " " + noisetowersBeingMade);

		//if there is a noisetower to make that isn't being made, make one
		if(task != Constants.Task.TOWERMAKING && task != Constants.Task.PASTRMAKING && noisetowersToMake > -1){
			goal = VectorFunctions.intToLoc(rc.readBroadcast(makeNoisetowerLocChan[noisetowersToMake]));
			rc.setIndicatorString(1, "" + goal.x + " " + goal.y);
			path = BugMove.generateBugPath(goal, rc.getLocation(), rc);
			
			task = Constants.Task.TOWERMAKING;

			rc.broadcast(noisetowerBeingMadeChan, noisetowersBeingMade + 1);
			rc.broadcast(makeNoisetowerChan, noisetowersToMake-1);
		}
		
		//if there is a pastr to make that isn't being made, make one
		if (task != Constants.Task.TOWERMAKING && task != Constants.Task.PASTRMAKING && pastrsToMake > -1){
			goal = VectorFunctions.intToLoc(rc.readBroadcast(makePastrLocChan[pastrsToMake]));
			path = BugMove.generateBugPath(goal, rc.getLocation(), rc);
			
			task = Constants.Task.PASTRMAKING;
			
			rc.broadcast(pastrBeingMadeChan, pastrsBeingMade + 1);
			rc.broadcast(makePastrChan, pastrsToMake-1);
		}
		
		//if robot is active, move or perform task
		if (rc.isActive()){
			//if have reached goal, perform designated task
			if (rc.getLocation().equals(goal)){
				//rc.setIndicatorString(1, "completing Task");
				switch (task){
				case PASTRMAKING:
					rc.setIndicatorString(1,"making PASTR");
					rc.broadcast(pastrBeingMadeChan, pastrsBeingMade - 1);
					rc.construct(RobotType.PASTR);
					break;
				case TOWERMAKING:
					rc.setIndicatorString(1,"making tower");
					rc.broadcast(noisetowerBeingMadeChan, noisetowersBeingMade - 1);
					rc.construct(RobotType.NOISETOWER);
					break;
				case ATTACKING:
					Robot[] nearbyEnemies = rc.senseNearbyGameObjects(Robot.class,10,rc.getTeam().opponent());
					if (nearbyEnemies.length > 0) {
						RobotInfo robotInfo = rc.senseRobotInfo(nearbyEnemies[0]);
						rc.attackSquare(robotInfo.location);
					}
					break;
				default:

				}

			}
			// if not already at goal, move towards goal
			// TODO: allow interruptions
			else{
				rc.setIndicatorString(1, "followingPath");
				BugMove.followPath(path);
			}
		}

		return new RobotData(task, goal, path);
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
			pastrLoc = new MapLocation(randall.nextInt(pastrDistance),randall.nextInt(pastrDistance));
		}

		//broadcast that a channel should be made, and in desired position
		// make sure that position is broadcast before soldiers are told to read it
		rc.broadcast(makePastrLocChan[currentMakeCount + 1 ], VectorFunctions.locToInt(pastrLoc));
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
		rc.broadcast(makeNoisetowerLocChan[currentMakeCount + 1], VectorFunctions.locToInt(noisetowerLoc));
		rc.broadcast(makeNoisetowerChan, currentMakeCount + 1);
		
		noisetowersOrderedThisTurn++;
	}
}
	