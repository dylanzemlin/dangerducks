package dangerduck2;

import battlecode.common.*;

import java.util.ArrayList;
import java.util.Random;

public class Pathing {
    static Random rng = new Random();
    static MapLocation currentTargetLocation = null;
    static MapLocation currentSourceLocation = null;
    static Direction bugDirection = null;
    static ArrayList<MapLocation> currentLine = new ArrayList<>();
    static boolean tracingObstacle = false;
    static int turnsSpentFindingLine = 0;
    static float lastGoodDistance = 999999;
    static int directionProgress = 0;
    static boolean direction;

    private static void generatePathingLine()
    {
        // Generate a line of map locations between us and the target location
        MapLocation currentLocation = currentSourceLocation;
        while (currentLocation.distanceSquaredTo(currentTargetLocation) > 2)
        {
            currentLine.add(currentLocation);
            currentLocation = currentLocation.add(currentLocation.directionTo(currentTargetLocation));
        }

        currentLine.add(currentTargetLocation);
        if (currentLine.size() > 1)
        {
            currentLine.remove(0);
        }
    }

    private static void traceObstacle(MapInfo[] infos) throws GameActionException
    {
        final RobotController rc = Globals.controller;
        turnsSpentFindingLine += 1;

        // Check if we are on a point on the line
        if (currentLine.isEmpty())
        {
            tracingObstacle = false;
            return;
        }

        for (MapLocation location : currentLine)
        {
            float distanceToTarget = rc.getLocation().distanceSquaredTo(currentTargetLocation);
            if (rc.getLocation().distanceSquaredTo(location) <= 2 && distanceToTarget < lastGoodDistance)
            {
                tracingObstacle = false;
                bugDirection = null;
                return;
            }
        }

        if (rc.onTheMap(rc.getLocation().add(bugDirection)))
        {
            MapInfo bugInfo = rc.senseMapInfo(rc.getLocation().add(bugDirection));
            if (bugInfo.isWater() && rc.getCrumbs() >= GameConstants.FILL_COST && Constants.PATHING_ALLOW_WATER_FILL && Globals.pickupFlagDelay == 0)
            {
                if (rc.canDropFlag(rc.getLocation()) && rc.hasFlag())
                {
                    Globals.hasFlag = false;
                    rc.dropFlag(rc.getLocation());
                    Globals.pickupFlagDelay = Constants.PATHING_FLAG_DROP_TO_FILL_DELAY;
                }

                if (!rc.hasFlag() && rc.isActionReady())
                {
                    rc.fill(rc.getLocation().add(bugDirection));
                }
            }
        }

        // Check if we can move in the bug direction
        if (rc.canMove(bugDirection))
        {
            rc.move(bugDirection);
            // bugDirection = bugDirection.rotateLeft();
            return;
        }

        // Check if the right of the bug direction is blocked
        Direction rightDirection = bugDirection.rotateRight();
        if (rc.canMove(rightDirection))
        {
            rc.move(rightDirection);
            bugDirection = rightDirection;
            return;
        }

        // Check if the left of the bug direction is blocked
        Direction leftDirection = bugDirection.rotateLeft();
        if (rc.canMove(leftDirection))
        {
            rc.move(leftDirection);
            bugDirection = leftDirection;
            return;
        }

        if (direction)
        {
            bugDirection = bugDirection.rotateLeft();
        } else {
            bugDirection = bugDirection.rotateRight();
        }

        if (directionProgress++ > 30)
        {
            direction = !direction;
            directionProgress = 0;
        }
    }

    public static void moveTowards(MapLocation location) throws GameActionException
    {
        final RobotController rc = Globals.controller;
        rc.setIndicatorLine(rc.getLocation(), location, 0, 0, 255);

        // Generate a line of map locations between us and the target location
        turnsSpentFindingLine++;
        if (!location.equals(currentTargetLocation) || turnsSpentFindingLine > 10)
        {
            currentSourceLocation = rc.getLocation();
            currentTargetLocation = location;
            currentLine = new ArrayList<>();
            turnsSpentFindingLine = 0;
            generatePathingLine();
            rc.setIndicatorString("Generating new line");
        }

        if (currentLine.isEmpty())
        {
            rc.setIndicatorString("Line is empty");
            return;
        }

        // Get the map information around us
        MapInfo[] infos = rc.senseNearbyMapInfos();

        // If we are not tracing an obstacle, try and move towards the target location
        MapLocation nextLocation = currentLine.get(0);
        Direction dir = rc.getLocation().directionTo(nextLocation);
        if (rc.canMove(dir))
        {
            tracingObstacle = false;
            bugDirection = null;
            rc.setIndicatorString("Moving towards target");
            rc.move(dir);
            if (currentLine.isEmpty())
            {
                return;
            }

            currentLine.remove(0);
            return;
        }

        // If we are tracing an obstacle, try and move around it
        if (tracingObstacle)
        {
            rc.setIndicatorString("Tracing obstacle");
            traceObstacle(infos);
            return;
        }

        // If we can't move towards the target location, try and move around the obstacle
        tracingObstacle = true;
        bugDirection = rc.getLocation().directionTo(currentTargetLocation);
        rc.setIndicatorString("Moving around obstacle");
        lastGoodDistance = rc.getLocation().distanceSquaredTo(currentTargetLocation);
        traceObstacle(infos);
    }

    public static void moveRandom() throws GameActionException
    {
        final RobotController rc = Globals.controller;
        Direction dir = Direction.values()[rng.nextInt(8)];
        if (rc.canMove(dir))
        {
            rc.move(dir);
        }
    }
}
