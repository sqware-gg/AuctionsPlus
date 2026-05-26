package dev.auctionsplus.listing;

import java.util.UUID;
import org.bukkit.inventory.ItemStack;

public final class ClaimItem {
    private final long id;
    private final UUID ownerUuid;
    private final String ownerName;
    private final long sourceListingId;
    private final ClaimReason reason;
    private final long createdAtMillis;
    private final ItemStack item;

    public ClaimItem(long id, UUID ownerUuid, String ownerName, long sourceListingId, ClaimReason reason,
                     long createdAtMillis, ItemStack item) {
        this.id = id;
        this.ownerUuid = ownerUuid;
        this.ownerName = ownerName == null ? "" : ownerName;
        this.sourceListingId = sourceListingId;
        this.reason = reason == null ? ClaimReason.EXPIRED : reason;
        this.createdAtMillis = createdAtMillis;
        this.item = item.clone();
    }

    public long id() {
        return id;
    }

    public UUID ownerUuid() {
        return ownerUuid;
    }

    public String ownerName() {
        return ownerName;
    }

    public long sourceListingId() {
        return sourceListingId;
    }

    public ClaimReason reason() {
        return reason;
    }

    public long createdAtMillis() {
        return createdAtMillis;
    }

    public ItemStack item() {
        return item.clone();
    }
}
