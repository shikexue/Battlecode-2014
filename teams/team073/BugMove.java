package team073;

import java.util.ArrayList;
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
		public static ArrayList<MapLocation> generateBugPath(MapLocation destination, MapLocation start, RobotController rcin){
			rc = rcin;
			//rc.setIndicatorString(0, "beginning to generate path");
			placeOnPath = 0;
			Direction dir = null;
			//ArrayList<Direction> path = new ArrayList<Direction>();
			ArrayList<MapLocation> pastPos = new ArrayList<MapLocation>();
			MapLocation pos = start;
			pastPos.add(start);
			STATE state = STATE.CLEAR;
			MapLocation startBugLoc = null;
			while(!pos.equals(destination)){
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
						rc.setIndicatorString(2, ""+newDir);
						dir = newDir;
						pos = pos.add(newDir);
						pastPos.add(pos);
						rc.setIndicatorString(0, ""+pastPos.subList(Math.max(0, pastPos.size()-30), pastPos.size()));
						rc.setIndicatorString(1, ""+state);
						rc.yield();
						//intentional fallthrough
					}
				case BUGGING:
					int before = Clock.getBytecodeNum();
					Direction moveDir = bug(pos, dir, whileBugging);
					rc.setIndicatorString(2, ""+(Clock.getBytecodeNum()-before));
					rc.setIndicatorString(2, "In bugging: "+moveDir);
					dir = moveDir;
					pos = pos.add(moveDir);
					pastPos.add(pos);
				}
				rc.setIndicatorString(0, ""+pastPos.subList(Math.max(0, pastPos.size()-30), pastPos.size()));
				rc.setIndicatorString(1, ""+state);
				//rc.setIndicatorString(2, ""+(Clock.getBytecodeNum()-before));
				rc.yield();
			}
			return pastPos;
		}
		//When moving around an obstacle, runs this
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
		private static boolean canPathThrough(MapLocation desiredPos) {
			TerrainTile toCheck = rc.senseTerrainTile(desiredPos);
			if(toCheck.equals(TerrainTile.OFF_MAP)||toCheck.equals(TerrainTile.VOID)){
				return false;
			}
			return true;
		}

		//Attempts to path (not move) in a straight line toward the target
		private static  Direction simplePath(MapLocation pos, Direction desiredDir) {
			//rc.setIndicatorString(2, "simplePathing");
			int forwardInt = desiredDir.ordinal();
			rc.setIndicatorString(2, "simple pathing. Desired Dir is " + desiredDir);
			for(int directionalOffset:tryRestrainedForward){
				Direction trialDir = allDirections[(forwardInt+directionalOffset+8)%8];
				if(canPathThrough(pos.add(trialDir))){
					return trialDir;
				}
			}
			return null;
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
		
		//Moves toward target 
		public static void simpleMoveTo(MapLocation desiredPos) throws GameActionException {
			Direction desiredDir = rc.getLocation().directionTo(desiredPos);
			for(int directionalOffset:tryForward){
				int forwardInt = desiredDir.ordinal();
				Direction trialDir = allDirections[(forwardInt+directionalOffset+8)%8];
				if(rc.canMove(trialDir)&&rc.isActive()){
					rc.move(trialDir);
				}
			}
		}
		//Moves in a direction
		public static void simpleMove(Direction desiredDir) throws GameActionException {
			for(int directionalOffset:tryForward){
				int forwardInt = desiredDir.ordinal();
				Direction trialDir = allDirections[(forwardInt+directionalOffset+8)%8];
				if(rc.canMove(trialDir)&&rc.isActive()){
					rc.move(trialDir);
					rc.yield();
				}
			}
		}
}
