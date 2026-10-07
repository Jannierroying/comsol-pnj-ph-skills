# EDGE-JPD-001：边缘衍射直角梯形 PH

> 公开版说明：下文的模型、哈希、运行记录与用户确认描述原作者本机的历史归档；这些含解 `.mph`、论文 PDF、原始图和日志未随本仓库分发。可复用源码已提供，路径改为环境变量，本次发布只做编译检查，未重新运行仿真。新环境的计算及确认状态应另行记录。

公开源码：[EdgePHBuild.java](../assets/edge-diffraction-trapezoid-fig1b/EdgePHBuild.java)、[EdgePHDisplay.java](../assets/edge-diffraction-trapezoid-fig1b/EdgePHDisplay.java)。使用步骤见 [源码运行说明](../assets/README.md)。

## 用户确认与适用范围

状态：**用户已确认可行**。日期：2026-10-05。用户原话：“模型可行”，对应已交付的 Xu 2024 图 1(b) 梯形 PH 含解模型。

这是均匀平面波照明、各向同性无损直角梯形的二维 PH 参考案例。已完成单组参数求解、设置和光源方向核验、基本场图。确认没有扩展到所有文献变体、PH 定量指标或全面收敛研究。

## 来源与交付物

Ji Xu 等，*Analysis of the impact of edge diffraction on the manipulation of photonic nanojets and photonic hooks using an energy-based model in diffraction-based structures*，J. Phys. D: Appl. Phys. 57 (2024) 295104，DOI **10.1088/1361-6463/ad4160**。基准为图 1(b)，尺寸定义见图 1(e) 和典型尺寸说明；图 2(d)、图 6(a) 使用同一基准结构。

项目根目录：`PROJECTS_ROOT\2026-10-05_Xu2024_EdgeDiffraction_PH_Fig1b`。以下路径相对此目录；大型模型保留在这里，不复制进 skill。

- 含解模型：`models\Xu2024_Fig1b_EdgeDiffraction_PH.mph`，147,813,900 字节。
- SHA256：`D42086E1074DAD8AD0BA1BDF36CA077B8187B2CA9D7AF3E7E5A4A959FC8EEC2F`。
- 场图：`figures\Xu2024_Fig1b_FullField.png`、`figures\Xu2024_Fig1b_PH.png`。
- 来源：`source\Xu2024_EdgeDiffraction.pdf`、`source\paper_text.txt`、`source\geometry_fig1.png`。
- 构建/显示脚本：`scripts\EdgePHBuild.java`、`scripts\EdgePHDisplay.java`。
- 设置/证据：`复现说明.md`、`workflow_state.json`、`validation\source_parameter_mapping.json`、`validation\settings_and_mesh.txt`、`validation\solve_status.json`、`validation\computed_solution.txt`、`validation\excitation_direction_check.txt`、`validation\user_confirmation.json`。
- 最终成功日志：`logs\build_validated.log`、`logs\display.log`。`logs\build.log` 是首次验证记录写入被 COMSOL 默认安全设置阻止的历史日志，不代表最终执行结果。

## 文献参数与几何定义

- 普通二维全波模型，沿 z 无限长且均匀，面内电场 Ex/Ey、磁场 Hz；不是二维轴对称有限柱模型。
- 真空波长 `lambda_ref=1[um]`，背景为空气 `n_bg=1`，粒子 `n_particle=1.46`，各向同性无损，`epsilon_r=n^2`、`mu_r=1`、`sigma=0`。
- `L=3*lambda_ref=3[um]`；`alpha=18.43494882292201[deg]`，即 `tan(alpha)=1/3`，斜面倾角相对竖直方向定义。
- **L 是梯形上边长度和高度**。下边是 `L+L*tan(alpha)=4[um]`，不能把 L 当最长下边，否则会改变文献几何。
- 出射面中心置原点，顶点依次为 `(-L-L*tan(alpha),-L/2)`、`(0,-L/2)`、`(0,L/2)`、`(-L,L/2)`；本例数值为 `(-4,-1.5),(0,-1.5),(0,1.5),(-3,1.5)` μm。入射斜面上端向右偏移；坐标原点是本次实现选择。
- 均匀平面波沿 +x 传播、Ey/Hz 偏振。原文归一化 `|E0|^2=1`，本例物理振幅 `Eamp=1[V/m]`；背景场 `{0,Eamp*exp(-i*k_bg*x),0}`，组件变量 `k_bg=2*pi*n_bg*freq/c_const`，求解散射场。
- 一组波长和结构，无参数化扫描，无金属遮挡或局部照明变体。

