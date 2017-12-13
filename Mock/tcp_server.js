var server = require('net').createServer();
var port = 8234;
server.on('listening', function() {
    console.log('Server is listening on port', port);
});
server.on('connection', function(socket) {
    console.log('Server has a new connection');

    while(true) {
        var file = require('fs').createReadStream('v1.jpg');
        file.on('data',function(data){
               socket.write(data);
               console.log('Server sent a file!!!!!!');
        });
        socket.on("end",function(){
               file.end("wanbi")
               console.log('file end..............');
            });
    }

});
server.on('close', function() {
    console.log('Server is now closed');
});
server.on('error', function(err) {
    console.log('Error occurred:', err.message);
});
server.listen(port);