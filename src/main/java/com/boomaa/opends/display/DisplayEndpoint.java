package com.boomaa.opends.display;

import com.boomaa.opends.data.holders.Protocol;
import com.boomaa.opends.data.holders.Remote;
import com.boomaa.opends.data.receive.parser.PacketParser;
import com.boomaa.opends.data.receive.parser.ParserNull;
import com.boomaa.opends.data.send.creator.PacketCreator;
import com.boomaa.opends.display.frames.ErrorBox;
import com.boomaa.opends.display.frames.MainFrame;
import com.boomaa.opends.display.updater.ElementUpdater;
import com.boomaa.opends.networking.NetworkReloader;
import com.boomaa.opends.networking.TCPInterface;
import com.boomaa.opends.networking.UDPInterface;
import com.boomaa.opends.networking.UDPTransform;
import com.boomaa.opends.util.Clock;
import com.boomaa.opends.util.InitChecker;

import java.lang.reflect.InvocationTargetException;

public class DisplayEndpoint implements MainJDEC {
    public static UDPInterface RIO_UDP_INTERFACE;
    public static TCPInterface RIO_TCP_INTERFACE;
    public static UDPInterface FMS_UDP_INTERFACE;
    public static TCPInterface FMS_TCP_INTERFACE;
    private static ElementUpdater updater;
    private static PacketCreator creator;
    private static ProtocolClass parserClass = new ProtocolClass("com.boomaa.opends.data.receive.parser.Parser");
    private static ProtocolClass creatorClass = new ProtocolClass("com.boomaa.opends.data.send.creator.Creator");
    private static ProtocolClass updaterClass = new ProtocolClass("com.boomaa.opends.display.updater.Updater");
    public static InitChecker NET_IF_INIT = new InitChecker();

    private static final Clock generalClock = new Clock(1000) {
        @Override
        public void onCycle() {
            parserClass.update();
            creatorClass.update();
            updaterClass.update();
            try {
                updater = (ElementUpdater) Class.forName(updaterClass.toString()).getConstructor().newInstance();
                creator = (PacketCreator) Class.forName(creatorClass.toString()).getConstructor().newInstance();
            } catch (NoSuchMethodException | ClassNotFoundException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
                e.printStackTrace();
                ErrorBox.show(e.getMessage());
                System.exit(1);
            }
        }
    };

    private static final Clock rioTcpClock = new Clock(20) {
        @Override
        public void onCycle() {
            if (updater != null && creator != null) {
                if (NET_IF_INIT.get(Remote.ROBO_RIO, Protocol.TCP)) {
                    byte[] rioTcpGet = RIO_TCP_INTERFACE.doInteract(creator.dsToRioTcp());
                    if (rioTcpGet == null) {
                        updater.updateFromRioTcp(ParserNull.getInstance());
                        NET_IF_INIT.set(false, Remote.ROBO_RIO, Protocol.TCP);
                    } else {
                        updater.updateFromRioTcp(getPacketParser("RioToDsTcp", rioTcpGet));
                    }
                } else {
                    updater.updateFromRioTcp(ParserNull.getInstance());
                    NetworkReloader.reloadRio(Protocol.TCP);
                }
            }
        }
    };

    private static final Clock rioUdpClock = new Clock(20) {
        @Override
        public void onCycle() {
            if (updater != null && creator != null) {
                if (NET_IF_INIT.get(Remote.ROBO_RIO, Protocol.UDP)) {
                    RIO_UDP_INTERFACE.doSend(creator.dsToRioUdp());
                    UDPTransform rioUdpGet = RIO_UDP_INTERFACE.doReceieve();
                    if (rioUdpGet == null || rioUdpGet.isBlank()) {
                        updater.updateFromRioUdp(ParserNull.getInstance());
                        NET_IF_INIT.set(false, Remote.ROBO_RIO, Protocol.UDP);
                    } else {
                        updater.updateFromRioUdp(getPacketParser("RioToDsUdp", rioUdpGet.getBuffer()));
                    }
                } else {
                    updater.updateFromRioUdp(ParserNull.getInstance());
                    NetworkReloader.reloadRio(Protocol.UDP);
                }
            }
        }
    };

    private static final Clock fmsTcpClock = new Clock(20) {
        @Override
        public void onCycle() {
            if (updater != null && creator != null && MainJDEC.FMS_TYPE.getSelectedItem() == FMSType.REAL) {
                if (NET_IF_INIT.get(Remote.FMS, Protocol.TCP)) {
                    byte[] fmsTcpGet = FMS_TCP_INTERFACE.doInteract(creator.dsToFmsTcp());
                    if (fmsTcpGet == null) {
                        updater.updateFromFmsTcp(ParserNull.getInstance());
                        NET_IF_INIT.set(false, Remote.FMS, Protocol.TCP);
                    } else {
                        updater.updateFromFmsTcp(getPacketParser("FmsToDsTcp", fmsTcpGet));
                    }
                } else {
                    updater.updateFromFmsTcp(ParserNull.getInstance());
                    NetworkReloader.reloadFms(FMSType.REAL, Protocol.TCP);
                }
            } else if (MainJDEC.FMS_TYPE.getSelectedItem() == FMSType.NONE && FMS_TCP_INTERFACE != null) {
                updater.updateFromFmsTcp(ParserNull.getInstance());
            }
        }
    };

    private static final Clock fmsUdpClock = new Clock(20) {
        @Override
        public void onCycle() {
            if (updater != null && creator != null && MainJDEC.FMS_TYPE.getSelectedItem() == FMSType.REAL) {
                if (NET_IF_INIT.get(Remote.FMS, Protocol.UDP)) {
                    FMS_UDP_INTERFACE.doSend(creator.dsToFmsUdp());
                    UDPTransform fmsUdpGet = FMS_UDP_INTERFACE.doReceieve();
                    if (fmsUdpGet == null || fmsUdpGet.isBlank()) {
                        updater.updateFromFmsUdp(ParserNull.getInstance());
                        NET_IF_INIT.set(false, Remote.FMS, Protocol.UDP);
                    } else {
                        updater.updateFromFmsUdp(getPacketParser("FmsToDsUdp", fmsUdpGet.getBuffer()));
                    }
                } else {
                    updater.updateFromFmsUdp(ParserNull.getInstance());
                    NetworkReloader.reloadFms(FMSType.REAL, Protocol.UDP);
                }
            } else if (MainJDEC.FMS_TYPE.getSelectedItem() == FMSType.NONE && FMS_UDP_INTERFACE != null) {
                updater.updateFromFmsUdp(ParserNull.getInstance());
            }
        }
    };

    public static void main(String[] args) {
        MainFrame.display();
        generalClock.start();
        fmsTcpClock.start();
        fmsUdpClock.start();
        rioTcpClock.start();
        rioUdpClock.start();
    }

    public static PacketParser getPacketParser(String name, byte[] data) {
        try {
            return (PacketParser) Class.forName(parserClass + "$" + name).getConstructor(byte[].class).newInstance(data);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Integer[] getValidProtocolYears() {
        return new Integer[] {
                2020
        };
    }
}
