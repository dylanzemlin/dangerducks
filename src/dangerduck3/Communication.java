package dangerduck3;

import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;

public class Communication {
    public enum SharedIndex {
        DUCK_IDS(0),
        SWARM_LOCATION(1),
        SWARM_LENGTH(2),
        TEAM_1_GOAL(3),
        TEAM_2_GOAL(4),
        TEAM_3_GOAL(5),
        TEAM_1_FLAG_POSITION(6),
        TEAM_2_FLAG_POSITION(7),
        TEAM_3_FLAG_POSITION(8);

        private final int index;

        SharedIndex(int index) {
            this.index = index;
        }

        public int getIndex() {
            return index;
        }
    }

    private static int toIndex(final SharedIndex index) {
        return index.getIndex();
    }

    public static void write(final SharedIndex index, final boolean value) throws GameActionException
    {
        Globals.controller.writeSharedArray(toIndex(index), value ? 1 : 0);
    }

    public static void write(final SharedIndex index, final MapLocation value) throws GameActionException
    {
        int x = value.x;
        int y = value.y;
        int encoded = (x << 8) | y;
        Globals.controller.writeSharedArray(toIndex(index), encoded);
    }

    public static boolean readBoolean(final SharedIndex index) throws GameActionException
    {
        return Globals.controller.readSharedArray(toIndex(index)) == 1;
    }

    public static MapLocation readLocation(final SharedIndex index) throws GameActionException
    {
        int encoded = Globals.controller.readSharedArray(toIndex(index));
        int x = (encoded & 0xFF00) >> 8;
        int y = (encoded & 0x00FF);
        return new MapLocation(x, y);
    }

    public static boolean hasValidLocation(final SharedIndex index) throws GameActionException
    {
        int encoded = Globals.controller.readSharedArray(toIndex(index));
        return encoded != GameConstants.MAX_SHARED_ARRAY_VALUE;
    }

    public static void clearLocation(final SharedIndex index) throws GameActionException
    {
        Globals.controller.writeSharedArray(toIndex(index), GameConstants.MAX_SHARED_ARRAY_VALUE);
    }

    public static int getDuckId() throws GameActionException
    {
        int nextId = Globals.controller.readSharedArray(toIndex(SharedIndex.DUCK_IDS));
        Globals.controller.writeSharedArray(toIndex(SharedIndex.DUCK_IDS), nextId + 1);
        return nextId;
    }

    public static int getSwarmLength() throws GameActionException
    {
        return Globals.controller.readSharedArray(toIndex(SharedIndex.SWARM_LENGTH));
    }

    public static void setSwarmLength() throws GameActionException
    {
        Globals.controller.writeSharedArray(toIndex(SharedIndex.SWARM_LENGTH), Constants.BEHAVIOUR_FLAG_SWARM_LENGTH);
    }

    public static void reduceSwarmLength() throws GameActionException
    {
        int swarmLength = getSwarmLength();
        if (swarmLength > 0)
        {
            Globals.controller.writeSharedArray(toIndex(SharedIndex.SWARM_LENGTH), swarmLength - 1);
        }
    }

    public static TeamGoal getTeamGoal() throws GameActionException
    {
        int teamGoal = Globals.controller.readSharedArray(toIndex(SharedIndex.TEAM_1_GOAL));
        return TeamGoal.values()[teamGoal];
    }

    public static void write(TeamGoal goal) throws GameActionException
    {
        if (goal == Globals.teamGoal)
        {
            return;
        }

        int idx = toIndex(SharedIndex.TEAM_1_GOAL) + (Globals.team - 1);
        Globals.controller.writeSharedArray(idx, goal.ordinal());
    }

    public static void writeFlagPosition(MapLocation location) throws GameActionException
    {
        int idx = toIndex(SharedIndex.TEAM_1_FLAG_POSITION) + (Globals.team - 1);
        int x = location.x;
        int y = location.y;
        int encoded = (x << 8) | y;
        Globals.controller.writeSharedArray(idx, encoded);
    }

    public static MapLocation getFlagPosition() throws GameActionException
    {
        int idx = toIndex(SharedIndex.TEAM_1_FLAG_POSITION) + (Globals.team - 1);
        int encoded = Globals.controller.readSharedArray(idx);
        int x = (encoded & 0xFF00) >> 8;
        int y = (encoded & 0x00FF);
        if (encoded == 0)
        {
            return null;
        }

        return new MapLocation(x, y);
    }
}
