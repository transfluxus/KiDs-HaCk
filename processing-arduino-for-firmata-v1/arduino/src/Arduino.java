/**
 * Arduino.java - Arduino/firmata library for Processing
 * Copyright (C) 2006-07 David A. Mellis 
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General
 * Public License along with this library; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA  02111-1307  USA
 *
 * Processing code to communicate with the firmata Arduino
 * firmware used by Pduino.
 *
 * $Id$
 *
 * For more information, see:
 * http://pure-data.cvs.sourceforge.net/pure-data/externals/hardware/arduino/
 */
 
package cc.arduino;

import processing.core.*;
import processing.serial.*;

/**
 * Together with the firmata firmware (an Arduino sketch uploaded to the
 * Arduino board), this class allows you to control the Arduino board from
 * Processing: reading from and writing to the digital pins and reading the
 * analog inputs.
 */
public class Arduino {
  /**
   * Constant to set a pin to input mode (in a call to pinMode()).
   */
  public static final int INPUT = 0;
  /**
   * Constant to set a pin to output mode (in a call to pinMode()).
   */
  public static final int OUTPUT = 1;

  /**
   * Constant to write a high value (+5 volts) to a pin (in a call to
   * digitalWrite()).
   */
  public static final int LOW = 0;
  /**
   * Constant to write a low value (0 volts) to a pin (in a call to
   * digitalWrite()).
   */
  public static final int HIGH = 1;
  
  private final int DIGITAL_MESSAGE        = 0x90; // send data for a digital pin
  private final int ANALOG_MESSAGE         = 0xE0; // send data for an analog pin (or PWM)
  //private final int PULSE_MESSAGE          = 0xA0; // proposed pulseIn/Out message (SysEx)
  //private final int SHIFTOUT_MESSAGE       = 0xB0; // proposed shiftOut message (SysEx)
  private final int REPORT_ANALOG_PIN      = 0xC0; // enable analog input by pin #
  private final int REPORT_DIGITAL_PORTS   = 0xD0; // enable digital input by port pair
  private final int START_SYSEX            = 0xF0; // start a MIDI SysEx message
  private final int SET_DIGITAL_PIN_MODE   = 0xF4; // set a digital pin to INPUT or OUTPUT 
  private final int END_SYSEX              = 0xF7; // end a MIDI SysEx message
  private final int REPORT_VERSION         = 0xF9; // report firmware version
  private final int SYSTEM_RESET           = 0xFF; // reset from MIDI

  PApplet parent;
  Serial serial;
  SerialProxy serialProxy;
  
  int waitForData = 0;
  int executeMultiByteCommand = 0;
  int multiByteChannel = 0;
  int[] storedInputData = new int[2];

  int[] digitalOutputData = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
  int[] digitalInputData  = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
  int[] analogInputData = { 0, 0, 0, 0, 0, 0, 0, 0, 0 };

  int majorVersion = 0;
  int minorVersion = 0;
  
  // We need a class descended from PApplet so that we can override the
  // serialEvent() method to capture serial data.  We can't use the Arduino
  // class itself, because PApplet defines a list() method that couldn't be
  // overridden by the static list() method we use to return the available
  // serial ports.  This class needs to be public so that the Serial class
  // can access its serialEvent() method.
  public class SerialProxy extends PApplet {
    public SerialProxy() {
      // Create the container for the registered dispose() methods, so that
      // our Serial instance can register its dispose() method (which it does
      // automatically).
      disposeMethods = new RegisteredMethods();
    }

    public void serialEvent(Serial which) {
      // Notify the Arduino class that there's serial data for it to process.
      checkForInput();
    }
  }
  
  public void dispose() {
    this.serial.dispose();
  }
  
  /**
   * Get a list of the available Arduino boards; currently all serial devices
   * (i.e. the same as Serial.list()).  In theory, this should figure out
   * what's an Arduino board and what's not.
   */
  public static String[] list() {
    return Serial.list();
  }

  /**
   * Create a proxy to an Arduino board running the firmata firmware (used with
   * PDuino).  
   *
   * @param parent the Processing sketch creating this Arduino board
   * (i.e. "this").
   * @param iname the name of the serial device associated with the Arduino
   * board (e.g. one the elements of the array returned by Arduino.list())
   * @param irate the baud rate to use to communicate with the Arduino board
   * (this depends on the firmata version used: 0.1 is at 19200, 0.2 at 115200,
   * 1.0 at 57600)
   */
  public Arduino(PApplet parent, String iname, int irate) {
    this.parent = parent;
    this.serialProxy = new SerialProxy();
    this.serial = new Serial(serialProxy, iname, irate);
    
    try{
      Thread.sleep(2000);
    } catch(InterruptedException ie) {}
    
    for (int i = 0; i < 6; i++) {
      serial.write(REPORT_ANALOG_PIN | i);
      serial.write(1);
    }
    
    parent.registerDispose(this);
  }
  
