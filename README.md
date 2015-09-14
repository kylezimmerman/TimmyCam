# TimmyCam
An Android Wear App for Conestoga College's TimmyCam. It allows you to check out the timmy cam on your wrist.

#Download
You can download an APK in the [releases section](https://github.com/kylezimmerman/TimmyCam/releases)

#Project Structure
There are two modules, one for the phone (which does the actual downloading) and the other is the Wear App.
When built, the result is an APK that includes both apps and the phone will automatically install the wear app on connected wear devices.

##/mobile
This is a standard Android app. [MessageListener.java](https://github.com/kylezimmerman/TimmyCam/blob/master/mobile/src/main/java/com/zimmster/timmycam/MessageListener.java) is where all the logic happens.

##/wear
This is the actual Android Wear app.

#Devices
So far I've only tested this on a Moto 360, but it should work on all Android Wear devices.

#License
Standard MIT License. See [LICENSE.txt](https://github.com/kylezimmerman/TimmyCam/blob/master/LICENSE.txt)
