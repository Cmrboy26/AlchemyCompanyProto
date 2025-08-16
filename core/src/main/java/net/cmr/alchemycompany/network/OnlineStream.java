package net.cmr.alchemycompany.network;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import net.cmr.alchemycompany.network.packet.Packet;

public class OnlineStream extends Stream {

    public OnlineStream(InputStream input, OutputStream output, boolean isClient) {
        super(isClient);
    }

    @Override
    protected void connect() throws IOException {
        if (isClient()) {

        } else {

        }
    }

    @Override
    protected void waiting() throws IOException {
        if (isClient()) {

        } else {

        }
    }

    @Override
    protected void join() throws IOException {
        if (isClient()) {

        } else {

        }
    }

    @Override
    protected void recievePackets() {
        if (isClient()) {

        } else {

        }
    }

    @Override
    protected void disconnect() throws IOException {
        if (isClient()) {

        } else {

        }
    }

    @Override
    public void sendPacket(Packet packet) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'sendPacket'");
    }
}
