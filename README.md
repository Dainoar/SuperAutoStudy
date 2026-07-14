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
- **灵活部署**：提供 Docker Compose 一键部署方案（支持 ARM/x86）。

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

- **完整版（含中间件）**：参考 [部署文档](https://doc.xxtmooc.com/pages/793dcb/)
- **轻量版（仅刷课）**：使用 `lightweight` 分支或预编译 JAR（见 [Releases](https://github.com/DuanInnovator/SuperAutoStudy/releases)）

> 💡 推荐首次使用者阅读 [快速上手指南](https://doc.xxtmooc.com/pages/793dcb/)。

---

## 📚 文档与支持

- 📘 [完整使用文档](https://doc.xxtmooc.com)
- ❓ [常见问题与解决方案](https://github.com/DuanInnovator/SuperAutoStudy/issues)
- 📩 反馈邮箱：3049643162@qq.com

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
