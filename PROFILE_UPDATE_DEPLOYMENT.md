# 个人中心功能更新部署说明

## 更新内容

本次更新为个人中心页面添加了以下功能：
1. 用户头像上传和更新
2. 用户昵称编辑
3. 用户密码修改
4. 优化的个人信息展示界面

## 数据库迁移

### 1. 执行数据库迁移脚本

需要为 `user_info` 表添加两个新字段：`avatar` 和 `nick_name`

```bash
# 连接到MySQL数据库
mysql -u root -p lib_manage

# 执行迁移脚本
source docker/migration_add_user_fields.sql
```

或者手动执行以下SQL：

```sql
USE lib_manage;

-- 添加头像字段
ALTER TABLE `user_info` 
ADD COLUMN `avatar` VARCHAR(255) DEFAULT 'default.jpg' COMMENT '用户头像文件名' AFTER `password_hash`;

-- 添加昵称字段
ALTER TABLE `user_info` 
ADD COLUMN `nick_name` VARCHAR(64) DEFAULT NULL COMMENT '用户昵称' AFTER `user_name`;

-- 更新现有用户的默认头像
UPDATE `user_info` SET `avatar` = 'default.jpg' WHERE `avatar` IS NULL;
```

### 2. 创建头像存储目录

```bash
# 创建头像存储目录
mkdir -p /sources/avatars

# 设置权限（确保应用有读写权限）
chmod 755 /sources/avatars
```

### 3. 准备默认头像

将默认头像文件 `default.jpg` 放置到 `/sources/avatars/` 目录下。

## 后端更新

### 修改的文件：

1. **模型类** - `src/main/java/com/example/demo/model/UserInfo.java`
   - 添加了 `nickName` 和 `avatar` 字段

2. **Mapper** - `src/main/java/com/example/demo/mapper/UserInfoMapper.java`
   - 修改了 `updateUserNickName` 方法，更新 `nick_name` 字段

3. **配置类** - `src/main/java/com/example/demo/config/WebConfig.java`
   - 添加了 `/avatars/**` 路径映射

4. **控制器** - `src/main/java/com/example/demo/controller/UserController.java`
   - 已有 `/user/avatar`、`/user/nickname`、`/user/password` 接口

5. **服务类** - `src/main/java/com/example/demo/service/UserService.java`
   - 已有头像上传、昵称更新、密码更新的业务逻辑

## 前端更新

### 修改的文件：

1. **API接口** - `libman-frontend/src/api/user.js`
   - 添加了 `updateAvatar()`、`updateNickname()`、`updatePassword()` 方法

2. **用户Store** - `libman-frontend/src/stores/user.js`
   - 添加了 `nickName` 计算属性

3. **个人中心页面** - `libman-frontend/src/views/ProfileView.vue`
   - 完全重构，添加了头像上传、昵称编辑、密码修改功能
   - 优化了UI布局和用户体验

## 配置说明

### application.properties

确保以下配置正确：

```properties
# 头像存储目录
app.avatars.dir=/sources/avatars
```

如果使用Docker部署，需要在docker-compose.yml中挂载头像目录：

```yaml
volumes:
  - ./avatars:/sources/avatars
```

## 功能说明

### 1. 头像上传
- 支持格式：PNG、JPEG
- 文件大小限制：2MB
- 点击头像右下角的相机图标即可上传
- 上传成功后自动刷新显示

### 2. 昵称编辑
- 昵称长度：2-32个字符
- 点击"编辑"按钮进入编辑模式
- 支持保存和取消操作

### 3. 密码修改
- 需要输入旧密码验证
- 新密码最少6个字符
- 需要确认新密码

## 测试步骤

1. **数据库迁移测试**
   ```bash
   # 检查字段是否添加成功
   mysql -u root -p -e "DESCRIBE lib_manage.user_info;"
   ```

2. **后端服务测试**
   ```bash
   # 重启后端服务
   ./gradlew bootRun
   ```

3. **前端测试**
   ```bash
   cd libman-frontend
   npm run dev
   ```

4. **功能测试**
   - 访问 http://localhost:5173/profile
   - 测试头像上传功能
   - 测试昵称编辑功能
   - 测试密码修改功能

## 注意事项

1. 确保 `/sources/avatars` 目录有正确的读写权限
2. 确保数据库迁移脚本已成功执行
3. 头像文件会自动备份，成功上传后删除备份
4. 密码在前端和后端都会进行SHA256哈希处理
5. 所有操作都需要用户登录状态

## 回滚方案

如果需要回滚，执行以下SQL：

```sql
USE lib_manage;

-- 删除新增的字段
ALTER TABLE `user_info` DROP COLUMN `avatar`;
ALTER TABLE `user_info` DROP COLUMN `nick_name`;
```

## 常见问题

### Q: 头像上传失败
A: 检查以下几点：
- 文件格式是否为PNG或JPEG
- 文件大小是否超过2MB
- `/sources/avatars` 目录权限是否正确
- 后端服务是否正常运行

### Q: 昵称无法保存
A: 检查：
- 昵称长度是否在2-32个字符之间
- 数据库字段是否正确添加
- 后端服务日志是否有错误信息

### Q: 密码修改失败
A: 检查：
- 旧密码是否正确
- 新密码是否符合长度要求（最少6个字符）
- 两次输入的新密码是否一致

## 联系方式

如有问题，请联系开发团队。