const cardReceiveId = "channel-unsupported-protocol-ws-receive-JSONMessage";
const cardSendId = "channel-unsupported-protocol-ws-send-JSONMessage";
const classNamePublishButtonLabel = "mdc-button__label";
const classNamePublishButtonText = "Publish";
const inputTextareaClassName = "pce-textarea";

const outputClassName = "websocket-output-console";

var socketWStestExt = null;

function pushCustomAlert(text, seconds = 3)
{
    let alert = document.createElement('div');
    alert.id="my-custom-alert-3fesf43f";
    alert.style = "padding: 20px;" +
        "padding: 20px;" +
        "  position: absolute;" +
        "  transform: translateY(66.66vh);" +
        "  z-index: 999;" +
        "  right: 30px;" +
        "  background-color: rgb(33, 150, 243);" +
        "  border-radius: 5px;" +
        "  color: white;" +
        "  margin-bottom: 15px;" +
        "  top: 0;";
    alert.innerText = text;
    setTimeout(function (){
        document.getElementById('my-custom-alert-3fesf43f').remove();
    }, seconds*1000);
    document.body.appendChild(alert);
}

function buildCustomPublish(htmlContainer) {
    return function (event) {
        let areas = Array.from(htmlContainer.getElementsByClassName(inputTextareaClassName));
        // last textarea of card holds payload
        if (areas.length < 1) {
            alert("Payload textarea not found (update resources/static/js/springwolf-websocket.js) ");
            return;
        }
        const textareaPayload = areas[areas.length - 1];
        const payloadText = textareaPayload.value;

        console.log("Sending: " + payloadText);
        try {
            const jsonData = JSON.parse(payloadText);

            socketWStestExt.send(JSON.stringify(jsonData));
            console.log("sent payload!");
            pushCustomAlert("Sent by websocket!");

        } catch (e) {
            console.error("Failed parsing ", e);
            pushCustomAlert("Incorrect JSON payload!");
        }
    };
}

function nativeWebsocketExtension() {

    //Load after angular UI
    if(document.getElementById(cardReceiveId) == null && document.getElementById(cardSendId) == null)
    {
        setTimeout(nativeWebsocketExtension, 300);
        return;
    }

    const wsUrl = (window.location.protocol === 'https:' ? 'wss://' : 'ws://') + window.location.host + '/ws';

    socketWStestExt = new WebSocket(wsUrl);

    socketWStestExt.onopen = function () {
        console.log("Open websocket: " + wsUrl);
    };

    socketWStestExt.onerror = function (err) {
        console.error("websocket error:", err);
        pushCustomAlert("Error sending websocket: " + err.message, 10);
    };

    socketWStestExt.onmessage = function (ev) {
        const jsonMessage = JSON.parse(ev.data);
        const outList = document.getElementsByClassName(outputClassName);
        for (const ele of outList) {
            ele.value += "["+jsonMessage.author + "] " + jsonMessage.messageType +": "+ jsonMessage.data + "\n";
        }
    }

    let possibleButtons = [];
    if(document.getElementById(cardReceiveId) != null) {
        possibleButtons.push(...Array.from(
            document.getElementById(cardReceiveId).getElementsByTagName('button')));

        const info = document.getElementById(cardReceiveId).
        getElementsByClassName('protocol-badge');
        if(info.length > 0)
        {
            info[0].innerText = "WS (JS-Extension loaded)"
        }
    }
    if(document.getElementById(cardSendId) != null) {
        possibleButtons.push(...Array.from(document.getElementById(cardSendId).getElementsByTagName('button')));

        const info = document.getElementById(cardSendId).
        getElementsByClassName('protocol-badge');
        if(info.length > 0)
        {
            info[0].innerText = "WS (JS-Extension loaded)"
        }
    }
    //console.log(possibleButtons);
    if (possibleButtons.length < 1)
    {
        pushCustomAlert("Card buttons not found in #"+cardReceiveId+" or #"+cardSendId+" (update resources/static/js/springwolf-websocket.js) ", 30);
        return;
    }

    for(const btn of possibleButtons)
    {
        let labels = btn.getElementsByClassName(classNamePublishButtonLabel);
        if(labels.length > 0)
        {
            if(labels[0].innerText.trim() == classNamePublishButtonText)
            {
                //Remove all events from button 'Publish'
                const clonedElement = btn.cloneNode(true);


                clonedElement.onclick = buildCustomPublish(btn.parentElement.parentElement.parentElement);

                const h6 = document.createElement("h6");
                h6.innerText = "Websocket Received:";
                const websocketOutput = document.createElement("textarea");
                websocketOutput.className = outputClassName;
                btn.parentElement.parentElement.appendChild(h6);
                btn.parentElement.parentElement.appendChild(websocketOutput);

                btn.parentNode.replaceChild(clonedElement, btn);
                console.log("Updated Event-Button " + classNamePublishButtonText);
            }
        }
    }

    console.log("Springwolf WebSocket-extension loaded!");
}

nativeWebsocketExtension();

