function setCookie(cname,cvalue) {
  document.cookie = cname + "=" + cvalue;
}


function getCookie(cname) {
  var name = cname + "=";
  var ca = document.cookie.split(';');
  for(var i=0; i<ca.length; i++)
  {
    var c = ca[i].trim();
    if (c.indexOf(name)==0) return c.substring(name.length,c.length);
  }
  return "";
}

function uuid() {
    var s = [];
    var hexDigits = "0123456789abcdef";
    for (var i = 0; i < 36; i++) {
        s[i] = hexDigits.substr(Math.floor(Math.random() * 0x10), 1);
    }
    s[14] = "4";  // bits 12-15 of the time_hi_and_version field to 0010
    s[19] = hexDigits.substr((s[19] & 0x3) | 0x8, 1);  // bits 6-7 of the clock_seq_hi_and_reserved to 01
    s[8] = s[13] = s[18] = s[23] = "-";

    var uuid = s.join("");
    return uuid;
}

function UserAction(userId, browserName, browserCode, browserUserAgent, currentLang, screenWidth, screenHeight) {
    this.userId = userId;
    this.browserName = browserName;
    this.browserCode = browserCode;
    this.browserUserAgent = browserUserAgent;
    this.currentLang = currentLang;
    this.screenWidth = screenWidth;
    this.screenHeight = screenHeight;
}

var Ajax={
  get: function(url, fn) {
    // XMLHttpRequest对象用于在后台与服务器交换数据
    var xhr = new XMLHttpRequest();
    xhr.open('GET', url, true);
    xhr.onreadystatechange = function() {
      // readyState == 4说明请求已完成
      if (xhr.readyState == 4 && xhr.status == 200 || xhr.status == 304) {
        // 从服务器获得数据
        fn.call(this, xhr.responseText);
      }
    };
    xhr.send();
  },
  post: function (url, data) {
    var xhr = new XMLHttpRequest();
    xhr.open("POST", url, true);
    // 添加http头，发送信息至服务器时内容编码类型
    xhr.setRequestHeader("Content-Type", "application/json");
    xhr.onreadystatechange = function() {
      if (xhr.readyState == 4 && (xhr.status == 200 || xhr.status == 304)) {

      }
    };
    xhr.send(data);
  }
}

document.getElementById("add-shopping-cart").addEventListener("click", function(){
    //浏览器信息
    var browserName = navigator.appName
    var browserCode = navigator.appCodeName
    var browserUserAgent = navigator.userAgent
    var currentLang = navigator.language;   //判断除IE外其他浏览器使用语言
    if(!currentLang){//判断IE浏览器使用语言
        currentLang = navigator.browserLanguage;
    }

    //电脑屏幕信息
    var screenWidth = screen.width
    var screenHeight = screen.height

    var userId = getCookie("westar-c")
    if (!userId) {
        userId = uuid()
        setCookie("westar-c", userId)
    }

    var jsonData = JSON.stringify(new UserAction(userId, browserName,
    browserCode, browserUserAgent, currentLang, screenWidth, screenHeight))

    Ajax.post("http://127.0.0.1:8090/user/action", jsonData)
})