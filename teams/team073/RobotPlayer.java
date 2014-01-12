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
	
	public static void run(RobotController rcin){
		rc = rcin;
		randall.setSeed(rc.getRobot().getID());
		ArrayList<MapLocation> path = new ArrayList<MapLocation>();
		if(rc.getType()==RobotType.SOLDIER){
			path = BugMove.generateBugPath(rc.senseEnemyHQLocation(), rc.getLocation(), rc);
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
		//rc.setIndicatorString(0, "successfully generated path");
		BugMove.followPath(path);
		//bugMove(rc.senseEnemyHQLocation());
	}
	
	

	private static void runHeadquarters() throws GameActionException {
		Direction spawnDir = Direction.SOUTH_EAST;
		if(rc.isActive()&&rc.canMove(spawnDir)&&rc.senseRobotCount()<1/*GameConstants.MAX_ROBOTS*/){
			rc.spawn(spawnDir);
		}
		
		int editingChannel = (Clock.getRoundNum()%2);
		int usingChannel = ((Clock.getRoundNum()+1)%2);
		rc.broadcast(editingChannel, 0);
		rc.broadcast(editingChannel+2, 0);
	}
}