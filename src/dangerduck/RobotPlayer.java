package dangerduck;

import battlecode.common.*;

import java.util.Random;

public strictfp class RobotPlayer {
    static int turnCount = 0;
    static final Random rng = new Random();

    private static GamePhase phase = GamePhase.SETUP;
    private static DuckAssignment assignment = DuckAssignment.None;

    /*
        Shared[0]:
            Bits[0]: Has a leader been declared yet (1)
            Bits[1 - 5]: How many robots have been set as attackers (up to 31)
            Bits[6 - 10]: How many robots have been set as defenders (up to 31)
            Bits[11 - 15]: How many robots have been set as scouts (up to 31)

        Shared[1]:
            Bits[0 - 7]: The x coordinate of the leader
            Bits[8 - 15]: The y coordinate of the leader
     */

    static final Direction[] directions = {
        Direction.NORTH,
        Direction.NORTHEAST,
        Direction.EAST,
        Direction.SOUTHEAST,
        Direction.SOUTH,
        Direction.SOUTHWEST,
        Direction.WEST,
        Direction.NORTHWEST,
    };

    private static void determineAssignment(RobotController rc)
    {
        // We don't need to waste anymore bytecode, just give up
        if (turnCount > 30)
        {
            assignment = DuckAssignment.Scout;
            return;
        }

        int shared;
        try {
            shared = rc.readSharedArray(0);
        } catch (GameActionException e) {
            System.out.println("Tried to read shared array but failed :(");
            e.printStackTrace();
            return;
        }

        boolean hasLeader = (shared & 1) == 1;
        if (!hasLeader)
        {
            shared |= 1;
            try {
                rc.writeSharedArray(0, shared);
                System.out.println("Leader found!");
                assignment = DuckAssignment.Leader;
            } catch (GameActionException e) {
                System.out.println("Tried to write shared array but failed :(");
                e.printStackTrace();
                return;
            }
        }

        assignment = DuckAssignment.Scout;
    }

    private static void determineSetupAction(RobotController rc)
    {
        MapLocation[] flags = rc.senseBroadcastFlagLocations();
        if (flags.length == 0)
        {
            // Cry
            return;
        }

        MapLocation flag = flags[0];
        Direction dir = rc.getLocation().directionTo(flag);
        try {
            if (rc.canMove(dir)) rc.move(dir);
        } catch (GameActionException e) {
            System.out.println("Tried to move but failed :(");
            e.printStackTrace();
        }

//        if (assignment == DuckAssignment.Leader)
//        {
//            // Move towards center of the map
//            MapLocation[] flags = rc.senseBroadcastFlagLocations();
//            if (flags.length == 0)
//            {
//                // Cry
//                return;
//            }
//
//            MapLocation flag = flags[0];
//            Direction dir = rc.getLocation().directionTo(flag);
//            try {
//                if (rc.canMove(dir)) rc.move(dir);
//            } catch (GameActionException e) {
//                System.out.println("Tried to move but failed :(");
//                e.printStackTrace();
//            }
//            return;
//        }
//
//        // Get the leaders location and try and move towards it
//        int shared;
//        try {
//            shared = rc.readSharedArray(1);
//        } catch (GameActionException e) {
//            System.out.println("Tried to read shared array (leader location) but failed :(");
//            e.printStackTrace();
//            return;
//        }
//
//        int leaderX = shared & 0xFF;
//        int leaderY = (shared >> 8) & 0xFF;
//        MapLocation leaderLoc = new MapLocation(leaderX, leaderY);
//        Direction dir = rc.getLocation().directionTo(leaderLoc);
//        try {
//            if (rc.canMove(dir)) rc.move(dir);
//        } catch (GameActionException e) {
//            System.out.println("Tried to move but failed :(");
//            e.printStackTrace();
//        }
    }

    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {
        // You can also use indicators to save debug notes in replays.
        rc.setIndicatorString("Hello world!");

        while (true) {
            // This code runs during the entire lifespan of the robot, which is why it is in an infinite
            // loop. If we ever leave this loop and return from run(), the robot dies! At the end of the
            // loop, we call Clock.yield(), signifying that we've done everything we want to do.

            turnCount += 1;  // We have now been alive for one more turn!

            if (turnCount >= GameConstants.SETUP_ROUNDS && phase != GamePhase.LIVE)
            {
                rc.setIndicatorString("Entering live phase!");
                phase = GamePhase.LIVE;
            }

            if (assignment == DuckAssignment.None)
            {
                rc.setIndicatorString("Determining assignment...");
                determineAssignment(rc);
            }

            if (assignment == DuckAssignment.Leader)
            {
                rc.setIndicatorString("I am the leader!");
                int shared;
                try {
                    shared = rc.readSharedArray(1);
                    shared &= 0xFF00;
                    shared |= rc.getLocation().x;
                    shared |= rc.getLocation().y << 8;
                    rc.writeSharedArray(1, shared);
                } catch (GameActionException e) {
                    System.out.println("Tried to write shared array (leader location) but failed :(");
                    e.printStackTrace();
                    return;
                }
            }

            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode.
            try {
                if (!rc.isSpawned())
                {
                    MapLocation[] spawnLocs = rc.getAllySpawnLocations();
                    MapLocation randomLoc = spawnLocs[rng.nextInt(spawnLocs.length)];
                    if (rc.canSpawn(randomLoc)) rc.spawn(randomLoc);
                }
                else
                {
                    if (phase == GamePhase.SETUP)
                    {
                        rc.setIndicatorString("Setting up...");
                        determineSetupAction(rc);
                    }
                }

            } catch (GameActionException e) {
                // Oh no! It looks like we did something illegal in the Battlecode world. You should
                // handle GameActionExceptions judiciously, in case unexpected events occur in the game
                // world. Remember, uncaught exceptions cause your robot to explode!
                System.out.println("GameActionException");
                e.printStackTrace();

            } catch (Exception e) {
                // Oh no! It looks like our code tried to do something bad. This isn't a
                // GameActionException, so it's more likely to be a bug in our code.
                System.out.println("Exception");
                e.printStackTrace();

            } finally {
                // Signify we've done everything we want to do, thereby ending our turn.
                // This will make our code wait until the next turn, and then perform this loop again.
                Clock.yield();
            }
            // End of loop: go back to the top. Clock.yield() has ended, so it's time for another turn!
        }

        // Your code should never reach here (unless it's intentional)! Self-destruction imminent...
    }
    public static void updateEnemyRobots(RobotController rc) throws GameActionException{
        // Sensing methods can be passed in a radius of -1 to automatically 
        // use the largest possible value.
        RobotInfo[] enemyRobots = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
        if (enemyRobots.length != 0){
            rc.setIndicatorString("There are nearby enemy robots! Scary!");
            // Save an array of locations with enemy robots in them for future use.
            MapLocation[] enemyLocations = new MapLocation[enemyRobots.length];
            for (int i = 0; i < enemyRobots.length; i++){
                enemyLocations[i] = enemyRobots[i].getLocation();
            }
            // Let the rest of our team know how many enemy robots we see!
            if (rc.canWriteSharedArray(0, enemyRobots.length)){
                rc.writeSharedArray(0, enemyRobots.length);
                int numEnemies = rc.readSharedArray(0);
            }
        }
    }
}
