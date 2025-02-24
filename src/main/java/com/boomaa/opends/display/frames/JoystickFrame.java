package com.boomaa.opends.display.frames;

import com.boomaa.opends.display.MainJDEC;
import com.boomaa.opends.display.PopupBase;
import com.boomaa.opends.display.elements.GBCPanelBuilder;
import com.boomaa.opends.display.elements.HideableLabel;
import com.boomaa.opends.display.elements.StickyButton;
import com.boomaa.opends.usb.Component;
import com.boomaa.opends.usb.HIDDevice;
import com.boomaa.opends.usb.IndexTracker;
import com.boomaa.opends.usb.Joystick;
import com.boomaa.opends.usb.ControlDevices;
import com.boomaa.opends.usb.XboxController;
import com.boomaa.opends.util.Clock;
import com.boomaa.opends.util.NumberUtils;

import javax.swing.*;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;

public class JoystickFrame extends PopupBase {
    private static final int BTN_PER_ROW = 16;
    private ValueUpdater valueUpdater;

    public JoystickFrame() {
        super("Joysticks", new Dimension(520, 275));
    }

    @Override
    public void config() {
        super.config();
        super.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        EmbeddedJDEC.DISABLE_BTN.setVerticalTextPosition(SwingConstants.TOP);
        EmbeddedJDEC.DISABLE_BTN.setHorizontalTextPosition(SwingConstants.CENTER);

        EmbeddedJDEC.LIST.setVisibleRowCount(-1);
        EmbeddedJDEC.LIST.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        EmbeddedJDEC.LIST.setLayoutOrientation(JList.VERTICAL);
        EmbeddedJDEC.LIST_SCR.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        EmbeddedJDEC.LIST_SCR.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        EmbeddedJDEC.LIST_SCR.setPreferredSize(new Dimension(200, 100));
        ctrlToDisplay();

        EmbeddedJDEC.UP_BTN.setEnabled(false);
        EmbeddedJDEC.DOWN_BTN.setEnabled(false);
        EmbeddedJDEC.DISABLE_BTN.setEnabled(false);
        EmbeddedJDEC.INDEX_SET.setEnabled(false);
        EmbeddedJDEC.INDEX_SET.setColumns(4);

        EmbeddedJDEC.LIST.getSelectionModel().addListSelectionListener((e) -> {
            HIDDevice device = EmbeddedJDEC.LIST.getSelectedValue();
            if (device != null) {
                EmbeddedJDEC.INDEX_SET.setEnabled(true);
                EmbeddedJDEC.INDEX_SET.setText(String.valueOf(device.getIdx()));

                EmbeddedJDEC.DISABLE_BTN.setEnabled(true);
                EmbeddedJDEC.DISABLE_BTN.setSelected(device.isDisabled());

                EmbeddedJDEC.BUTTONS.clear();
                EmbeddedJDEC.BUTTON_GRID.removeAll();
                GBCPanelBuilder gbcButton = new GBCPanelBuilder(EmbeddedJDEC.BUTTON_GRID);
                boolean[] buttons = device.getButtons();
                int row = 0;
                for (int i = 0; i < buttons.length; i++) {
                    JCheckBox cb = new JCheckBox();
                    cb.setEnabled(false);
                    cb.setSelected(buttons[i]);
                    EmbeddedJDEC.BUTTONS.add(i, cb);
                    if (i != 0 && i % BTN_PER_ROW == 0) {
                        row += 2;
                    }
                    gbcButton.clone().setX(i % BTN_PER_ROW).setY(row).build(new JLabel(String.valueOf(i + 1)));
                    gbcButton.clone().setX(i % BTN_PER_ROW).setY(row + 1).build(cb);
                }
            } else {
                EmbeddedJDEC.INDEX_SET.setEnabled(false);
                EmbeddedJDEC.INDEX_SET.setText("");
            }
        });
        EmbeddedJDEC.UP_BTN.addActionListener(e -> {
            int idx = EmbeddedJDEC.LIST.getSelectedIndex();
            swapDeviceIndices(idx, idx - 1);
        });
        EmbeddedJDEC.DOWN_BTN.addActionListener(e -> {
            int idx = EmbeddedJDEC.LIST.getSelectedIndex();
            swapDeviceIndices(idx, idx + 1);
        });
        EmbeddedJDEC.AUTO_ORDER_BTN.addActionListener(e -> {
            if (!PopupBase.isAlive(AutoOrderFrame.class)) {
                new AutoOrderFrame();
            } else {
                PopupBase.getAlive(AutoOrderFrame.class).reopen();
            }
        });
        EmbeddedJDEC.DISABLE_BTN.addActionListener(e -> EmbeddedJDEC.LIST.getSelectedValue()
                .setDisabled(EmbeddedJDEC.DISABLE_BTN.isSelected()));
        EmbeddedJDEC.RELOAD_BTN.addActionListener(e -> resetControllerDisplay());
        EmbeddedJDEC.CLOSE_BTN.addActionListener(e -> {
            for (HIDDevice device : ControlDevices.getAll().values()) {
                if (!device.isDisabled() && device.getIdx() >= IndexTracker.MAX_JS_NUM) {
                    MessageBox.show("Index \"" + device.getIdx()
                            + "\" for controller \"" + device.getName()
                            + "\"\n greater than maximum enabled joystick index of \""
                            + (IndexTracker.MAX_JS_NUM - 1) + "\"", MessageBox.Type.ERROR);
                    return;
                }
            }
            MainJDEC.IS_ENABLED.setEnabled(true);
            this.dispose();
        });

        content.setLayout(new GridBagLayout());
        GBCPanelBuilder base = new GBCPanelBuilder(content).setFill(GridBagConstraints.BOTH).setAnchor(GridBagConstraints.CENTER).setInsets(new Insets(5, 5, 5, 5));
        GBCPanelBuilder end = base.clone().setFill(GridBagConstraints.NONE).setAnchor(GridBagConstraints.LINE_END);

        base.clone().setPos(0, 0, 1, 1).setFill(GridBagConstraints.NONE).build(new JLabel("Index"));
        base.clone().setPos(0, 1, 1, 1).setFill(GridBagConstraints.NONE).build(EmbeddedJDEC.INDEX_SET);
        base.clone().setPos(0, 2, 1, 1).build(EmbeddedJDEC.RELOAD_BTN);

        base.clone().setPos(1, 0, 2, 3).build(EmbeddedJDEC.LIST_SCR);

        end.clone().setPos(4, 0, 1, 1).build(new JLabel("X: "));
        end.clone().setPos(4, 1, 1, 1).build(new JLabel("Y: "));
        end.clone().setPos(4, 2, 1, 1).build(new JLabel("Z: "));
        end.clone().setPos(6, 0, 1, 1).build(new JLabel("RX: "));
        end.clone().setPos(6, 1, 1, 1).build(new JLabel("RY: "));

        base.clone().setPos(5, 0, 1, 1).setAnchor(GridBagConstraints.LINE_START).build(EmbeddedJDEC.VAL_X);
        base.clone().setPos(5, 1, 1, 1).setAnchor(GridBagConstraints.LINE_START).build(EmbeddedJDEC.VAL_Y);
        base.clone().setPos(5, 2, 1, 1).setAnchor(GridBagConstraints.LINE_START).build(EmbeddedJDEC.VAL_Z);
        base.clone().setPos(7, 0, 1, 1).setAnchor(GridBagConstraints.LINE_START).build(EmbeddedJDEC.VAL_RX);
        base.clone().setPos(7, 1, 1, 1).setAnchor(GridBagConstraints.LINE_START).build(EmbeddedJDEC.VAL_RY);

        base.clone().setPos(3, 0, 1, 1).setAnchor(GridBagConstraints.LINE_START).build(EmbeddedJDEC.UP_BTN);
        base.clone().setPos(3, 1, 1, 1).setAnchor(GridBagConstraints.LINE_START).build(EmbeddedJDEC.DOWN_BTN);
        base.clone().setPos(3, 2, 1, 1).setAnchor(GridBagConstraints.LINE_START).build(EmbeddedJDEC.DISABLE_BTN);

        base.clone().setPos(0, 3, 7, 1).setAnchor(GridBagConstraints.LINE_START).build(EmbeddedJDEC.BUTTON_GRID);

        base.clone().setPos(0, 4, 1, 1).build(EmbeddedJDEC.AUTO_ORDER_BTN);
        base.clone().setPos(1, 4, 7, 1).build(EmbeddedJDEC.CLOSE_BTN);

        EmbeddedJDEC.BUTTON_GRID.setLayout(new GridBagLayout());

        if (valueUpdater == null) {
            valueUpdater = new ValueUpdater();
        }
        valueUpdater.start();
    }

