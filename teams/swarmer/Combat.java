package swarmer;

import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

public class Combat {
	public static void kamikaze(RobotController rc, MapLocation m) throws GameActionException{
		//If specific conditions are met, kamikaze is better than normal fighting.
		//Kamikaze is always preferable when it will at least trade 1 for 1 and
		//splash onto another enemy soldier robot while avoiding splashing allies.
		//It is also sometimes useful for destroying pastures. Because normal soldier
		//combat will trade 1 for 1 anyway, it is useful to kamikaze to destroy a pasture
		//and damage a nearby enemy soldier.
		int kamikazeChannel = 5000;
		//tentatively, we're setting the kamikaze channel to 5000, will change depending on messaging system.
		//this channel contains the number of robots that want to suicide.
		if(rc.readBroadcast(kamikazeChannel) > 1){
			
		}
		//once each 
		
	}
	public static void patrol(RobotController rc, MapLocation m){
		
	}
	public static void swarm(RobotController rc, MapLocation m){
		//First move to a gathering place at a robot's
		//location. Once there, wait until the band (sized 3)
		//all broadcasts their arrival. At that point, move as a group to 
		//combat location.
		int swarmChannel = 6000;
		BugMove.generateBugPath(m, rc.getLocation(), rc);
		
		
	}
	public static void attack(RobotController rc, MapLocation m){
		
	}
	
	public static void towerAttack(RobotController rc, MapLocation m){
		//We want to herd cows 
	}
	
	public static void HQAttack(RobotController rc) throws GameActionException{
		//HQ deals splash damage, targets with range distance squared 15
		//Attempts to avoid splashing on own robots and splashes enemy robots out of range.
		//Not very efficient, can probably just disregard friendly fire in most cases.
		Robot[] nearbyRobots = rc.senseNearbyGameObjects(Robot.class);
		int[][] target = {{0, 0, 0, 0, 0, 0, 0, 0, 0},
						  {0, 0, 0, 0, 0, 0, 0, 0, 0},
						  {0, 0, 0, 0, 0, 0, 0, 0, 0},
						  {0, 0, 0, 0, 0, 0, 0, 0, 0},
						  {0, 0, 0, 0, 0, 0, 0, 0, 0},
						  {0, 0, 0, 0, 0, 0, 0, 0, 0},
						  {0, 0, 0, 0, 0, 0, 0, 0, 0},
						  {0, 0, 0, 0, 0, 0, 0, 0, 0},
						  {0, 0, 0, 0, 0, 0, 0, 0, 0}};
		//target contains an integer array indicating desirability of targeting a location.
		//target[4][4] is the HQ, the other squares can be mapped to the grid at target[y][x]
		//each direct hit on an enemy adds value 2 and splash adds value 1.
		//hits on own robots adds value -2 and -1 respectively.
		
		//Commenting out check for active because of bug in 1.1.1
		//if(rc.isActive()){
		MapLocation HQLoc = rc.getLocation();
		for(Robot rob:nearbyRobots){
			MapLocation robLoc = rc.senseRobotInfo(rob).location;
			if(HQLoc.x - robLoc.x <= 4 && robLoc.x - HQLoc.x <= 4 && robLoc.y - HQLoc.y <= 4 && HQLoc.y - robLoc.y <= 4){
				//avoid using absolute values to save bytecodes, checks to see whether robot is in attackable range
				int relativeX = robLoc.x - HQLoc.x;
				int relativeY = robLoc.y - HQLoc.y;
				if(rob.getTeam().equals(rc.getTeam())){
					//Subtract 1 in order to avoid attacking allied robots.
					for(int i = -1; i < 2; i++){
						for(int j = -1; j < 2; j++){
							int xIndex = (relativeX + i + 4) % 9;
							int yIndex = (relativeY + j + 4) % 9;
							if(xIndex < 9 && xIndex >= 0 && yIndex < 9 && yIndex >= 0)
									target[yIndex][xIndex] -= 1;
						}
					}
					//Try very hard not to attack center of allied robot.
					target[(relativeY + 4) % 9][(relativeX + 4) % 9] -= 1;
				} else {
					//Add 1 to prioritize attacking enemy robots.
					for(int i = -1; i < 2; i++){
						for(int j = -1; j < 2; j++){
							int xIndex = (relativeX + i + 4) % 9;
							int yIndex = (relativeY + j + 4) % 9;
							if(xIndex < 9 && xIndex >= 0 && yIndex < 9 && yIndex >= 0)
									target[yIndex][xIndex] += 1;
						}
					}
					//Try very hard to attack center of enemy robot.
					target[(relativeY + 4) % 9][(relativeX + 4) % 9] += 1;
				}
			}
		}
		if(nearbyRobots.length > 0){
			//Pick the square which causes the most net damage.
			MapLocation newTarget = null;
			int maxValue = 0;
			for(int i = 1; i < 8; i++){
				for(int j = 1; j < 8; j++){
					if(target[j][i] > maxValue && (i != 0 || j != 0) && (i != 0 || j != 8) && (i != 8 || j != 0) && (i != 8 || j!= 8)){
						newTarget = new MapLocation(i + HQLoc.x - 4, j + HQLoc.y - 4);
						maxValue = target[j][i];
					}
				}
			}
			if(newTarget != null && rc.canAttackSquare(newTarget)){
				rc.attackSquare(newTarget);
			}
		}
		//}
	}
	
	public static Robot[] senseEnemyRobots(RobotController rc) throws GameActionException{
		//Return a list of MapLocations representing s
		return rc.senseNearbyGameObjects(Robot.class, rc.getType().sensorRadiusSquared, rc.getTeam().opponent());
	}
}
