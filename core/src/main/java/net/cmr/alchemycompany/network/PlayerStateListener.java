package net.cmr.alchemycompany.network;

import java.util.UUID;

public interface PlayerStateListener {

    public void onPlayerConnected(UUID playerUUID, Stream stream);
    public void onPlayerDisconnected(UUID playerUUID, Stream stream);

    public default boolean isFinished() {
        return false;
    }

}
