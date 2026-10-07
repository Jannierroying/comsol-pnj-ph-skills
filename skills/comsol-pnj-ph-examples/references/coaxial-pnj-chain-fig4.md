# CHAIN-AO-001：同轴 PNJ 驻波干涉热点链

> 公开版说明：下文的模型、哈希、运行记录与用户确认描述原作者本机的历史归档；这些含解 `.mph`、论文 PDF、原始图和日志未随本仓库分发。可复用源码已提供，路径改为环境变量，本次发布只做编译检查，未重新运行仿真。新环境的计算及确认状态应另行记录。

公开源码：[PNJChainBuild.java](../assets/coaxial-pnj-chain-fig4/PNJChainBuild.java)、[PNJChainDisplay.java](../assets/coaxial-pnj-chain-fig4/PNJChainDisplay.java)。使用步骤见 [源码运行说明](../assets/README.md)。

## 确认与适用范围

- 状态：用户已确认可行。确认日期：2026-10-05；用户原话：“复现模型可行”，对应本次已交付的 Xu 2024 图 4(a) 模型。
- COMSOL 6.3；适用于两个有限长同轴介质圆柱，由相向传播的相干径向偏振光照明，在柱间形成热点链。
- 本次完成单组参数计算和基本场图。确认不等于论文所有参数、定量热点指标、光学力或完整收敛验证已经复现。

## 文献与文件

Ji Xu 等，*Modulation of super-diffraction-limited chain hotspots via standing wave interference of coaxial photonic nanojets*，Applied Optics 63(29), 7735–7740 (2024)，DOI：10.1364/AO.539726。参考图 4(a)。

- 用户源文献：用户提供的原始 PDF（未随公开版分发）。
- 项目目录：`PROJECTS_ROOT\2026-10-05_Xu2024_Coaxial_PNJ_Chain_Fig4`。下列相对路径均以此为根。
- 最终含解模型：`models\Xu2024_Fig4_PNJChain.mph`；248,391,539 字节。
- SHA256：`244269270D5CE3E8C6672CDD8F6D5D9BD87D4D64D28CD126AF2FC2C39DEE4AAE`。
- 场图：`figures\Xu2024_Fig4_FullField.png`、`figures\Xu2024_Fig4_PNJChain.png`。
- 文献副本/文字：`source\Xu2024_AO_PNJChain.pdf`、`source\paper_text.txt`。
- 设置与证据：`复现说明.md`、`workflow_state.json`、`validation\settings_and_mesh.txt`、`validation\cylinder_mesh_audit.txt`、`validation\port_direction_check.txt`、`validation\computed_solution.txt`、`validation\solve_status.json`、`validation\user_confirmation.json`。
- 脚本：`scripts\PNJChainBuild.java`、`scripts\PNJChainMeshAudit.java`、`scripts\PNJChainDisplay.java`。

## 本例参数

- 真空波长 `lambda0=532[nm]`；背景折射率 1，圆柱折射率 1.33，无损。
- 圆柱半径 `rc=6.36*lambda0=3.38352[um]`，长度 `lc=4*lambda0=2.128[um]`。
- 两柱表面间距 `gap=10*lambda0=5.32[um]`，不是中心间距。
- 轴对称坐标 `(r,z)`：左柱 `0≤r≤rc,-lc≤z≤0`；右柱 `0≤r≤rc,gap≤z≤gap+lc`。
- 光束宽度 `omega=9*lambda0=4.788[um]`，径向场轮廓 `E_profile=Abeam*r*exp(-(r/omega)^2)`。
- 两束等幅相干相向入射，`delta_phi=0[deg]`。当前只有此组解，没有参数扫描。
- 补充归一化：入射峰值电场 `Epeak=1[V/m]`，峰值位置 `r=omega/sqrt(2)`；`Abeam=sqrt(2*exp(1))*Epeak/omega`。
- 对截断端口面积积分所得单束功率 `Pbeam=1.2990929618926656e-13[W]`。这是本例为电场归一化设置的功率，不是原文实验激光功率。

## 不同于简单平面 PNJ 的设置

