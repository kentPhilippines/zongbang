Vue.http.interceptors.push(function (request) {
    return function (response) {
        /*	if (response.body.code != 200) {
                response.ok = false;
                layer.alert(response.body.message, {
                    title : '提示',
                    icon : 7,
                    time : 3000
                });
            }*/
    };
});

/**
 * 获取url参数
 * @param name
 * @returns
 */
function getQueryString(name) {
    var reg = new RegExp('(^|&)' + name + '=([^&]*)(&|$)', 'i');
    var r = window.location.search.substr(1).match(reg);
    if (r != null)
        return unescape(r[2]);
    return null;
}

function voiceBroadcast(text) {
    var url = "http://tts.baidu.com/text2audio?lan=zh&ie=UTF-8&text=" + encodeURI(text);        // baidu文字转语音
    var audio = new Audio(url);
    audio.src = url;
    audio.play();
}

function getOrderAutoBroadcast() {
    Vue.http.get('/order/findOrder').then(function (res) {
        if (res.body.success) {
            console.log(res.body.message)
            voiceBroadcast(res.body.message);
        }
    });
}

setInterval(() => {
    this.getOrderAutoBroadcast();//开启语音订单消息通知
}, 4000);
