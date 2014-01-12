package herdingPlayer;

import battlecode.common.*;

import java.util.*;

import herdingPlayer.BugMove;
import herdingPlayer.RobotPlayer.Task;
import herdingPlayer.VectorFunctions;

public class RobotPlayer {
	public enum Task {
		HERDING, ATTACKING, DEFENDING, PASTRMAKING, TOWERMAKING
	}

	static RobotController rc;
	static Random randall = new Random();
	static Direction allDirections[] = Direction.values();



	// defining channels
	static int numPastrChan = 0;
	static int makePastrChan = 1; //channel will have -1 if no pastures ordered
	static int makePastrLocChan[] = new int[]{2,3,4,5};

	/*
	 * Calls the run functions of the different types of robot
	 */
	public static void run(RobotController rcIn) {
		try{
			rc = rcIn;
			rc.broadcast(makePastrChan, -1);


			ArrayList<MapLocation> path = new ArrayList<MapLocation>();
			Task task = Task.ATTACKING; 
			MapLocation goal = rc.senseEnemyHQLocation();

			while(true){
				if(rc.getType()==RobotType.HQ){
					runHQ();
				}else if(rc.getType()==RobotType.SOLDIER){
					runSoldier(task, path, goal);
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

		// if there are no pastures, send order to make one
		if (rc.readBroadcast(numPastrChan) == 0){
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
	private static void runSoldier(Task task, ArrayList<MapLocation> path, MapLocation goal) throws GameActionException{
		//TODO: Select group of cows to herd
		//TODO: herd cows towards pastr

		// if there is a command sent out to make a pastr, make one 
		int lastPastrNum = rc.readBroadcast(makePastrChan);
		if (lastPastrNum>-1){
			goal = VectorFunctions.intToLoc(rc.readBroadcast(makePastrLocChan[lastPastrNum]));
			path = BugMove.generateBugPath(goal, rc.getLocation(), rc);
			task = Task.PASTRMAKING;

			//decrement makePastrChan so other robots don't try to make the same pastr
			rc.broadcast(makePastrChan, lastPastrNum-1);
		}

		//if have reached goal, perform designated task
		if (rc.getLocation().equals(goal)){
			switch (task){
			case PASTRMAKING:
				rc.construct(RobotType.PASTR);
			case TOWERMAKING:
				rc.construct(RobotType.NOISETOWER);
			case ATTACKING:
				Robot[] nearbyEnemies = rc.senseNearbyGameObjects(Robot.class,10,rc.getTeam().opponent());
				if (nearbyEnemies.length > 0) {
					RobotInfo robotInfo = rc.senseRobotInfo(nearbyEnemies[0]);
					rc.attackSquare(robotInfo.location);
				}
			default:
				
			}
		}
		// if not already at goal, move towards goal
		// TODO: allow interruptions
		else{
			
		}


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
		rc.broadcast(makePastrChan, currentMakeCount + 1);	
	}
}