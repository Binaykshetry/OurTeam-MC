package com.ourteam.manager;

import com.ourteam.OurTeam;
import org.jetbrains.annotations.NotNull;

/**
 * Extension class wrapping OurTeam placeholders under the "nteam" namespace pattern.
 */
public class NTeamPlaceholders extends OurTeamPlaceholders {

    public NTeamPlaceholders(OurTeam plugin) {
        super(plugin);
    }

    @Override
    public @NotNull String getIdentifier() {
        return "nteam";
    }
}
