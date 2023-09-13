var stompClient = null;

// 假设你的表单中有两个输入字段，一个是发送者的名称，另一个是消息内容
var senderInput = document.getElementById('sender');
var messageInput = document.getElementById('message');
var messageForm = document.getElementById('messageForm');

messageForm.addEventListener('submit', function (event) {
    // 阻止表单的默认提交行为
    event.preventDefault();

    // 获取发送者名称和消息内容
    var sender = senderInput.value.trim();
    var message = messageInput.value.trim();

    // 验证发送者和消息内容是否为空
    if (sender === '' || message === '') {
        // 如果有任何一个字段为空，显示错误消息
        alert('发送者和消息内容不能为空！');
        return; // 阻止继续执行下面的代码
    }

});

function connect() {
    var socket = new SockJS('/chat');
    stompClient = Stomp.over(socket);

    stompClient.connect({}, function (frame) {
        console.log('Connected: ' + frame);
        stompClient.subscribe('/topic/public', function (response) {
            showMessage(JSON.parse(response.body));
        });
    });
}

function showMessage(message) {
    $("#chat").append("<p>" + message.sender + ": " + message.content + "</p>");
}

function sendMessage() {
    var message = $("#message").val();
    var sender = $("#sender").val();
    stompClient.send("/app/chat.sendMessage", {}, JSON.stringify({ sender: sender, content: message }));
    $("#message").val("");
}

$(function () {
    connect();
    $("#messageForm").on('submit', function (e) {
        e.preventDefault();
        sendMessage();
    });
});