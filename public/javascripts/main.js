var ws = new WebSocket(feedUrl);
var results = document.getElementsByClassName("js-ssl-results")[0];
ws.onmessage = function(msg) {
    var data = JSON.parse(msg.data);
    var el = document.getElementById(data.url);
    if (!el) {
        el = document.createElement("li");
        el.className = "ssl-result--checking";
        el.id = data.url;
        var a = document.createElement("a");
        a.href = "https://" + data.url;
        var url = document.createElement("h3");
        url.className = "ssl-result__url";
        url.appendChild(document.createTextNode(data.url));
        a.appendChild(url);
        el.appendChild(a);
    } else {
        if (data.error) {
            el.className = "ssl-result--error";
            var error = document.createElement("span");
            error.className = "ssl-result__error";
            error.appendChild(document.createTextNode(data.error));
            el.appendChild(error);
        } else {
            el.className = "ssl-result--" + getExpiryStatus(data.expiry);
            var expiry = document.createElement("span");
            expiry.className = "ssl-result__expiry";
            expiry.appendChild(document.createTextNode(new Date(data.expiry).toString().replace(/[0-9]{1,2}:[0-9]{1,2}:[0-9]{1,2}.*/, "")));
            el.appendChild(expiry);
        }
    }
    results.appendChild(el);
}

var getExpiryStatus = function(expiry) {
    var month = 730.5 * 60 * 60 * 1000; // average month in ms
    var now = new Date();
    var expiry = new Date(expiry);
    var remainder = expiry - now;
    if (remainder < 0) {
        return 'expired';
    } else if (remainder < month) {
        return 'expiring-very-soon';
    } else if (remainder < month * 6) {
        return 'expiring-soon';
    } else {
        return 'ok';
    }
}