## 本例补全的数值设置与实操差异

原文给出 PML、折射率相关非均匀网格和单元小于 `lambda0/30`。计算域、PML 厚度、阶次及求解器由本次补全，不能写为作者原始设置。

1. 物理区 `x=[-6,8] μm`、`y=[-3.5,3.5] μm`，外围八域 Cartesian PML，厚 1 μm。材料、网格和 PML 使用命名选择。
2. 空气 `h_air=lambda_ref/45`，粒子 `h_particle=h_air/n_particle`，最小尺寸为各自最大尺寸的 1/8，增长率 1.15；粒子局部最大尺寸限制明确启用。
3. 自由三角形物理区、映射四边形 PML，二阶边元；PARDISO 磁盘外求解开启，`oocmemory=2048 MB` 是求解器内存策略参数，不是进程总内存上限。
4. 实测 602,386 个三角形、93,150 个 PML 四边形；最大三角形边长 **29.225498 nm**，小于 `lambda_ref/30=33.333333 nm`；粒子内 **19.403316 nm**，小于本例额外采用的 `lambda_ref/(30*n_particle)=22.831050 nm`。
5. 本例实际物理域 `[5,7]`、粒子域 `[7]`、PML 域 `[1,2,3,4,6,8,9,10]`。重建几何时重新检查命名选择，不硬套域号。
6. 建模和绘图使用 COMSOL 6.3 Java API，目标研究由 MCP 实际求解。`EdgePHBuild.java` 不计算研究；`EdgePHDisplay.java` 只重载已有解、替换绘图组、导出并保存，不重新求解。
7. COMSOL 默认 Java 文件访问设置曾阻止 PrintWriter 写验证记录；本次仅在任务专用 `runtime_prefs` 中启用文件访问，未改全局设置。未来执行脚本按实际所需权限设置，不照搬整个旧首选项目录。
8. 两个 Java 类独立定义 ROOT，分别可编译；如引入类间依赖，需核对编译类路径。执行前改 ROOT、文件名及视图范围，新任务不得覆盖参考模型。

## 实际计算、显示与确认边界

- MCP 求解成功，耗时 **215.647959 秒**。重载 `sol1` 尺寸 `[3759380,1]`，3,759,380 个自由度、一组解；解波长 `1.0e-6[m]`，波长域研究列表 `lambda_ref`。
- 入射侧 `(-5 μm,0)` 的 `ewfd.Poavx=+0.0011946443094944473[W/m^2]`，支持 +x 传播方向。这是基本光源核查，不是完整能量守恒或边界收敛证明。
- 两张 Surface 图均直接使用 **`ewfd.normE^2`、Rainbow、V²/m²**，只显示物理区。可见出射侧弯曲增强光束；不叠加中线、拟合或能流线。
- 原文图 6(a) 两臂长度等是作者数据，不能写成本次测量。本次没有拐点提取、臂长/角度、FWHM、峰值拟合或全面网格/区域/PML 收敛分析。
- `computed_solution.txt` 中“待用户验证”保留为当时的历史记录；当前状态以 `workflow_state.json` 和 `user_confirmation.json` 为准。

## 复用

用户可以说“参考 EDGE-JPD-001，改变梯形折射率并生成基本场图”。先在 `PROJECTS_ROOT` 建新任务目录，另存模型与脚本；原案例只读。更改 L 或 alpha 后重建几何、选择与网格，检查上下边定义及斜面方向；更改波长/折射率后重查介质内网格。需要扫描时遵守共享规则，每参数独立研究/扫描，角度范围每项带 `[deg]`。

此例为无损均匀照明基准。新增金属遮挡、边缘照明、入射区能量加倍或凹凸形貌时须按文献重新建模；能量加倍对应场振幅乘 `sqrt(2)`，不能将振幅直接乘 2。原案例可行不代表变体已确认；归档和查阅不会自动开启计算或结果分析。
