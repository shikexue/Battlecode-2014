package herdingPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Random;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.TerrainTile;

public class BugMove {
	public static RobotController rc;
	static Direction allDirections[] = Direction.values();
	static Random randall = new Random();
	static int enterBugging[] = new int[]{0,-2,-1,-3,-4, 3, 2, 1, 0,};
	static int whileBugging[] = new int[]{2,1,0,-1,-2,-3,-4};
	static int tryForward[] = new int[]{0,1,-1,2,-2};
	static int tryRestrainedForward[] = new int[]{0};
	static int placeOnPath;

	public static enum STATE {
		BUGGING, CLEAR
	}
	//Generates a path to a given destination and returns a list of locations that make up the path
	public static ArrayList<MapLocation> generateBugPath(MapLocation destination, MapLocation start, RobotController rcin,  MapLocation centerOfRange, int rangeSquared){
		rc = rcin;
		//rc.setIndicatorString(0, "beginning to generate path");
		placeOnPath = 0;
		Direction dir = start.directionTo(destination);
		//ArrayList<Direction> path = new ArrayList<Direction>();
		ArrayList<MapLocation> pastPos = new ArrayList<MapLocation>();
		MapLocation pos = start;
		pastPos.add(start);
		STATE state = STATE.CLEAR;
		MapLocation startBugLoc = null;
		while(!pos.equals(destination)&&pastPos.size() < 150){
			//rc.setIndicatorString(0, ""+path);
			//rc.setIndicatorString(2, ""+state);
			Direction desiredDir = pos.directionTo(destination);
			if(state==STATE.BUGGING){ //If we are closer than we started, and are unblocked, we are clear
				if(pos.distanceSquaredTo(destination) < startBugLoc.distanceSquaredTo(destination)&&canPathThrough(pos.add(desiredDir), centerOfRange, rangeSquared)){
					state = STATE.CLEAR;
					//rc.setIndicatorString(1, "passed obstacle");
				}
			}
			switch(state){
			case CLEAR:
				Direction newDir = simplePath(pos, desiredDir, centerOfRange, rangeSquared);	
				if(newDir != null){
					dir = newDir;
					pos = pos.add(newDir);
					pastPos.add(pos);
					break;
				} else {
					state = STATE.BUGGING;
					startBugLoc = pos;
					newDir = bug(pos, dir, enterBugging, centerOfRange, rangeSquared);
					//rc.setIndicatorString(2, ""+newDir);
					dir = newDir;
					pos = pos.add(newDir);
					pastPos.add(pos);
					//rc.setIndicatorString(0, ""+pastPos.subList(Math.max(0, pastPos.size()-30), pastPos.size()));
					//rc.setIndicatorString(1, ""+state);
					//rc.yield();
					//intentional fallthrough
				}
			case BUGGING:
				//int before = Clock.getBytecodeNum();
				Direction moveDir = bug(pos, dir, whileBugging, centerOfRange, rangeSquared);
				//rc.setIndicatorString(2, ""+(Clock.getBytecodeNum()-before));
				//rc.setIndicatorString(2, "In bugging: "+moveDir);
				dir = moveDir;
				pos = pos.add(moveDir);
				pastPos.add(pos);
			}
			//rc.setIndicatorString(0, ""+pastPos.subList(Math.max(0, pastPos.size()-30), pastPos.size()));
			//rc.setIndicatorString(1, ""+state);
			//rc.setIndicatorString(2, ""+(Clock.getBytecodeNum()-before));
			//rc.yield();
		}
		//add goal to end
		pastPos.add(destination);
		return pastPos;
	}
	public static ArrayList<MapLocation> generateBugPath(MapLocation destination, MapLocation start, RobotController rcin){
		rc = rcin;
		//rc.setIndicatorString(0, "beginning to generate path");
		placeOnPath = 0;
		Direction dir = start.directionTo(destination);
		//ArrayList<Direction> path = new ArrayList<Direction>();
		ArrayList<MapLocation> pastPos = new ArrayList<MapLocation>();
		MapLocation pos = start;
		pastPos.add(start);
		STATE state = STATE.CLEAR;
		MapLocation startBugLoc = null;
		while(!pos.equals(destination)&&pastPos.size()<150){
			//rc.setIndicatorString(0, ""+path);
			//rc.setIndicatorString(2, ""+state);
			Direction desiredDir = pos.directionTo(destination);
			if(state==STATE.BUGGING){ //If we are closer than we started, and are unblocked, we are clear
				if(pos.distanceSquaredTo(destination) < startBugLoc.distanceSquaredTo(destination)&&canPathThrough(pos.add(desiredDir))){
					state = STATE.CLEAR;
					//rc.setIndicatorString(1, "passed obstacle");
				}
			}
			switch(state){
			case CLEAR:
				Direction newDir = simplePath(pos, desiredDir);	
				if(newDir != null){
					dir = newDir;
					pos = pos.add(newDir);
					pastPos.add(pos);
					break;
				} else {
					state = STATE.BUGGING;
					startBugLoc = pos;
					newDir = bug(pos, dir, enterBugging);
					//rc.setIndicatorString(2, ""+newDir);
					dir = newDir;
					pos = pos.add(newDir);
					pastPos.add(pos);
					//rc.setIndicatorString(0, ""+pastPos.subList(Math.max(0, pastPos.size()-30), pastPos.size()));
					//rc.setIndicatorString(1, ""+state);
					//rc.yield();
					//intentional fallthrough
				}
			case BUGGING:
				//int before = Clock.getBytecodeNum();
				Direction moveDir = bug(pos, dir, whileBugging);
				//rc.setIndicatorString(2, ""+(Clock.getBytecodeNum()-before));
				//rc.setIndicatorString(2, "In bugging: "+moveDir);
				dir = moveDir;
				pos = pos.add(moveDir);
				pastPos.add(pos);
			}
			//rc.setIndicatorString(0, ""+pastPos.subList(Math.max(0, pastPos.size()-30), pastPos.size()));
			//rc.setIndicatorString(1, ""+state);
			//rc.setIndicatorString(2, ""+(Clock.getBytecodeNum()-before));
			//rc.yield();
		}
		//add goal to end
		pastPos.add(destination);
		return pastPos;
	}
	//When moving around an obstacle, runs this
	private static Direction bug(MapLocation pos, Direction dir, int[] directionalLooks, MapLocation centerOfRange, int rangeSquared) {
		//Try different directions, in order
		int forwardInt = dir.ordinal();
		for(int directionalOffset:directionalLooks){
			Direction tryDir = allDirections[(forwardInt + directionalOffset+8)%8];
			MapLocation tryPos = pos.add(tryDir);
			if(canPathThrough(tryPos, centerOfRange, rangeSquared)){
				return tryDir;
			}
		}
		return allDirections[(forwardInt+4)%8];
	}	
	private static Direction bug(MapLocation pos, Direction dir, int[] directionalLooks) {
		//Try different directions, in order
		int forwardInt = dir.ordinal();
		for(int directionalOffset:directionalLooks){
			Direction tryDir = allDirections[(forwardInt + directionalOffset+8)%8];
			MapLocation tryPos = pos.add(tryDir);
			if(canPathThrough(tryPos)){
				return tryDir;
			}
		}
		return allDirections[(forwardInt+4)%8];
	}	
	//Checks if a given bit of terrain is passable
	private static boolean canPathThrough(MapLocation desiredPos, MapLocation centerOfRange, int rangeSquared) {
		TerrainTile toCheck = rc.senseTerrainTile(desiredPos);

		if(toCheck.equals(TerrainTile.OFF_MAP)||
				toCheck.equals(TerrainTile.VOID)||
				rc.senseHQLocation().equals(desiredPos)||
				desiredPos.distanceSquaredTo(centerOfRange)>=rangeSquared){
			return false;
		}
		return true;
	}	
	private static boolean canPathThrough(MapLocation desiredPos) {
		TerrainTile toCheck = rc.senseTerrainTile(desiredPos);

		if(toCheck.equals(TerrainTile.OFF_MAP)||
				toCheck.equals(TerrainTile.VOID)||
				rc.senseHQLocation().equals(desiredPos)){
			return false;
		}
		return true;
	}	

