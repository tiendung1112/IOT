const express = require("express"); 
const app = express();
const cors = require('cors');// cho phép call api từ file html 
app.use(cors())
const server = require("http").Server(app); // tạo ra server 
const io = require("socket.io")(server, {
  cors: {
    origin: '*'
  }
}); // biến server thường thành server socket 

let conversationId= 12;
io.on("connection", (socket) => { 
  socket.on("login",(userId)=>{
      console.log("có người đăng nhập",userId);
      socket.join(Number(userId)); // client joined 1 
  })
  socket.on("post",(inforpost,message, listUserId)=>{
      console.log(inforpost);
      console.log(listUserId)
      console.log(message)
      socket.to(listUserId).emit("notify_post",inforpost, message);// 2,1
  })
  socket.on("releasePost",(inforpost, message, userRelease, userSender)=>{
    console.log(inforpost);
    console.log(userSender)
    console.log(message)
    socket.to(userSender).emit("notify_post",inforpost, message);// 2,1
})
});

server.listen(3030,()=>{
  console.log(`server listening on  http://localhost:3030`)
});
