package team073;

import battlecode.common.*;

public class Herder{
	
	static double[][] coarseCowinessMap;
	
	/*
	 * Returns a 2d array that assesses the cowiness bigBoxSize-sized squares of the map
	 * Cowiness - summed growth rate for the box
	 * @bigBoxSize - side length of the coarse grid squares
	 */
	public static void assessCoarseCowiness(int bigBoxSize,RobotController rc){
		int coarseWidth = rc.getMapWidth()/bigBoxSize;
		int coarseHeight = rc.getMapHeight()/bigBoxSize;
		double[][] fineCowinessMap = rc.senseCowGrowth();
		coarseCowinessMap = new double[coarseWidth][coarseHeight];
		
		for(int x=0;x<coarseWidth*bigBoxSize;x++){
			for(int y=0;y<coarseHeight*bigBoxSize;y++){
				coarseCowinessMap[x/bigBoxSize][y/bigBoxSize]+=fineCowinessMap[x][y];
			}
		}
	}
	
	/*
	 * Returns the best maplocation (the center of the cowiest coarse map squares a given distance from hq) 
	 * @numPastrs: number of pastrs in returned list
	 * @int distToHQ: 
	 */
//	public MapLocation[] getGoodPastrLocation(int numPastrs, int distToHQ){
		
		// iterating through 
//		for (int x = 0; x < coarseCowinessMap.length; x++){
//			for (int y = 0; y < coarseCowinessMap[0].length; y++){
				
//			}
//		}
//	}
}