package com.ourteam.events;

import com.ourteam.model.Team;
import org.bukkit.event.Event;

/**
 * Abstract base class for all team-related events in OurTeam.
 */
public abstract class TeamEvent extends Event {
    protected final Team team;

    public TeamEvent(Team team) {
        this.team = team;
    }

    /**
     * Gets the team involved in this event.
     */
    public Team getTeam() {
        return team;
    }
}
