// 公共导航栏用户信息显示逻辑
function fetchUserInfo() {
    fetch('/user/info')
        .then(resp => resp.json())
        .then(result => {
            if(result.status === 'SUCCESS' && result.data) {
                document.getElementById('userName').textContent = result.data.userName;
                document.getElementById('loginLink').textContent = '退出登录';
                document.getElementById('loginLink').href = '/view/login';
                // update avatar if provided (add timestamp to avoid cache)
                if (result.data.avatar) {
                    const nav = document.getElementById('navAvatar');
                    if (nav) nav.src = `/avatars/${result.data.avatar}?t=${Date.now()}`;
                }
            } else {
                document.getElementById('userName').textContent = '未登录';
                document.getElementById('loginLink').textContent = '登录';
                document.getElementById('loginLink').href = '/view/login';
            }
        });
}
// 页面加载后自动执行
if(document.getElementById('userName') && document.getElementById('loginLink')) {
    fetchUserInfo();
}
