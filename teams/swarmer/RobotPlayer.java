package swarmer;

import java.util.ArrayList;
import java.util.Random;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

public class RobotPlayer {
	static Random rand = new Random();
	static Direction[] directions = {Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST, Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST};
	
	
	public static void run(RobotController rc){
		rand.setSeed((rc.getRobot().getID()));
		ArrayList<MapLocation> path = null;
		while(true){
			try{
				//System.out.println(BugMove.placeOnPath);
				if(rc.getType() == RobotType.HQ){
					runHQ(rc);
				} else if(rc.getType() == RobotType.SOLDIER){
					runSoldier(rc);
				} else if(rc.getType() == RobotType.NOISETOWER){
					if((path == null || rc.readBroadcast(4002) == 0) && rc.sensePastrLocations(rc.getTeam()).length > 0){
						//TODO use broadcasting to keep track of #pastrs.
						path = getTowerPath(rc);
						rc.setIndicatorString(2, "plan to shoot "+path.get(0));
					}
					runTower(rc, path);
				}
			} catch(Exception e){
				e.printStackTrace();
			}
			rc.yield();
		}
	}
	
	private static ArrayList<MapLocation> getTowerPath(RobotController rc) throws GameActionException{
		MapLocation[] allyPastrLocs = rc.sensePastrLocations(rc.getTeam());
		MapLocation bestTarget = new MapLocation(rc.readBroadcast(4001)/100,rc.readBroadcast(4001)%100);
		rc.broadcast(4000, 0); //reset cow channel for next sensing
		rc.broadcast(4002, 1); //set channel for having found a path
		MapLocation bestAllyPastr = allyPastrLocs[0];
		int bestDistance = bestAllyPastr.distanceSquaredTo(bestTarget);
		for(int i = 1; i < allyPastrLocs.length; i++){
			if(allyPastrLocs[i].distanceSquaredTo(bestTarget) < bestDistance){
				bestAllyPastr = allyPastrLocs[i];
				bestDistance = allyPastrLocs[i].distanceSquaredTo(bestTarget);
			}
		}
		rc.broadcast(4005, bestAllyPastr.x * 100 + bestAllyPastr.y);
		return BugMove.mergePath(BugMove.generateBugPath(bestAllyPastr, bestTarget, rc));
		//for(int i = 0; i < 10; i++){
		//	path = BugMove.simplefyPath(path);
		//}
	}
	
	private static void runTower(RobotController rc, ArrayList<MapLocation >path) throws GameActionException{
		BugMove.shootPath(path);
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
		//rc.sense
		if(rc.isActive()){
			Direction moveDir = directions[rand.nextInt(8)];
			int towerChannel = 1000;
			int pastureChannel = 2000;
			//TODO Actually put buildings in reasonable places
			//Combat.kamikaze(rc, rc.getLocation());
			if(rc.readBroadcast(towerChannel) == 0 && rc.getLocation().distanceSquaredTo(rc.senseHQLocation()) > 15){
				rc.broadcast(towerChannel, rc.readBroadcast(towerChannel) + 1);
				rc.broadcast(4003, rc.getLocation().x*100 + rc.getLocation().y);
				rc.construct(RobotType.NOISETOWER);
			} else if(rc.readBroadcast(pastureChannel) < 3 && rc.getLocation().distanceSquaredTo(rc.senseHQLocation()) > 4){
				rc.broadcast(pastureChannel, rc.readBroadcast(pastureChannel) + 1);
				rc.construct(RobotType.PASTR);
			} else if(rc.canMove(moveDir)){
				rc.sneak(moveDir);
			} 
		senseTowerCows(rc);
		}
	}
	
	private static void senseTowerCows(RobotController rc) throws GameActionException{
		int cowChannel = 4000;
		int cowLocChannel = 4001;
		for(MapLocation nearby:MapLocation.getAllMapLocationsWithinRadiusSq(rc.getLocation(), 35)){
			int cows = (int)rc.senseCowsAtLocation(nearby);
			if (cows > rc.readBroadcast(cowChannel) && nearby.distanceSquaredTo(new MapLocation(rc.readBroadcast(4003)/100, rc.readBroadcast(4003)%100)) <= 361){
				boolean farPasture = true;
				for(MapLocation allyPasture:rc.sensePastrLocations(rc.getTeam())){
					if (nearby.distanceSquaredTo(allyPasture) < GameConstants.PASTR_RANGE)
						farPasture = false;
				}
				if (farPasture){
					rc.broadcast(cowChannel, cows);
					rc.broadcast(cowLocChannel, nearby.x * 100 + nearby.y);
				}
				//TODO vector function that shit
			}
		}
		rc.setIndicatorString(0, ""+rc.readBroadcast(cowLocChannel));
	}
	
	private static void runHQ(RobotController rc) throws GameActionException{
		rc.setIndicatorString(1, ""+rc.readBroadcast(4003));
		Combat.HQAttack(rc);
		Direction spawnDir = Direction.NORTH;
		if(rc.isActive() && rc.canMove(spawnDir) && rc.senseRobotCount() < GameConstants.MAX_ROBOTS){
			rc.spawn(spawnDir);
		}
	}
}
