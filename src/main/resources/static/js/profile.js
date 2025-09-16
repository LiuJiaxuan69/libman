document.addEventListener('DOMContentLoaded', function() {
    const nickSaveBtn = document.getElementById('nickSaveBtn');
    const nickNameInput = document.getElementById('nickNameInput');
    const nickMsg = document.getElementById('nickMsg');

    const avatarInput = document.getElementById('avatarInput');
    const avatarSaveBtn = document.getElementById('avatarSaveBtn');
    const avatarMsg = document.getElementById('avatarMsg');
    const currentAvatar = document.getElementById('currentAvatar');

    const pwdSaveBtn = document.getElementById('pwdSaveBtn');
    const oldPwd = document.getElementById('oldPwd');
    const newPwd = document.getElementById('newPwd');
    const confirmPwd = document.getElementById('confirmPwd');
    const pwdMsg = document.getElementById('pwdMsg');

    nickSaveBtn.addEventListener('click', async () => {
        nickMsg.textContent = '';
        const nick = nickNameInput.value.trim();
        if (nick.length < 2 || nick.length > 32) { nickMsg.textContent = '昵称长度应在2到32个字符之间'; return; }
        try {
            const form = new URLSearchParams();
            form.append('nickName', nick);
            const res = await fetch('/user/nickname', { method: 'POST', body: form, headers: { 'Content-Type': 'application/x-www-form-urlencoded' }});
            const j = await res.json();
            if (j.status === 'SUCCESS') {
                nickMsg.style.color = 'green';
                nickMsg.textContent = '昵称更新成功';
                // 更新页面中的用户名显示
                const userNameSpan = document.getElementById('userName');
                if (userNameSpan) userNameSpan.textContent = nick;
                // update session-backed data if returned
                if (j.data && j.data.userName) {
                    if (userNameSpan) userNameSpan.textContent = j.data.userName;
                }
            } else {
                nickMsg.style.color = 'red';
                nickMsg.textContent = j.errorMessage || '更新失败';
            }
        } catch (e) { nickMsg.textContent = '网络错误'; }
    });

    avatarSaveBtn.addEventListener('click', async () => {
        avatarMsg.textContent = '';
        const file = avatarInput.files[0];
        if (!file) { avatarMsg.textContent = '请选择图片文件'; return; }
        if (!['image/png','image/jpeg'].includes(file.type)) { avatarMsg.textContent = '只允许 PNG 或 JPEG'; return; }
        if (file.size > 2*1024*1024) { avatarMsg.textContent = '文件过大，最大2MB'; return; }
        const fd = new FormData();
        fd.append('avatar', file);
        try {
            const res = await fetch('/user/avatar', { method: 'POST', body: fd });
            const j = await res.json();
            if (j.status === 'SUCCESS') {
                avatarMsg.style.color = 'green';
                avatarMsg.textContent = '头像上传成功';
                // 更新 navbar 和本页的头像（添加时间戳防止缓存）
                const user = j.data;
                if (user && user.avatar) {
                    const newSrc = `/avatars/${user.avatar}?t=${Date.now()}`;
                    const navAvatar = document.getElementById('navAvatar');
                    if (navAvatar) navAvatar.src = newSrc;
                    const currentAvatarEl = document.getElementById('currentAvatar');
                    if (currentAvatarEl) currentAvatarEl.src = newSrc;
                } else {
                    // fallback to a reload if response unexpected
                    setTimeout(() => location.reload(), 800);
                }
            } else {
                avatarMsg.style.color = 'red';
                avatarMsg.textContent = j.errorMessage || '上传失败';
            }
        } catch (e) { avatarMsg.textContent = '网络错误'; }
    });

    pwdSaveBtn.addEventListener('click', async () => {
        pwdMsg.textContent = '';
        const oldVal = oldPwd.value;
        const newVal = newPwd.value;
        const confirmVal = confirmPwd.value;
        if (!oldVal || !newVal || !confirmVal) { pwdMsg.textContent = '请填写所有字段'; return; }
        if (newVal !== confirmVal) { pwdMsg.textContent = '两次新密码不一致'; return; }
        if (newVal === oldVal) { pwdMsg.textContent = '新密码不能与旧密码相同'; return; }
        try {
            const form = new URLSearchParams();
            form.append('oldPassword', oldVal);
            form.append('newPassword', newVal);
            const res = await fetch('/user/password', { method: 'POST', body: form, headers: { 'Content-Type': 'application/x-www-form-urlencoded' }});
            const j = await res.json();
            if (j.status === 'SUCCESS') {
                pwdMsg.style.color = 'green';
                pwdMsg.textContent = '密码修改成功';
            } else {
                pwdMsg.style.color = 'red';
                pwdMsg.textContent = j.errorMessage || '修改失败';
            }
        } catch (e) { pwdMsg.textContent = '网络错误'; }
    });
});
