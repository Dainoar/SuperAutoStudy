# SuperAutoStudy — 超星学习通智能辅助平台

[![GitHub Release](https://img.shields.io/github/v/release/DuanInnovator/SuperAutoStudy?style=flat-square)](https://github.com/DuanInnovator/SuperAutoStudy/releases)
[![GitHub Stars](https://img.shields.io/github/stars/DuanInnovator/SuperAutoStudy?style=social)](https://github.com/DuanInnovator/SuperAutoStudy)
[![GitHub Forks](https://img.shields.io/github/forks/DuanInnovator/SuperAutoStudy?style=social)](https://github.com/DuanInnovator/SuperAutoStudy/fork)
[![Issues](https://img.shields.io/github/issues/DuanInnovator/SuperAutoStudy?color=blue)](https://github.com/DuanInnovator/SuperAutoStudy/issues)
[![License](https://img.shields.io/github/license/DuanInnovator/SuperAutoStudy?color=orange)](LICENSE)

> 基于 Spring Boot + Dubbo + RabbitMQ 的分布式学习任务自动化平台，支持视频学习、章节测验等核心功能。

---

## 📌 简介

SuperAutoStudy 是一个**开源、可自部署**的学习通任务自动化工具，旨在通过模拟真实用户行为，辅助完成课程学习任务。项目采用微服务架构，支持高并发任务调度与智能答题，适用于个人部署或二次开发。

> ⚠️ **免责声明**  
> 本项目仅用于技术研究与学习交流。请严格遵守所在院校或机构的学术规范，**切勿用于违反教学纪律的行为**。开发者不对任何滥用后果承担责任。

---

## ✨ 核心特性

- **高兼容性**：基于学习通官方 API，模拟真实用户操作，规避基础检测。
- **智能答题**：集成自研 Super 题库（百万级），支持自动匹配与提交。
- **断点续学**：任务异常中断后可自动恢复进度。
- **分布式架构**：
  - 服务治理：Apache Dubbo
  - 异步调度：RabbitMQ
  - 缓存加速：Redis
- **灵活部署**：支持本地或服务器部署；请按下方配置完成数据库和运行密钥设置。

---

## 📦 功能支持

| 模块         | 状态   | 说明 |
|--------------|--------|------|
| 视频学习     | ✅ 已支持 | 自动播放、进度同步、防挂机检测 |
| 章节测验     | ✅ 已支持 | 接入 Super 题库，支持自动答题 |
| 考试系统     | 🔜 开发中 | 计划支持模拟考试与错题回放 |

---

## 🚀 快速开始

### 1. 克隆项目

```bash
git clone https://github.com/DuanInnovator/SuperAutoStudy.git
cd SuperAutoStudy
```

### 2. 部署方式

- 创建 MySQL 数据库后导入 `sql/tihai.sql`。该文件会重建表，仅应在新数据库中执行。
- 复制并填写 `src/main/resources/application-dev.yml` 中的数据库配置。
- 设置 `SUPER_AUTO_CREDENTIAL_KEY`：值必须为 16、24 或 32 字节的随机字符串。它用于加密数据库中的账号密码和 Cookie，丢失后旧数据无法解密。
- 对外部署还必须设置 `SUPER_AUTO_API_TOKEN`，并在调用时传入 `Authorization: Bearer <token>` 或 `X-API-Token: <token>`；未配置令牌时仅允许本机回环地址访问。
- 执行 `mvn package`，再运行 `java -jar target/SuperAuto-1.0-SNAPSHOT.jar`。

首次部署请先调用 `POST /chaoxing/course` 验证账号并获取课程，再调用 `POST /chaoxing` 提交任务。可用 `GET /chaoxing/tasks?loginAccount=...` 查看任务，`DELETE /chaoxing/tasks/{taskId}` 暂停尚未执行的任务。

> 数据库从旧版本升级时，请先清理重复的“账号 + 课程”任务，再执行 `sql/migrations/V2__task_integrity.sql`。

> 💡 推荐首次使用者阅读 [快速上手指南](https://doc.xxtmooc.com/pages/793dcb/)。

---

## 📚 文档与支持

- 📘 [完整使用文档](https://doc.xxtmooc.com)
- ❓ [常见问题与解决方案](https://github.com/DuanInnovator/SuperAutoStudy/issues)
- 📩 反馈邮箱：thwy@xxtmooc.com

欢迎通过以下方式参与项目：
- 提交 [Issue](https://github.com/DuanInnovator/SuperAutoStudy/issues) 报告问题
- 发起 [Pull Request](https://github.com/DuanInnovator/SuperAutoStudy/pulls) 贡献代码
- 在 [Discussions](https://github.com/DuanInnovator/SuperAutoStudy/discussions) 中提出建议

---

## 📜 许可证

本项目采用 [Apache License 2.0](LICENSE) 开源协议。

```
Copyright © 2024–2025 DuanInnovator
```

---

## ☕ 支持作者

如果本项目对您有所帮助，欢迎 Star ⭐ 或扫码支持：

<div style="display: flex; gap: 16px; margin: 16px 0;">
  <img src="https://github.com/user-attachments/assets/9028bb09-9d4d-441b-93a3-3de341bc5c14" width="128" alt="WeChat Pay">
  <img src="https://github.com/user-attachments/assets/db77485d-6345-4471-a09e-7aadd4932787" width="128" alt="Alipay">
</div>

> 合理使用，尊重教育，技术向善。
```