1. **二维轴对称矢量模型**，方位模数 `m=0`，使用 `Er/Ez`，`Ephi=0`。它表示有限圆柱的旋转对称三维场，不能替换为普通二维无限长圆柱模型。
2. 使用 `ewfd` 全场形式与**波长域**研究；两束光通过两个 UserDefined、DomainBacked 的内部 slit 端口同时激励。禁用逐端口扫描，否则两束光不会在同一解中干涉。
3. 端口切向场分量顺序为 `r,phi,z`，使用 `{E_profile,0,0}`；传播常数 `beta=2*pi*n_bg*freq/c_const`。底部端口设 `ForwardPort` 指向 `+z`，顶部端口设 `ReversePort` 指向 `-z`；端口分别位于对应外侧柱面之前一倍波长处。
4. 建模脚本以 `n_cyl=n_bg=1` 的粗网格单束空场对照实际核验传播方向。顶部默认 Forward 会主要向 PML 传播；改为 Reverse 后，物理区轴向能流与底部大小接近、符号相反。证据保存在 `port_direction_check.txt`。此对照不能替代最终 `n_cyl=1.33` 的计算。
5. 轴对称 PML 采用 **Cylindrical**，不是平面模型的 Cartesian。物理区 `Rair=2.5*omega=11.97[um]`、`Zmin=-lc-lambda0=-2.66[um]`、`Zmax=gap+lc+lambda0=7.98[um]`；PML 厚度 `lambda0`。本例 5 个 PML 域。
6. 物理区三角形网格：空气 `h_air=lambda0/45`，柱内 `h_cyl=h_air/n_cyl`，各自最小尺寸为最大尺寸的 1/8，增长率 1.15。必须核查柱内局部 Size 的最大尺寸约束实际启用。
7. 最终网格实测最大三角形边长 15.764293 nm，柱内最大边长 11.205849 nm，分别小于论文 `lambda0/30` 和 `lambda0/(30*n_cyl)`。网格共 2,802,375 个三角形、135,720 个 PML 四边形。
8. 使用一阶边元与 PARDISO 磁盘外求解，`oocmemory=2048 MB`。这些是本例资源设置；原文未明确有限元阶次，不能称一阶为论文要求。该值也不是整个进程的内存上限。

本例实际物理域 `[2,3,4]`、圆柱 `[3,4]`、PML `[1,5,6,7,8]`、底/顶端口边界 `[4]`/`[14]`。未来重建几何应重新检查命名选择，不能直接照抄编号。

## 实际计算与显示

- MCP 最终计算成功，耗时 343.803903 秒；`sol1` 尺寸 `[4479141,1]`，4,479,141 个自由度、一组解；解参数波长 `5.32e-7[m]`。
- 最终含解文件再次检查了柱内折射率 1.33、两端口同时开启、间距十倍波长和相位差 0°。
- Surface 表达式 **`ewfd.normE^2`**，颜色表 **Rainbow**，单位 `V^2/m^2`。由于入射峰值为 1 V/m，数值上等于按该峰值归一化的电场平方；它不是功率密度。
- 标量场在 `r=0` 处镜像，再用 Transformation2D 旋转 90°，使 `z` 沿水平方向展示。全场保留圆柱边界；热点局部图隐藏边缘，避免轴线盖住热点。这里只改变显示，不做拟合或数据指标提取。
- `computed_solution.txt` 中的“待用户确认”是计算完成时的历史记录；当前确认以 `user_confirmation.json` 和 `workflow_state.json` 为准。

## 验证边界与复用

- 原文图 4 色标 48.5、热点个数、表 1 强度及 FWHM 是论文结果，不得直接标为本次测量值。本次没有热点计数、峰值拟合、FWHM、光学力或完整区域/PML/阶次收敛研究。
- `PNJChainBuild.java` 会先求解粗网格空场端口对照，再建立精细目标模型；它本身不计算精细目标研究。目标解由 MCP 实际完成。
- `PNJChainMeshAudit.java` 核查网格，不启动研究；`PNJChainDisplay.java` 读取已有解、更新自己的绘图节点、导出并保存，不启动研究。
- 所有历史脚本均有本次 ROOT 或输出名称、局部绘图范围等固定设置。新任务先在 `PROJECTS_ROOT` 建独立目录，另存模型与脚本并改路径，保持本案例只读。
- 改相位差时写明确的 `deg` 单位；需要扫描才增加对应参数化扫描。改间距或圆柱尺寸需要重建几何、选择与网格；改光束/介质需重新核验端口传播方向和功率归一化。
- 可以说“参考 CHAIN-AO-001，将相位差改为指定范围并计算基本场图”；原案例确认不代表改参模型已经验证，查阅此记录不会自动启动结果分析。
