var zmq = require('zmq')

var chatSubscriber = zmq.socket('sub')
, chatPublisher = zmq.socket('pub')

var videoSubscriber = zmq.socket('sub')
, videoPublisher = zmq.socket('pub')

var evenSubscriber = zmq.socket('sub')
, eventPublisher = zmq.socket('pub')

var clientCount = 0
, clients = new Array()

// default port definition
// note: this defines the receiving port, the 
//       sending port is always equal to
//       receiving port + 1
var chatPort 	= 4000
, videoPort		= 4010
, eventPort 	= 4020

// check if ports were provided as arguments
process.argv.forEach(function (val, index, array) {
	switch(index) {
	case 2: 
		chatPort = val
		break;
	case 3:
		videoPort = val
		break;
	case 4:
		eventPort = val
	}
})

// implement format string
if (!String.prototype.format) {
  String.prototype.format = function() {
    var args = arguments;
    return this.replace(/{(\d+)}/g, function(match, number) { 
      return typeof args[number] != 'undefined'
        ? args[number]
        : match
      ;
    });
  };
}

// Chat forwarder -------------------------------------------
// a chat message has two fields, channel and data
chatSubscriber.on('message', function(channel, data) {
	console.log(channel.toString(), data.toString());
	// forward (publish) the received chat message to the subscribed clients
	chatPublisher.send([channel, data])
})

// subscribe to everything
chatSubscriber.subscribe('')
chatSubscriber.bind("tcp://*:{0}".format(Number(chatPort)), function(err) {
	if (err)
		console.log(err)
	else
		console.log("Msg Listening on {0}".format(Number(chatPort)))
})

chatPublisher.bind('tcp://*:{0}'.format(Number(chatPort)+1), function(err) {
	if (err) 
		console.log(err)
	else
		console.log('Msg Sending on {0}'.format(Number(chatPort)+1))
})

// Video Forwarder -------------------------------------------------
// a video message has 4 fields, target, width, height and data
videoSubscriber.on('message', function(target, width, height, data) {
	// forward (publish) the received video frame to the subscribed clients
	videoPublisher.send([target, width, height, data])
})

// subscribe to everything
videoSubscriber.subscribe('')
videoSubscriber.bind('tcp://*:{0}'.format(Number(videoPort)), function(err) {
	if (err)
		console.log(err)
	else
		console.log('Video Listening on {0}'.format(Number(videoPort)))
})

videoPublisher.bind('tcp://*:{0}'.format(Number(videoPort)+1), function(err) {
	if (err)
		console.log(err)
	else
		console.log('Video Sending on {0}'.format(Number(videoPort)+1))
})

// Client Register --------------------------------------------------
// an event message has two fields, event and data
evenSubscriber.on('message', function(event, data) {
	sendList = function() {
		// send the list as a comma separated string
		eventPublisher.send(['list', clients.join(';')])
	}

	// check event type
	if (event.toString() == 'register') {
		console.log(data.toString(), 'connected')

		// for the register event, data is the name of the client
		var index = clients.indexOf(data.toString())
		if (index == -1) {
			// if client was not registered already, at to list of clients
			clients.push(data.toString())
		}
		sendList()
	} else if (event.toString() == 'unregister') {
		console.log(data.toString(), 'disconnected')

		// for the unregister event, data is the name of the client
		var index = clients.indexOf(data.toString())
		if (index != -1) {
			// if client was registered, remove from list of clients
			clients.splice(index, 1)
		}
		sendList()
	} else if (event.toString() == 'list') {
		// for the list event we just send back the list of clients
		sendList()
	} else
		console.log('unknown event')
})

// subscribe to everything
evenSubscriber.subscribe('')
evenSubscriber.bind('tcp://*:{0}'.format(Number(eventPort)), function(err) {
	if (err)
		console.log(err)
	else
		console.log('Event Listening on {0}'.format(Number(eventPort)))
})

eventPublisher.bind('tcp://*:{0}'.format(Number(eventPort)+1), function(err) {
	if (err)
		console.log(err)
	else
		console.log('Event Sending on {0}'.format(Number(eventPort)+1))
})

// Shutdown ------------------------------------------------------------------
process.on('SIGINT', function() {
	console.log('\nclosing')
	chatSubscriber.close()
	chatPublisher.close()
	videoSubscriber.close()
	videoPublisher.close()
	eventPublisher.close()
	evenSubscriber.close()
})