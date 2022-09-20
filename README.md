# FlexiGroBots Surveilance Tracker #
This Android App is for enabling DJI Mavic Air 2 to track GPS positions that are received over MQTT broker. Application is a part of EU 
research project pilot where drone is used as an aid for situational awareness of robot tracktors. Main goal of the application is to stay 
over the robot tracktor on set altitude andprovide video feed for AI analytics that aim to detect objects and their movement around the tracker. 
Analytics part is separate software topic. Obejct location and movement vectors are then used for detecting possible collision events and provide 
warning for robot operator as well as for robot control system. Software can be also used to track drone controller in case MQTT broker is not used.
## Requirements ##
Hardware requirements:
- Android phone (version 9.0 or higher) iwth internet connection
- DJI Mavic Air 2 (should work with other drones supported by DJI Mobile SDK 4.16.2) 

Software and external service requirements:
- Android studio
- DJI developer credentials for generating API key for DJI SDK
- Google API key for Maps integration
- MQTT broker (optional) 
- RTMP video streaming service

## Functionalities ##
UI has four functions: Take off, Land, Start mission and Stop mission. Take Off and Land are self explanatory. 
## Disclaimer ##
BE WARNED!! This software is very much work in progress and basically a hack to get around the limitations of Air 2's lack of support for autonomous 
missions. Using this software may cause unintended behaviour of the drone and even crash in the worst case. Creators or asssociated organisations 
take ZERO responsibility of any damage or injuries that may be caused directly or indirectly by this software.
