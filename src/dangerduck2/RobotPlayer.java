package dangerduck2;

import battlecode.common.*;

import java.util.*;

public strictfp class RobotPlayer {
    static final Random rng = new Random();

    static boolean isCommander = false;

    static Direction[] directions = Direction.DIRECTION_ORDER;

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
            Pathing.moveTowards(targetLocation);
            return;
        }

        // Check if the rally point is set
        if (Communication.hasValidLocation(Communication.SharedIndex.RALLY_POINT))
        {
            MapLocation rallyPoint = Communication.readLocation(Communication.SharedIndex.RALLY_POINT);
            Pathing.moveTowards(rallyPoint);
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

        Pathing.moveTowards(nearestFlag);
    }

    private static void commenceDuckHeal(RobotController rc) throws GameActionException
    {
        if (rc.getHealth() < GameConstants.DEFAULT_HEALTH * Constants.BEHAVIOUR_HEALING_SELF_THRESHOLD && rc.canHeal(rc.getLocation()) && Constants.BEHAVIOUR_ALLOW_HEALING_SELF)
        {
            rc.heal(rc.getLocation());
            return;
        }

        if (!Constants.BEHAVIOUR_ALLOW_HEALING_TEAMMATES)
        {
            return;
        }

        // Check if there are any nearby allies that we can heal
        RobotInfo[] nearbyAllies = rc.senseNearbyRobots(-1, rc.getTeam());
        for (RobotInfo ally : nearbyAllies)
        {
            if (ally.getHealth() < GameConstants.DEFAULT_HEALTH * Constants.BEHAVIOUR_HEALING_TEAMMATES_THRESHOLD && rc.canHeal(ally.getLocation()))
            {
                rc.heal(ally.getLocation());
                return;
            }
        }
    }

    private static void commenceDuckStep(RobotController rc) throws GameActionException
    {
        if (!Actions.spawn())
        {
            return;
        }

        if (Globals.isSetup())
        {
            SetupActions.vacuum();
            return;
        }

        if (Globals.pickupFlagDelay > 0)
        {
            Globals.pickupFlagDelay -= 1;
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
            Actions.upgrade();
            duckCommanderStep(rc);

            int swarmLengthRemaining = Communication.getSwarmLength();
            if (Communication.hasValidLocation(Communication.SharedIndex.SWARM_LOCATION) && swarmLengthRemaining == 0)
            {
                Communication.clearLocation(Communication.SharedIndex.SWARM_LOCATION);
                return;
            }

            if (Communication.hasValidLocation(Communication.SharedIndex.SWARM_LOCATION) && swarmLengthRemaining > 0)
            {
                Communication.reduceSwarmLength();
            }
        }

        if (Globals.teamLeader)
        {
            rc.setIndicatorDot(rc.getLocation(), 255, 0, 0);
            SetupActions.setupWaterMines();
        }

        if (!rc.hasFlag() && Globals.hasFlag)
        {
            // We either captured the flag or dropped the flag
            Globals.hasFlag = false;
        }

        if (rc.hasFlag())
        {
            Globals.hasFlag = true;
            MapLocation nearestSpawn = rc.getAllySpawnLocations()[5];
            Pathing.moveTowards(nearestSpawn);
            return;
        }

        RobotInfo[] robots = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
        for (RobotInfo robot : robots)
        {
            if (robot.hasFlag() && Constants.BEHAVIOUR_ENABLE_FLAG_SWARM)
            {
                // The enemy team has our FLAG :(
                Communication.write(Communication.SharedIndex.SWARM_LOCATION, robot.getLocation());
            }
        }

        if (Communication.hasValidLocation(Communication.SharedIndex.SWARM_LOCATION))
        {
            MapLocation swarmLocation = Communication.readLocation(Communication.SharedIndex.SWARM_LOCATION);
            float distance = rc.getLocation().distanceSquaredTo(swarmLocation);
            if (distance < 45)
            {
                Pathing.moveTowards(swarmLocation);
                for (RobotInfo robot : robots)
                {
                    if (rc.canAttack(robot.getLocation()))
                    {
                        rc.attack(robot.getLocation());
                        return;
                    }
                }
                return;
            }
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
        for (RobotInfo robot : robots)
        {
            if (rc.canAttack(robot.getLocation()))
            {
                rc.attack(robot.getLocation());
                Direction d = rc.getLocation().directionTo(robot.getLocation()).opposite();
                if (rc.canMove(d))
                {
                    rc.move(d);
                }
                return;
            }
        }

        commenceDuckMove(rc);
        commenceDuckHeal(rc);
    }

    @SuppressWarnings("unused")
    public static void run(RobotController rc) {
        while (true) {
            Globals.turn += 1;

            try {
                if (!Globals.initialized)
                {
                    Globals.controller = rc;
                    Globals.id = Communication.getDuckId();
                    Globals.team = (int)(Globals.id / (GameConstants.ROBOT_CAPACITY / 3f));
                    Globals.initialized = true;

                    // Assign to first robot on each team
                    if (Globals.id == 0)
                    {
                        Globals.teamLeader = true;
                    }

                    rc.setIndicatorString("ID: " + Globals.id);
                }

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
