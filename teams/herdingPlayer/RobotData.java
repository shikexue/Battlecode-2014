package herdingPlayer;

//TODO: beware of import dependencies
import java.util.ArrayList;

import battlecode.common.MapLocation;
import herdingPlayer.Constants;

public class RobotData{
	Constants.Task task;
	ArrayList<MapLocation> path;
	MapLocation goal;
	boolean taskSet;
	
	public RobotData(Constants.Task setTask, MapLocation setGoal, ArrayList<MapLocation> setPath, boolean setTaskSet){
		task = setTask;
		goal = setGoal;
		path = setPath;
		taskSet = setTaskSet;
	}
	
	public void setData(Constants.Task setTask, MapLocation setGoal, ArrayList<MapLocation> setPath){
		task = setTask;
		goal = setGoal;
		path = setPath;
	}
	
	public RobotData(Constants.Task setTask, MapLocation setGoal, int setPriority){
		task = setTask;
		goal = setGoal;
		path = new ArrayList<MapLocation>();
	}
	
	public Constants.Task getTask(){
		return task;
	}
	
	public MapLocation getGoal() {
		return goal;
	}
	
	public ArrayList<MapLocation> getPath(){
		return path;
	}
	
	public boolean getTaskSet(){
		return taskSet;
	}
	
}


