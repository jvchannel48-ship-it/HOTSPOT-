const express=require("express");
const http=require("http");
const cors=require("cors");
const {Server}=require("socket.io");
const crypto=require("crypto");
const app=express();
app.use(cors());
app.use(express.json());
const server=http.createServer(app);
const io=new Server(server,{cors:{origin:"*",methods:["GET","POST"]}});
const rooms=new Map();
const clean=s=>String(s||"").replace(/[^a-zA-Z0-9_-]/g,"").slice(0,32);
app.get("/",(_,res)=>res.json({name:"VIDEO KARAOKE Remote API",status:"ok",health:"/healthz"}));
app.get("/healthz",(_,res)=>res.json({ok:true,service:"video-karaoke-remote"}));
app.post("/api/rooms",(_,res)=>{
 let code; do{code=crypto.randomBytes(3).toString("hex").toUpperCase()}while(rooms.has(code));
 const room={code,createdAt:Date.now(),player:null,remoteCount:0};rooms.set(code,room);
 res.json({ok:true,code});
});
app.get("/api/rooms/:code", (req,res)=>{
 const room=rooms.get(clean(req.params.code).toUpperCase());
 if(!room)return res.status(404).json({ok:false,error:"Room not found"});
 res.json({ok:true,code:room.code,playerConnected:!!room.player,remoteCount:room.remoteCount});
});
io.on("connection",socket=>{
 let joined=null,role=null;
 socket.on("join",({code,as}={})=>{
  const c=clean(code).toUpperCase(),room=rooms.get(c);
  if(!room||!["player","remote"].includes(as)){socket.emit("join-error",{error:"Invalid room or role"});return;}
  joined=c;role=as;socket.join(c);
  if(as==="player"){room.player=socket.id;socket.to(c).emit("player-status",{connected:true});}
  else{room.remoteCount++;socket.to(c).emit("remote-status",{connected:true,count:room.remoteCount});}
  socket.emit("joined",{ok:true,code:c,as});
 });
 socket.on("command",({action,payload}={})=>{
  if(!joined||role!=="remote")return;
  const allowed=["play","pause","stop","next","previous","volume","mute","seek"];
  if(!allowed.includes(action))return;
  io.to(joined).emit("player-command",{action,payload:payload??null,at:Date.now()});
 });
 socket.on("player-state",state=>{if(joined&&role==="player")socket.to(joined).emit("player-state",state);});
 socket.on("disconnect",()=>{
  if(!joined)return;const room=rooms.get(joined);if(!room)return;
  if(role==="player"&&room.player===socket.id){room.player=null;socket.to(joined).emit("player-status",{connected:false});}
  if(role==="remote"){room.remoteCount=Math.max(0,room.remoteCount-1);socket.to(joined).emit("remote-status",{connected:room.remoteCount>0,count:room.remoteCount});}
 });
});
const port=process.env.PORT||10000;
server.listen(port,"0.0.0.0",()=>console.log("VIDEO KARAOKE Remote API listening on "+port));