  /**
   * Returns the last known value read from the digital pin: HIGH or LOW.
   *
   * @param pin the digital pin whose value should be returned (from 2 to 13,
   * since pins 0 and 1 are used for serial communication)
   */
  public int digitalRead(int pin) {
    return digitalInputData[pin];
  }

  /**
   * Returns the last known value read from the analog pin: 0 (0 volts) to
   * 1023 (5 volts).
   *
   * @param pin the analog pin whose value should be returned (from 0 to 5)
   */
  public int analogRead(int pin) {
    return analogInputData[pin];
  }

  /**
   * Set a digital pin to input or output mode.
   *
   * @param pin the pin whose mode to set (from 2 to 13)
   * @param mode either Arduino.INPUT or Arduino.OUTPUT
   */
  public void pinMode(int pin, int mode) {
    serial.write(SET_DIGITAL_PIN_MODE);
    serial.write(pin);
    serial.write(mode);
  }

  /**
   * Write to a digital pin (the pin must have been put into output mode with
   * pinMode()).
   *
   * @param pin the pin to write to (from 2 to 13)
   * @param value the value to write: Arduino.LOW (0 volts) or Arduino.HIGH
   * (5 volts)
   */
  public void digitalWrite(int pin, int value) {
    int transmitByte;
    
    digitalOutputData[pin] = value;
    serial.write(DIGITAL_MESSAGE);
    
    transmitByte = 0;
    for (int i = 0; i <= 6; i++)
      if (digitalOutputData[i] != 0)
        transmitByte |= 1 << i;
    serial.write(transmitByte);
    
    transmitByte = 0;
    for (int i = 7; i <= 13; i++)
      if (digitalOutputData[i] != 0)
        transmitByte |= (1 << (i - 7));
    serial.write(transmitByte);
  }
  
  /**
   * Write an analog value (PWM-wave) to a digital pin.
   *
   * @param pin the pin to write to (must be 9, 10, or 11, as those are they
   * only ones which support hardware pwm)
   * @param the value: 0 being the lowest (always off), and 255 the highest
   * (always on)
   */
  public void analogWrite(int pin, int value) {
    serial.write(ANALOG_MESSAGE | (pin & 0x0F));
    serial.write(value & 0x7F);
    serial.write(value >> 7);
  }

  private void setDigitalInputs(int inputData0, int inputData1) {
    for (int i = 0; i < 7; i++) {
      //System.out.println("digital pin " + i +       " is " + ((inputData0 >> i) & 1));
      //System.out.println("digital pin " + (i + 7) + " is " + ((inputData1 >> i) & 1));
      digitalInputData[i]   = (inputData0 >> i) & 1;
      digitalInputData[i+7] = (inputData1 >> i) & 1;
    }
  }

  private void setAnalogInput(int pin, int inputData0, int inputData1) {
    //System.out.println("analog pin " + pin + " is " + (inputData1 * 128 + inputData0));
    analogInputData[pin] = (inputData1 * 128 + inputData0);
  }

  private void setVersion(int inputData0, int inputData1) {
    //System.out.println("version is " + inputData1 + "." + inputData0);
    majorVersion = inputData1;
    minorVersion = inputData0;
  }

  private void checkForInput() {
    while (serial.available() > 0)
      processInput(serial.read());
  }

  private void processInput(int inputData) {
    int command;
    if (waitForData > 0 && inputData < 128) {
      waitForData--;
      storedInputData[waitForData] = inputData;
      
      if((executeMultiByteCommand!=0) && (waitForData==0)) {
        //we got everything
        switch(executeMultiByteCommand) {
        case DIGITAL_MESSAGE:
          setDigitalInputs(storedInputData[1], storedInputData[0]);
          break;
        case ANALOG_MESSAGE:
          setAnalogInput(multiByteChannel, storedInputData[1], storedInputData[0]);
          break;
        case REPORT_VERSION:
          setVersion(storedInputData[1], storedInputData[0]);
          break;
        }
      }
    } else {
      if(inputData < 0xF0) {
        command = inputData & 0xF0;
        multiByteChannel = inputData & 0x0F;
      } else {
        command = inputData;
        // commands in the 0xF* range don't use channel data
      }
      switch (command) {
      case DIGITAL_MESSAGE:
      case ANALOG_MESSAGE:
      case REPORT_VERSION:
        waitForData = 2;
        executeMultiByteCommand = command;
        break;      
      }
    }
  }
}
