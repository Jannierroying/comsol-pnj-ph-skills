# JANUS-LG-001：三维 Janus 微球与 LG 涡旋光 PH

> 公开版说明：下文的模型、哈希、运行记录与用户确认描述原作者本机的历史归档；这些含解 `.mph`、论文 PDF、原始图和日志未随本仓库分发。可复用源码已提供，路径改为环境变量，本次发布只做编译检查，未重新运行仿真。新环境的计算及确认状态应另行记录。

公开源码：[JanusLGBuild.java](../assets/janus-lg-vortex-fig1/JanusLGBuild.java)、[JanusLGSolve.java](../assets/janus-lg-vortex-fig1/JanusLGSolve.java)、[JanusLGDisplay.java](../assets/janus-lg-vortex-fig1/JanusLGDisplay.java)。使用步骤见 [源码运行说明](../assets/README.md)。

## 用户确认与适用范围

状态：**用户已确认可行**。日期：2026-10-05。用户原话：“可行”，紧接本次 Shi 2025 图 1 基准模型、含解 `.mph`、基本场图与复现说明的交付。

适用：三维双折射率 Janus 球在 x 线偏振 LG（p=0、l=2）涡旋光下的全波场分布与 PH 基准。确认覆盖本次 COMSOL 模型可运行与基本场图；不等同于全文复现、手性分布或弯曲角的定量一致性，也不证明网格收敛。

## 来源与实际交付物

Yiyu Shi 等，*Curved chirality distributions of optical vortices induced by photonic hooks*，Optics Letters 50(6), 1755–1758 (2025)，DOI **10.1364/OL.543787**。本次选择图 1 的基准条件，实际交付其结构、光源、求解及电场平方图；未生成原文的手性密度和 Poynting 矢量图。

项目目录：`PROJECTS_ROOT\2026-10-05_Shi2025_Janus_LG_PH_Fig1`。以下路径相对此目录，模型留在项目中，不复制到 skill。

- 含解模型：`models\Shi2025_Janus_LG_PH_Fig1.mph`，6,630,990 字节。
- SHA256：`4B848A5C8BC2516F736C3BF5B41858BC599D348353D588BE74EBFB059306EE9D`。
- 基本场图：`figures\Shi2025_Fig1_Field.png`。
- 构建、计算、显示：`scripts\JanusLGBuild.java`、`scripts\JanusLGSolve.java`、`scripts\JanusLGDisplay.java`。每个类独立定义 ROOT。
- 光源检查：`scripts\prepare_source.py`、`scripts\source_arrays.txt`；Python 验证依赖 NumPy/SciPy，Java 建模类已内嵌求积节点。
- 文献副本：`source\Shi2025_Janus_LG_PH.pdf`、全文和页面图；原始位置为 用户提供的原始 PDF（未随公开版分发）。
- 参数、解与确认：`README.md`、`workflow_state.json`、`validation\settings_and_mesh.txt`、`validation\source_quadrature_validation.json`、`validation\solve_status.txt`、`validation\computed_solution.txt`、`validation\artifact_manifest.json`、`validation\user_confirmation.json`。
- 成功日志：`logs\build_validated.log`、`logs\solve.log`、`logs\display.log`。其他尝试日志属于历史过程。

## 文献物理条件

- **真正的三维球体**，半径 `R=1.5[um]`，球心 `(0,0,2[um])`；`x=0`（yoz 平面）为两半分界，不能用普通二维圆柱或普通轴对称球模型替代。
- `x>0` 半球 `n1=1.4`，`x<0` 半球 `n2=1.55`，水背景 `n_bg=1.33`；各向同性无损，`epsilon_r=n^2`、`mu_r=1`、`sigma=0`。
- 真空波长 `lambda_ref=632.8[nm]`；Wavelength 研究，列表 `lambda_ref`，单组参数。
- LG `p=0,l=2`，束腰 `w0=1.5[um]`，束腰平面 `z=0`，束中心 `xLG=0[um]`，幅度 `Eamp=1[V/m]`，沿 +z 入射。
- x 线偏振主分量与纵向分量一起使用：背景场 `{LG_Ex,0,LG_Ez}`。原文束腰标量场为 `2*E0*(x+i*y)^2/w0^2*exp(-(x^2+y^2)/w0^2)`。

## 数值实现与实操差异

