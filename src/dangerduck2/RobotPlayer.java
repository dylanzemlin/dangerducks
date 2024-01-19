package dangerduck2;

import battlecode.common.*;

import java.util.Random;

public strictfp class RobotPlayer {
    static int turnCount = 0;
    static final Random rng = new Random();

    static MapLocation currentTargetLocation = null;
    static MapLocation currentSourceLocation = null;
    static boolean isCommander = false;

    /*
        ----------------- SHARED ARRAYS -----------------

        0 (Map Location): Flag 1
        1 (Map Location): Flag 2
        2 (Map Location): Flag 3
        3 (Boolean): Containers Commander
        4 (Map Location): Goal
        5 (Map Location): Rally
     */

    public MapLocation extractSharedLocation(RobotController rc, int index) throws GameActionException {
        int shared = rc.readSharedArray(index);
        int x = (shared & 0xFF00) >> 8;
        int y = (shared & 0x00FF);
        return new MapLocation(x, y);
    }

    public void writeSharedLocation(RobotController rc, MapLocation location, int index) throws GameActionException {
        int x = location.x;
        int y = location.y;
        int shared = (x << 8) | y;
        rc.writeSharedArray(index, shared);
    }

    public boolean isSharedLocationSet(RobotController rc, int index) throws GameActionException {
        int shared = rc.readSharedArray(index);
        return shared != 0;
    }

    public void writeSharedBoolean(RobotController rc, boolean value, int index) throws GameActionException {
        int shared = value ? 1 : 0;
        rc.writeSharedArray(index, shared);
    }

    public boolean extractSharedBoolean(RobotController rc, int index) throws GameActionException {
        int shared = rc.readSharedArray(index);
        return shared == 1;
    }

    private void moveTowards(RobotController rc, MapLocation location) throws GameActionException
    {
        rc.setIndicatorLine(rc.getLocation(), location, 200, 200, 100);
    }

    private void trySpawn(RobotController rc) throws GameActionException
    {
        if (rc.isSpawned())
        {
            return;
        }

        MapLocation[] spawnLocs = rc.getAllySpawnLocations();
        MapLocation randomLoc = spawnLocs[rng.nextInt(spawnLocs.length)];
        rc.spawn(randomLoc);
    }

    private void commenceDuckStep(RobotController rc) throws GameActionException
    {
        // Try and spawn the robot
        trySpawn(rc);

        // Check if the commander flag is set
        boolean isCommanderSet = extractSharedBoolean(rc, 3);
        if (isCommanderSet)
        {
            writeSharedLocation(rc, rc.getLocation(), 4);
            isCommander = true;
        }

        // If we are the commander, set the target location to the first flag
        if (isCommander)
        {
            MapLocation[] droppedFlags = rc.senseBroadcastFlagLocations();
            if (droppedFlags.length == 0)
            {
                // Set the target location to us, as a rally point
                writeSharedLocation(rc, rc.getLocation(), 4);
                writeSharedLocation(rc, rc.getLocation(), 5);
                return;
            }

            MapLocation firstFlag = droppedFlags[0];
            writeSharedLocation(rc, firstFlag, 4);
            writeSharedLocation(rc, firstFlag, 5);
            moveTowards(rc, firstFlag);
            return;
        }

        // If we are not the commander, move towards the target location
        if (!isSharedLocationSet(rc, 4))
        {
            return;
        }

        MapLocation targetLocation = extractSharedLocation(rc, 4);
        moveTowards(rc, targetLocation);
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
