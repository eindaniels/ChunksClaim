package de.eindaniel.chunksClaim;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ClaimData {
    private UUID owner;
    private Set<UUID> trusted;
    private boolean adminClaim;

    public ClaimData(UUID owner, boolean adminClaim) {
        this.owner = owner;
        this.adminClaim = adminClaim;
        this.trusted = new HashSet<>();
    }

    public UUID getOwner() { return owner; }
    public void setOwner(UUID owner) { this.owner = owner; }
    public Set<UUID> getTrusted() { return trusted; }
    public boolean isAdminClaim() { return adminClaim; }
    public void setAdminClaim(boolean adminClaim) { this.adminClaim = adminClaim; }

    public void addTrusted(UUID player) { trusted.add(player); }
    public void removeTrusted(UUID player) { trusted.remove(player); }

}
