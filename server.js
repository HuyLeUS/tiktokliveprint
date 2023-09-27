const Websocket = require("ws");
const {WebcastPushConnection} = require('tiktok-live-connector');
const server = new Websocket.Server({port:8000});

let tiktokUserName = "tongkhotuixach.qc";
let tiktokLiveConnection = new WebcastPushConnection(tiktokUserName);


server.on("connection", (ws) => {
    tiktokLiveConnection.connect().then(state => {
        console.info(`Connected to roomId ${state.roomId}`);
    }).catch(err => {
        console.error('Failed to connect',err);
    })

    tiktokLiveConnection.on('chat',data => {
        console.log(`${data.uniqueId} (userId:${data.userId}) sends ${data.comment}`);
        ws.send(data.comment)
    })
    ws.on("message",(data)=>{
        console.log(data)
    })
})