    private static synchronized void swapDeviceIndices(int aIndex, int bIndex) {
        HIDDevice aDevice = EmbeddedJDEC.LIST_MODEL.getElementAt(aIndex);
        try {
            HIDDevice bDevice = EmbeddedJDEC.LIST_MODEL.getElementAt(bIndex);
            aDevice.setIdx(bIndex);
            bDevice.setIdx(aIndex);
            EmbeddedJDEC.LIST_MODEL.set(aIndex, bDevice);
            EmbeddedJDEC.LIST_MODEL.set(bIndex, aDevice);
            EmbeddedJDEC.LIST.setSelectedIndex(bIndex);
        } catch (ArrayIndexOutOfBoundsException ignored) {
            int listSize = EmbeddedJDEC.LIST_MODEL.size();
            aDevice.setIdx(bIndex);
            if (bIndex >= listSize) {
                EmbeddedJDEC.LIST_MODEL.removeElement(aDevice);
                EmbeddedJDEC.LIST_MODEL.addElement(aDevice);
                EmbeddedJDEC.LIST.setSelectedIndex(listSize - 1);
            }
        }
    }

    private void resetControllerDisplay() {
        EmbeddedJDEC.LIST_MODEL.clear();
        ControlDevices.clearAll();
        ControlDevices.findAll();
    }

