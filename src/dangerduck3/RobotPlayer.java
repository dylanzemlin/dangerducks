package dangerduck3;

import battlecode.common.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

public strictfp class RobotPlayer {
    static final Random rng = new Random();
    static Direction[] directions = Direction.DIRECTION_ORDER;

    private static void commenceDuckMove(RobotController rc) throws GameActionException
    {
        // Check if the target location is set
        if (Globals.teamGoal == TeamGoal.SEARCH_FOR_FLAG)
        {
            if (Actions.regroup())
            {
                return;
            }

            if (Actions.moveTowardsFlag())
            {
                return;
            }
        }

        if (Globals.teamGoal == TeamGoal.DEFEND_RETURNING_FLAG)
        {
            if (Actions.moveTowardsReturningFlag())
            {
                return;
            }
        }

        if (Actions.regroup())
        {
            return;
        }

        // Find any dropped flags
        MapLocation[] nearbyFlags = rc.senseBroadcastFlagLocations();
        if (nearbyFlags.length == 0)
        {
            Pathing.moveRandom();
            return;
        }

        MapLocation nearestFlag = nearbyFlags[0];
        Pathing.moveTowards(nearestFlag);
    }

    private static void commenceDuckStep(RobotController rc) throws GameActionException
    {
        if (!Actions.spawn())
        {
            return;
        }

        if (Globals.isSetup() && !Globals.teamLeader)
        {
            SetupActions.vacuum();
            return;
        }

        if (Globals.teamLeader && Globals.isSetup() && Constants.STRATEGY_PLACE_WATER_MINES)
        {
            rc.setIndicatorDot(rc.getLocation(), 255, 0, 0);
            SetupActions.setupWaterMines();
            return;
        }

        if (Globals.pickupFlagDelay > 0)
        {
            Globals.pickupFlagDelay -= 1;
        }

        // If we are the commander, set the target location to the first flag
        if (Globals.commander)
        {
            Actions.upgrade();

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

        if (!rc.hasFlag() && Globals.hasFlag)
        {
            // We either captured the flag or dropped the flag
            Globals.hasFlag = false;

            if (Utilities.isInSpawn())
            {
                Communication.write(TeamGoal.WAR);
                Utilities.print("Captured flag, setting team goal to war");
            } else {
                Communication.write(TeamGoal.SEARCH_FOR_FLAG);
                Utilities.print("Flag dropped, setting team goal to search for flag");
            }
        }

        if (rc.hasFlag())
        {
            Globals.hasFlag = true;
            Communication.write(TeamGoal.DEFEND_RETURNING_FLAG);
            Communication.writeFlagPosition(rc.getLocation());
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
                Actions.attack(false);
                return;
            }
        }

        // Check if there are any flags within range
        FlagInfo[] nearbyFlags = rc.senseNearbyFlags(-1, rc.getTeam().opponent());
        ArrayList<FlagInfo> nearbyFlagsList = new ArrayList<>(Arrays.asList(nearbyFlags));
        nearbyFlagsList.removeIf(flag -> flag.isPickedUp());
        if (!nearbyFlagsList.isEmpty())
        {
            Actions.captureFlag(nearbyFlagsList.toArray(new FlagInfo[0]));
            return;
        }

        Actions.attack(true);
        commenceDuckMove(rc);
        Actions.plantMine();
        Actions.heal();
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
                    if (Globals.id % (int)(GameConstants.ROBOT_CAPACITY / 3f) == 0)
                    {
                        Globals.teamLeader = true;
                    }

                    if (Globals.id == 0)
                    {
                        Globals.commander = true;
                    }
                }

                Globals.teamGoal = Communication.getTeamGoal();
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
