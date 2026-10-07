# COMSOL PNJ / PH Skills

用于 COMSOL Multiphysics 6.3 的中文 Agent Skills：基础 PNJ / PH 建模、文献与跨软件复现、新想法研究，以及复杂机制、光线追迹和结果分析。

建议从 `$comsol-pnj-ph` 总入口开始，说明目标、参数、执行方式及本次交付范围；它按任务读取相关子 skill。Skill 是给 AI 的工作流和参考资料，实际计算需要本机可用的 COMSOL、对应模块及 GUI / API / MCP 执行能力。

## 包含的 9 个 skill

- [comsol-pnj-ph](skills/comsol-pnj-ph/SKILL.md)：总入口，按目标组合相关模块。
- [comsol-pnj-ph-basic](skills/comsol-pnj-ph-basic/SKILL.md)：简单微粒产生基础 PNJ 或单 PH。
- [comsol-pnj-ph-reproduce](skills/comsol-pnj-ph-reproduce/SKILL.md)：按文献、已有模型或其他软件结果重建与改参。
- [comsol-pnj-ph-create](skills/comsol-pnj-ph-create/SKILL.md)：检索资料、理论论证、讨论候选方案，再试验建模。
- [comsol-pnj-ph-advanced](skills/comsol-pnj-ph-advanced/SKILL.md)：部分照明、金属、GRIN、各向异性及复杂源等机制。
- [comsol-ph-multiarm](skills/comsol-ph-multiarm/SKILL.md)：同心双椭圆核壳多臂 PH 专例；原始用户论文需自行提供。
- [comsol-pnj-ph-raytrace](skills/comsol-pnj-ph-raytrace/SKILL.md)：几何光学折射、反射及全反射路径。
- [comsol-pnj-ph-results](skills/comsol-pnj-ph-results/SKILL.md)：焦点、峰值、FWHM、长度、弯曲角及收敛核验等定量分析。
- [comsol-pnj-ph-examples](skills/comsol-pnj-ph-examples/SKILL.md)：5 个由作者用户确认可行的案例参数、证据范围及复用方法。

## 安装到 Codex

下载仓库 ZIP 并解压，或克隆本仓库。将 `skills/` 下的 **9 个文件夹一起**复制到用户级 `~/.agents/skills/`，或者目标项目的 `.agents/skills/`。Windows 用户级路径通常为 `%USERPROFILE%\.agents\skills`。保留原文件夹名及内部结构，子 skill 之间使用相对路径。

安装完成后在 Codex 的 skill 选择界面查看，或输入 `$comsol-pnj-ph`。若没有出现，重新打开会话；已有同名 skill 时先确认使用的是哪个安装位置，避免重复版本。

也可以在 Codex 中提出：“用 skill-installer 从 `Jannierroying/comsol-pnj-ph-skills` 仓库的 `skills/` 路径安装这 9 个 COMSOL skill。”安装位置及命令能力以当前运行环境为准。Skill 格式与发现规则见 [Codex 官方技能文档](https://learn.chatgpt.com/docs/build-skills)。

## 使用示例

```text
$comsol-pnj-ph
基础模式：用简单二维圆柱产生 PNJ。缺少的波长、粒径和折射率主动给初值并说明依据。
通过可用的 COMSOL MCP/API 实际建模和计算，生成 ewfd.normE^2、Rainbow 场图；暂不做定量分析。
```

```text
$comsol-pnj-ph
复现模式：读取我提供的论文，只复现指定图的一组条件。
逐项列出文献参数、推导值和补充设置，计算并保存模型及基本场图。
```

```text
$comsol-pnj-ph
创造模式：我的想法是改变核壳界面来产生双 PH。
先检索我指定的本地文献库和网络原始论文，分析可行性并和我讨论，再进入建模试验。
```

```text
$comsol-pnj-ph
参考 DE-IR-001：外壳固定60°，内核使用 range(30[deg],5[deg],100[deg]) 扫描。
尺寸参数 S 固定0.6，不扫描外壳。实际计算并生成 ewfd.normE^2、Rainbow 场图；不做其他指标分析。
```

所有波动模型优先使用波长域研究。同拓扑改参优先放入同一模型，每个独立扫描参数设置相应研究与参数化扫描；角度 range 每一项写单位。计算和基本场图是仿真交付的一部分，定量后处理按用户要求另行启动。光线追迹使用自身的 gop / RayTracing 研究，不套用波长域及电场表达式。

## 输出位置

公开版中的 `PROJECTS_ROOT` 是用户指定的项目根目录；未指定时使用当前工作区的 `projects/`。每次独立任务建新子文件夹，同任务迭代使用同一文件夹。可以在提示词中写“本次输出到 E:\codex\Projects 下的新任务目录”。

Java 案例脚本使用 `PNJPH_PROJECT_ROOT` 环境变量，指这一次任务目录，未设置会报错。构建、计算、显示必须使用同一个目录；与 skill 安装目录和项目根目录分开。源码和运行顺序见 [案例源码说明](skills/comsol-pnj-ph-examples/assets/README.md)。

## 参考案例及验证范围

参考库包含连续梯度圆柱、同轴 PNJ 热点链、梯形边缘衍射 PH、同心双椭圆内核旋转扫描、三维 Janus 微球与 LG 涡旋光。详见 [案例索引](skills/comsol-pnj-ph-examples/references/catalog.md)。公开源码提供其中 4 个案例的构建与基本显示步骤，另有双椭圆光线追迹源码。

案例“可行”表示原作者本机完成计算与基本场图后得到用户确认；不自动表示原论文全部指标已复现，也不代表全部网格与 PML 收敛已经验证。历史 `.mph` 文件、论文 PDF、原始运行日志、图及用户确认文件未随本仓库分发。案例记录中的相对交付物路径与模型哈希用于描述历史归档，不是当前下载包中已存在的文件。需要论文时使用 DOI 或用户提供的合法来源。

2026-10-07 公开版本整理了本机路径并检查 skill 格式、内部引用及 Java 编译；未为发布重新运行仿真。新环境执行、参数变体和不同求解方法应重新核验，并记录各自状态。当前仓库提供 skills 与源码，不包含 COMSOL 安装包、许可证或已配置的 MCP 服务。