    private void ctrlToDisplay() {
        for (HIDDevice hid : ControlDevices.getAll().values()) {
            EmbeddedJDEC.LIST_MODEL.add(EmbeddedJDEC.LIST_MODEL.size(), hid);
        }
    }

    @Override
    public void reopen() {
        valueUpdater = new ValueUpdater();
        valueUpdater.start();
        super.reopen();
    }

    @Override
    public void dispose() {
        if (valueUpdater != null) {
            valueUpdater.end();
        }
        super.dispose();
    }

    public interface EmbeddedJDEC {
        DefaultListModel<HIDDevice> LIST_MODEL = new DefaultListModel<>();
        JList<HIDDevice> LIST = new JList<>(LIST_MODEL);
        JScrollPane LIST_SCR = new JScrollPane(LIST);

        HideableLabel VAL_X = new HideableLabel(true);
        HideableLabel VAL_Y = new HideableLabel(true);
        HideableLabel VAL_Z = new HideableLabel(true);
        HideableLabel VAL_RX = new HideableLabel(true);
        HideableLabel VAL_RY = new HideableLabel(true);

        JTextField INDEX_SET = new JTextField("");
        JButton RELOAD_BTN = new JButton("↻");
        JButton AUTO_ORDER_BTN = new JButton("Auto");
        StickyButton CLOSE_BTN = new StickyButton("Close", 5);

        JButton UP_BTN = new JButton("↑");
        JButton DOWN_BTN = new JButton("↓");

