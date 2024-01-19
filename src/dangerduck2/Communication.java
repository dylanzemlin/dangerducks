package dangerduck2;

import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class Communication {
    public static RobotController controller;

    public enum SharedIndex {
        HAS_LEADER(0),
        CURRENT_TARGET(1),
        RALLY_POINT(2);

        private final int index;

        private SharedIndex(int index) {
            this.index = index;
        }

        public int getIndex() {
            return index;
        }
    }

    public static void initialize(final RobotController controller) throws GameActionException
    {
        Communication.controller = controller;
    }

    private static int toIndex(final SharedIndex index) {
        return index.getIndex();
    }

    public static void write(final SharedIndex index, final boolean value) throws GameActionException
    {
        controller.writeSharedArray(toIndex(index), value ? 1 : 0);
    }

    public static void write(final SharedIndex index, final MapLocation value) throws GameActionException
    {
        int x = value.x;
        int y = value.y;
        int encoded = (x << 8) | y;
        controller.writeSharedArray(toIndex(index), encoded);
    }

    public static boolean readBoolean(final SharedIndex index) throws GameActionException
    {
        return controller.readSharedArray(toIndex(index)) == 1;
    }

    public static MapLocation readLocation(final SharedIndex index) throws GameActionException
    {
        int encoded = controller.readSharedArray(toIndex(index));
        int x = (encoded & 0xFF00) >> 8;
        int y = (encoded & 0x00FF);
        return new MapLocation(x, y);
    }

    public static boolean hasValidLocation(final SharedIndex index) throws GameActionException
    {
        int encoded = controller.readSharedArray(toIndex(index));
        return encoded != GameConstants.MAX_SHARED_ARRAY_VALUE;
    }

    public static void clearLocation(final SharedIndex index) throws GameActionException
    {
        controller.writeSharedArray(toIndex(index), GameConstants.MAX_SHARED_ARRAY_VALUE);
    }
}
