当前默认执行规范见[参数化扫描、计算与基本场图](../../comsol-pnj-ph/references/simulation-and-display.md)。下文的 G、定点评价及 FWHM 是历史分析记录，只供用户恢复定量分析后参考；基本场图必须直接使用 `ewfd.normE^2`、Rainbow。

# COMSOL 6.3 基础 PNJ：MCP 与模型 API 的已验证流程

本参考记录 2026-10-04 实际完成的一次二维圆柱 PNJ 求解，供非 GUI 建模复用。先读取本次用户参数；下面的域号和数值只对应所列几何，改几何后必须重新核对。整个流程不需要操作 GUI，但**并非纯 MCP**：当前 COMSOL MCP 不能直接设置 PML、任意材料属性、背景场和 Wavelength 研究步，也不能创建全部所需结果图。对此使用 COMSOL 6.3 自带 Java 模型 API，经 `comsolcompile` 与 `comsolbatch` 运行，再用 MCP 载入、网格、求解、评价、保存。不要把 API 批处理称作 MCP 原生功能。

1. 在 `PROJECTS_ROOT` 下新建本次任务专用文件夹，指定模型、Java 源码、批处理日志、图与数据的最终路径。若受执行环境限制，中间文件可先落在可写工作目录，完成后归集到本次文件夹并重新载入最终 `.mph` 核验。用户指定其他目录时遵从用户要求。
2. 用 `comsol_status`、`model_list` 检查 MCP。它可能是独立 COMSOL 会话，无法看到 GUI 中尚未保存的模型；需要时先保存 GUI `.mph` 再 `model_load`，或从头建立。`physics_add` 不能把显示标签/变量前缀 `ewfd` 当作物理场类型。COMSOL 6.3 实测 Java API 类型为 `ElectromagneticWavesFrequencyDomain`，标签可设 `ewfd`。当前 MCP `study_create` 不接受 `Wavelength`，须由 Java API 建立 `model.study("std1").create("wave", "Wavelength")`，并设置 `plist=lambda_ref`。
3. 本次基线：`lambda_ref=532[nm]`、`d_particle=4*lambda_ref`、`n_particle=1.5`、`n_bg=1`、`Eamp=1[V/m]`。二维圆柱位于原点；外矩形左下角为 `(-9*lambda_ref,-9*lambda_ref)`，尺寸 `28*lambda_ref × 18*lambda_ref`，四周各添加厚 `t_pml=lambda_ref/n_bg` 的 Rectangle Layer。圆半径 `d_particle/2`。构建后实测 10 域、28 边：外围 PML 域 `1,2,3,4,6,7,8,9`，中心空气域 5，圆域 10。只有域号与实际几何对应时才能使用这些选择。
4. Java API 中建立 `PML` 坐标系，仅选外围八域，`ScalingType=Cartesian`；背景材料选空气及 PML 域，`epsilon_r=n_bg^2`，粒子材料只选圆域，`epsilon_r=n_particle^2`，两者 `mu_r=1`、`sigma=0`。ewfd 的 `WaveEquationElectric` 采用 `RelativePermittivity`。组件变量设 `k_bg=2*pi*n_bg*freq/c_const` 和 `G=(abs(ewfd.Ex)^2+abs(ewfd.Ey)^2)/Eamp^2`；`BackgroundField` 设 `SolveFor=scatteredField`，`Eb={0,Eamp*exp(-i*k_bg*x),0}`。核对 +x 平均能流及无粒子对照，不能只看云图判断方向。
5. 本次**实际使用** `mesh1.autoMeshSize(3)`，对照模型使用 `autoMeshSize(2)`，MCP 的 `mesh_create` 建成网格；自动生成的网格序列含 Size、Distribution、FreeTri 和 Map。参数 `h_air=lambda_ref/10`、`h_particle=lambda_ref/(12*n_particle)` 虽在模型中定义，**本次没有绑定到网格 Size 节点**。若要严格实现这两个最大单元尺寸和 PML 厚度方向 8 层，应另行设置具体 Size/Distribution 节点并重新计算；不能从参数存在推断网格已按其执行。
6. `study_solve_async` 与 `study_wait` 可对已有波长域研究求解。`results_evaluate(["x","y","G"])` 返回网格节点数据；坐标遵循模型几何单位，本例为 μm。精确定点用 `results_global_evaluate("comp1.at2(1.16[um],0[um],G)")`。提取外部主瓣时剔除粒子与 PML；近表面离轴热点与轴向 PNJ 峰应分别描述，不能混用峰值。FWHM 需标出所用横截线、半高阈值和插值方式，采样过疏时只报估计值。
7. 本次 n=1.5 模型在轴上 `(1.16 μm,0)` 的 `G≈7.48`，该截线横向 FWHM 约 `0.37 μm`；外部近表面离轴网格节点热点约 `9.66`，不应直接当成轴向焦点。`autoMeshSize(3)` 与 `(2)` 在轴上上述点的 G 分别约 `7.476` 与 `7.481`。把 `n_particle` 改为 1 并重新求解，抽样点 `G≈1`。这只验证本次模型的背景场和局部网格敏感性，**尚未完成计算域、PML 厚度、全曲线 FWHM 的完整收敛检查**；新参数必须重新验证。
8. 保存含解 `.mph`，重载后检查 Wavelength 研究步、解及关键定点评价仍可用。当前 MCP 能调用已有导出节点导出图，但没有建立任意绘图组的通用工具；可由 Java API 添加 Surface/Export 节点后再经 MCP `results_export_image` 导出。导出图若包含 PML，不能把其颜色计入物理区域测量。将最终 `.mph`、可复用脚本、图、原始/处理后数据与简要验证记录都归入本次项目文件夹。
