package dangerduck2;

import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.GlobalUpgrade;
import battlecode.common.MapLocation;

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
}
