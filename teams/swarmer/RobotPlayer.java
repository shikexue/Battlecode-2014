package swarmer;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

public class RobotPlayer {
	public static void run(RobotController rc){
		while(true){
			try{
				if(rc.getType() == RobotType.HQ){
					runHQ(rc);
				} else if(rc.getType() == RobotType.SOLDIER){
					runSoldier(rc);
				}
			} catch(Exception e){
				e.printStackTrace();
			}
			rc.yield();
		}
	}
	
	private static void runSoldier(RobotController rc) throws GameActionException{
		Robot[] nearbyRobots = rc.senseNearbyGameObjects(Robot.class);
		for(Robot rob:nearbyRobots){
			if(!rob.getTeam().equals(rc.getTeam())){
				if(rc.isActive() && rc.canAttackSquare(rc.senseRobotInfo(rob).location)){
					rc.attackSquare(rc.senseRobotInfo(rob).location);
				}
			}
		}
		
		if(rc.isActive()){
			Direction moveDir = Direction.EAST;
			
			//Combat.kamikaze(rc, rc.getLocation());
			if(rc.canMove(moveDir)){
				rc.move(moveDir);
			} else {
				//rc.construct(RobotType.PASTR);
				if(rc.senseRobotCount() > 5){
					rc.selfDestruct();
				}
			}
		}
	}
	
	private static void runHQ(RobotController rc) throws GameActionException{
		
		Combat.HQAttack(rc);
		rc.setIndicatorString(0, ""+rc.getType().attackRadiusMaxSquared);
		Direction spawnDir = Direction.NORTH;
		if(rc.isActive() && rc.canMove(spawnDir) && rc.senseRobotCount() < GameConstants.MAX_ROBOTS){
			rc.spawn(spawnDir);
		}
	}
}
