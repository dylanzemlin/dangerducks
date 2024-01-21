package dangerduck3;

import battlecode.common.GameConstants;
import battlecode.common.RobotController;

public class Globals {
    public static int turn;
    public static int id;
    public static int team;
    public static boolean teamLeader;
    public static int waterMinesPlaced = 0;
    public static boolean initialized;
    public static boolean commander;
    public static TeamGoal teamGoal;
    public static RobotController controller;
    /**
     * Whether or not the robot has the flag.
     */
    public static boolean hasFlag = false;
    /**
     * The number of turns to wait before picking up the flag.
     */
    public static int pickupFlagDelay;

    public static boolean isSetup()
    {
        return turn <= GameConstants.SETUP_ROUNDS;
    }
}
