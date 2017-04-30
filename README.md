# RainbowHAT Countdown Demo
A demo application of the buzzer and capacitive buttons on the RainbowHAT attached to a Raspberry PI Model 3 B running androidthings preview 3.

This is a non-production code example. The application is configured to handle touch input from the 3 buttons (A, B and C) and ouputs an alternate note each second through the buzzer.

In the example: button A starts and stops the countdown, button B adds a second to the countdown and button C sets the countdown time to the default (24 seconds).

## Hardware
For this example I used the following:
 - [Raspberry Pi Model 3 B](https://www.raspberrypi.org/products/raspberry-pi-3-model-b/)

![hardware](https://github.com/juliusspencer/ATCountdown2/blob/master/doc_resources/atcountdown.jpg)

## Configuration
The hardware is accessed through the RainbowHAT driver:

	compile 'com.google.android.things.contrib:driver-rainbowhat:0.2'

and the corresponding code to access a capacitive AndroidThings [Button](com.google.android.things.contrib.driver.button.Button):

	mButtonA = RainbowHat.openButton(RainbowHat.BUTTON_A);