	//Attempts to path (not move) in a straight line toward the target
	private static Direction simplePath(MapLocation pos, Direction desiredDir, MapLocation centerOfRange, int rangeSquared) {
		//rc.setIndicatorString(2, "simplePathing");
		int forwardInt = desiredDir.ordinal();
		//rc.setIndicatorString(2, "simple pathing. Desired Dir is " + desiredDir);
		for(int directionalOffset:tryRestrainedForward){
			Direction trialDir = allDirections[(forwardInt+directionalOffset+8)%8];
			if(canPathThrough(pos.add(trialDir), centerOfRange, rangeSquared)){
				return trialDir;
			}
		}
		return null;
	}
	private static Direction simplePath(MapLocation pos, Direction desiredDir) {
		//rc.setIndicatorString(2, "simplePathing");
		int forwardInt = desiredDir.ordinal();
		//rc.setIndicatorString(2, "simple pathing. Desired Dir is " + desiredDir);
		for(int directionalOffset:tryRestrainedForward){
			Direction trialDir = allDirections[(forwardInt+directionalOffset+8)%8];
			if(canPathThrough(pos.add(trialDir))){
				return trialDir;
			}
		}
		return null;
	}

	//This does mutate original path
	public static ArrayList<MapLocation> simplefyPath(ArrayList<MapLocation> originalPath, MapLocation centerOfRange, int rangeSquared){
		for(int i = 0; i < originalPath.size()-3; i++){ 
			Direction newDir = originalPath.get(i+1).directionTo(originalPath.get(i+2));
			if(canPathThrough(originalPath.get(i).add(newDir), centerOfRange, rangeSquared)){
				originalPath.set(i+1, originalPath.get(i).add(newDir));
			}
			if(i+3 < originalPath.size()-1){
				if(originalPath.get(i+1).equals(originalPath.get(i+3))||originalPath.get(i+1).isAdjacentTo(originalPath.get(i+3))){
					originalPath.remove(i+2);
				}
			}
		}
		return originalPath;
	}

