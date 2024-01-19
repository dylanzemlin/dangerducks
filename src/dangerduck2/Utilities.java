package dangerduck2;

import battlecode.common.MapLocation;

import java.util.Random;

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
}
