package dangerduck2;

import battlecode.common.*;

import java.util.*;

public strictfp class RobotPlayer {
    static int turnCount = 0;
    static final Random rng = new Random();

    static MapLocation currentTargetLocation = null;
    static MapLocation currentSourceLocation = null;
    static int turnsSpentFindingLine = 0;
    static float lastGoodDistance = 999999;
    static boolean hasFlag = false;
    static ArrayList<MapLocation> currentLine = new ArrayList<>();
    static boolean tracingObstacle = false;
    static Direction bugDirection = null;
    static boolean isCommander = false;

    static Direction[] directions = Direction.DIRECTION_ORDER;

    private static void generatePathingLine(RobotController rc)
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

    private static void traceObstacle(RobotController rc, MapInfo[] infos) throws GameActionException
    {
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

        boolean goLeft = rng.nextBoolean();
        if (goLeft)
        {
            bugDirection = bugDirection.rotateLeft();
        } else {
            bugDirection = bugDirection.rotateRight();
        }
    }

    private static void moveTowards(RobotController rc, MapLocation location) throws GameActionException
    {
        if (isCommander)
        {
            rc.setIndicatorLine(rc.getLocation(), location, 0, 200, 100);
        } else {
            rc.setIndicatorLine(rc.getLocation(), location, 200, 200, 100);
        }

        if (rc.hasFlag())
        {
            rc.setIndicatorLine(rc.getLocation(), location, 200, 0, 0);
        }

        // Generate a line of map locations between us and the target location
        turnsSpentFindingLine++;
        if (!location.equals(currentTargetLocation) || turnsSpentFindingLine > 10)
        {
            currentSourceLocation = rc.getLocation();
            currentTargetLocation = location;
            currentLine = new ArrayList<>();
            turnsSpentFindingLine = 0;
            generatePathingLine(rc);
            rc.setIndicatorString("Generating new line");
        }

        if (currentSourceLocation != null)
        {
            if (isCommander)
            {
                rc.setIndicatorLine(currentSourceLocation, location, 0, 200, 100);
            } else {
                rc.setIndicatorLine(currentSourceLocation, location, 200, 200, 100);
            }
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
        rc.setIndicatorDot(nextLocation, 0, 0, 255);
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
            traceObstacle(rc, infos);
            return;
        }

        // If we can't move towards the target location, try and move around the obstacle
        tracingObstacle = true;
        bugDirection = rc.getLocation().directionTo(currentTargetLocation);
        rc.setIndicatorString("Moving around obstacle");
        lastGoodDistance = rc.getLocation().distanceSquaredTo(currentTargetLocation);
        traceObstacle(rc, infos);
    }

    private static void trySpawn(RobotController rc) throws GameActionException
    {
        if (rc.isSpawned())
        {
            return;
        }

        MapLocation[] spawnLocs = rc.getAllySpawnLocations();
        for (MapLocation spawnLoc : spawnLocs)
        {
            if (rc.canSpawn(spawnLoc))
            {
                rc.spawn(spawnLoc);
                return;
            }
        }

        // Can't spawn D:
    }

    private static void duckCommanderStep(RobotController rc) throws GameActionException
    {
        // Check for any dropped flags
        MapLocation[] flags = rc.senseBroadcastFlagLocations();
        if (flags.length == 0)
        {
            return;
        }

        // Set the target location to the first flag
        MapLocation targetLocation = flags[0];
        Communication.write(Communication.SharedIndex.CURRENT_TARGET, targetLocation);
    }

    private static void duckCommanderInit(RobotController rc) throws GameActionException
    {
        // Set the rally point to the middle spawn point
        MapLocation[] spawnLocs = rc.getAllySpawnLocations();
        MapLocation middleSpawn = spawnLocs[spawnLocs.length / 2];
        Communication.write(Communication.SharedIndex.RALLY_POINT, middleSpawn);
    }

    private static void commenceDuckMove(RobotController rc) throws GameActionException
    {
        // Check if the target location is set
        if (Communication.hasValidLocation(Communication.SharedIndex.CURRENT_TARGET))
        {
            MapLocation targetLocation = Communication.readLocation(Communication.SharedIndex.CURRENT_TARGET);
            moveTowards(rc, targetLocation);
            return;
        }

        // Check if the rally point is set
        if (Communication.hasValidLocation(Communication.SharedIndex.RALLY_POINT))
        {
            MapLocation rallyPoint = Communication.readLocation(Communication.SharedIndex.RALLY_POINT);
            moveTowards(rc, rallyPoint);
            return;
        }

        // Move in a random direction
        Direction randomDirection = directions[rng.nextInt(directions.length)];
        if (rc.canMove(randomDirection))
        {
            rc.move(randomDirection);
        }
    }

    private static void tryCaptureNearestFlag(RobotController rc, FlagInfo[] flags) throws GameActionException {
        // Check if we can capture any of the flags
        for (FlagInfo flag : flags)
        {
            if (rc.canPickupFlag(flag.getLocation()))
            {
                try {
                    rc.pickupFlag(flag.getLocation());
                    rc.setIndicatorString("Captured flag");
                } catch (GameActionException e)
                {
                    // Ignore
                }
                return;
            }
        }

        // Move towards the nearest flag
        MapLocation nearestFlag = flags[0].getLocation();
        if (flags[0].isPickedUp())
        {
            return;
        }

        moveTowards(rc, nearestFlag);
    }

    private static void commenceDuckHeal(RobotController rc) throws GameActionException
    {
        // If we are below 3/4 health, heal
        if (rc.getHealth() < GameConstants.DEFAULT_HEALTH * 0.75f && rc.canHeal(rc.getLocation()))
        {
            rc.heal(rc.getLocation());
            return;
        }

        // Check if there are any nearby allies that we can heal
        RobotInfo[] nearbyAllies = rc.senseNearbyRobots(-1, rc.getTeam());
        for (RobotInfo ally : nearbyAllies)
        {
            if (ally.getHealth() < GameConstants.DEFAULT_HEALTH * 0.75f && rc.canHeal(ally.getLocation()))
            {
                rc.heal(ally.getLocation());
                return;
            }
        }
    }

    private static void commenceDuckStep(RobotController rc) throws GameActionException
    {
        Communication.initialize(rc);

        // Try and spawn the robot
        try {
            trySpawn(rc);
        } catch (GameActionException e)
        {
            e.printStackTrace();
            return;
        }

        if (!rc.isSpawned())
        {
            return;
        }

        // Check if the commander flag is set
        boolean isCommanderSet = Communication.readBoolean(Communication.SharedIndex.HAS_LEADER);
        if (!isCommanderSet)
        {
            Communication.write(Communication.SharedIndex.HAS_LEADER, true);
            isCommander = true;
            duckCommanderInit(rc);
        }

        // If we are the commander, set the target location to the first flag
        if (isCommander)
        {
            rc.setIndicatorString("I am commander");
            duckCommanderStep(rc);
            return;
        }

        if (!rc.hasFlag() && hasFlag)
        {
            // We either captured the flag or dropped the flag
            hasFlag = false;
        }

        if (rc.hasFlag())
        {
            hasFlag = true;
            MapLocation nearestSpawn = rc.getAllySpawnLocations()[5];
            moveTowards(rc, nearestSpawn);
            return;
        }

        // Check if there are any flags within range
        FlagInfo[] nearbyFlags = rc.senseNearbyFlags(-1, rc.getTeam().opponent());
        ArrayList<FlagInfo> nearbyFlagsList = new ArrayList<>(Arrays.asList(nearbyFlags));
        nearbyFlagsList.removeIf(flag -> flag.isPickedUp());
        if (!nearbyFlagsList.isEmpty())
        {
            tryCaptureNearestFlag(rc, nearbyFlagsList.toArray(new FlagInfo[0]));
            return;
        }

        // Check if we can attack any of the robots
        RobotInfo[] robots = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
        for (RobotInfo robot : robots)
        {
            if (rc.canAttack(robot.getLocation()))
            {
                rc.attack(robot.getLocation());
                return;
            }
        }

        commenceDuckMove(rc);
        commenceDuckHeal(rc);
    }

    @SuppressWarnings("unused")
    public static void run(RobotController rc) {
        while (true) {
            turnCount += 1;  // We have now been alive for one more turn!

            try {
                commenceDuckStep(rc);
            } catch (GameActionException e) {
                System.out.println("GameActionException");
                e.printStackTrace();

            } catch (Exception e) {
                System.out.println("Exception");
                e.printStackTrace();
            } finally {
                Clock.yield();
            }
        }
    }
}
