<!-- Uses markdown syntax for neat display at github -->

# ZMQ Video Chat

This Android project serves as a simple showcase application to demonstrate the use of ØMQ and Node.js to stream Video from one Android smartphone to another. The project uses [ØMQ](http://www.zeromq.org/) as the communication layer to send and receive video frames and chat messages in a publish-subscribe pattern. The messages are published to a server running a [Node.js](http://nodejs.org/) module which forwards the messages to all subscribers.

## ØMQ on Android

The ØMQ library was created for Android by checking out and following the steps described on https://github.com/claudemamo/android-jzeromq

## Node.js Server

To run the Video Chat server, first thing to do is install the node. 

	$ cd server/videochat.node
	$ npm install

If you encounter problems downloading zmq for node.js, try this first:

	$ sudo add-apt-repository ppa:chris-lea/libpgm
	$ sudo apt-get update
	$ sudo apt-get install libzmq-dev

Now you can run the node:

	$ node videochat_server.js

By default, the server uses the following ports:

- Chat Incoming	Port 4000
- Chat Outgoing	Port 4001
- Video Incoming Port 4010
- Video Outgoing Port 4011
- Event Incoming Port 4020
- Event Outgoing Port 4021

Alternatively, the incoming ports can be defined as commandline arguments when starting the node, while the outgoing ports are always set to incoming port +1

	$ node videochat_server.js 5000 5002 5004

## Android App

The Android App can be compiled from source or downloaded from the Android market as [ZMQ Video Chat](https://play.google.com/store/apps/details?id=org.dobots.zmqvideochat)

Upon startup, first thing to do is edit the connection settings from the menu. 

### Settings

<img src="https://raw.github.com/eggerdo/ZmqVideoChat/master/doc/settings.png" alt="Settings Dialog" height="363" width="334" />

Choose a nickname for yourself (the nickname should be unique, but as of now no checks are made to ensure that), set the address of the server you are running the videchat.node and enter the ports that you use on the server. By default this would be:

- Chat Port 4000
- Video Port 4010
- Event Port 4020

Once the settings are saved, connection will be established. No connection status is available, as the design of ØMQ is done in such a way that publishers can exist without a subscriber present, thus we have no direct feedback if a connection is established. However, if you see your own nickname in the list of users, connection was successfully established. If not, select Refresh from the menu first and check the list of users again before searching for the problem.

### Main View

<img src="https://raw.github.com/eggerdo/ZmqVideoChat/master/doc/main_overlay.png" alt="Main View" height="640px" width="360px" />

The application consists of 3 main areas. On top is the Video area, on the left side is the chat window, on the right side is the list of currently registered users.

#### Video

On the left is the video from your own camera. By default, the front camera should be used. If your smartphone doesn't have a front camera, the back camera will be used. On the right side, the video of your partner will be displayed. In order to receive a video you first have to select a user from the list (of course you can also choose yourself). Clicking a second time on the user will stop the video again.

By default the video will be unscaled and it's size depends on the smartphone of your partner. Press long on the partner's video and a menu will pop up to give you the option to scale the image or rotate it.

#### Chat

Sent and received chat messages will be displayed in the chat window. 

#### Users

The nicknames of all currently registered users will be displayed. By selecting a nickname from the list, the video of this user will be displayed as partner. Selecting the name again will stop the video feed.

## Conclusion

As mentioned before, this application is meant as a showcase. It is not tested extensively and may have bugs still. However, feel free to use it as it is or to improve on it. If you use it in another project, a notification would be appreciated. 
