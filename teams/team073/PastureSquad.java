package team073;

import java.util.ArrayList;
import java.util.Random;

import battlecode.common.*;

public class PastureSquad extends Squad{

	static Random randall = new Random();
	
	final int PASTR_ID = 0;
	final int TOWER_ID = 1;
	
	final int SQUAD_SIZE = 5;
	RobotController rc;
	int robotIDs[];
	
	MapLocation pastrLoc;
	MapLocation noiseTowerLoc;
	MapLocation defenseLocA;
	MapLocation defenseLocB;
	MapLocation defenseLocC;
	
	public PastureSquad(RobotController rc, int robotIDs[])
	{
		this.rc = rc;
		this.robotIDs = robotIDs;
		
		setPastrLoc();//TODO: find best pasture location
		setNoiseTowerLoc();
		setDefendersLoc();
	}

	public void setPastrLoc()
	{
		pastrLoc = findRelativeLoc(rc.senseHQLocation(), 10);
	}
	
	public void setNoiseTowerLoc()
	{
		noiseTowerLoc = findRelativeLoc(pastrLoc, 4);
		
		while(noiseTowerLoc.equals(pastrLoc))
			noiseTowerLoc = findRelativeLoc(pastrLoc, 4);
	}
	
	public void setDefendersLoc()
	{
		defenseLocA = findRelativeLoc(pastrLoc, 5);
		
		while(defenseLocA.equals(pastrLoc) || defenseLocA.equals(noiseTowerLoc))
			defenseLocA = findRelativeLoc(pastrLoc, 5);

		defenseLocB = findRelativeLoc(pastrLoc, 5);
		
		while(defenseLocB.equals(pastrLoc) || defenseLocB.equals(noiseTowerLoc) || defenseLocB.equals(defenseLocA));
			defenseLocB = findRelativeLoc(pastrLoc, 5);
			
		defenseLocC = findRelativeLoc(pastrLoc, 5);
		
		while(defenseLocC.equals(pastrLoc) || defenseLocC.equals(noiseTowerLoc) || defenseLocC.equals(defenseLocA) || defenseLocC.equals(defenseLocB));
			defenseLocC = findRelativeLoc(pastrLoc, 5);
	}
	
	public MapLocation findRelativeLoc(MapLocation relativeObject, int distance)
	{
		MapLocation relativeLoc = new MapLocation(0, 0);
		
		int x = relativeObject.x;
		int y = relativeObject.y;
		
		relativeLoc = new MapLocation(VectorFunctions.abs(randall.nextInt(2*distance)-distance + x),VectorFunctions.abs(randall.nextInt(2*distance)-distance + y));
		
		while(rc.senseTerrainTile(relativeLoc).ordinal() > 1){ //0 NORMAL, 1 ROAD, 2 VOID, 3 OFF_MAP
			relativeLoc = new MapLocation(VectorFunctions.abs(randall.nextInt(2*distance)-distance + x),VectorFunctions.abs(randall.nextInt(2*distance)-distance + y));
		}
		
		return relativeLoc;
	}
	
	public void act() throws GameActionException
	{
		boolean movingToPasture = false;
		boolean atPastrLoc = false;
		boolean isPastr = false;
		
		boolean movingToDefensePosition = false;
		boolean atDefensePosition = false;
		
		ArrayList<MapLocation> pastrPath = new ArrayList<MapLocation>();
		
		//you are the pasture
		if(rc.getRobot().getID() == robotIDs[PASTR_ID])
		{
			//if not at pastr location or moving towards it, generate the path and follow it
			if(!atPastrLoc && !movingToPasture)
			{
				pastrPath = BugMove.generateBugPath(pastrLoc, rc.getLocation(), rc);
				movingToPasture = true;
			}
			//if you're moving toward the pasture but not there yet, move towards it
			else if(movingToPasture && !atPastrLoc)
			{
				BugMove.followPath(pastrPath);
				if(rc.getLocation().equals(pastrLoc))
				{
					atPastrLoc = true;
					movingToPasture = false;
				}
			}
			//if you're at the pastr location, make a pasture
			else if(!isPastr && atPastrLoc)
			{
				rc.construct(RobotType.PASTR);
				isPastr = true;
			}
		}
		
		//you are the noisetower
		else if(rc.getRobot().getID() == robotIDs[TOWER_ID])
		{

			//herd cows toward pasture
		}
		//you are one of the defensive bots
		
		
		else if(rc.getRobot().getID() == robotIDs[2] || rc.getRobot().getID() == robotIDs[3] || rc.getRobot().getID() == robotIDs[4])
		{
			if(!movingToDefensePosition && !atDefensePosition)
			{
				
			}
			//defend around location
			//send distress signal if attacked
		}
		
	}
	
	public void checkForDistressCall()
	{
		
	}
	
	public void sendDistressCall()
	{
		
	}
	
	
}
