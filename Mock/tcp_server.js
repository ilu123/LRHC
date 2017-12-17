var serverCmd = require('net').createServer();
var serverMap = require('net').createServer();
var serverNav = require('net').createServer();
var serverVideo = require('net').createServer();
var serverDot = require('net').createServer();

var PORT_MAPS = 4101;
var PORT_COMMANDS = 4103;
var PORT_NAVIGATION = 4104;
var PORT_READ_VIDEO = 4096;
var PORT_DOT = 4097;

////////////////

serverCmd.on('listening', function() {
    console.log('serverCmd is listening on port');
});
serverCmd.on('connection', function(socket) {
    console.log('serverCmd has a new connection');
    socket.on('data', function(data) {
            console.log('serverCmd DATA ' + socket.remoteAddress + ': ' + data);
    });

});
serverCmd.on('close', function() {
    console.log('serverCmd is now closed');
});
serverCmd.on('error', function(err) {
    console.log('serverCmd Error occurred:', err.message);
});
serverCmd.listen(PORT_COMMANDS);

////////////////

serverMap.on('listening', function() {
    console.log('serverMap is listening on port');
});
serverMap.on('connection', function(socket) {
    console.log('serverMap has a new connection');
    socket.on('data', function(data) {
            console.log('serverMap DATA ' + socket.remoteAddress + ': ' + data);
    });

});
serverMap.on('close', function() {
    console.log('serverMap is now closed');
});
serverMap.on('error', function(err) {
    console.log('serverMap Error occurred:', err.message);
});
serverMap.listen(PORT_MAPS);

////////////////

serverDot.on('listening', function() {
    console.log('serverDot is listening on port');
});
serverDot.on('connection', function(socket) {
    console.log('serverDot has a new connection');
    socket.on('data', function(data) {
            console.log('serverDot DATA ' + socket.remoteAddress + ': ' + data);
    });
    var data = require('fs').readFileSync('v1.jpg');
    socket.write(data);
    console.log('serverDot sent a Dot!!!!!!');


});
serverDot.on('close', function() {
    console.log('serverDot is now closed');
});
serverDot.on('error', function(err) {
    console.log('serverDot Error occurred:', err.message);
});
serverDot.listen(PORT_DOT);

////////////////

serverNav.on('listening', function() {
    console.log('serverNav is listening on port');
});
serverNav.on('connection', function(socket) {
    console.log('serverNav has a new connection');
    socket.on('data', function(data) {
            console.log('serverNav DATA ' + socket.remoteAddress + ': ' + data);
    });

});
serverNav.on('close', function() {
    console.log('serverNav is now closed');
});
serverNav.on('error', function(err) {
    console.log('serverNav Error occurred:', err.message);
});
serverNav.listen(PORT_NAVIGATION);

////////////////

serverVideo.on('listening', function() {
    console.log('serverVideo is listening on port');
});
serverVideo.on('connection', function(socket) {
    console.log('serverVideo has a new connection');
    socket.on('data', function(data) {
            console.log('serverVideo DATA ' + socket.remoteAddress + ': ' + data);
    });
    var data = require('fs').readFileSync('v1.jpg');
    socket.write(data);
    console.log('serverVideo sent a Video!!!!!!');

});
serverVideo.on('close', function() {
    console.log('serverVideo is now closed');
});
serverVideo.on('error', function(err) {
    console.log('serverVideo Error occurred:', err.message);
});
serverVideo.listen(PORT_READ_VIDEO);