package com.boomaa.opends.display;

import com.boomaa.opends.data.holders.Protocol;
import com.boomaa.opends.data.holders.Remote;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class TeamNumListener implements DocumentListener {
    @Override
    public void insertUpdate(DocumentEvent e) {
        update();
    }

    @Override
    public void removeUpdate(DocumentEvent e) {
        update();
    }

    @Override
    public void changedUpdate(DocumentEvent e) {
        update();
    }

    private void update() {
        DisplayEndpoint.NET_IF_INIT.set(false, Remote.ROBO_RIO, Protocol.UDP);
        DisplayEndpoint.NET_IF_INIT.set(false, Remote.ROBO_RIO, Protocol.TCP);
        DisplayEndpoint.NET_IF_INIT.set(false, Remote.FMS, Protocol.UDP);
        DisplayEndpoint.NET_IF_INIT.set(false, Remote.FMS, Protocol.TCP);
    }
}
