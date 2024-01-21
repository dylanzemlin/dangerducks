package dangerduck3;

import battlecode.common.*;

public class Actions {
    /**
     * Attempts to spawn a robot.
     * @return True if the robot was spawned, false otherwise.
     */
    public static boolean spawn() throws GameActionException
    {
        if (Globals.controller.isSpawned())
        {
            return true;
        }

        MapLocation spawnLocation = Utilities.getSpawnLocation();
        if (spawnLocation == null)
        {
            return false;
        }

        Globals.controller.spawn(Utilities.getSpawnLocation());
        return false;
    }

    public static void upgrade() throws GameActionException
    {
        if (Globals.controller.canBuyGlobal(GlobalUpgrade.ATTACK))
        {
            Globals.controller.buyGlobal(GlobalUpgrade.ATTACK);
        }

        if (Globals.controller.canBuyGlobal(GlobalUpgrade.HEALING))
        {
            Globals.controller.buyGlobal(GlobalUpgrade.HEALING);
        }
    }

    public static void heal() throws GameActionException
    {
        if (!Constants.BEHAVIOUR_ALLOW_HEALING_TEAMMATES)
        {
            return;
        }

        // Check if there are any nearby allies that we can heal
        final RobotController rc = Globals.controller;
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

    public static void captureFlag(FlagInfo[] flags) throws GameActionException {
        // Check if we can capture any of the flags
        final RobotController rc = Globals.controller;
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

    public static boolean moveTowardsFlag() throws GameActionException
    {
        MapLocation[] flagLocations = Globals.controller.senseBroadcastFlagLocations();
        if (flagLocations.length > 0 && Globals.team <= flagLocations.length - 1)
        {
            MapLocation targetLocation = flagLocations[Globals.team];
            Pathing.moveTowards(targetLocation);
            return true;
        }

        return false;
    }

    public static boolean moveTowardsReturningFlag() throws GameActionException
    {
        MapLocation flagLocation = Communication.getFlagPosition();
        if (flagLocation == null)
        {
            return false;
        }

        Direction direction = Globals.controller.getLocation().directionTo(flagLocation);
        flagLocation = flagLocation.add(direction.opposite());
        flagLocation = flagLocation.add(direction.opposite());
        flagLocation = flagLocation.add(direction.opposite());

        Pathing.moveTowards(flagLocation);
        return true;
    }

    public static void plantMine() throws GameActionException
    {
        RobotController rc = Globals.controller;
        if (rc.getCrumbs() < 2500)
        {
            return;
        }

        MapLocation location = rc.getLocation().add(Direction.NORTH);
        boolean random = Math.random() > 0.5;
        TrapType type = random ? TrapType.EXPLOSIVE : TrapType.STUN;
        if (rc.canBuild(type, location) && rc.isActionReady())
        {
            rc.build(type, location);
        }
    }

    public static void attack(boolean move) throws GameActionException
    {
        final RobotController rc = Globals.controller;
        RobotInfo[] robots = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
        for (RobotInfo robot : robots)
        {
            if (rc.canAttack(robot.getLocation()))
            {
                rc.attack(robot.getLocation());
                Direction d = rc.getLocation().directionTo(robot.getLocation()).opposite();
                if (rc.canMove(d) && move)
                {
                    rc.move(d);
                }
                return;
            }
        }
    }

    public static boolean regroup() throws GameActionException
    {
        if (!Constants.BEHAVIOUR_REGROUP_ENABLED)
        {
            return false;
        }

        MapLocation regroupLocation = Utilities.getRegroupLocation();
        if (regroupLocation == null)
        {
            return false;
        }

        MapLocation currentLocation = Globals.controller.getLocation();
        if (currentLocation.distanceSquaredTo(regroupLocation) > Constants.BEHAVIOUR_REGROUP_IGNORE_RADIUS)
        {
            return false;
        }

        int nearbyAllies = Utilities.getNearbyAllyCount(currentLocation);
        if (nearbyAllies >= Constants.BEHAVIOUR_REGROUP_REQUIRED_ALLIES)
        {
            return false;
        }

        Pathing.moveTowards(regroupLocation);
        return true;
    }
}