        JPanel BUTTON_GRID = new JPanel();
        List<JCheckBox> BUTTONS = new ArrayList<>();

        JCheckBox DISABLE_BTN = new JCheckBox("Disable");
    }

    public static class ValueUpdater extends Clock {
        public ValueUpdater() {
            super(100);
        }

        @Override
        public void onCycle() {
            if (!PopupBase.getAlive(JoystickFrame.class).isVisible() || PopupBase.isVisible(AutoOrderFrame.class)) {
                return;
            }
            HIDDevice current = EmbeddedJDEC.LIST.getSelectedValue();
            int cListIdx = EmbeddedJDEC.LIST.getSelectedIndex();
            if (current != null) {
                EmbeddedJDEC.UP_BTN.setEnabled(cListIdx != 0);
                EmbeddedJDEC.DOWN_BTN.setEnabled(cListIdx != EmbeddedJDEC.LIST_MODEL.size() - 1);
                ControlDevices.updateValues();
                try {
                    int nFRCIdx = Integer.parseInt(EmbeddedJDEC.INDEX_SET.getText());
                    if (current.getIdx() != nFRCIdx) {
                        for (HIDDevice dev : ControlDevices.getAll().values()) {
                            if (dev.getIdx() == nFRCIdx) {
                                MessageBox.show("Duplicate index \"" + nFRCIdx + "\" for controller \"" + dev
                                        + "\"\nSetting controller \"" + dev + "\" on index \"" + dev.getIdx()
                                        + "\"\n to new index \"" + cListIdx + "\" and making requested index change",
                                    MessageBox.Type.WARNING);
                            }
                        }
                        swapDeviceIndices(cListIdx, nFRCIdx);
                        ControlDevices.reindexAll();
                    }
                } catch (NumberFormatException ignored) {
                }
                if (current instanceof Joystick) {
                    Joystick js = (Joystick) current;
                    EmbeddedJDEC.VAL_X.setText(NumberUtils.roundTo(js.getComponentValue(Component.Axis.X), 2));
                    EmbeddedJDEC.VAL_Y.setText(NumberUtils.roundTo(js.getComponentValue(Component.Axis.Y), 2));
                    EmbeddedJDEC.VAL_Z.setText(NumberUtils.roundTo(js.getComponentValue(Component.Axis.RZ), 2));
                    EmbeddedJDEC.VAL_RX.setText(" N/A");
                    EmbeddedJDEC.VAL_RY.setText(" N/A");
                } else if (current instanceof XboxController) {
                    XboxController xbox = (XboxController) current;
                    EmbeddedJDEC.VAL_X.setText(NumberUtils.roundTo(xbox.getComponentValue(Component.Axis.X), 2));
                    EmbeddedJDEC.VAL_Y.setText(NumberUtils.roundTo(xbox.getComponentValue(Component.Axis.Y), 2));
                    EmbeddedJDEC.VAL_RX.setText(NumberUtils.roundTo(xbox.getComponentValue(Component.Axis.RX), 2));
                    EmbeddedJDEC.VAL_RY.setText(NumberUtils.roundTo(xbox.getComponentValue(Component.Axis.RY), 2));
                    EmbeddedJDEC.VAL_Z.setText(" N/A");
                }
                boolean[] buttons = current.getButtons();
                for (int i = 0; i < buttons.length && i < EmbeddedJDEC.BUTTONS.size(); i++) {
                    EmbeddedJDEC.BUTTONS.get(i).setSelected(buttons[i]);
                }
            } else {
                EmbeddedJDEC.VAL_X.setText(" N/A");
                EmbeddedJDEC.VAL_Y.setText(" N/A");
                EmbeddedJDEC.VAL_Z.setText(" N/A");
                EmbeddedJDEC.VAL_RX.setText(" N/A");
                EmbeddedJDEC.VAL_RY.setText(" N/A");
            }
            EmbeddedJDEC.BUTTON_GRID.revalidate();
        }
    }
}
