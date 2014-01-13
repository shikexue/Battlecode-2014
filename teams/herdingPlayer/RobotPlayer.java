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



	// defining channels
	static int pastrBeingMadeChan = 0;
	static int makePastrChan = 1; //channel will have -1 if no pastures ordered
	static int makePastrLocChan[] = new int[]{23,4,5};

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

			// do this once to initialize channels at beginning of game
			if (rc.getType()==RobotType.HQ){
				rc.broadcast(makePastrChan, -1);
			}
			// if pastr has been made, decrement appropriate channel so hq doesn't double-count
			else if (rc.getType()==RobotType.PASTR){
				rc.broadcast(pastrBeingMadeChan, rc.readBroadcast(pastrBeingMadeChan)-1);
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
		tryToSpawn();
		rc.setIndicatorString(0, "" + rc.readBroadcast(makePastrChan));
		rc.setIndicatorString(1, "" + rc.readBroadcast(pastrBeingMadeChan));

		// if there are no pastures and none are being made, and none have been ordered, send order to make one
		//if ((rc.readBroadcast(pastrBeingMadeChan) == 0) && (rc.sensePastrLocations(rc.getTeam()).length == 0) && (rc.readBroadcast(makePastrChan) == -1)){
		
		// trying to make 2 pastrs
		int pastrBeingMade = rc.readBroadcast(pastrBeingMadeChan);
		int pastrAlreadyMade = rc.sensePastrLocations(rc.getTeam()).length; 
		int pastrUnansweredCommands = rc.readBroadcast(makePastrChan) + 1; // no commands sent when -1
		if(pastrBeingMade + pastrAlreadyMade + pastrUnansweredCommands < 2 ){
			sendMakeNearbyPastrCommand(5);
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
	private static RobotData runSoldier(Constants.Task task, ArrayList<MapLocation> path, MapLocation goal) throws GameActionException{
		//TODO: Select group of cows to herd
		//TODO: herd cows towards pastr

		rc.setIndicatorString(2, "" + task);

		//if there is a command sent out to make a pastr, make one 
		int lastPastrNum = rc.readBroadcast(makePastrChan);
		if (lastPastrNum > -1  && task != Constants.Task.PASTRMAKING){
			goal = VectorFunctions.intToLoc(rc.readBroadcast(makePastrLocChan[lastPastrNum]));
			path = BugMove.generateBugPath(goal, rc.getLocation(), rc);
			task = Constants.Task.PASTRMAKING;

			rc.broadcast(pastrBeingMadeChan, rc.readBroadcast(pastrBeingMadeChan)+1);

			//decrement makePastrChan so other robots don't try to make the same pastr
			rc.broadcast(makePastrChan, lastPastrNum-1);
		}


		//if robot is active, move or perform tasl
		if (rc.isActive()){
			//if have reached goal, perform designated task
			if (rc.getLocation().equals(goal)){
				//rc.setIndicatorString(1, "completing Task");
				switch (task){
				case PASTRMAKING:
					rc.construct(RobotType.PASTR);
					break;
				case TOWERMAKING:
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
		int currentMakeCount = rc.readBroadcast(makePastrChan);
		MapLocation pastrLoc = new MapLocation(randall.nextInt(pastrDistance),randall.nextInt(pastrDistance));

		// make sure that is not an obstacle 
		//TODO: try a few times to make sure is a field?
		while(rc.senseTerrainTile(pastrLoc).ordinal() > 1){ //0 NORMAL, 1 ROAD, 2 VOID, 3 OFF_MAP
			pastrLoc = new MapLocation(randall.nextInt(pastrDistance),randall.nextInt(pastrDistance));
		}

		//broadcast that a channel should be made, and in desired position
		// make sure that position is broadcast before soldiers are told to read it
		rc.broadcast(makePastrLocChan[currentMakeCount + 1 ], VectorFunctions.locToInt(pastrLoc));
		rc.broadcast(makePastrChan, currentMakeCount+1);
		
	}
	
	private static void sendMakePastrCommand(MapLocation pastrLocation) throws GameActionException{
		int currentMakeCount = rc.readBroadcast(makePastrChan);

		//broadcast that a channel should be made, and in desired position
		// make sure that position is broadcast before soldiers are told to read it
		rc.broadcast(makePastrLocChan[currentMakeCount + 1 ], VectorFunctions.locToInt(pastrLocation));
		rc.broadcast(makePastrChan, currentMakeCount + 1);	
	}
}