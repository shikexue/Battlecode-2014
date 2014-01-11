package team073;

//Clock.getbytecodenum before and after
//Maps probably < 100
//Djikstra's is not necessary; normals count as N roads
//If bugging: make roads higher priority? 
//Soldiers should self-destruct if faced with bad odds
//Bugging, with optimization
import battlecode.common.*;

import java.util.ArrayList;
import java.util.Random;

public class RobotPlayer{
	
	public static RobotController rc;
	static Direction allDirections[] = Direction.values();
	static Random randall = new Random();
	static int tryRight[] = new int[]{-1,-2,3,0,1,2,3,};
	static int tryLeft[] = new int[]{1,2,3,0,-1,-2,-3};
	static int tryForward[] = new int[]{0,1,-1,2,-2};
	static int placeOnPath;
	
	public static enum STATE {
		BUGGING, CLEAR
	}
	
	public static void run(RobotController rcin){
		rc = rcin;
		randall.setSeed(rc.getRobot().getID());
		ArrayList<MapLocation> path = new ArrayList<MapLocation>();
		if(rc.getType()==RobotType.SOLDIER){
			path = generateBugPath(rc.senseEnemyHQLocation());
		}
		while(true){
			try{
				if(rc.getType()==RobotType.HQ){//if I'm a headquarters
					runHeadquarters();
				}else if(rc.getType()==RobotType.SOLDIER){
					runSoldier(path);
				}
				rc.yield();
			}catch (Exception e){
				e.printStackTrace();
			}
		}
	}

	private static void runSoldier(ArrayList<MapLocation> path) throws GameActionException {
		rc.setIndicatorString(0, "successfully generated path");
		followPath(path);
		rc.yield();
	}
	
	private static ArrayList<MapLocation> generateBugPath(MapLocation destination){
		rc.setIndicatorString(0, "beginning to generate path");
		placeOnPath = 0;
		Direction dir = null;
		//ArrayList<Direction> path = new ArrayList<Direction>();
		ArrayList<MapLocation> pastPos = new ArrayList<MapLocation>();
		MapLocation pos = rc.getLocation();
		pastPos.add(rc.getLocation());
		STATE state = STATE.CLEAR;
		MapLocation startBugLoc = null;
		while(!pos.equals(destination)){
			//rc.setIndicatorString(0, ""+path);
			//rc.setIndicatorString(2, ""+state);
			Direction desiredDir = pos.directionTo(destination);
			if(state==STATE.BUGGING){ //If we are closer than we started, and are unblocked, we are clear
				if(pos.distanceSquaredTo(destination) < startBugLoc.distanceSquaredTo(destination)&&canPathThrough(pos.add(desiredDir))){
					state = STATE.CLEAR;
					rc.setIndicatorString(1, "passed obstacle");
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
					newDir = bug(pos, dir, tryRight);
					dir = newDir;
					pos = pos.add(newDir);
					pastPos.add(pos);
					//intentional fallthrough
				}
			case BUGGING:
				Direction moveDir = bug(pos, dir, tryLeft);
				dir = moveDir;
				pos = pos.add(moveDir);
				pastPos.add(pos);
			}
			rc.setIndicatorString(2, ""+pastPos.subList(Math.max(0, pastPos.size()-15), pastPos.size()));
			rc.yield();
		}
		return pastPos;
	}
	
	private static Direction bug(MapLocation pos, Direction dir, int[] directionalLooks) {
		//Try different directions, in order
		rc.setIndicatorString(1, "bugging");
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

	private static boolean canPathThrough(MapLocation desiredPos) {
		TerrainTile toCheck = rc.senseTerrainTile(desiredPos);
		if(toCheck.equals(TerrainTile.OFF_MAP)||toCheck.equals(TerrainTile.VOID)){
			return false;
		}
		return true;
	}

	private static  Direction simplePath(MapLocation pos, Direction desiredDir) {
		rc.setIndicatorString(2, "simplePathing");
		if(canPathThrough(pos.add(desiredDir))){
			return desiredDir;
		}
		return null;
	}

	private static void followPath(ArrayList<MapLocation> pathToFollow) throws GameActionException{
		if(placeOnPath < pathToFollow.size()-1){
			simpleMove(rc.getLocation().directionTo(pathToFollow.get(placeOnPath)));
			if(rc.getLocation().equals(pathToFollow.get(placeOnPath))){
				placeOnPath++;
			}
		}
	}
	
	
	private static void simpleMove(Direction desiredDir) throws GameActionException {
		for(int directionalOffset:tryForward){
			int forwardInt = desiredDir.ordinal();
			Direction trialDir = allDirections[(forwardInt+directionalOffset+8)%8];
			if(rc.canMove(trialDir)&&rc.isActive()){
				rc.move(trialDir);
			}
		}
	}

	private static MapLocation mladd(MapLocation m1, MapLocation m2){
		return new MapLocation(m1.x+m2.x,m1.y+m2.y);
	}
	
	private static MapLocation mldivide(MapLocation bigM, int divisor){
		return new MapLocation(bigM.x/divisor, bigM.y/divisor);
	}

	private static int locToInt(MapLocation m){
		return (m.x*100 + m.y);
	}
	
	private static MapLocation intToLoc(int i){
		return new MapLocation(i/100,i%100);
	}
	

	private static void runHeadquarters() throws GameActionException {
		Direction spawnDir = Direction.NORTH;
		if(rc.isActive()&&rc.canMove(spawnDir)&&rc.senseRobotCount()<GameConstants.MAX_ROBOTS){
			rc.spawn(Direction.NORTH);
		}
		
		int editingChannel = (Clock.getRoundNum()%2);
		int usingChannel = ((Clock.getRoundNum()+1)%2);
		rc.broadcast(editingChannel, 0);
		rc.broadcast(editingChannel+2, 0);
	}
}