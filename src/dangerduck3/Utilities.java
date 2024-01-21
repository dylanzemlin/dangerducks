package dangerduck3;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;

public class Utilities {
    public static MapLocation[] getSpawnLocations()
    {
        MapLocation[] locations = Globals.controller.getAllySpawnLocations();
        if (locations.length == 0)
        {
            return new MapLocation[0];
        }

        int team = Globals.team;
        int startIndex = team * 9;
        int endIndex = startIndex + 9;

        MapLocation[] teamLocations = new MapLocation[9];
        for (int i = startIndex; i < endIndex; i++)
        {
            teamLocations[i - startIndex] = locations[i];
        }
        return teamLocations;
    }

    public static MapLocation getSpawnLocation()
    {
        MapLocation[] locations = getSpawnLocations();

        for (MapLocation location : locations)
        {
            if (Globals.controller.canSpawn(location))
            {
                return location;
            }
        }

        return null;
    }

    public static MapLocation getRegroupLocation()
    {
        MapLocation[] locations = getSpawnLocations();
        return locations[4];
    }

    public static boolean isInSpawn()
    {
        MapLocation[] locations = getSpawnLocations();
        MapLocation currentLocation = Globals.controller.getLocation();
        for (MapLocation location : locations)
        {
            if (location.equals(currentLocation))
            {
                return true;
            }
        }
        return false;
    }

    public static void print(String message)
    {
        System.out.println("[" + Globals.id + " | " + Globals.team + "] " + message);
    }

    public static int getNearbyAllyCount(MapLocation location) throws GameActionException {
        return Globals.controller.senseNearbyRobots(-1, Globals.controller.getTeam()).length;
    }
}
