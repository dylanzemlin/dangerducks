package dangerduck2;

import battlecode.common.*;

public class SetupActions {
    public static void vacuum() throws GameActionException
    {
        MapInfo[] nearbyInfo = Globals.controller.senseNearbyMapInfos();
        for (MapInfo info : nearbyInfo)
        {
            if (info.getCrumbs() > 0)
            {
                Pathing.moveTowards(info.getMapLocation());
                return;
            }
        }

        // Move in a random direction
        Pathing.moveRandom();
    }

    public static void setupWaterMines() throws GameActionException
    {
        // Place water mines in checker pattern within our spawn area
        MapLocation[] spawnLocations = Utilities.getSpawnLocations();
        if (Globals.waterMinesPlaced > 8)
        {
            return;
        }

        if (Globals.waterMinesPlaced == 4)
        {
            Globals.waterMinesPlaced++;
        }
        MapLocation waterMineLocation = spawnLocations[Globals.waterMinesPlaced];

        if (Globals.controller.getLocation().distanceSquaredTo(waterMineLocation) > 2)
        {
            Pathing.moveTowards(waterMineLocation);
            return;
        }

        if (Globals.controller.canBuild(TrapType.WATER, waterMineLocation))
        {
            Globals.controller.build(TrapType.WATER, waterMineLocation);
            Globals.waterMinesPlaced++;
        }
    }
}
