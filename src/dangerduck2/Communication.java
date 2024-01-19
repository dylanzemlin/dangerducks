package dangerduck2;

import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class Communication {
    public enum SharedIndex {
        HAS_LEADER(0),
        CURRENT_TARGET(1),
        RALLY_POINT(2),
        DUCK_IDS(3),
        SWARM_LOCATION(4),
        SWARM_LENGTH(5);

        private final int index;

        private SharedIndex(int index) {
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
}