	//Mutates original path
	public static ArrayList<MapLocation> mergePath(ArrayList<MapLocation> originalPath){
		for(int i = 0; i < originalPath.size()-6; i++){
			MapLocation[] toCheck = new MapLocation[]{originalPath.get(i+3), originalPath.get(i+4), originalPath.get(i+5)};
			MapLocation current = originalPath.get(i);
			for(int j = 2; j > -1; j--){
				if(current.equals(toCheck[j])){
					for(int k = 0; k < j+1; k++){
						originalPath.remove(i+k+2);
					}
					break;
				}
			}
		}
		return originalPath;
	}
	//Given a list of continuous locations, visits each in turn. 
	public static void followPath(ArrayList<MapLocation> pathToFollow) throws GameActionException{
		if(placeOnPath < pathToFollow.size()-1){
			simpleMoveTo(pathToFollow.get(placeOnPath));
			if(rc.getLocation().equals(pathToFollow.get(placeOnPath))){
				placeOnPath++;
			}
		}
	}

	//shoots to move cows along path
	public static void shootPath(ArrayList<MapLocation> pathToFollow, int towerGetPathChan, int bestPastrChan, int towerShootLocChan) throws GameActionException{
		//TODO instead of passing the channel numbers have those in constants file.
		if(placeOnPath < pathToFollow.size()-1 && VectorFunctions.locToInt(pathToFollow.get(placeOnPath)) != rc.readBroadcast(bestPastrChan)){
			MapLocation toShoot = pathToFollow.get(placeOnPath).add(pathToFollow.get(placeOnPath + 1).directionTo(pathToFollow.get(placeOnPath)));
			if(rc.getLocation().distanceSquaredTo(toShoot) <= rc.getType().attackRadiusMaxSquared){
				rc.broadcast(towerShootLocChan, VectorFunctions.locToInt(toShoot));
				rc.attackSquareLight(toShoot);
			} else {
				rc.broadcast(towerGetPathChan, 0); //TODO: broadcasting here
			}
			//rc.setIndicatorString(2, "" + pathToFollow.get(placeOnPath));
			placeOnPath++;
		} else {
			rc.broadcast(towerGetPathChan, 0); //TODO: broadcasting here
		}
	}
	//Moves toward target 
	public static void simpleMoveTo(MapLocation desiredPos) throws GameActionException {
		Direction desiredDir = rc.getLocation().directionTo(desiredPos);
		for(int directionalOffset:tryForward){
			int forwardInt = desiredDir.ordinal();
			Direction trialDir = allDirections[(forwardInt+directionalOffset+8)%8];
			if(rc.canMove(trialDir)&&rc.isActive()){
				if(rc.getLocation().distanceSquaredTo(VectorFunctions.intToLoc(rc.readBroadcast(4004))) > 16)
					rc.move(trialDir);
				else
					rc.sneak(trialDir);
			}
		}
	}
	//Moves in a direction
	public static void simpleMove(Direction desiredDir) throws GameActionException {
		for(int directionalOffset:tryForward){
			int forwardInt = desiredDir.ordinal();
			Direction trialDir = allDirections[(forwardInt+directionalOffset+8)%8];
			if(rc.canMove(trialDir)&&rc.isActive()){
				rc.sneak(trialDir);
				//rc.yield();
			}
		}
	}


		public static boolean closeEnoughForTask(Constants.Task task, MapLocation destination, MapLocation currentPos){
			HashMap<Constants.Task, Integer> taskLeeway = new HashMap<Constants.Task, Integer>();
			taskLeeway.put(Constants.Task.HERDING, 0);
			taskLeeway.put(Constants.Task.ATTACKING, 10); 
			taskLeeway.put(Constants.Task.DEFENDING, 1);
			taskLeeway.put(Constants.Task.PASTRMAKING, 0);
			taskLeeway.put(Constants.Task.TOWERMAKING,0);
			if(taskLeeway.get(task) <= destination.distanceSquaredTo(currentPos)){
				return true;
			}
			return false;
		}

	}
