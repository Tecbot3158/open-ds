package com.boomaa.opends.usb;

import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Map;

public class USBInterface {
    private static Map<Integer, HIDDevice> controlDevices = new HashMap<>();
    private static int sendDataIterator = 0;
    private static int sendDescIterator = 0;

    public static void init() {
        GLFW.glfwInitHint(GLFW.GLFW_JOYSTICK_HAT_BUTTONS, GLFW.GLFW_FALSE);
        GLFW.glfwInit();
        findControllers();
        updateValues();
    }

    public static synchronized void findControllers() {
        GLFW.glfwPollEvents();
        for (int idx = 0; idx < GLFW.GLFW_JOYSTICK_LAST; idx++) {
            if (GLFW.glfwJoystickPresent(idx)) {
                controlDevices.put(idx, GLFW.glfwJoystickIsGamepad(idx) ? new XboxController(idx) : new Joystick(idx));
            }
        }
    }

    public static synchronized void clearControllers() {
        controlDevices.clear();
    }

    public static synchronized void updateValues() {
        GLFW.glfwPollEvents();
        for (int i = 0; i <= HIDDevice.MAX_JS_INDEX; i++) {
            HIDDevice ctrl = controlDevices.get(i);
            if (ctrl != null) {
                if (ctrl.needsRemove()) {
                    controlDevices.remove(ctrl.getFRCIdx());
                } else {
                    ctrl.update();
                }
            }
        }
    }

    public static synchronized void reindexControllers() {
        Map<Integer, HIDDevice> deviceMapTemp = new HashMap<>(controlDevices);
        clearControllers();
        for (HIDDevice device : deviceMapTemp.values()) {
            int devIdx = device.getFRCIdx();
            controlDevices.put(devIdx, device);
        }
    }

    public synchronized static int iterateSend(boolean isData) {
        int out;
        if (isData) {
            out = sendDataIterator;
            sendDataIterator++;
            sendDataIterator %= (HIDDevice.MAX_JS_INDEX + 1);
        } else {
            out = sendDescIterator;
            sendDescIterator++;
            sendDescIterator %= HIDDevice.MAX_JS_NUM;
        }
        return out;
    }

    public static int getDescIndex() {
        return sendDescIterator;
    }

    public static Map<Integer, HIDDevice> getControlDevices() {
        return controlDevices;
    }
}