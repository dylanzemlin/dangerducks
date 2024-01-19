package dangerduck2;

import battlecode.common.GameActionException;
import battlecode.common.MapInfo;
import battlecode.common.MapLocation;
import battlecode.common.TrapType;

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
        MapLocation middle = spawnLocations[spawnLocations.length / 2];
        Pathing.moveTowards(middle);

        MapLocation currentLocation = Globals.controller.getLocation();
        if (currentLocation.distanceSquaredTo(middle) > 2)
        {
            return;
        }

        MapInfo[] nearbyInfo = Globals.controller.senseNearbyMapInfos();
        for (MapInfo info : nearbyInfo)
        {
            if (info.getTrapType() != TrapType.NONE || info.isWater() || info.isWall())
            {
                continue;
            }

            if (info.getCrumbs() < TrapType.WATER.buildCost)
            {
                continue;
            }

            if (Globals.controller.canBuild(TrapType.WATER, info.getMapLocation()))
            {
                Globals.controller.build(TrapType.WATER, info.getMapLocation());
            }
            Globals.waterMinesPlaced += 1;
        }
    }
}