1. 原文是 FDTD/PML；本次迁移为 **COMSOL 6.3.0.290 三维全波 BEM**，接口类型 `ElectromagneticWavesBEM`。分段均匀介质可分别设置域波动方程；无界均匀水背景用辐射 Green 函数，不增加水体体网格或 PML。此实现不是相同的 FDTD 离散，不能用于连续 GRIN 等空间变化材料而不另选方法。
2. 物理接口 **tag 与变量作用域 identifier 是两项**。只以 tag `ewfd` 创建 BEM 时，其 identifier 仍可能为默认 `ebem`。本例显式调用 `physics("ewfd").identifier("ewfd")`，使原生 `ewfd.normE^2` 可用；稳定化等内部引用也核对为实际作用域。不要用自定义归一化变量冒充该表达式。
3. 外部水为无界域 0、默认第一个波动方程；两半球分别用独立 WaveEquationElectric 节点和材料。默认 wee1 的选择在本例不可编辑；不能硬改它，改用两有限域的独立节点。几何重建后核验命名域选择，不照搬本次域号。
4. 以圆柱波角谱构造整个矢量源，16 点 Gauss–Laguerre 径向求积；轴心使用正则化 Bessel 函数，避免 `1/rho^2` 的奇点。对整个复矢量场取共轭，适配 COMSOL `exp(+iωt)`，保持论文约定下的 l=2 和 +z 传播。
5. 原文近轴纵向谱投影为 `-kx/k`；本例使用 `-kx/kz` 保证各模式严格横向。这是明确的源实现差异。`k_bg=2*pi*n_bg*freq/c_const`，不是把真空波数直接当水中的波数。
6. `ell_paper` 只是记录参数，源模板固定 p=0、l=2；修改参数记录不会自动重建其他拓扑荷。大幅改波长、背景折射率或束腰时需重查求积误差、全部保留模式的传播条件；不能让错误分支导致沿 +z 增长的倏逝模式。
7. 最终边界网格 3,466 个三角形；二阶电场/一阶通量离散；`h_bnd=lambda_ref/(2.5*n2)`，实际最大边长 **0.232461717 μm**。设置值不是实际网格最大边长的证明。本例尚无网格或 BEM 压缩容差收敛证据。
8. 首次 8,602 三角形的 MCP 求解耗尽可用物理内存；当前 MCP 的取消只设置 Python 标记，未中断已进入的原生求解。该次任务后台进程经身份核验后停止，桌面 COMSOL 未关闭；最终改用原生批处理、4 线程，重算较少网格。此为当时的工具限制；未来以实际 MCP 版本和原生中断能力为准，不将结束进程当作默认流程。
9. 原生 Java 文件记录使用本次独立 `runtime_prefs`，只启用所需文件访问，未改全局设置。编译/执行前改 ROOT、输入输出路径；Build 会覆盖为未求解模型，Solve 才计算，Display 只读已有解并替换场图设置。
10. 无界 BEM 场显示使用 `Solution -> Grid3D -> CutPlane -> PlotGroup2D/Surface`。Grid3D 为 121×2×121 点，y=±0.001 μm，在 y=0 切面上显示；横轴 +z、纵轴 +x，实际数据范围 z=3–8 μm、x=−3–3 μm。两侧白色是等比例视图余量；显示网格不是仿真边界网格。

## 实际证据与确认边界

- 波长域研究运行 **646.3779378 秒**；保存并重新加载的 `sol1` 尺寸 `[55456,1]`，参数名 `lambda0`、值 `6.328e-7[m]`。
- 原生求解日志报告线性误差约 `0.00099`、残差 `6.3e-6`；这是本轮求解器记录，不能代替网格收敛验证。
- 光源求积对 48 点参考检查：分量峰值归一的最大误差约 `1.3464e-6`，束腰 Ex 对解析式误差约 `5.1951e-14`。此证据只验证本次源的求积实现，不证明与原文全部矢量近似完全一致。
- 场图原生表达式 **`ewfd.normE^2`、Rainbow、V²/m²**，已检查截面方向与显示范围。原文 I 是 Poynting 能流强度，量纲/色标不与本图直接等同。
- **未进行**手性密度、弯曲角、FWHM、长度、峰值拟合、能流线、全文参数扫描或全面收敛分析。文中的 25.6° 等属于作者结果，不是本例测量。
- `computed_solution.txt` 中“待用户验证”保留为交付当时记录；当前确认以 `workflow_state.json` 和 `user_confirmation.json` 为准。

## 参考复用

用户可说“参考 JANUS-LG-001，改变另一半球折射率并生成基本场图”。在 `PROJECTS_ROOT` 为新任务建目录，另存模型和脚本后改参，不覆盖参考案例。有限域拓扑、材料、光源模式和频率改变后重查选择、网格和源；原例可行不代表变体已确认。

连续 GRIN、各向异性、复杂背景、其他 l/p、不同偏振或真正的手性结果目标需要重新评估接口与源，而非只替换几个常量。归档或查阅本例不会自动重新计算或开启结果分析。
